package stdweb.ethereum

import org.ethereum.core.Account
import org.ethereum.core.Block
import org.ethereum.core.TransactionExecutionSummary
import org.ethereum.db.RepositoryImpl
import org.springframework.beans.factory.annotation.Autowired
import stdweb.Core.*
import stdweb.Entity.LedgerAccount
import stdweb.Repository.*
import java.math.BigDecimal

public class LedgerSyncService
{
    var syncStatus : SyncStatus = SyncStatus.stopped

    @Autowired var blockRepo        : LedgerBlockRepository? = null
    @Autowired var accRepo          : LedgerAccountRepository? = null
    @Autowired var ledgerRepo       : LedgerEntryRepository? = null

    @Autowired var txRepo           : LedgerTxRepository? = null

    @Autowired var logRepo          : LedgerTxLogRepository? = null
    @Autowired var receiptRepo      : LedgerTxReceiptRepository? = null

    @Autowired var ethereumBean     : EthereumBean? = null

    private    var listener         : EthereumListener?=null

    //private var ethereum: Ethereum? = null

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


    //@Transactional
    public fun loadBlockData(block : Block, summaries : List<TransactionExecutionSummary>)
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

            (replayBlock!!).write(summaries);

            println ("block saved:"+block.number)
        }
        else
            throw NullPointerException("at least one repo is null")
    }

    constructor()
    {}

    public fun start() {
        //blockRepo!!.deleteBlockAll(blockRepo!!.findOne(160854))
        //println ("LedgSyncService started")
    }
}