package stdweb.ethereum


import org.ethereum.core.*
import org.ethereum.util.ByteUtil
import org.ethereum.vm.program.InternalTransaction
import org.spongycastle.util.encoders.Hex
import org.springframework.transaction.annotation.Transactional
import stdweb.Core.*
import stdweb.Entity.*
import stdweb.Repository.*
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

class ReplayBlockWrite : ReplayBlock
{

    private val entries         = ArrayList<LedgerEntry>()


    private var blockRepo:      LedgerBlockRepository
    private var accRepo:        LedgerAccountRepository
    private var ledgRepo:       LedgerEntryRepository
    private var txRepo:         LedgerTxRepository

    private var logRepo:        LedgerTxLogRepository
    private var receiptRepo:    LedgerTxReceiptRepository



    private fun getSendEntryType(tx: Transaction, sendAcc: LedgerAccount, recvAcc: LedgerAccount): EntryType {

        if (tx.isContractCreation)
            return EntryType.ContractCreation
        if (recvAcc.isContract)
            return EntryType.Call
        else
            return EntryType.Send
    }

    private fun getRecvEntryType(tx: Transaction, account: LedgerAccount): EntryType {

        if (tx.isContractCreation)
            return EntryType.ContractCreated

        if (account.isContract)
            return EntryType.CallReceive
        else
            return EntryType.Receive
    }

    var ledgerBlock : LedgerBlock? = null

    private val zeroAccount : LedgerAccount

    fun createLedgerBlock() {

        val b=block
        var parent : LedgerBlock? = null

        var coinbaseAccount = ledgerSync.getOrCreateLedgerAccount(b?.coinbase ?: Utils.ZERO_BYTE_ARRAY_20 )
        val ledgBlock = LedgerBlock()

        with(ledgBlock)
        {
            coinbase        = coinbaseAccount
            id              = b?.number?.toInt() as Int
            hash            = b?.hash ?: ByteUtil.EMPTY_BYTE_ARRAY
            parentHash      = parent?.hash ?: ByteUtil.EMPTY_BYTE_ARRAY
            timestamp       = b?.timestamp ?: 0
            difficulty      = b?.difficultyBI?.toLong() ?: 0
            gasLimit        = BigInteger(1, b?.gasLimit).toLong()
            gasUsed         = b?.gasUsed ?: 0
            txCount         = b?.transactionsList?.size ?: 0
            unclesCount     = b?.uncleList?.size ?: 0
            blockSize       = b?.encoded?.size ?: 0
            receiptTrieRoot = b?.receiptsRoot ?: ByteUtil.EMPTY_BYTE_ARRAY
            stateRoot       = b?.stateRoot ?: ByteUtil.EMPTY_BYTE_ARRAY
            txTrieRoot      = b?.txTrieRoot ?: ByteUtil.EMPTY_BYTE_ARRAY
            reward          = BigDecimal(blockReward)
            fee             = BigDecimal(blockFee)
        }
        this.ledgerBlock = blockRepo.save(ledgBlock)
    }

    fun getOrCreateTx(tran: Transaction, ind : Int ) : Tx
    {
        var tx = txRepo.findByHash(tran.hash) ?: Tx(tran.hash)
        with (tx)
        {
            block=ledgerBlock
            from                = ledgerSync.getOrCreateLedgerAccount(tran.sender)
            to                  = ledgerSync.getOrCreateLedgerAccount(tran.receiveAddress ?: tran.contractAddress)
            value               = BigDecimal(BigInteger(1, tran.value))
            gas                 = BigInteger(1,tran.gasLimit).toLong()
            gasPrice            = BigDecimal(BigInteger(1,tran.gasPrice))
            contractCreate      = tran.isContractCreation
            fee                 = tx.gasPrice.multiply(BigDecimal.valueOf(tx.gas))
            nonce               = BigInteger(1,tran.nonce).toLong()
            extradata           = tran.data
            txindex             = ind
            //println ("create ledg_tx:"+tx)
        }
        return txRepo.save(tx)
    }



    fun addRewardEntries() {

        val coinbase = ledgerSync.getOrCreateLedgerAccount(block.coinbase)

        var totalBlockReward = Block.BLOCK_REWARD
        var uncleReward = BigDecimal.ZERO

        // Add extra rewards based on number of uncles
        block.uncleList.forEach ({
            uncle ->

            uncleReward             = getUncleReward( uncle )
            totalBlockReward        = totalBlockReward.add(Block.INCLUSION_REWARD)

            with (LedgerEntry())
            {
                Account             = ledgerSync.getOrCreateLedgerAccount(uncle.coinbase)
                tx                  = null
                offsetAccount       = zeroAccount
                amount              = uncleReward
                block               = ledgerBlock
                blockTimestamp      = ledgerBlock?.timestamp ?: 0
                depth               = 0.toByte()
                gasUsed             = 0
                entryType           = EntryType.UncleReward
                fee                 = BigDecimal.ZERO
                grossAmount         = uncleReward
                entryResult         = EntryResult.Ok

                entries             .add(this)
            }})

            with (LedgerEntry())
            {
                Account             = coinbase
                tx                  = null
                offsetAccount       = zeroAccount
                amount              = BigDecimal(totalBlockReward)
                block               = ledgerBlock
                blockTimestamp      = ledgerBlock?.timestamp ?: 0
                depth               = 0.toByte()
                gasUsed             = 0
                entryType           = EntryType.CoinbaseReward
                fee                 = BigDecimal.ZERO
                grossAmount         = this.amount
                entryResult         = EntryResult.Ok

                entries             .add(this)
            }

        val block_fee = entries.map({ x -> x.fee }).reduce( { acc, z -> acc.add(z)} )
        if (block_fee  != BigDecimal.ZERO)
            with (LedgerEntry())
            {
                Account             = coinbase
                tx                  = null
                offsetAccount       = zeroAccount
                amount              = block_fee
                block               = ledgerBlock
                blockTimestamp      = ledgerBlock?.timestamp ?: 0
                depth               = 0.toByte()
                gasUsed             = 0
                entryType           = EntryType.FeeReward
                fee                 = BigDecimal.ZERO
                grossAmount         = amount
                entryResult         = EntryResult.Ok

                entries.add(this)
            }
    }

