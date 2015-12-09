package stdweb.ethereum;

import org.ethereum.core.Block;
import org.ethereum.core.TransactionExecutionSummary;
import org.ethereum.facade.Ethereum;
import org.ethereum.listener.EthereumListenerAdapter;
import stdweb.Core.SyncStatus;

import java.util.List;

public class EthereumListener extends EthereumListenerAdapter {

    Ethereum ethereum;

    LedgerSyncService ledgerSync;

    public Ethereum getEthereum()
    {
        return ethereum;
    }


    private boolean syncDone = false;

    public EthereumListener(Ethereum ethereum) {
        System.out.println("listener created");
        this.ethereum = ethereum;
    }

    @Override
    public void onBlockExecuted(Block block,List<TransactionExecutionSummary> summaries)
    {
        System.out.print("on block exec"+block.getNumber());
        if (ledgerSync==null)
        {
            System.out.println(" - ledgSync is null!!!\n");
            return;
        }
        if (ledgerSync.getLock().isLocked())
            System.out.println("LedgerSync is locked");

        if (ledgerSync.getSyncStatus()== SyncStatus.onBlockSync) {
                ledgerSync.saveBlockData(block, summaries);
        }
    }




//    @Override
//    public void onBlock(Block block, List<TransactionReceipt> receipts) {
//        //System.out.println("on block "+block.getNumber());
//        //ReplayBlock current = ReplayBlock.CURRENT(block);
//        //ledgerSync.write(current);
////        if (sqlDb.getSyncStatus()== SyncStatus.onBlockSync)
////            try {
////                //ledgerStore.deleteBlocksFrom(block.getNumber());
////                sqlDb.write(ReplayBlock.CURRENT(block));
////                if (block.getNumber() % 1 == 0)
////                    System.out.println("On Block Ledger_DEL  insert:"+ block.getNumber());
////            } catch (SQLException e) {
////                e.printStackTrace();
////            }
//
//    }

//    @Override
//    public void onTransactionExecuted(TransactionExecutionSummary summary)
//    {
//
//        SqlDb sqlDb = SqlDb.getSqlDb();
//        if (sqlDb.getSyncStatus()== SyncStatus.onBlockSync
//                || sqlDb.getSyncStatus()== SyncStatus.bulkLoading
//                || sqlDb.getSyncStatus()== SyncStatus.SingleInsert) {
//
//            ReplayBlock replayBlock = ReplayBlock.CURRENT(summary.getBlock());
//            replayBlock.addTxEntries(summary);
//
//            //System.out.println("onTx executed:" + summary.toString());
//        }
//    }

//    @Override
//    public void onBlock(Block block, List<TransactionReceipt> receipts) {
//        SqlDb sqlDb = SqlDb.getSqlDb();
//
//        if (sqlDb.getSyncStatus()== SyncStatus.onBlockSync)
//            try {
//                //ledgerStore.deleteBlocksFrom(block.getNumber());
//                sqlDb.write(ReplayBlock.CURRENT(block));
//                if (block.getNumber() % 1 == 0)
//                    System.out.println("On Block Ledger_DEL  insert:"+ block.getNumber());
//            } catch (SQLException e) {
//                e.printStackTrace();
//            }
//        else
//            System.out.println("Block:"+ block.getNumber());
//    }
//
//    @Override
//    public void onTransactionExecuted(TransactionExecutionSummary summary)
//    {
//
//        SqlDb sqlDb = SqlDb.getSqlDb();
//        if (sqlDb.getSyncStatus()== SyncStatus.onBlockSync
//                || sqlDb.getSyncStatus()== SyncStatus.bulkLoading
//                || sqlDb.getSyncStatus()== SyncStatus.SingleInsert) {
//
//            ReplayBlock replayBlock = ReplayBlock.CURRENT(summary.getBlock());
//            replayBlock.addTxEntries(summary);
//
//            //System.out.println("onTx executed:" + summary.toString());
//        }
//    }

    /**
     *  Mark the fact that you are touching
     *  the head of the chain
     */
    @Override
    public void onSyncDone() {

        System.out.println(" ** SYNC DONE ** ");
        syncDone = true;
    }

    public void setLedgerSync(LedgerSyncService ledgerSync) {
        this.ledgerSync = ledgerSync;
    }

    public LedgerSyncService getLedgerSync() {
        return ledgerSync;
    }

    /**
     * Just small method to estimate total power off all miners on the net
     * @param block
     */
//    private void calcNetHashRate(Block block){
//
//        if ( block.getNumber() > 1000){
//
//            long avgTime = 1;
//            long cumTimeDiff = 0;
//            Block currBlock = block;
//            for (int i=0; i < 1000; ++i){
//
//                Block parent = ethereum.getBlockchain().getBlockByHash(currBlock.getParentHash());
//                long diff = currBlock.getTimestamp() - parent.getTimestamp();
//                cumTimeDiff += Math.abs(diff);
//                currBlock = parent;
//            }
//
//            avgTime = cumTimeDiff / 1000;
//
//            BigInteger netHashRate = block.getDifficultyBI().divide(BIUtil.toBI(avgTime));
//            double hashRate = netHashRate.divide(new BigInteger("1000000000")).doubleValue();
//
//            System.out.println("Net hash rate: " + hashRate + " GH/s");
//        }
//
//    }

}
