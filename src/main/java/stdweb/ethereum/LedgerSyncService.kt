package stdweb.ethereum


import org.ethereum.core.Account
import org.ethereum.core.Block
import org.ethereum.core.TransactionExecutionSummary
import org.ethereum.db.RepositoryImpl

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import org.springframework.transaction.annotation.Transactional
import org.springframework.util.Assert
import stdweb.Core.*
import stdweb.Entity.LedgerAccount

import stdweb.Repository.*
import java.math.BigDecimal
import java.sql.SQLException
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import javax.annotation.PostConstruct


@Service
open class LedgerSyncService
{
    open var syncStatus : SyncStatus = SyncStatus.stopped
    open var nextStatus : SyncStatus = SyncStatus.stopped

    @Autowired open var blockRepo        : LedgerBlockRepository? = null
    @Autowired open var accRepo          : LedgerAccountRepository? = null
    @Autowired open var ledgerRepo       : LedgerEntryRepository? = null
    @Autowired open var txRepo           : LedgerTxRepository? = null
    @Autowired open var logRepo          : LedgerTxLogRepository? = null
    @Autowired open var receiptRepo      : LedgerTxReceiptRepository? = null
    @Autowired open var ethereumBean     : EthereumBean? = null
    public     open var listener         : EthereumListener?=null

    open val lock : ReentrantLock = ReentrantLock()

    //todo: LedgerSyncServ use logger instead of print

    fun getOrCreateLedgerAccount( addr : ByteArray) : LedgerAccount
    {

        var account = accRepo!!.findByAddress(addr) ?: LedgerAccount(addr)

        if (account.id==-1) {
            //account.isContract = isContract(account.address)
            //account = fillLedgerAccount(account)
            account = accRepo!!.save(account)
        }
        else
        {
            //todo: upd balance and nonce
            //account = fillLedgerAccount(account)
            //account = accRepo!!.save(account)
        }

        return account
    }

    fun fillLedgerAccount(ledgerAccount: LedgerAccount): LedgerAccount {
        val acc = Account()
        acc.address = ledgerAccount.address

        val repository = ethereumBean?.ethereum?.repository as RepositoryImpl
        val nonce = repository.getNonce(ledgerAccount.address)
        ledgerAccount.nonce = nonce.toLong()

        val balance = repository.getBalance(ledgerAccount.address)
        ledgerAccount.balance = BigDecimal(balance)
        repository.flushNoReconnect()
        // todo: ledgerAccount.name= ... get addres name from blockchain

        return ledgerAccount
    }

    fun isContract(addr: ByteArray): Boolean {

        val repository = ethereumBean?.ethereum?.repository as RepositoryImpl
        val contractDetails = repository.getContractDetails(addr) ?: return false
        contractDetails.code ?: return false

        repository.flushNoReconnect()
        //if (contractDetails.code == null)
        //    return false

        return (contractDetails.code.size != 0)
    }

    private var nextSyncBlock: Int =-1

    fun ledgerBulkLoad(_block: Int) {

        println("Ledger Start BulkLoad from :" + _block)
        syncStatus = SyncStatus.bulkLoading

        this.nextSyncBlock = _block
        blockRepo?.deleteBlockWithEntriesFrom(_block)

        if ( ! (syncLedgerThread?.isAlive ?: false)) {
            val thread=createSyncLedgerThread()
            thread.start()
            syncLedgerThread=thread
        }
    }

    fun ledgerBulkLoad() {

        ledgerBulkLoad(blockRepo?.topBlock()?.id ?: Int.MAX_VALUE )
    }

    private var syncLedgerThread: Thread? = null


    fun replayAndSaveBlock(blockNumber: Int) {

        //todo: LedgerSyncService. autowire in ctor
        if (ethereumBean?.blockchain?.bestBlock?.number ?:0 < blockNumber) {
            println("cannot insert block.Top block is: " + ethereumBean?.blockchain?.bestBlock?.number)
            return
        }

        val block = ethereumBean?.blockchain?.getBlockByNumber(blockNumber.toLong())

        Assert.isTrue(block != null, "blockBy number is null:" + blockNumber)
        //todo: throw exception here if null
        if (block == null)
            return

        val replayBlock = ReplayBlockWrite(
                this,block,
                blockRepo !!,
                accRepo !!,
                ledgerRepo!!,
                txRepo!!,
                logRepo!!,
                receiptRepo!!
        )
        saveBlockData(block,replayBlock.summaries)
    }

    @Synchronized @Throws(SQLException::class, InterruptedException::class)
    fun createSyncLedgerThread() : Thread {

        println("create Ledger BulkLoadThread")
        ethereumBean?.blockchainStopSync()

        return  Thread {
            while (syncStatus == SyncStatus.bulkLoading) {
                if (nextSyncBlock <= ethereumBean?.blockchain?.bestBlock?.number?.toInt() ?: 0)
                {
                    replayAndSaveBlock(nextSyncBlock)
                    nextSyncBlock++
                    }
                else {
                    syncStatus = nextStatus
                    println("finished bulkloading . Block:" + ethereumBean?.blockchain?.bestBlock?.number)
                    println("Snc status set to: " + syncStatus)
                    break
                }
            }
        }
    }

    fun stopSync() {
        println("stop BulkLoad")
        syncStatus = SyncStatus.stopped
        nextSyncBlock = 1000000000
    }

    @Transactional open fun saveBlockData(block : Block, summaries : List<TransactionExecutionSummary>)
    {
        try {
            lock.lock()
            val replayBlock = ReplayBlockWrite(
                    this, block,
                    blockRepo!!,
                    accRepo!!,
                    ledgerRepo!!,
                    txRepo!!,
                    logRepo!!,
                    receiptRepo!!
            )

            when (syncStatus) {
                SyncStatus.onBlockSync -> {
                    replayBlock.summaries=summaries
                    replayBlock.write ()
                }
                SyncStatus.bulkLoading,
                SyncStatus.SingleInsert -> {
                    replayBlock.run()
                    replayBlock.write()
                }
            }


        }
        catch ( e : KotlinNullPointerException)
        {
            throw NullPointerException("at least one repo is null")
        }
        finally
        {
            lock.unlock()
        }
    }

    @Transactional open fun start() {
            try{
                lock.lock()
                if (blockRepo!!.findOne(0)==null)
                    ensureGenesis()
            }
            finally {
                lock.unlock()
            }

        //syncStatus= SyncStatus.onBlockSync
        //ethereumBean?.blockchainStartSync()

        println ("LedgSyncService started")
    }

    private fun ensureGenesis() {
        if (blockRepo!!.findOne(0)==null)
        {
            println("ensure genesis")
            val block0=ethereumBean!!.blockchain.getBlockByNumber(0)
            var genesis=GenesisBlockWrite(this,block0,blockRepo!!,ledgerRepo!!)
            genesis.writeGenesis()
        }
    }

    fun status(): Map<String,String> {

        val map                     = HashMap<String, String>()
        val sqlTopBlock             = blockRepo?.topBlock()?.id
        val blockChainSyncStatus    = ethereumBean?.blockchainSyncStatus
        val ledgSyncStatus          = syncStatus

        var topBlock=ethereumBean?.blockchain?.bestBlock?.number
        map.put("SqlTopBlock",sqlTopBlock.toString())
        map.put("TopBlock",topBlock.toString())
        map.put("BlockchainSyncStatus",blockChainSyncStatus.toString())
        map.put("LedgSyncStatus",ledgSyncStatus.toString())

        return map
    }

    @PostConstruct
    private fun initService()
    {
        println("LedgerSync initService:"+this.hashCode())
    }

    constructor()
    {}
}