package stdweb.rest


import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import stdweb.Core.Utils
import stdweb.Entity.LedgerBlock
import stdweb.Repository.EntityQueryRange
import stdweb.Repository.LedgerBlockRepository
import stdweb.ethereum.EthereumBean
import java.util.*
import javax.servlet.http.HttpServletRequest

@RestController
class BlockController {

    @Autowired
    var ethereumBean : EthereumBean? = null

    @Autowired
    var repo: LedgerBlockRepository? = null


    @RequestMapping(value = "/bestBlock", method = arrayOf( RequestMethod.GET), produces = arrayOf(MediaType.APPLICATION_JSON_VALUE))
    @ResponseBody
    fun getBestBlock(request: HttpServletRequest): String {

        return repo?.topBlock()?.id.toString()
    }

    @RequestMapping(value = "/block/{blockId}", method = arrayOf( RequestMethod.GET), produces = arrayOf(MediaType.APPLICATION_JSON_VALUE))
    //@ResponseStatus(value = HttpStatus.OK)
    fun getBlock(@PathVariable blockId : String, request : HttpServletRequest ) :  ResponseEntity<LedgerBlock>
    {
            val t1=System.currentTimeMillis();
            var ledgerBlock = repo?.get(blockId);

            Utils.log("block",t1,request,ResponseEntity<Any?>(null,HttpStatus.NOT_IMPLEMENTED));

            var ret : ResponseEntity<LedgerBlock>

            if (ledgerBlock == null) {
                ret = ResponseEntity(null, HttpStatus.NOT_FOUND)
            }
            else
                ret = ResponseEntity<LedgerBlock>(ledgerBlock, HttpStatus.OK)


            Utils.log("block",t1,request,ret);
            return ret
        }
    //


        @RequestMapping(value = "/blocks/{blockId}", method = arrayOf( RequestMethod.GET), produces = arrayOf(MediaType.APPLICATION_JSON_VALUE))
        fun  getBlockList(@PathVariable blockId : String ,request : HttpServletRequest ): List<LedgerBlock>? {

            var ret : ResponseEntity<List<LedgerBlock>>

            val t1=System.currentTimeMillis();

            var top =repo?.topBlock()?.id ?: 0
            var offset=top

            if (!blockId.equals("top")) {
                offset = repo?.get(blockId)?.id ?: top
            }

            var content = repo?.getBlockRange(offset-40,offset)

            if (content==null)
                ret= ResponseEntity(null, HttpStatus.INTERNAL_SERVER_ERROR)
            else
                ret= ResponseEntity(content, HttpStatus.OK)

            Utils.log("blocks", t1, request,ret);

            return content
        }
    //
    //    public LedgerBlock getBlockObject(String blockId) throws HashDecodeException, SQLException {
    //        long t1=System.currentTimeMillis();
    //        LedgerBlock block;
    //        if (blockId.equals("top"))
    //            //block=ethereum.getBlockchain().getBestBlock();
    //            //block=bstore.getTopBlock();
    //            block=bstore.get("top");
    //
    //        else  if (blockId.length() >= 64) {
    //            //block=ethereum.getBlockchain().getBlockByHash(hash_decode(blockId));
    //            block=bstore.get(blockId);
    //
    //        }else {
    //            int blockNo = Integer.parseInt(blockId);
    //            //block = ethereum.getBlockchain().getBlockByNumber(blockNo);
    //            block=bstore.get(blockNo);
    //
    //
    //        }
    //        return block;
    //    }
    //
    //
    //    public List<LedgerBlock> getList(LedgerBlock toplistblock) throws SQLException {
    //        int height =toplistblock.getId();// ethereum.getBlockchainImpl().getBestBlock().getNumber();
    //        ArrayList<LedgerBlock> blocks = new ArrayList<>();
    //
    //        long endHeight=Math.max(height-40,0);
    //
    //        for (int i=height;i>=endHeight;--i) {
    //            //Block block=ethereum.getBlockchain().getBlockByNumber(i);
    //            LedgerBlock block = bstore.findOne(i);
    //            blocks.add(block);
    //        }
    //
    //        return  blocks;
    //    }

}
