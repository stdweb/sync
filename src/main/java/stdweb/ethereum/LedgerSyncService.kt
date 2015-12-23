package stdweb.ethereum


import org.ethereum.core.Account
import org.ethereum.core.Block
import org.ethereum.core.TransactionExecutionSummary
import org.ethereum.db.ByteArrayWrapper
import org.ethereum.db.RepositoryImpl
import org.spongycastle.util.encoders.Hex

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import org.springframework.transaction.annotation.Transactional
import org.springframework.util.Assert
import stdweb.Core.*
import stdweb.Entity.AmountEntity
import stdweb.Entity.LedgerAccount
import stdweb.Entity.LedgerBlock

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

    var q : HashMap<Sha3Hash,ReplayBlockWrite> = HashMap()
    var chain : LinkedList<ReplayBlockWrite> = LinkedList()

    open val lock : ReentrantLock = ReentrantLock()

    //todo: LedgerSyncServ use logger instead of print

    fun getOrCreateLedgerAccount(addr: ByteArray, block : LedgerBlock? ,isContract : Boolean = false) : LedgerAccount
    {

        var account = accRepo!!.findByAddress(addr) ?: LedgerAccount(addr)

        if (account.id==-1) {
            //account.firstBlock  = genesis
            account.isContract  = isContract//isContract(account.address)

            account.firstBlock  = block
            account.lastBlock   = block
            //account = fillLedgerAccount(account)
            account = accRepo!!.save(account)
            //println ("created acc: ${account.id} : ${account.toString()} :isContract ${account.isContract}" )
        }
        else
        {
            account.lastBlock =  block
            account = accRepo!!.save(account)
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



    private var nextSyncBlock: Int =-1

    fun ledgerBulkLoad1() {

        //ledgerBulkLoad(blockRepo?.topBlock()?.id ?: Int.MAX_VALUE )
        val bestBlock   =ethereumBean!! .blockchain.bestBlock.number
        val sqlTop      =blockRepo!!    .topBlock()!!.id

        if (bestBlock>sqlTop)
        for (i in sqlTop+1 .. bestBlock)
        {
            val block=ethereumBean!! .blockchain.getBlockByNumber(i)

            val replayBlock = ReplayBlockWrite(
                    this,block,
                    blockRepo !!,
                    accRepo !!,
                    ledgerRepo!!,
                    txRepo!!,
                    logRepo!!,
                    receiptRepo!!
            )
            replayBlock.run()
            replayBlock.write()
            println ("bulk write ${block.number}")
        }
    }

    private fun enqueue(replayBlock: ReplayBlockWrite) {
        //already stored block
        print("enq, block ${replayBlock.block.number} --> ")
        if (blockRepo!!.findByHash(replayBlock.block.hash)!=null) {
            println ("block ${replayBlock.block.number} already stored")
            return;
        }
        val blockchain=ethereumBean!!.blockchain
        val sqlTopBlock=blockRepo!!.topBlock()!!

        if (replayBlock.block.number>=sqlTopBlock.id+5){
            println("enq >=+5 , getting block ${ sqlTopBlock.id.toLong()+1 } from blochain")
            val block2add=blockchain.getBlockByNumber(sqlTopBlock.id.toLong()+1)
            println ("got block ${sqlTopBlock.id.toLong()+1}")

            var r=q.get(Sha3Hash(block2add.hash))
            if (r==null){
                print ("replay ")
                r= ReplayBlockWrite(this,block2add,blockRepo!!,accRepo!!,ledgerRepo!!,txRepo!!,logRepo!!,receiptRepo!!)
                r.run()

            }
            if (r?.isChildOf(sqlTopBlock.hash) ?: false) {
                r?.write()
                q.remove(Sha3Hash(block2add.hash))
                println ("write and remove from q: ${r?.block?.number} , q size : ${q.size}")
            }
            else{
                println ("replayblock  ${r.block.number} : ${Hex.toHexString(r.block.hash)} " +
                        "parent: ${Hex.toHexString(r.block.parentHash)} \n" +
                        "not child of sql top ${sqlTopBlock.id} hash : ${sqlTopBlock.hash_str} ")
            }
        }

        println ("block ${replayBlock.block.number} added to queue, q size : ${q.size}")
        q.putIfAbsent(Sha3Hash(replayBlock.block.hash), replayBlock)

        //clear old blocks in queue
        //println("before clear old blocks inq , size ${q.size}")
        q
                .filter     { it.value.block.number<sqlTopBlock.id }
                .forEach    { q.remove( Sha3Hash(it.value.block.hash)) }
//        println("after clear old blocks inq , size ${q.size}")
//        println("")

        //Thread.sleep(2000)
        println("<-- finish q , block ${replayBlock.block.number}")
        println("_________________________________________________________")

    }

    fun deleteTopBlockData() {
        val topblock=blockRepo?.topBlockId()
        deleteBlockData(topblock ?: -1)
    }
    @Transactional open fun deleteBlockData(id: Int)
    {
        accRepo?.updAccFirstBlock2null(id)
        accRepo?.updAccLastBlock2null(id)

        blockRepo?.deleteBlockWithEntries(id)
    }

    private fun rebranchSqlDb(newBlock: Block) {
           //block not exists, parent exists, blockDiff<1
        val bestBlock   =ethereumBean!! .blockchain.bestBlock.number
        val sqlTop      =blockRepo!!    .topBlock()!!.id


        //val block=ethereumBean!!.blockchain.getBlockByHash()


    }

    fun tmpWrite(block : Block,summaries: List<TransactionExecutionSummary>)
    {
        val replayBlock = ReplayBlockWrite(
                this,block,
                blockRepo !!,
                accRepo !!,
                ledgerRepo!!,
                txRepo!!,
                logRepo!!,
                receiptRepo!!
        )

        replayBlock.summaries=summaries

        try {
            replayBlock.write()
        }
        catch( e : RuntimeException)
        {
            println ("cannot write block ${block.number}")
            println(block.toString())
            throw e
        }


        print ("  tmp write ${block.number}  ")

    }


    @Transactional open fun saveBlockData(newBlock : Block, summaries : List<TransactionExecutionSummary>)
    {
        //tmpWrite(newBlock,summaries)
        //return

        try {
            lock.lock()

            //val newBlockHash        =block.hash
            //val newBlockParentHash  =block.parentHash

            val sqlTop=blockRepo!!.topBlock()!!

            val blockExists  = blockRepo!!.findByHash(newBlock.hash) !=null
            val parentExists = blockRepo!!.findByHash(newBlock.parentHash) !=null

            val blockDiff = newBlock.number.toInt()-sqlTop.id

            if (blockExists){
                //exists in sql -- skipping
                println ("bskip ${newBlock.number} : ${Hex.toHexString(newBlock.hash)}")
                return
            }else{
                if (parentExists)//block not exists in sql , parent exists
                    when {
                        blockDiff == 1 ->
                        if (Arrays.equals(newBlock.parentHash, sqlTop.hash)){
                            println ("b:${newBlock.number} blockExists ${blockExists}, parentExist ${parentExists}, blockDiff ${blockDiff} ")
                            //normal blockchain sync loading
                            val replayBlock = ReplayBlockWrite(this, newBlock,blockRepo!!,accRepo!!,ledgerRepo!!,txRepo!!,logRepo!!,receiptRepo!!)
                            replayBlock.summaries = summaries
                            println ("before write")
                            replayBlock.write()
                            println ("after write")
                            return
                        }
                        blockDiff < 1 -> {
                            println ("b:${newBlock.number} hash ${Hex.toHexString(newBlock.hash).substring(0,10)} blockExists ${blockExists}, parentExist ${parentExists}," +
                                    " blockDiff ${blockDiff} , rebranch")
                            rebranchSqlDb(newBlock)
                        }// need rebranch
                        blockDiff > 1 -> {
                            println ("b:${newBlock.number} hash ${Hex.toHexString(newBlock.hash).substring(0,10)}  blockExists ${blockExists}, parentExist ${parentExists}, blockDiff ${blockDiff} ")
                        }// never?
                        else          -> {
                            println ("ELSE: b:${newBlock.number} hash ${Hex.toHexString(newBlock.hash).substring(0,10)}   blockExists ${blockExists}, parentExist ${parentExists}, blockDiff ${blockDiff} ")
                        }
                    }
                else//parent and block not exist in sql
                    when {
                        blockDiff > 1   -> {
                            println ("b:${newBlock.number} hash ${Hex.toHexString(newBlock.hash).substring(0,10)}  blockExists ${blockExists}, parentExist ${parentExists}, blockDiff ${blockDiff} ledgBulkload")
                            this.ledgerBulkLoad1()
                        } //need bulkloading
                        blockDiff ==1   -> {
                            println ("b:${newBlock.number} hash ${Hex.toHexString(newBlock.hash).substring(0,10)}  blockExists ${blockExists}, parentExist ${parentExists}, blockDiff ${blockDiff} ")
                        }
                        blockDiff < 1   -> {
                            println ("b:${newBlock.number} hash ${Hex.toHexString(newBlock.hash).substring(0,10)}  blockExists ${blockExists}, parentExist ${parentExists}, blockDiff ${blockDiff} ")
                        }

                    }
            }
            //println("lock aquired in thread " +Thread.currentThread().id)

//            when (syncStatus) {
//                SyncStatus.onBlockSync -> {
//                    replayBlock.summaries=summaries
//                }
//                SyncStatus.bulkLoading,
//                SyncStatus.SingleInsert -> {
//                    replayBlock.run()
//                }
//            }
            //println ("block wo ledg ${block.number} hash:  ${Hex.toHexString(block.hash)}")
            //replayBlock.write()
            //enqueue( replayBlock)
        }
        catch ( e : Exception){throw RuntimeException("Error writing block ",e)}
        finally{ lock.unlock() }
    }



    @Transactional open fun start() {
        try{
                lock.lock()
                //println("Genesis lock aquired in thread " +Thread.currentThread().id)
                if (blockRepo!!.findOne(0)==null)
                    ensureGenesis()
            }
            finally {
                //println("Genesis unlock ,count : " +lock.holdCount)
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
            var genesis=GenesisBlockWrite(this,block0,blockRepo!!,ledgerRepo!!,accRepo!!)
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

    fun old_ledgerBulkLoad(_block: Int) {

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

    //    fun isContract(addr: ByteArray): Boolean {
    //
    //        val repository = ethereumBean?.ethereum?.repository as RepositoryImpl
    //        val contractDetails = repository.getContractDetails(addr) ?: return false
    //        contractDetails.code ?: return false
    //
    //        contractDetails.syncStorage()
    //        repository.flushNoReconnect()
    //        //if (contractDetails.code == null)
    //        //    return false
    //
    //        return (contractDetails.code.size != 0)
    //    }

}