package stdweb.rest;


import org.ethereum.core.Block;
import org.ethereum.core.BlockchainImpl;
import org.ethereum.facade.Ethereum;
import org.ethereum.net.eth.handler.Eth;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.web.bind.annotation.PathVariable;
import stdweb.Core.*;
import stdweb.ethereum.EthereumBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
public class MyRestController {

    private static final Logger logger = LoggerFactory.getLogger("rest");
    @Autowired
    EthereumBean ethereumBean;

    @RequestMapping(value = "/bestBlock", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getBestBlock(HttpServletRequest request) throws IOException {
        logger.info("ip:"+request.getRemoteAddr());
        logger.info("method:"+request.getMethod());
        logger.info("servletPath:"+request.getServletPath());
        logger.info("requestUri:"+request.getRequestURI());
        return ethereumBean.getBestBlock();
    }

    @RequestMapping(value = "/test",method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String test() {
        return String.valueOf(System.currentTimeMillis());
    }

//    @RequestMapping(value = "/error")
//    public String error() {
//        return "Error handling";
//    }


    public String checkDelta(Block block) throws InterruptedException, SQLException {
        LedgerStore ledgerStore = LedgerStore.getLedgerStore(ethereumBean.getListener());
        BlockchainImpl blockchain = (BlockchainImpl)ethereumBean.getEthereum().getBlockchain();

        long stopOn = blockchain.getStopOn();
        blockchain.setStopOn(0);
        Thread.sleep(500);
        BigDecimal trieDelta = BlockchainQuery.getTrieDelta(block);
        BigDecimal ledgerBlockDelta = ledgerStore.getQuery().getLedgerBlockDelta(block);

        long number = block.getNumber();
        String result="";
        if (trieDelta.equals(ledgerBlockDelta))
            result="Block delta correct:" + number + ": " + Convert2json.BI2ValStr(trieDelta.toBigInteger(), false);
        else {
            result="Block delta incorrect:" + number + ", trie - ledger: " + Convert2json.BI2ValStr(trieDelta.toBigInteger(), false) + " - " + Convert2json.BI2ValStr(ledgerBlockDelta.toBigInteger(), false);
        }

        blockchain.setStopOn(stopOn);

        return result;
    }



    @RequestMapping(value = "/difficulty", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getDifficulty() throws IOException {

        Ethereum ethereum = ethereumBean.getEthereum();
        BlockchainImpl blockchain = (BlockchainImpl) ethereum.getBlockchain();

        return ethereumBean.getDifficulty();
    }


    @RequestMapping(value = "/blocks/{blockId}", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getBlockList(@PathVariable String blockId) throws IOException {
        String result="no err";
        try {

            Block block=ethereumBean.getBlock(blockId);
            List<Block> blocks = ethereumBean.getBlockList(block);

            result= Convert2json.BlockList2json(blocks, ethereumBean.getListener());
            //result=ethereumBean.checkBlocks();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            //result=e.toString();
        }
        //return Convert2json.BlockList2json(blocks);
        return result;
    }

    @RequestMapping(value = "/block/{blockId}", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getBlock(@PathVariable String blockId) throws IOException {

        return ethereumBean.getBlockStr(blockId);
    }

    @RequestMapping(value = "/balance/{blockId}", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getBalance(@PathVariable String blockId) throws IOException, SQLException {

        return ethereumBean.getBalance(blockId);
    }

    @RequestMapping(value = "/txs/{blockId}", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getTxList(@PathVariable String blockId) throws IOException {

        try {
            Block block=ethereumBean.getBlock(blockId);
            LedgerQuery ledgerQuery = LedgerStore.getLedgerStore(ethereumBean.getListener()).getQuery();

            String s = ledgerQuery.LedgerSelectByBlock(block.getNumber());

            s=s.replace(":"," ");
            return s;


        } catch (SQLException e) {
            e.printStackTrace();
        }

        return  null;
    }

//    @RequestMapping(value = "/txs/{blockId}", method = GET, produces = APPLICATION_JSON_VALUE)
//    @ResponseBody
//    public String getTxList(@PathVariable String blockId) throws IOException {
//        String result="no err";
//        try {
//
//            Block block=ethereumBean.getBlock(blockId);
//
//            ReplayBlock replayBlock = new ReplayBlock(ethereumBean.getListener(), block);
//            replayBlock.run();
//
//            List<Transaction> txList = replayBlock.getTxList();
//            HashMap<Transaction, Long> txGasUsedList = replayBlock.getTxGasUsedList();
//
//            result=Convert2json.TxList2json(replayBlock);
//            return  result;
//        }
//        catch (Exception e)
//        {
//            result=e.toString();
//            //result=e.getStackTrace().toString();
//        }
//        //return Convert2json.BlockList2json(blocks);
//        return result;
//    }

    @RequestMapping(value = "/tx/{txId}", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String gettx(@PathVariable String txId) throws IOException {

        try {
            LedgerQuery ledgerQuery = LedgerStore.getLedgerStore(ethereumBean.getListener()).getQuery();
            String s = ledgerQuery.LedgerSelectByTx(txId);

            s=s.replace(":"," ");
            return s;


        } catch (SQLException e) {
            e.printStackTrace();
        }

        return  "error";

    }
    @RequestMapping(value = "/account/{accountId}/{offset}", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getAccountLedger(@PathVariable String accountId,@PathVariable String offset) throws IOException {

        try {
            LedgerStore ledgerStore = LedgerStore.getLedgerStore(ethereumBean.getListener());
            LedgerQuery ledgerQuery = LedgerQuery.getQuery(ledgerStore);

            long t1=System.currentTimeMillis();
            JSONArray jsonArray = ledgerQuery.LedgerSelect(accountId, offset);
            long t2=System.currentTimeMillis();
            Utils.TimeDiff("LedgerSelectSql ",t1,t2);
            //JSONObject entriesJson=new JSONObject();

            t1=System.currentTimeMillis();
            JSONObject entriesJson=ledgerQuery.acc_entry_count(accountId,offset);
            t2=System.currentTimeMillis();
            Utils.TimeDiff("page and entries count ", t1, t2);


            if(accountId.startsWith("0x"))
                accountId=accountId.substring(2);
            byte[] acc=Hex.decode(accountId);
            LedgerAccount account=new LedgerAccount(acc);

            t1=System.currentTimeMillis();
            //BigDecimal ledgerBlockBalance = ledgerQuery.getLedgerAccountBalance(account,ledgerQuery.getSqlTopBlock());
            BigDecimal ledgerBlockBalance = account.getBalance();
            t2=System.currentTimeMillis();
            Utils.TimeDiff("count acc balance ", t1, t2);

            //BigDecimal ledgerBlockBalance=BigDecimal.ZERO;
            entriesJson.put("balance",Convert2json.BD2ValStr(ledgerBlockBalance,true));
            entriesJson.put("addresstype",account.isContract() ? "Contract" : "Account");

            entriesJson.put("entries",jsonArray);
            String s=entriesJson.toJSONString();

            s=s.replace(":"," ");
            return s;

        } catch (SQLException e) {
            e.printStackTrace();
            return  e.toString();
        }
    }

}
