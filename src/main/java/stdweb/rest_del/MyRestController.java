package stdweb.rest_del;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import DEL.Ledger_DEL.EthereumBean_DEL;

//@RestController
public class MyRestController {

    private static final Logger logger = LoggerFactory.getLogger("rest");
    @Autowired
    EthereumBean_DEL ethereumBean;



//    @RequestMapping(value = "/test",method = GET, produces = APPLICATION_JSON_VALUE)
//    @ResponseBody
//    public String test() {
//        return String.valueOf(System.currentTimeMillis());
//    }

//    @RequestMapping(value = "/error")
//    public String error() {
//        return "Error handling";
//    }

//
//    public String checkDelta(Block block) throws InterruptedException, SQLException {
//        LedgerStore ledgerStore = LedgerStore.getLedgerStore(ethereumBean.getListener());
//        BlockchainImpl blockchain = (BlockchainImpl)ethereumBean.getEthereum().getBlockchainImpl();
//
//        long stopOn = blockchain.getStopOn();
//        blockchain.setStopOn(0);
//        Thread.sleep(500);
//        BigDecimal trieDelta = BlockchainQuery.getTrieDelta(block);
//        BigDecimal ledgerBlockDelta = ledgerStore.getQuery().getLedgerBlockDelta(block);
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



//    @RequestMapping(value = "/difficulty", method = GET, produces = APPLICATION_JSON_VALUE)
//    @ResponseBody
//    public String getDifficulty() throws IOException {
//
//        Ethereum ethereum = ethereumBean.getEthereum();
//        BlockchainImpl blockchain = (BlockchainImpl) ethereum.getBlockchainImpl();
//
//        return ethereumBean.getDifficulty();
//    }



//


//    @RequestMapping(value = "/balance/{blockId}", method = GET, produces = APPLICATION_JSON_VALUE)
//    @ResponseBody
//    public String getBalance(@PathVariable String blockId) throws IOException, SQLException, HashDecodeException {
//
//        long t1=System.currentTimeMillis();
//        return ethereumBean.getBalance(blockId);
//    }



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



}
