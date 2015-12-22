package stdweb.ethereum;

import javassist.bytecode.ByteArray;
import org.ethereum.core.*;
import org.ethereum.db.BlockStore;
import org.ethereum.db.ByteArrayWrapper;
import org.ethereum.db.RepositoryImpl;
import org.ethereum.db.RepositoryTrack;
import org.ethereum.vm.LogInfo;
import org.ethereum.vm.program.ProgramResult;
import org.ethereum.vm.program.invoke.ProgramInvokeFactory;
import org.jetbrains.annotations.NotNull;
import stdweb.Core.*;
import stdweb.Entity.LedgerAccount;
import stdweb.Entity.LedgerBlock;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


public abstract class ReplayBlock {

    protected final EthereumBean ethereumBean;
    protected final BlockchainImpl blockchain;
    protected final LedgerSyncService ledgerSync;

    protected boolean blockReplayed=false;
    protected HashMap<byte[],LedgerAccount> accounts        = new HashMap<byte[],LedgerAccount>();
    protected void addAmount(LedgerAccount acc, BigDecimal amount)
    {
        if (accounts.get(acc.getAddress())==null)
            accounts.put(acc.getAddress(),acc);

        acc.setBalance(acc.getBalance().add(amount));
    }

    //////////////////////////////////////////////////////////////////


//    public static ReplayBlock GENESIS() throws AddressDecodeException, HashDecodeException {
//        ReplayBlock replayBlock = new ReplayBlock(ethe 0);
//        replayBlock.loadGenesis();
//        return replayBlock;
//    }


    protected Block block;
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
        blockReplayed=true;
        summaries= new ArrayList<>();

        BlockStore blockStore = blockchain.getBlockStore();
        ProgramInvokeFactory programInvokeFactory = blockchain.getProgramInvokeFactory();

        Repository track = blockchain.getRepository();

        Repository snapshot;
        if (block.getNumber() == 0)
            snapshot = track.getSnapshotTo(null);
        else
            snapshot = track.getSnapshotTo(blockchain.getBlockByHash(block.getParentHash()).getStateRoot());


//        if (snapshot instanceof RepositoryImpl)
//            System.out.println("Impl");
//        else if (snapshot instanceof RepositoryTrack)
//            System.out.println("Track");
//        else
//            System.out.println("smth else");

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


    public boolean isParentOf(@NotNull ReplayBlockWrite replayBlock) {
        return Arrays.equals(this.block.getHash(),replayBlock.block.getParentHash());
    }
    public boolean isChildOf(@NotNull byte[] parenthash) {
        return Arrays.equals(this.block.getParentHash(),parenthash);
    }
    public boolean isChildOf(@NotNull ReplayBlockWrite replayBlock) {
        return Arrays.equals(this.block.getParentHash(),replayBlock.block.getHash());
    }
}

