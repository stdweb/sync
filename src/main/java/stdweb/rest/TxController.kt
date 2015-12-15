package stdweb.rest


import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import stdweb.Core.Utils
import DEL.Ledger_DEL.LedgerQuery
import DEL.Ledger_DEL.SqlDb
import DEL.Ledger_DEL.EthereumBean_DEL
import org.springframework.http.MediaType

import javax.servlet.http.HttpServletRequest
import java.io.IOException
import java.sql.SQLException

import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.annotation.RequestMethod.GET
import stdweb.Core.EntryType
import stdweb.Entity.LedgerBlock
import stdweb.Entity.LedgerEntry
import stdweb.Entity.Tx
import stdweb.Repository.LedgerBlockRepository
import stdweb.Repository.LedgerEntryRepository
import stdweb.Repository.LedgerTxRepository
import java.math.BigDecimal
import java.util.*

@RestController
class TxController {

    private val logger = LoggerFactory.getLogger("rest")

    @Autowired var ledgRepo     : LedgerEntryRepository?    = null
    @Autowired var blockRepo    : LedgerBlockRepository?    = null
    @Autowired var txRepo       : LedgerTxRepository?       = null

    @RequestMapping(value = "/tx/{txId}", method = arrayOf( RequestMethod.GET), produces = arrayOf(MediaType.APPLICATION_JSON_VALUE))
    @ResponseBody
    fun gettx(@PathVariable  txId : String,request : HttpServletRequest )  : List<LedgerEntry> {

        val t1=System.currentTimeMillis();
        val ret : ResponseEntity<LedgerEntry>
        ret = ResponseEntity(null, HttpStatus.OK)

        val tx      = txRepo    ?.findByHash(Utils.hash_decode(txId))
        val content = ledgRepo  ?.getByTxId(tx?.id) ?: ArrayList<LedgerEntry>()

        val result=content.filter { it.entryType in
                listOf(
                        EntryType.Send ,
                        EntryType.InternalCall,
                        EntryType.Call,
                        EntryType.NA,
                        EntryType.Genesis,
                        EntryType.ContractCreation )
        }
        Utils.log("tx",t1,request,ret);
        return content
    }

    @RequestMapping(value = "/txs/{blockId}",method = arrayOf( RequestMethod.GET), produces = arrayOf(MediaType.APPLICATION_JSON_VALUE))
    @ResponseBody
    fun getTxList(@PathVariable blockId : String ,request : HttpServletRequest ) : List<LedgerEntry> {
        val t1=System.currentTimeMillis();
        val ret : ResponseEntity<LedgerEntry>
        ret = ResponseEntity(null, HttpStatus.OK)

        val block   = blockRepo ?.get(blockId)
        val content = ledgRepo  ?.getByBlockId(block?.id) ?: ArrayList<LedgerEntry>()

        val result  = ArrayList<LedgerEntry>()
        with (content){
            val f1=filter { it.entryType in
                    listOf(
                            EntryType.CoinbaseReward,
                            EntryType.UncleReward,
                            EntryType.FeeReward) }

            val f2=filter { it.entryType in
                    listOf(
                            EntryType.Send ,
                            EntryType.InternalCall,
                            EntryType.Call,
                            EntryType.NA,
                            EntryType.Genesis,
                            EntryType.ContractCreation )}

            f2.forEach { it.amount= it.amount.abs() }
            result.addAll(f1.union(f2))
        }

        Utils.log("txs",t1,request,ret);
        return result
    }

    //    @Autowired
    //    EthereumBean_DEL ethereumBean;


    //    @RequestMapping(value = "/txs/{blockId}", method = GET, produces = APPLICATION_JSON_VALUE)
    //    @ResponseBody
    //    public String getTxList(@PathVariable String blockId,HttpServletRequest request) throws IOException {
    //        long t1=System.currentTimeMillis();
    //        try {
    //            Block block=ethereumBean.getBlock(blockId);
    //            LedgerQuery ledgerQuery = SqlDb.getSqlDb().getQuery();
    //
    //            String s = ledgerQuery.LedgerSelectByBlock(block.getNumber());
    //
    //            s=s.replace(":"," ");
    //            Utils.log("TxList",t1,request);
    //            return s;
    //
    //
    //        } catch (SQLException e) {
    //            e.printStackTrace();
    //        }
    //
    //        return  null;
    //    }


}
