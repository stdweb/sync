package stdweb.rest

import DEL.Ledger_DEL.SqlDb
import DEL.Ledger_DEL.TestBalances
import org.ethereum.core.BlockchainImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import stdweb.Core.SyncStatus
import stdweb.Repository.LedgerBlockRepository
import DEL.Ledger_DEL.EthereumBean_DEL
import stdweb.ethereum.EthereumBean
import stdweb.ethereum.LedgerSyncService
import java.io.IOException
import java.sql.SQLException
import java.util.*
import javax.servlet.http.HttpServletRequest


@RestController
//@RequestMapping(value = "/cmd")
class CmdController
{

    val blockRepo   : LedgerBlockRepository
    val ledgSync    : LedgerSyncService
    val ethereumBean: EthereumBean

    @RequestMapping(value = "/cmd/{cmd}/{param}", method = arrayOf( RequestMethod.GET), produces = arrayOf(MediaType.APPLICATION_JSON_VALUE))
    @ResponseBody
    fun cmd(@PathVariable  cmd : String, @PathVariable param : String, request : HttpServletRequest) : String
    {
        val i = Integer.parseInt(param)
        var ret=""
        when (cmd) {
            "delete" -> {
                blockRepo.deleteBlockWithEntries(i)

                ret = "block deleted:" + i
            }
            "insert" -> {
                ledgSync.replayAndSaveBlock(i)
                //sqlDb.setNextStatus(sqlDb.getSyncStatus())
                //sqlDb.setSyncStatus(SyncStatus.SingleInsert)
                //sqlDb.replayAndInsertBlock(i.toLong())

                ret = "manual block inserted:" + i
            }
            "checkall" -> {

                ret = TestBalances.checkBalance(i.toLong())
                ret += "\n"
                ret += TestBalances.checkAccountsBalance(i.toLong(), true)
            }
            "check" -> {
                //ret=TestBalances.checkBalance(i);
                ret += "\n"
                ret += TestBalances.checkAccountsBalance(i.toLong(), false)
            }

            "findempty" -> {
                //ret=TestBalances.checkBalance(i);
                ret += "\n"
                ret += TestBalances.findEmpty(i.toLong(), false)
            }
        }
        return ret
    }

    @RequestMapping(value = "/ledger/{cmd}", method = arrayOf( RequestMethod.GET), produces = arrayOf(MediaType.APPLICATION_JSON_VALUE))
    @ResponseBody
    @Throws(IOException::class, SQLException::class, InterruptedException::class)
    fun ledgerCmd(@PathVariable cmd: String): Map<String,String> {

        when (cmd.toLowerCase()) {
            "stop" -> ledgSync.stopSync()
            "start" -> {}//ledgSync.ledgerBulkLoad()
            "sync" -> ledgSync.nextStatus = SyncStatus.onBlockSync
            "stopsync" -> {
                ledgSync.syncStatus = SyncStatus.stopped
                ledgSync.nextStatus = SyncStatus.stopped
            }
            else -> try {
                val block = Integer.parseInt(cmd)
                ledgSync.old_ledgerBulkLoad(block)
                //ethereumBean.ledgerStartSync(block);
            } catch (e: NumberFormatException) {
                //return "Wrong cmd in method /ledger/{cmd}"
            }
        }//ethereumBean.ledgerStartSync(Long.MAX_VALUE);
        val result = ledgSync.status()
        return result
    }

    @RequestMapping(value = "/blockchain/{cmd}", method = arrayOf( RequestMethod.GET), produces = arrayOf(MediaType.APPLICATION_JSON_VALUE))
    @ResponseBody
    fun blockchainCmd(@PathVariable cmd: String): Map<String,String> {

        var result= HashMap<String, String>()

        when (cmd.toLowerCase()) {
            "stop" -> ethereumBean.blockchainStopSync()
            "start" -> {
                ledgSync.syncStatus = SyncStatus.onBlockSync
                ledgSync.nextStatus = SyncStatus.onBlockSync
                ethereumBean.blockchainStartSync()
            }
            "check" -> result.put ("check balance","not implemented")//TestBalances.checkBalance()
        }//status
        result.putAll( ledgSync.status())

        return result
    }



    @Autowired
    constructor(_blockRepo : LedgerBlockRepository,_ledgSync : LedgerSyncService, _eth : EthereumBean)
    {
        this.blockRepo=_blockRepo
        this.ledgSync=_ledgSync
        this.ethereumBean=_eth
    }

}

