package stdweb.ethereum

import org.ethereum.core.Account
import org.ethereum.core.Block
import org.ethereum.core.TransactionExecutionSummary
import org.ethereum.db.RepositoryImpl

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.annotation.Transactional
import stdweb.Core.*
import stdweb.Entity.LedgerAccount

import stdweb.Repository.*
import java.math.BigDecimal
import java.util.concurrent.locks.ReentrantLock
import javax.annotation.PostConstruct

@Service
//@EnableTransactionManagement
open class LedgerSyncService
{
    var syncStatus : SyncStatus = SyncStatus.stopped

    @Autowired open var blockRepo        : LedgerBlockRepository? = null
    @Autowired open var accRepo          : LedgerAccountRepository? = null
    @Autowired open var ledgerRepo       : LedgerEntryRepository? = null

    @Autowired open var txRepo           : LedgerTxRepository? = null

    @Autowired open var logRepo          : LedgerTxLogRepository? = null
    @Autowired open var receiptRepo      : LedgerTxReceiptRepository? = null

    @Autowired open var ethereumBean     : EthereumBean? = null

    public     open var listener         : EthereumListener?=null

    //open  var ethereum: Ethereum? = null

    open val lock : ReentrantLock = ReentrantLock()


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

    private var replayBlock: ReplayBlockWrite? = null


    @Transactional open fun saveBlockData(block : Block, summaries : List<TransactionExecutionSummary>)
    {

        if (accRepo!=null && blockRepo!=null && ledgerRepo!=null && txRepo!=null && logRepo!=null && receiptRepo!=null) {

            replayBlock = ReplayBlockWrite(
                    this,block,
                    blockRepo !!,
                    accRepo !!,
                    ledgerRepo!!,
                    txRepo!!,
                    logRepo!!,
                    receiptRepo!!
                    )

            //(replayBlock as ReplayBlockWrite).run();

            replayBlock!!write (summaries);

            println ("block saved:"+block.number)
        }
        else
            throw NullPointerException("at least one repo is null")
    }

    @Transactional open fun start() {
            try{
                lock.lock()
                println("genesis lock")
                if (blockRepo!!.findOne(0)==null) {
                    //blockRepo!!.deleteBlockWithEntries(0)
                    //blockRepo!!.delete(0)
                    ensureGenesis()
                    println ("genesis deleted")
                }
            }
            finally
            {
                lock.unlock()
                println("genesis unlock")
            }

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

    @PostConstruct
    private fun initService()
    {

        println("LedgerSync initService")
        //println ("lock :"+lock)
    }

    constructor()
    {}
}