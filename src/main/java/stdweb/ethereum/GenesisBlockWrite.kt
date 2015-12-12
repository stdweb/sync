package stdweb.ethereum

import org.ethereum.core.Block
import org.ethereum.util.ByteUtil
import org.spongycastle.util.encoders.Hex
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

import stdweb.Core.Convert2json
import stdweb.Core.EntryResult
import stdweb.Core.EntryType
import stdweb.Core.Utils
import stdweb.Entity.LedgerBlock
import stdweb.Entity.LedgerEntry

import stdweb.Repository.LedgerBlockRepository
import stdweb.Repository.LedgerEntryRepository
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*



class GenesisBlockWrite : ReplayBlock
{

    private var blockRepo:      LedgerBlockRepository

    private var ledgRepo:       LedgerEntryRepository



    fun createGenesisBlock() :LedgerBlock {

        val b=block
        var parent : LedgerBlock? = null

        var coinbaseAccount = ledgerSync.getOrCreateLedgerAccount(b?.coinbase ?: Utils.ZERO_BYTE_ARRAY_20 )

        var block=LedgerBlock()
        with(block)
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
        return blockRepo.save(block)
    }



    //@Transactional(propagation = Propagation.NESTED)
    fun  writeGenesis()
    {
        val genesis= createGenesisBlock()
        loadGenesis(genesis)
    }

    private val entries         = ArrayList<LedgerEntry>()

    fun loadGenesis(genesis : LedgerBlock ) {

        var blockchain=ethereumBean?.blockchain
        val zeroAccount=ledgerSync.getOrCreateLedgerAccount(Utils.ZERO_BYTE_ARRAY_20)

        val snapshot = blockchain!!.repository.getSnapshotTo(Utils.hash_decode("d7f8974fb5ac78d9ac099b9ad5018bedc2ce0a72dad1827a1709da30580f0544"))

        var entryNo = 0
        val accountsKeys = snapshot.accountsKeys

        //accountsKeys.forEach {
        //    address ->

        for (address in  accountsKeys){
            with(LedgerEntry())
            {
                entryNo++

//                if (entryNo>=500)
//                    return;

                val balance = BigDecimal(snapshot.getBalance(address))

                Account = ledgerSync.getOrCreateLedgerAccount(address)
                tx = null
                offsetAccount = zeroAccount
                amount = balance
                block = genesis
                blockTimestamp = 1438269973000L
                depth = 0.toByte()
                gasUsed = 0
                entryType = EntryType.Genesis
                fee = BigDecimal.ZERO
                grossAmount = this.amount
                entryResult = EntryResult.Ok

                entries.add(this)
                ledgRepo.save(this)
                println("acc " + entryNo + ":" + Hex.toHexString(address) + " = " + Convert2json.BD2ValStr(balance, false))
             }
        }
    }

    constructor(ledgerSync : LedgerSyncService, genesis : Block,_blockRepo : LedgerBlockRepository,_ledgerRepo : LedgerEntryRepository)
    : super(ledgerSync,genesis) {

        this.blockRepo=_blockRepo
        this.ledgRepo=_ledgerRepo
    }
}