package stdweb.ethereum;

import org.ethereum.core.Block;
import org.ethereum.core.TransactionExecutionSummary;
import org.ethereum.core.TransactionReceipt;
import org.ethereum.facade.Ethereum;
import org.ethereum.listener.EthereumListenerAdapter;
import org.ethereum.util.BIUtil;
import stdweb.Core.LedgerStore;
import stdweb.Core.SyncStatus;

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.List;

public class EthereumListener extends EthereumListenerAdapter {



    Ethereum ethereum;

    public Ethereum getEthereum()
    {
        return ethereum;
    }


    private boolean syncDone = false;

    public EthereumListener(Ethereum ethereum) {
        this.ethereum = ethereum;

    }



    @Override
    public void onBlock(Block block, List<TransactionReceipt> receipts) {
        LedgerStore ledgerStore = LedgerStore.getLedgerStore(this);
        if (ledgerStore.getSyncStatus()== SyncStatus.onBlockSync)
            try {
                ledgerStore.insertBlock(block.getNumber());
                if (block.getNumber() % 1 == 0)
                    System.out.println("On Block Ledger  insert:" + block.getNumber());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        else
            System.out.println("Block:"+ block.getNumber());
    }

    @Override
    public void onTransactionExecuted(TransactionExecutionSummary summary)
    {
        //BigInteger gasUsed = summary.getGasUsed();
        //return;

//        if (Hex.toHexString(summary.getTransactionHash()).equals("275de8f52e08e8f66c7d21900d9ee8bedb1114d2eb82706a7a7e65f6d7e4b745"))
//        {
//            for ( InternalTransaction tx:summary.getInternalTransactions())
//            {
//                System.out.println("---------------------listener>");
//                String sender=Hex.toHexString(tx.getSender());
//                String receiver=Hex.toHexString(tx.getReceiveAddress());
//                String valStr=Convert2json.BI2ValStr(tx);
//                System.out.println(sender+" -> "+receiver+" := "+valStr);
//                System.out.println("<---------------------listener");
//
//            }
//        }
    }

    /**
     *  Mark the fact that you are touching
     *  the head of the chain
     */
    @Override
    public void onSyncDone() {

        System.out.println(" ** SYNC DONE ** ");
        syncDone = true;
    }

    /**
     * Just small method to estimate total power off all miners on the net
     * @param block
     */
    private void calcNetHashRate(Block block){

        if ( block.getNumber() > 1000){

            long avgTime = 1;
            long cumTimeDiff = 0;
            Block currBlock = block;
            for (int i=0; i < 1000; ++i){

                Block parent = ethereum.getBlockchain().getBlockByHash(currBlock.getParentHash());
                long diff = currBlock.getTimestamp() - parent.getTimestamp();
                cumTimeDiff += Math.abs(diff);
                currBlock = parent;
            }

            avgTime = cumTimeDiff / 1000;

            BigInteger netHashRate = block.getDifficultyBI().divide(BIUtil.toBI(avgTime));
            double hashRate = netHashRate.divide(new BigInteger("1000000000")).doubleValue();

            System.out.println("Net hash rate: " + hashRate + " GH/s");
        }

    }

}
