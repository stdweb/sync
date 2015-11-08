package stdweb.Core;

import org.ethereum.core.Block;
import org.ethereum.core.BlockchainImpl;
import org.spongycastle.util.encoders.Hex;
import stdweb.ethereum.EthereumBean;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

/**
 * Created by bitledger on 01.11.15.
 */
public class TestBalances {

    public static String checkBalance() throws InterruptedException, SQLException {
        long number = EthereumBean.getBlockchain().getBestBlock().getNumber();
        return checkBalance(number);
    }
    public static  String checkBalance(long number) throws InterruptedException, SQLException {

        LedgerStore ledgerStore = LedgerStore.getLedgerStore(EthereumBean.getListener());
        int sqlTopBlock = ledgerStore.getQuery().getSqlTopBlock();
        Block blockByNumber = EthereumBean.getBlockchain().getBlockByNumber(Math.min(number, sqlTopBlock));
        return checkBalance(blockByNumber);
    }

    public static String checkBalance(Block block) throws InterruptedException, SQLException {

        LedgerStore ledgerStore = LedgerStore.getLedgerStore(EthereumBean.getListener());
        BlockchainImpl blockchain = (BlockchainImpl)EthereumBean.getBlockchain();

        long stopOn = blockchain.getStopOn();
        blockchain.setStopOn(0);
        Thread.sleep(500);
        BigDecimal trieBalance = BlockchainQuery.getTrieBalance(block);
        BigDecimal ledgerBlockBalance = ledgerStore.getQuery().getLedgerBlockBalance(block.getNumber());

        long number = block.getNumber();
        String result="";
        if (trieBalance.equals(ledgerBlockBalance))
            result="Block balance correct:" + number + ": " + Convert2json.BI2ValStr(trieBalance.toBigInteger(), false);
        else {
            result="Block balance incorrect:" + number + ", trie - ledger: " + Convert2json.BI2ValStr(trieBalance.toBigInteger(), false) + " - " + Convert2json.BI2ValStr(ledgerBlockBalance.toBigInteger(), false);
        }

        blockchain.setStopOn(stopOn);

        result+="\n"+"ledger count:"+ledgerStore.getQuery().ledgerCount(block.getNumber())+"\n";
        result+="Coinbase delta:"+ Hex.toHexString(block.getCoinbase())+" -> "+Convert2json.BD2ValStr(BlockchainQuery.getCoinbaseTrieDelta(block),false);

        return result;
    }

    public static String checkAccountsBalance(long blockNumber,boolean checkAll) throws InterruptedException, SQLException {
        return checkAccountsBalance(EthereumBean.getBlockchain().getBlockByNumber(blockNumber), checkAll);
    }



    public static String checkAccountsBalance(Block block,boolean checkAll) throws InterruptedException, SQLException {

        LedgerStore ledgerStore = LedgerStore.getLedgerStore(EthereumBean.getListener());
        BlockchainImpl blockchain = (BlockchainImpl)EthereumBean.getBlockchain();

        long stopOn = blockchain.getStopOn();
        blockchain.setStopOn(0);
        Thread.sleep(500);

        LedgerQuery query = LedgerQuery.getQuery(LedgerStore.getLedgerStore(EthereumBean.getListener()));


        long l1 = System.currentTimeMillis();
        HashMap<LedgerAccount, BigDecimal> balancesOnBlock = query.getLedgerBalancesOnBlock(block,checkAll);
        long l2 = System.currentTimeMillis();
        System.out.println("getLedg balances"+(l2-l1));

        String result="Incorrect balances on block:"+block.getNumber()+"\n\n\n";
        result+=String.format("%-43s%-40s%-40s%-40s%n","Account","Trie","Ledger","Diff");
        for (LedgerAccount acc : balancesOnBlock.keySet())
        {
            BigDecimal trieBalance=acc.getBalance(block);
            BigDecimal ledgBalance=balancesOnBlock.get(acc);


            if (!trieBalance.equals(ledgBalance)){
                result+=String.format("%-43s%-40s%-40s%-40s%n",
                        acc.toString(),
                        Convert2json.BD2ValStr(trieBalance, false),
                        Convert2json.BD2ValStr(ledgBalance, false),
                        Convert2json.BD2ValStr(trieBalance.subtract(ledgBalance), false)
                        );

                //result+="Bl:" + block.getNumber() + ",Balance Incorrect trie - ledger: " + Convert2json.BI2ValStr(trieBalance.toBigInteger(), false) + " - " + Convert2json.BI2ValStr(ledgBalance.toBigInteger(), false);
                result+="\n";
            }
        }

        blockchain.setStopOn(stopOn);
        return result;
    }

    public static String findEmpty(long i, boolean b) throws SQLException {
        LedgerQuery query = LedgerQuery.getQuery(LedgerStore.getLedgerStore(EthereumBean.getListener()));
        List<Long> emptyBlockList = query.getEmptyBlockList(i);

        String ret="";
        long prev=0;
        for (Long block : emptyBlockList)
        {
            if (prev==0 && block==i) {
                prev=block;
                continue;
            }
            if (block!=prev+1) {
                Block blockByNumber = EthereumBean.getBlockchain().getBlockByNumber(block);

                ret += "Ledger empty :" + block + ((blockByNumber==null) ? "Blockchain null" : "")+"\n";
            }

            prev=block;
        }

        return ret;
    }
}
