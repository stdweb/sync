package stdweb.rest;


import org.ethereum.core.Block;
import org.ethereum.core.BlockchainImpl;
import org.ethereum.facade.Ethereum;
import org.ethereum.net.eth.handler.Eth;
import org.spongycastle.util.encoders.Hex;
import org.springframework.web.bind.annotation.PathVariable;
import stdweb.Core.Convert2json;
import stdweb.Core.Ledger;
import stdweb.Core.LedgerStore;
import stdweb.Core.SyncStatus;
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

    @RequestMapping(value = "/ledger/{cmd}/{param}", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String sql(@PathVariable String cmd, @PathVariable String param) throws IOException,  InterruptedException {
        LedgerStore ledgerStore = LedgerStore.getLedgerStore(ethereumBean.getListener());

        int i = Integer.parseInt(param);

        String ret="";
        try {
            switch (cmd) {
                case "delete":
                    ledgerStore.deleteBlocksFrom(i);
                    ret="block deleted:"+i;
                    break;
                case "insert":
                    ledgerStore.insertBlock(i);
                    ret= "block inserted:"+i;
                    break;
                case "check":
                    ret=checkBalance(i);
                    break;
            }
        }
        catch (SQLException e)
        {
            System.out.println(e.getMessage()+"\n");
            e.printStackTrace();
        }
        return  ret;


    }

    @RequestMapping(value = "/ledger/{cmd}", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String ledger(@PathVariable String cmd) throws IOException, SQLException, InterruptedException {

        BlockchainImpl blockchain = (BlockchainImpl)ethereumBean.getEthereum().getBlockchain();
        LedgerStore ledgerStore = LedgerStore.getLedgerStore(ethereumBean.getListener());
        switch (cmd.toLowerCase())
        {
            case "stop":
                ledgerStore.stopSync();
                break;
                //ethereumBean.ledgerStopSync();
            case "start":
                ledgerStore.ledgerBulkLoad();
                //ethereumBean.ledgerStartSync(Long.MAX_VALUE);
                break;
            case "sync":
                ledgerStore.setNextStatus(SyncStatus.onBlockSync);
                break;
            case "stopsync":
                ledgerStore.setSyncStatus(SyncStatus.stopped);
                ledgerStore.setNextStatus(SyncStatus.stopped);
                break;



            default:

                try {

                    int block = Integer.parseInt(cmd);

                    ledgerStore.ledgerBulkLoad(block);
                    //ethereumBean.ledgerStartSync(block);
                }
                catch (NumberFormatException e)
                {
                    return "Wrong cmd in method /ledger/{cmd}";
                }
        }
        String result = blockchainStatus( );

        return result;
    }

    @RequestMapping(value = "/blockchain/{cmd}", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String blockchain(@PathVariable String cmd) throws IOException, SQLException, InterruptedException {

        BlockchainImpl blockchain = (BlockchainImpl)ethereumBean.getEthereum().getBlockchain();
        String result = blockchainStatus( );
        switch (cmd.toLowerCase())
        {
            case "stop":
                ethereumBean.blockchainStopSync();

                break;
            case "start":
                ethereumBean.blockchainStartSync();

                break;

            case "check":
                result+=checkBalance();
                break;

            //status
        }
        //long stopBlockNo=Long.valueOf(blockId);


        return result;
    }


    public String checkDelta(Block block) throws InterruptedException, SQLException {
        LedgerStore ledgerStore = LedgerStore.getLedgerStore(ethereumBean.getListener());
        BlockchainImpl blockchain = (BlockchainImpl)ethereumBean.getEthereum().getBlockchain();

        long stopOn = blockchain.getStopOn();
        blockchain.setStopOn(0);
        Thread.sleep(500);
        BigDecimal trieDelta = ledgerStore.getTrieDelta(block);
        BigDecimal ledgerBlockDelta = ledgerStore.getLedgerBlockDelta(block);

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

    public String checkBalance() throws InterruptedException, SQLException {
        long number = EthereumBean.getBlockchain().getBestBlock().getNumber();
        return checkBalance(number);
    }
    public String checkBalance(long number) throws InterruptedException, SQLException {

        LedgerStore ledgerStore = LedgerStore.getLedgerStore(ethereumBean.getListener());
        int sqlTopBlock = ledgerStore.getSqlTopBlock();
        Block blockByNumber = EthereumBean.getBlockchain().getBlockByNumber(Math.min(number, sqlTopBlock));
        return checkBalance(blockByNumber);
    }
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

        result+="\n"+"ledger count:"+ledgerStore.ledgerCount(block.getNumber())+"\n";
        result+="Coinbase delta:"+ Hex.toHexString(block.getCoinbase())+" -> "+Convert2json.BD2ValStr(ledgerStore.getCoinbaseTrieDelta(block),false);

        return result;
    }
    private String blockchainStatus(  ) throws SQLException, InterruptedException {

        BlockchainImpl blockchain = (BlockchainImpl)ethereumBean.getEthereum().getBlockchain();
        String result="TopBlock:"+blockchain.getBestBlock().getNumber()+"\n";
        result+="stopOn:"+blockchain.getStopOn()+"\n";

        result+="Blockchain syncStatus:"+EthereumBean.getBlockchainSyncStatus()+"\n";

        if (blockchain.getStopOn()<=blockchain.getBestBlock().getNumber())
            result+=String.format("Top block %s, Blockchain is stopped\n",String.valueOf(blockchain.getBestBlock().getNumber()));
        else
            result+=String.format("Top block %s, Blockchain is loading\n",String.valueOf(blockchain.getBestBlock().getNumber()));

        try {
            result+=String.format("Ledger sql Top block: %s\n",LedgerStore.getLedgerStore(ethereumBean.getListener()).getSqlTopBlock());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        LedgerStore ledgerStore = LedgerStore.getLedgerStore(ethereumBean.getListener());
        result+="Ledger syncStatus:"+ledgerStore.getSyncStatus()+"\n";
        result+="Ledger nextStatus:"+ledgerStore.getNextStatus()+"\n";
        //result+="Ledger sql top block:"+ledgerStore.getSqlTopBlock()+"\n";


        //result+=checkBalance(blockchain.getBestBlock())+"\n";
        //result+=checkDelta(blockchain.getBestBlock())+"\n";

        //result+="ledgerSyncBlock: "+EthereumBean.getLedgerSyncBlock()+"\n";

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
