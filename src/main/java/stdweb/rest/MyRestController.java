package stdweb.rest;


import org.ethereum.core.Block;
import org.ethereum.core.BlockchainImpl;
import org.ethereum.facade.Ethereum;
import org.springframework.web.bind.annotation.PathVariable;
import stdweb.Core.Convert2json;
import stdweb.Core.LedgerStore;
import stdweb.ethereum.EthereumBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
public class MyRestController {

    @Autowired
    EthereumBean ethereumBean;

    @RequestMapping(value = "/bestBlock", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getBestBlock() throws IOException {
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

    @RequestMapping(value = "/ledger/{cmd}", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String ledger(@PathVariable String cmd) throws IOException, SQLException, InterruptedException {

        BlockchainImpl blockchain = (BlockchainImpl)ethereumBean.getEthereum().getBlockchain();
        switch (cmd.toLowerCase())
        {
            case "stop":
                blockchain.setStopOn(0);break;
            case "start":
                blockchain.setStopOn(0);
                //EthereumBean.setLedgerSyncBlock(0);
                ethereumBean.syncLedger();
                break;

            //status
        }
        //long stopBlockNo=Long.valueOf(blockId);
        String result = blockchainStatus( blockchain);

        return result;
    }

    @RequestMapping(value = "/blockchain/{cmd}", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String blockchain(@PathVariable String cmd) throws IOException, SQLException, InterruptedException {

        BlockchainImpl blockchain = (BlockchainImpl)ethereumBean.getEthereum().getBlockchain();
        switch (cmd.toLowerCase())
        {
            case "stop":
                blockchain.setStopOn(0);break;
            case "start":
                blockchain.setStopOn(Long.MAX_VALUE);break;

            //status
        }
        //long stopBlockNo=Long.valueOf(blockId);
        String result = blockchainStatus( blockchain);

        return result;
    }

//    public String checkDelta(Block block) throws InterruptedException, SQLException {
//        LedgerStore ledgerStore = LedgerStore.getLedgerStore(ethereumBean.getListener());
//        BlockchainImpl blockchain = (BlockchainImpl)ethereumBean.getEthereum().getBlockchain();
//
//        long stopOn = blockchain.getStopOn();
//        blockchain.setStopOn(0);
//        Thread.sleep(500);
//        BigDecimal trieDelta = ledgerStore.getTrieDelta(block);
//        BigDecimal ledgerBlockDelta = ledgerStore.getLedgerBlockDelta(block);
//
//        long number = block.getNumber();
//        String result="";
//        if (trieDelta.equals(ledgerBlockDelta))
//            result="Block delta correct:" + number + ": " + Convert2json.BI2ValStr(trieDelta.toBigInteger(), false);
//        else {
//            result="Block delta incorrect:" + number + ", trie - ledger: " + Convert2json.BI2ValStr(trieDelta.toBigInteger(), false) + " - " + Convert2json.BI2ValStr(ledgerBlockDelta.toBigInteger(), false);
//        }
//
//        blockchain.setStopOn(stopOn);
//
//        return result;
//    }

    public String checkBalance(Block block) throws InterruptedException, SQLException {
        LedgerStore ledgerStore = LedgerStore.getLedgerStore(ethereumBean.getListener());
        BlockchainImpl blockchain = (BlockchainImpl)ethereumBean.getEthereum().getBlockchain();

        long stopOn = blockchain.getStopOn();
        blockchain.setStopOn(0);
        Thread.sleep(500);
        BigDecimal trieBalance = ledgerStore.getTrieBalance(block);
        BigDecimal ledgerBlockBalance = ledgerStore.getLedgerBlockBalance(block);

        long number = block.getNumber();
        String result="";
        if (trieBalance.equals(ledgerBlockBalance))
            result="Block balance correct:" + number + ": " + Convert2json.BI2ValStr(trieBalance.toBigInteger(), false);
        else {
            result="Block balance incorrect:" + number + ", trie - ledger: " + Convert2json.BI2ValStr(trieBalance.toBigInteger(), false) + " - " + Convert2json.BI2ValStr(ledgerBlockBalance.toBigInteger(), false);
        }

        blockchain.setStopOn(stopOn);

        return result;
    }
    private String blockchainStatus( BlockchainImpl blockchain) throws SQLException, InterruptedException {

        String result="TopBlock:"+blockchain.getBestBlock().getNumber()+"\n";
        result+="stopOn:"+blockchain.getStopOn()+"\n";

        if (blockchain.getStopOn()<=blockchain.getBestBlock().getNumber())
            result+=String.format("Top block %s, Blockchain is stopped\n",String.valueOf(blockchain.getBestBlock().getNumber()));
        else
            result+=String.format("Top block %s, Blockchain is loading\n",String.valueOf(blockchain.getBestBlock().getNumber()));

        try {
            result+=String.format("Ledger Top block: %s\n",LedgerStore.getLedgerStore(ethereumBean.getListener()).ledgerTopBlock());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        result+=checkBalance(blockchain.getBestBlock())+"\n";
        //result+=checkDelta(blockchain.getBestBlock())+"\n";

        result+="ledgerSyncBlock: "+EthereumBean.getLedgerSyncBlock()+"\n";

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
            result=e.toString();
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
            LedgerStore ledgerStore = LedgerStore.getLedgerStore(ethereumBean.getListener());

            String s = ledgerStore.LedgerSelectByBlock(String.valueOf(block.getNumber()));

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

    @RequestMapping(value = "/account/{accountId}", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getAccountLedger(@PathVariable String accountId) throws IOException {

        return ethereumBean.getAccountLedger(accountId);
    }

}
