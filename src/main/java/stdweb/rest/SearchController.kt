package stdweb.rest

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import stdweb.Core.Utils
import stdweb.Entity.LedgerEntry
import stdweb.Repository.LedgerAccountRepository
import stdweb.Repository.LedgerBlockRepository
import stdweb.Repository.LedgerTxRepository
import java.util.*
import javax.servlet.http.HttpServletRequest

@RestController
class SearchController
{
    @Autowired var accRepo      : LedgerAccountRepository?    = null
    @Autowired var blockRepo    : LedgerBlockRepository?    = null
    @Autowired var txRepo       : LedgerTxRepository?       = null

    @RequestMapping(value = "/search/{search_string}",method = arrayOf( RequestMethod.GET), produces = arrayOf(MediaType.APPLICATION_JSON_VALUE))
    @ResponseBody
    fun search(@PathVariable search_string: String, request: HttpServletRequest): HashMap<String, String> {

        val res: ResponseEntity<LedgerEntry>
        val t1 = System.currentTimeMillis()
        res = ResponseEntity(null, HttpStatus.NOT_IMPLEMENTED)
        val map=HashMap<String,String>()
        try{

                val addr = Utils.address_decode(search_string)
                val acc=accRepo!!.findByAddress(addr)
                if (acc!=null) {
                    map.put("resulttype", "address")
                    map.put("address", acc.toString())
                    Utils.log("founc acc", t1, request, res)
                    return map
                }
                try {
                    val bnum = Integer.parseInt(search_string)
                    val blockByNum = blockRepo!!.findOne(bnum)
                    if (blockByNum != null) {
                        map.put("resulttype", "block")
                        map.put("blocknumber", blockByNum.id.toString())
                        map.put("blockhash", blockByNum.hash_str)
                        Utils.log("found block", t1, request, res)
                        return map
                    }
                }
                catch (e : Exception){
                    println (e.message)
                }

                val hash=Utils.hash_decode(search_string)
                val block=blockRepo!!.findByHash(hash)
                if (block!=null)
                {
                    map.put("resulttype", "block")
                    map.put("blocknumber", block.id.toString())
                    map.put("blockhash",block.hash_str)
                    Utils.log("found block", t1, request, res)
                    return map
                }


            try{
                val hash=Utils.hash_decode(search_string)
                val tx=txRepo!!.findByHash(hash)
                if (tx!=null)
                {
                    map.put("resulttype", "tx")
                    map.put("tx", tx.hash_str())
                    Utils.log("found tx", t1, request, res)
                    return map
                }
            }
            catch (e : Exception){}
        }
        catch (e : Exception){}

            Utils.log("not found", t1, request, res)
            map.put("resulttype", "")

            Utils.log("found tx", t1, request, res)
            return map

    }
}