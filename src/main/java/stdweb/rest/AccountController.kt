package stdweb.rest


import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import java.io.IOException
import java.math.BigDecimal
import java.sql.SQLException
import javax.servlet.http.HttpServletRequest


import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.annotation .RequestMethod.GET
import stdweb.Core.*
import stdweb.Core.Utils.address_decode
import stdweb.Entity.LedgerAccount
import stdweb.Entity.LedgerEntry
import stdweb.Repository.LedgerAccountRepository
import stdweb.Repository.LedgerBlockRepository
import stdweb.Repository.LedgerEntryRepository
import java.util.*

/**
 * Created by bitledger on 13.11.15.
 */

@RestController
class AccountController{

    @Autowired
    var ledgRepo : LedgerEntryRepository? = null

    @Autowired
    var blockRepo : LedgerBlockRepository? = null


    @Autowired
    var acckRepo : LedgerAccountRepository? = null

    @RequestMapping(value = "/account/{accountId}/{page}", method = arrayOf( RequestMethod.GET), produces = arrayOf(MediaType.APPLICATION_JSON_VALUE))
    @ResponseBody
    fun getAccountLedger(@PathVariable accountId: String, @PathVariable page: String, request: HttpServletRequest)
            : HashMap<String,Any> {

        val res: ResponseEntity<LedgerEntry>
        val t1 = System.currentTimeMillis()
        res = ResponseEntity(null, HttpStatus.NOT_IMPLEMENTED)

        val addr=address_decode(accountId)
        val acc   = acckRepo?.findByAddress(addr)
        val offs=(page.toInt()-1)*25
        val zeroAccount= LedgerAccount(Utils.ZERO_BYTE_ARRAY_20)

        //val content = ledgRepo?.getAccountLedgerPage(acc!!.id,offs) ?:  ArrayList<LedgerEntry>()
        //val content = ledgRepo?.getAccountLedger(acc!!.id,offs,page.toInt()*25) ?:  ArrayList<LedgerEntry>()
        val content = ledgRepo?.getAccountLedger(acc!!.id,offs,page.toInt()*25) ?:  ArrayList<LedgerEntry>()

        val result  = ArrayList<LedgerEntry>()

        content.forEach {
            result.add(it)
            if (it.fee.signum()==1)
                with (LedgerEntry()) {
                    account             = it.account
                    tx                  = it.tx
                    offsetAccount       = zeroAccount
                    amount              = it.fee.negate()
                    block               = it.block
                    blockTimestamp      = it.blockTimestamp
                    depth               = it.depth
                    gasUsed             = 0
                    entryType           = EntryType.TxFee
                    fee                 = BigDecimal.ZERO
                    grossAmount         = this.fee.negate()
                    entryResult         = EntryResult.Ok

                    balance             =it.balance
                    it.balance          =it.balance.add(it.fee)
                    result.add(this)
                }
        }

        val ret=HashMap<String,Any>()


        val firstBlock  =blockRepo?.findOne(ledgRepo?.getFirstAccountBlock(acc?.id ))
        val lastBlock   =blockRepo?.findOne(ledgRepo?.getLastAccountBlock(acc?.id ))

        val entriesCount=ledgRepo?.getEntriesCount(acc!!.id) ?: 0
        with(ret){
            put("entries_count", entriesCount)
            put("page_count",entriesCount/25+1)
            put("balance",Convert2json.BD2ValStr(acc?.balance,false))
            put("addresstype",if (acc?.isContract ?: false) "Contract" else "account")
            put("entries", result)
            put("firstblock","${firstBlock?.id}, Date ${firstBlock?.BlockDateTime}")
            put("lastblock","${lastBlock?.id}, Date ${lastBlock?.BlockDateTime}")
        }
        Utils.log("AccountLedger", t1, request, res)

        return ret
    }


}


