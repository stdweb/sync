package stdweb.rest;

import org.ethereum.core.Block;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import stdweb.Core.Convert2json;
import stdweb.Core.HashDecodeException;
import stdweb.Core.Utils;
import stdweb.Ledger.Ledger;
import stdweb.Ledger.LedgerBlock;
import stdweb.Ledger.LedgerBlockStore;
import stdweb.ethereum.EthereumBean;
import stdweb.ethereum.EthereumListener;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static stdweb.Core.Utils.hash_decode;

/**
 * Created by bitledger on 12.11.15.
 */
@RestController
public class BlockController {

    @Autowired
    EthereumBean ethereumBean;

    LedgerBlockStore bstore;

    @RequestMapping(value = "/bestBlock", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getBestBlock(HttpServletRequest request) throws IOException, SQLException {
        return String.valueOf(bstore.getTopBlock().getNumber());
        //return ethereumBean.getBestBlock();
    }

    @RequestMapping(value = "/block/{blockId}", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getBlock(@PathVariable String blockId,HttpServletRequest request) throws IOException {

        long t1=System.currentTimeMillis();
        String ret=getBlockStr(blockId);
        Utils.log("block",t1,request);
        return ret;
    }

    @RequestMapping(value = "/blocks/{blockId}", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getBlockList(@PathVariable String blockId,HttpServletRequest request) throws IOException {
        long t1=System.currentTimeMillis();
        String result="no err";
        try {

            LedgerBlock block=getBlockObject(blockId);
            List<LedgerBlock> blocks = getList(block);

            result= BlockList2json(blocks, ethereumBean.getListener());
            //result=ethereumBean.checkBlocks();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            //result=e.toString();
        }
        //return Convert2json.BlockList2json(blocks);
        Utils.log("blocks", t1, request);
        return result;
    }

    public LedgerBlock getBlockObject(String blockId) throws HashDecodeException, SQLException {
        long t1=System.currentTimeMillis();
        LedgerBlock block;
        if (blockId.equals("top"))
            //block=ethereum.getBlockchain().getBestBlock();
            block=bstore.getTopBlock();

        else  if (blockId.length() >= 64) {
            //block=ethereum.getBlockchain().getBlockByHash(hash_decode(blockId));
            block=bstore.get(blockId);

        }else {
            Long blockNo = Long.parseLong(blockId);
            //block = ethereum.getBlockchain().getBlockByNumber(blockNo);
            block=bstore.get(blockNo);


        }
        return block;
    }

    public String getBlockStr(String blockId) {

        String result = "";
        try {
            LedgerBlock block = getBlockObject(blockId);
            if(block!=null)
                result = block.toJSON();
            else{}
        }
        catch (NumberFormatException e)
        {
            e.printStackTrace();
            //result=e.toString();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            //result=e.toString();
        }
        return  result;
    }

    public static String BlockList2json(List<LedgerBlock> blockList, EthereumListener listener)
    {
        String result="[";
        for (LedgerBlock block : blockList)
        {
            try {
                result += block.toJSON() + ",";
            }
            catch (Exception e)
            {
                result="Err in block:"+String.valueOf(block.getNumber());
            }
        }
        if (result.endsWith(","))
            result=result.substring(0,result.length()-1);

        result+="]";
        return result;
    }

    public List<LedgerBlock> getList(LedgerBlock toplistblock) throws SQLException {
        long height =toplistblock.getNumber();// ethereum.getBlockchainImpl().getBestBlock().getNumber();
        ArrayList<LedgerBlock> blocks = new ArrayList<>();

        long endHeight=Math.max(height-40,0);

        for (long i=height;i>=endHeight;--i) {
            //Block block=ethereum.getBlockchain().getBlockByNumber(i);
            LedgerBlock block = bstore.get(i);
            blocks.add(block);
        }

        return  blocks;
    }



    BlockController() throws SQLException {
        bstore=LedgerBlockStore.getInstance();
    }

}
