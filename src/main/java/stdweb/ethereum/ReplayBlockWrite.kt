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
    private val txs             = ArrayList<Tx>()
    private val txlogs          = ArrayList<TxLog>()
    private val txreceipts      = ArrayList<TxReceipt>()
    private val chaingedAccs    = ArrayList<LedgerAccount>()



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

        val sqlTopBlock=blockRepo.topBlock()!!


        if (!Arrays.equals(sqlTopBlock.hash,block.parentHash))
        {
            println("ledgerBlock CreateError ${block.number} parent does not match")
            return
        }

        var coinbaseAccount = ledgerSync.getOrCreateLedgerAccount(b?.coinbase ?: Utils.ZERO_BYTE_ARRAY_20,null)
        val ledgBlock =  LedgerBlock()

        with(ledgBlock)
        {
            coinbase        = coinbaseAccount
            id              = b?.number?.toInt() as Int
            hash            = b?.hash ?: ByteUtil.EMPTY_BYTE_ARRAY
            parentHash      = sqlTopBlock.hash
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
            reward          = BigDecimal(blockReward).add(BigDecimal(totalUncleReward))
            fee             = BigDecimal(blockFee)
        }

        this.ledgerBlock    = blockRepo.save(ledgBlock)
        coinbaseAccount     .firstBlock=this.ledgerBlock
        coinbaseAccount     .lastBlock=this.ledgerBlock
        coinbaseAccount     = accRepo.save(coinbaseAccount)
    }

    fun getOrCreateTx(tran: Transaction, ind : Int ) : Tx
    {
        var tx = txRepo.findByHash(tran.hash) ?: Tx(tran.hash)
        with (tx)
        {
            block               = ledgerBlock
            from                = ledgerSync.getOrCreateLedgerAccount(tran.sender,ledgerBlock)
            to                  = ledgerSync.getOrCreateLedgerAccount(tran.receiveAddress ?: tran.contractAddress,ledgerBlock)
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

        val coinbase = ledgerSync.getOrCreateLedgerAccount(block.coinbase,ledgerBlock)

        var totalBlockReward = Block.BLOCK_REWARD
        var uncleReward = BigDecimal.ZERO
        var block_fee  = BigDecimal.ZERO

        if (entries.count()>0)
            block_fee = entries
                .map    ({ x -> x.fee })
                .reduce ({ acc, z -> acc.add(z)} )

        if (block_fee  != BigDecimal.ZERO)
            with (LedgerEntry())
            {
                account             = coinbase
                account?.balance    = account?.balance?.add(block_fee)
                balance             = account?.balance ?: BigDecimal.ZERO
                account?.entrCnt    = account?.entrCnt?.plus(1)
                accentryind = account?.entrCnt ?: 0
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

                accRepo             .save(account)
                entries             .add(0,this)
            }

        // Add extra rewards based on number of uncles
        block.uncleList.forEach {
            uncle ->

            uncleReward             = getUncleReward( uncle )
            totalBlockReward        = totalBlockReward.add(Block.INCLUSION_REWARD)

            with (LedgerEntry()){
                account             = ledgerSync.getOrCreateLedgerAccount(uncle.coinbase,ledgerBlock)
                account?.balance    = account?.balance?.add(uncleReward)
                account?.entrCnt    = account?.entrCnt?.plus(1)
                accentryind = account?.entrCnt ?: 0
                balance             = account?.balance ?: BigDecimal.ZERO
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
                entries             .add(0,this)
                accRepo             .save(account)
            }}

            with (LedgerEntry()){
                account             = coinbase

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

                account?.balance    = account?.balance?.add(this.amount)
                account?.entrCnt    = account?.entrCnt?.plus(1)
                accentryind = account?.entrCnt ?: 0
                balance             = account?.balance ?: BigDecimal.ZERO

                entryResult         = EntryResult.Ok

                entries             .add(0,this)
                accRepo             .save(account)
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

        if (summary.receipt==null) {
            println("receipt null for ${Hex.toHexString(summary.transaction.hash)} , block ${block.number}")
            print ("")
        }


        saveReceipt (summary,ledg_tx)
        saveLogs    (summary,ledg_tx)
    }

    fun addTxEntries(_tx: Transaction, _gasUsed: Long, ledg_tx : Tx, isFailed: Boolean) {

        val senderAcc       = ledgerSync.getOrCreateLedgerAccount(_tx.sender,ledgerBlock)
        val recvAcc         = ledgerSync.getOrCreateLedgerAccount(_tx.contractAddress ?: _tx.receiveAddress,ledgerBlock,_tx.contractAddress!=null)
        val entryResult1    = if (isFailed) EntryResult.Failed else EntryResult.Ok

        //ledger send entry
        with (LedgerEntry()){
            tx              = ledg_tx
            account         = senderAcc

            offsetAccount   = recvAcc
            amount          = if (isFailed) BigDecimal.ZERO else Convert2json.val2BigDec(_tx.value).negate()
            entryResult     = entryResult1
            block           = ledgerBlock
            blockTimestamp  = ledgerBlock?.timestamp ?: 0
            entryType       = getSendEntryType(_tx, senderAcc, recvAcc)

            if (_tx is InternalTransaction) {
                fee         = BigDecimal.valueOf(0)
                depth       = (_tx.deep + 1).toByte()
                gasUsed     = 0
            }
            else {
                gasUsed     = _gasUsed
                fee         = ledg_tx.gasPrice.multiply(BigDecimal.valueOf(gasUsed))
            }
            grossAmount     = amount.subtract(fee)
            account?.balance= account?.balance?.add(grossAmount)
            account?.entrCnt= account?.entrCnt?.plus(1)
            accentryind = account?.entrCnt ?: 0
            balance         = account?.balance ?: BigDecimal.ZERO

            accRepo         .save   (account)
            entries         .add    (this)
        }

        //ledgerEntryRecv
        with (LedgerEntry()){
            tx              = ledg_tx
            offsetAccount   = senderAcc
            account         = recvAcc
            amount          = if (isFailed) BigDecimal.ZERO else Convert2json.val2BigDec(_tx.value)
            entryResult     = entryResult1
            block           = ledgerBlock
            blockTimestamp  = ledgerBlock?.timestamp ?: 0
            gasUsed         = 0
            entryType       = getRecvEntryType(_tx, recvAcc )
            fee             = BigDecimal.valueOf(0)

            grossAmount     = amount

            account?.balance= account?.balance?.add(grossAmount)
            account?.entrCnt= account?.entrCnt?.plus(1)
            accentryind = account?.entrCnt ?: 0
            balance         = account?.balance ?: BigDecimal.ZERO

            if (_tx is InternalTransaction)
                depth       = (_tx.deep + 1).toByte()

            accRepo         .save   (account)
            entries         .add    (this)
        }
    }


    fun checkParent() : Boolean
    {
        val sqltopHash=blockRepo.topBlock()!!.hash

        val sqlTop = blockRepo.topBlock()
        val foundParent =blockRepo.findByHash(block.parentHash)

        if (foundParent!=null)
            if (foundParent.id ==block.number.toInt()-1)
            {
                if (Arrays.equals(foundParent.hash,block.parentHash))
                    return true
                else
                {
                    println("parent hash does not match")
                    return false
                }
            }
            else
            {
                println ("new block is ${block.number}, but parent is ${foundParent?.id}")
                return false
            }
        else
        {
            println("parent for new block ${block.number} not found in sql ledg")
            return false
        }
    }

    fun printWriteStatus(msg : String)
    {
        val sqltopHash=blockRepo.topBlock()!!.hash
        println ("${msg} : ${block.number}     <-- hash ${Hex.toHexString(block.hash).substring(0,10)} " +
                "parent ${Hex.toHexString(block.parentHash).substring(0,10)} " +
                "${if (Hex.toHexString(block.parentHash).equals(Hex.toHexString(sqltopHash))) " Match" else " NotMatch"}" )

//        println("sqltop hash ${Hex.toHexString(sqltopHash)} - newblock parent ${Hex.toHexString(block.parentHash)} " +
//                "eq: ${Arrays.equals(block.parentHash,sqltopHash)}")
//        println("")
    }

    fun write()  {

        //connectBlock()
//        if (!checkParent())
//        {
//            printWriteStatus("wrong parent, skipping")
//            return
//        }

        val b = blockRepo.findOne(this.getBlock().getNumber().toInt())

        if (b!=null) {
            printWriteStatus("bskip")
            return;
            //blockRepo.deleteBlockWithEntries(b)
        }
        printWriteStatus("bsaved")

        createLedgerBlock()

        summaries.     forEach {       addTxEntries ( it ) }
        if (this.block.number != 0L)   addRewardEntries()

        entries.       forEach {       ledgRepo.save( it ) }

        //todo: use logger

    }

    private fun connectBlock() {

        val parentHash=block.parentHash
        val sqlTopBlock=blockRepo.topBlock()!!
        if (parentHash != (sqlTopBlock.hash)) {
            println("skip block ${block.number}, hash ${block.hash} " +
                    "parentHash ${Hex.toHexString(parentHash)}")
        }

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
                address     = ledgerSync.getOrCreateLedgerAccount(it.address,ledgerBlock)
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

        zeroAccount=ledgerSync.getOrCreateLedgerAccount(Utils.ZERO_BYTE_ARRAY_20,ledgerBlock)
    }
}