    fun addTxEntries(summary: TransactionExecutionSummary ) {

        val ledg_tx= getOrCreateTx(summary.transaction,summary.entryNumber )
        val calcGasUsed = summary.gasLimit.subtract(summary.gasLeftover.add(summary.gasRefund)).toLong()

        addTxEntries(summary.transaction, calcGasUsed, ledg_tx, summary.isFailed)

        summary.internalTransactions.forEach { t ->
            addTxEntries(t, 0,
                    ledg_tx, t.isRejected)
        }

        saveReceipt(summary,ledg_tx)
        saveLogs(summary,ledg_tx)
    }

    fun addTxEntries(_tx: Transaction, _gasUsed: Long, ledg_tx : Tx, isFailed: Boolean) {

        val senderAcc       = ledgerSync.getOrCreateLedgerAccount(_tx.sender)
        val recvAcc         = ledgerSync.getOrCreateLedgerAccount(_tx.contractAddress ?: _tx.receiveAddress)
        val entryResult1    = if (isFailed) EntryResult.Failed else EntryResult.Ok

        with (LedgerEntry())//ledger send entry
        {
            tx              = ledg_tx
            Account         = senderAcc
            offsetAccount   = recvAcc
            amount          = Convert2json.val2BigDec(_tx.value).negate()
            entryResult     = entryResult1
            block           = ledgerBlock
            blockTimestamp  = ledgerBlock?.timestamp ?: 0
            entryType       = getSendEntryType(_tx, senderAcc, recvAcc)
            grossAmount     = amount.subtract(fee)

            if (_tx is InternalTransaction) {
                fee         = BigDecimal.valueOf(0)
                depth       = (_tx.deep + 1).toByte()
                gasUsed     = 0
            }
            else {
                fee         = BigDecimal((BigInteger(1, _tx.gasPrice)).multiply(BigInteger.valueOf(gasUsed)))
                gasUsed     = _gasUsed
            }
            entries.add(this)
        }

        with (LedgerEntry())//ledgerEntryRecv
        {
            tx              = ledg_tx
            offsetAccount   = senderAcc
            Account         = recvAcc
            amount          = Convert2json.val2BigDec(_tx.value)
            entryResult     = entryResult1
            block           = ledgerBlock
            blockTimestamp  = ledgerBlock?.timestamp ?: 0
            gasUsed         = 0
            entryType       = getRecvEntryType(_tx, recvAcc )
            fee             = BigDecimal.valueOf(0)

            grossAmount     = amount
            if (_tx is InternalTransaction)
                depth       = (_tx.deep + 1).toByte()
            entries.add(this)
        }
    }

    fun write()  {

        val b = blockRepo.findOne(this.getBlock().getNumber().toInt())
        if (b!=null)
            blockRepo.deleteBlockWithEntries(b)

        createLedgerBlock()

        if (this.block.number != 0L)    addRewardEntries()

        summaries.     forEach {       addTxEntries ( it ) }
        entries.       forEach {       ledgRepo.save( it ) }
        //todo: use logger
        println ("block saved:" + block.number)
    }

    private fun saveReceipt(summary: TransactionExecutionSummary, _tx : Tx) {

        val r=summary.receipt
        with (TxReceipt())
        {
            id              = _tx.id
            block           = ledgerBlock
            tx              = _tx
            postTxState     = r.postTxState
            cumulativeGas   = r.cumulativeGas
            bloomFilter     = r.bloomFilter.data

            receiptRepo.save(this)
        }
    }

    private fun saveLogs(summary: TransactionExecutionSummary,_tx : Tx) {

        var _ind=0
        summary .receipt .logInfoList .forEach {
            with (TxLog())
            {
                tx          = _tx
                block       = ledgerBlock
                ind         = _ind ++
                address     = ledgerSync.getOrCreateLedgerAccount(it.address)
                data        = it.data

                val q = it.topics.size
                if ( q >= 1 ) topic1 = it.topics[0].data
                if ( q >= 2 ) topic2 = it.topics[1].data
                if ( q >= 3 ) topic3 = it.topics[2].data
                if ( q >= 4 ) topic4 = it.topics[3].data

                logRepo.save(this)
            }
        }
    }

    fun printEntries() {
        for (entry in entries) {
            println(entry)
        }
    }

    constructor(ledgerSync: LedgerSyncService, _block: Block,
                _blockRepo : LedgerBlockRepository,
                _accRepo : LedgerAccountRepository,
                _ledgRepo : LedgerEntryRepository,
                _txrepo : LedgerTxRepository,

                _logRepo : LedgerTxLogRepository,
                _receiptRepo : LedgerTxReceiptRepository
                ) :super (ledgerSync,_block)
    {

        this.blockRepo      = _blockRepo
        this.accRepo        = _accRepo
        this.ledgRepo       =_ledgRepo
        this.txRepo         =_txrepo
        this.logRepo        =_logRepo
        this.receiptRepo    =_receiptRepo

        zeroAccount=ledgerSync.getOrCreateLedgerAccount(Utils.ZERO_BYTE_ARRAY_20)
    }


}