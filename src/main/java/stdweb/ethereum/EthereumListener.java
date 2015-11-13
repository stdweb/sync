package stdweb.ethereum;

import org.ethereum.core.Block;
import org.ethereum.core.TransactionExecutionSummary;
import org.ethereum.core.TransactionReceipt;
import org.ethereum.facade.Ethereum;
import org.ethereum.listener.EthereumListenerAdapter;
import org.ethereum.util.BIUtil;
import stdweb.Ledger.LedgerStore;
import stdweb.Ledger.ReplayBlock;
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
        LedgerStore ledgerStore = LedgerStore.getLedgerStore();

        if (ledgerStore.getSyncStatus()== SyncStatus.onBlockSync)
            try {
                //ledgerStore.deleteBlocksFrom(block.getNumber());
                ledgerStore.write(ReplayBlock.CURRENT(block));
                if (block.getNumber() % 1 == 0)
                    System.out.println("On Block Ledger  insert:"+ block.getNumber());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        else
            System.out.println("Block:"+ block.getNumber());
    }

    @Override
    public void onTransactionExecuted(TransactionExecutionSummary summary)
    {

        LedgerStore ledgerStore = LedgerStore.getLedgerStore();
        if (ledgerStore.getSyncStatus()== SyncStatus.onBlockSync
                || ledgerStore.getSyncStatus()== SyncStatus.bulkLoading
                || ledgerStore.getSyncStatus()== SyncStatus.SingleInsert) {

            ReplayBlock replayBlock = ReplayBlock.CURRENT(summary.getBlock());
            replayBlock.addTxEntries(summary);

            //System.out.println("onTx executed:" + summary.toString());
        }
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
