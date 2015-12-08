package stdweb.ethereum;

import org.ethereum.core.*;
import org.ethereum.db.BlockStore;
import org.ethereum.vm.LogInfo;
import org.ethereum.vm.program.ProgramResult;
import org.ethereum.vm.program.invoke.ProgramInvokeFactory;
import stdweb.Core.*;
import stdweb.Entity.LedgerBlock;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;


abstract class ReplayBlock {

    protected final EthereumBean ethereumBean;
    protected final BlockchainImpl blockchain;
    protected final LedgerSyncService ledgerSync;


    //////////////////////////////////////////////////////////////////


//    public static ReplayBlock GENESIS() throws AddressDecodeException, HashDecodeException {
//        ReplayBlock replayBlock = new ReplayBlock(ethe 0);
//        replayBlock.loadGenesis();
//        return replayBlock;
//    }


    private Block block;
    public BigInteger getTotalUncleReward() {
        BigInteger totalUncleReward = BigInteger.ZERO;
        for (BlockHeader blockHeader : this.getBlock().getUncleList()) {
            totalUncleReward = totalUncleReward.add(this.getUncleReward(blockHeader).toBigInteger());
        }
        return totalUncleReward;
    }
    public BigDecimal getUncleReward(BlockHeader uncle) {
        BigDecimal uncleReward;
        uncleReward = new BigDecimal(block.BLOCK_REWARD)
                .multiply(BigDecimal.valueOf(8 + uncle.getNumber() - block.getNumber()).divide(new BigDecimal(8)));
        return uncleReward;
    }
    public BigInteger getBlockFee() {

        BigInteger fee = BigInteger.ZERO;
        for (Transaction tx : block.getTransactionsList()) {
            if (tx.getGasPrice() == null)
                continue;
            fee = fee.add(BigInteger.valueOf(tx.transactionCost()).multiply(new BigInteger(1, tx.getGasPrice())));
        }
        return fee;
    }
    public BigInteger getBlockReward() {
        BigInteger totalBlockReward = Block.BLOCK_REWARD;
        totalBlockReward = totalBlockReward.add(Block.INCLUSION_REWARD.multiply(BigInteger.valueOf(block.getUncleList().size())));
        return totalBlockReward;
    }


    public ReplayBlock(LedgerSyncService _ledgerSync, Block _block) {
        this.ledgerSync=_ledgerSync;
        this.ethereumBean=ledgerSync.getEthereumBean();
        this.blockchain = this.ethereumBean.getBlockchain();
        this.block = _block;
    }

//    public ReplayBlock(LedgerSyncService ledgerSync, long blockNo) {
//
//        this.ethereumBean=ledgerSync.getEthereumBean();
//        this.blockchain = this.ethereumBean.getBlockchain();
//
//        this.block=this.ethereumBean.getBlockchain().getBlockByNumber(blockNo);
//
//        if (block == null)
//            System.out.println("Replayblock ctor. BlockNo not found:" + blockNo);
//    }


    List<TransactionExecutionSummary> summaries = null;

    public void run() throws HashDecodeException, AddressDecodeException {
        if (block == null) {
            return;
        }
        summaries= new ArrayList<>();

        BlockStore blockStore = blockchain.getBlockStore();
        ProgramInvokeFactory programInvokeFactory = blockchain.getProgramInvokeFactory();

        Repository track = blockchain.getRepository();

        Repository snapshot;
        if (block.getNumber() == 0)
            snapshot = track.getSnapshotTo(null);
        else
            snapshot = track.getSnapshotTo(blockchain.getBlockByHash(block.getParentHash()).getStateRoot());

        long totalGasUsed = 0;

        for (Transaction tx : block.getTransactionsList()) {

            TransactionExecutor executor = new TransactionExecutor(tx, block.getCoinbase(),
                    snapshot, blockStore,
                    programInvokeFactory, block, ethereumBean.getListener(), totalGasUsed);

            executor.setLocalCall(false);
            executor.init();
            executor.execute();
            executor.go();

            summaries.add(executor.finalization());

            totalGasUsed += executor.getGasUsed();
            ProgramResult result = executor.getResult();
            long gasRefund = Math.min(result.getFutureRefund(), result.getGasUsed() / 2);

        }

        //printEntries();
    }

    public Block getBlock() {
        return this.block;
    }



}

