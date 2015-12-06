package DEL.Ledger_DEL;

import org.ethereum.core.*;
import org.ethereum.crypto.HashUtil;
import org.ethereum.db.BlockStore;
import org.ethereum.util.ByteUtil;
import org.ethereum.vm.program.InternalTransaction;
import org.ethereum.vm.program.ProgramResult;
import org.ethereum.vm.program.invoke.ProgramInvokeFactory;
import org.spongycastle.util.encoders.Hex;
import stdweb.Core.*;
import stdweb.ethereum.EthereumBean_DEL;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static stdweb.Core.Utils.hash_decode;

/**
 * Created by bitledger on 24.09.15.
 */
public class ReplayBlock_DEL {

    BlockHeader header;

    //////////////////////////////////////////////////////////////////

    private static ReplayBlock_DEL currentReplayBlock;

    public static ReplayBlock_DEL GENESIS() throws AddressDecodeException, HashDecodeException {
        ReplayBlock_DEL replayBlock = new ReplayBlock_DEL(0);
        replayBlock.loadGenesis();
        return replayBlock;
    }

//    public static ReplayBlock CURRENT(Block _block) {
//
//        if (currentReplayBlock == null) {
//            Assert.isTrue(_block != null, "replayBlock.block cannot be null.1");
//            currentReplayBlock = new ReplayBlock(_block);
//        } else {
//            Assert.isTrue(currentReplayBlock.block != null, "replayBlock.block cannot be null.2");
//            Assert.isTrue(currentReplayBlock.getBlock().getNumber() == _block.getNumber(), "if replayBlock is not null, then its number must be equal to param");
//        }
//        return currentReplayBlock;
//    }


    private Block block;

    private final List<LedgerEntry> entries = new ArrayList<>();

    public List<LedgerEntry> getLedgerEntries() {
        return entries;
    }


    public void addRewardEntries() {
        LedgerAccount_del coinbase = new LedgerAccount_del(block.getCoinbase());
        HashMap<LedgerAccount_del, EntryType> accounts = new HashMap<>();

        accounts.put(coinbase, EntryType.CoinbaseReward);

        block.getUncleList().forEach(uncle ->
                accounts.put(new LedgerAccount_del(uncle.getCoinbase()), EntryType.UncleReward));

        BigInteger totalBlockReward = Block.BLOCK_REWARD;
        BigDecimal uncleReward = BigDecimal.ZERO;

        // Add extra rewards based on number of uncles
        if (block.getUncleList().size() > 0) {
            for (BlockHeader uncle : block.getUncleList()) {
                uncleReward = getUncleReward(uncle);

                totalBlockReward = totalBlockReward.add(Block.INCLUSION_REWARD);

                LedgerEntry uncleEntry = new LedgerEntry();


                uncleEntry.setAccountDel(new LedgerAccount_del(uncle.getCoinbase()));
                uncleEntry.setTxhash(ByteUtil.ZERO_BYTE_ARRAY);
                uncleEntry.setOffsetAccountDel(LedgerAccount_del.GenesisAccount());
                uncleEntry.setAmount(uncleReward);
                uncleEntry.setBlock(this.block);
                uncleEntry.setBlockNumber(this.block.getNumber());
                uncleEntry.setBlockTimestamp(this.block.getTimestamp());
                uncleEntry.setDepth((byte) 0);
                uncleEntry.setGasUsed(0);
                uncleEntry.setEntryType(EntryType.UncleReward);
                uncleEntry.setFee(BigDecimal.ZERO);
                uncleEntry.setGrossAmount(uncleReward);
                uncleEntry.setExtraData(Hex.toHexString(ByteUtil.ZERO_BYTE_ARRAY));
                uncleEntry.setEntryResult(EntryResult.Ok);

                entries.add(uncleEntry);
            }
        }

        LedgerEntry coinbaseEntry = new LedgerEntry();
        coinbaseEntry.setAccountDel(coinbase);
        coinbaseEntry.setTxhash(ByteUtil.ZERO_BYTE_ARRAY);
        coinbaseEntry.setOffsetAccountDel(LedgerAccount_del.GenesisAccount());
        coinbaseEntry.setAmount(new BigDecimal(totalBlockReward));
        coinbaseEntry.setBlock(this.block);
        coinbaseEntry.setBlockNumber(this.block.getNumber());
        coinbaseEntry.setBlockTimestamp(this.block.getTimestamp());
        coinbaseEntry.setDepth((byte) 0);
        coinbaseEntry.setGasUsed(0);
        coinbaseEntry.setEntryType(EntryType.CoinbaseReward);
        coinbaseEntry.setFee(BigDecimal.ZERO);
        coinbaseEntry.setGrossAmount(coinbaseEntry.getAmount());
        coinbaseEntry.setExtraData(Hex.toHexString(ByteUtil.ZERO_BYTE_ARRAY));
        coinbaseEntry.setEntryResult(EntryResult.Ok);

        entries.add(coinbaseEntry);

        BigDecimal fee = entries.stream().map(x -> x.getFee()).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (!fee.equals(BigDecimal.ZERO)) {
            coinbaseEntry = new LedgerEntry();
            coinbaseEntry.setAccountDel(coinbase);
            coinbaseEntry.setTxhash(ByteUtil.ZERO_BYTE_ARRAY);
            coinbaseEntry.setOffsetAccountDel(LedgerAccount_del.GenesisAccount());
            coinbaseEntry.setAmount(fee);
            coinbaseEntry.setBlock(this.block);
            coinbaseEntry.setBlockNumber(this.block.getNumber());
            coinbaseEntry.setBlockTimestamp(this.block.getTimestamp());
            coinbaseEntry.setDepth((byte) 0);
            coinbaseEntry.setGasUsed(0);
            coinbaseEntry.setEntryType(EntryType.FeeReward);
            coinbaseEntry.setFee(BigDecimal.ZERO);
            coinbaseEntry.setGrossAmount(coinbaseEntry.getAmount());
            coinbaseEntry.setExtraData(Hex.toHexString(ByteUtil.ZERO_BYTE_ARRAY));
            coinbaseEntry.setEntryResult(EntryResult.Ok);

            entries.add(coinbaseEntry);
        }
    }

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

    public void printEntries() {
        for (LedgerEntry entry : entries) {
            System.out.println(entry);
        }
    }

    public void addTxEntries(TransactionExecutionSummary summary) {

        Transaction tx = summary.getTransaction();
        long calcGasUsed = summary.getGasLimit().subtract(summary.getGasLeftover().add(summary.getGasRefund())).longValue();

        addTxEntries(tx, calcGasUsed, summary.getEntryNumber(), summary.isFailed());

        summary.getInternalTransactions()
                .forEach(t -> addTxEntries(t, 0,
                        summary.getEntryNumber(), t.isRejected()));

    }

    public void addTxEntries(Transaction tx, long gasUsed, int _txNumber, boolean isFailed) {

        LedgerEntry ledgerEntrySend = new LedgerEntry();
        LedgerEntry ledgerEntryRecv = new LedgerEntry();

        ledgerEntryRecv.setTx(new Tx(tx));
        ledgerEntrySend.setTx(new Tx(tx));
        //ledgerEntryRecv.receipt=receipt;
        //ledgerEntrySend.receipt=receipt;

        ledgerEntryRecv.setTxNumber(_txNumber);
        ledgerEntrySend.setTxNumber(_txNumber);

        byte[] txhash = (tx instanceof InternalTransaction)
                ? ((InternalTransaction) tx).getParentHash() : tx.getHash();

        ledgerEntrySend.setTxhash(txhash);
        ledgerEntryRecv.setTxhash(txhash);

        ledgerEntrySend.setAccountDel(new LedgerAccount_del(tx.getSender()));
        ledgerEntrySend.setOffsetAccountDel(new LedgerAccount_del(tx.getContractAddress() == null
                ? tx.getReceiveAddress() : tx.getContractAddress()));

        ledgerEntryRecv.setOffsetAccountDel(new LedgerAccount_del(tx.getSender()));
        ledgerEntryRecv.setAccountDel(new LedgerAccount_del(tx.getContractAddress() == null
                ? tx.getReceiveAddress() : tx.getContractAddress()));


        ledgerEntrySend.setAmount(Convert2json.val2BigDec(tx.getValue()).negate());
        ledgerEntryRecv.setAmount(Convert2json.val2BigDec(tx.getValue()));

        EntryResult entryResult = isFailed ? EntryResult.Failed : EntryResult.Ok;
        //EntryResult entryResult= EntryResult.Ok;
        ledgerEntryRecv.setEntryResult(entryResult);
        ledgerEntrySend.setEntryResult(entryResult);

        ledgerEntryRecv.setBlock(this.block);
        ledgerEntrySend.setBlock(this.block);

        ledgerEntryRecv.setBlockNumber(this.block.getNumber());
        ledgerEntrySend.setBlockNumber(this.block.getNumber());

        ledgerEntryRecv.setBlockTimestamp(this.block.getTimestamp());
        ledgerEntrySend.setBlockTimestamp(this.block.getTimestamp());

        //call depth
        if (tx instanceof InternalTransaction) {
            ledgerEntryRecv.setDepth((byte) (((InternalTransaction) tx).getDeep() + 1));
            ledgerEntrySend.setDepth((byte) (((InternalTransaction) tx).getDeep() + 1));
        }
        if (tx instanceof InternalTransaction)
            ledgerEntrySend.setGasUsed(0);
        else {

            ledgerEntrySend.setGasUsed(gasUsed);
        }
        ledgerEntryRecv.setGasUsed(0);
        ledgerEntryRecv.setEntryType(getRecvEntryType(tx, ledgerEntryRecv.getAccountDel()));
        ledgerEntrySend.setEntryType(getSendEntryType(tx, ledgerEntrySend.getAccountDel(), ledgerEntrySend.getOffsetAccountDel()));
        ledgerEntryRecv.setFee(BigDecimal.valueOf(0));
        if (tx instanceof InternalTransaction)
            ledgerEntrySend.setFee(BigDecimal.valueOf(0));
        else
            ledgerEntrySend.setFee(new BigDecimal((new BigInteger(1, tx.getGasPrice())).multiply(BigInteger.valueOf(ledgerEntrySend.getGasUsed()))));

        ledgerEntryRecv.setGrossAmount(ledgerEntryRecv.getAmount());
        ledgerEntrySend.setGrossAmount(ledgerEntrySend.getAmount().subtract(ledgerEntrySend.getFee()));

        byte[] data = (tx.getData() == null) ? ByteUtil.ZERO_BYTE_ARRAY : tx.getData();
        ledgerEntryRecv.setExtraData(Hex.toHexString(data));
        ledgerEntrySend.setExtraData(Hex.toHexString(data));

        entries.add(ledgerEntrySend);
        entries.add(ledgerEntryRecv);
    }

    private EntryType getSendEntryType(Transaction tx, LedgerAccount_del sendAcc, LedgerAccount_del recvAcc) {

        if (tx.isContractCreation())
            return EntryType.ContractCreation;
        if (recvAcc.isContract())
            return EntryType.Call;
        else
            return EntryType.Send;
    }
    private EntryType getRecvEntryType(Transaction tx, LedgerAccount_del account) {

        if (tx.isContractCreation())
            return EntryType.ContractCreated;

        if (account.isContract())
            return EntryType.CallReceive;
        else
            return EntryType.Receive;
    }

    public ReplayBlock_DEL(Block _block) {
        // this.listener=_listener;
        //this.ethereum=_listener.getEthereum();
        this.block = _block;
    }


    public ReplayBlock_DEL(long blockNo) {
        // this.listener=_listener;
        //this.ethereum=_listener.getEthereum();

        this.block = EthereumBean_DEL.getBlockByNumber(blockNo);
        ;
        if (block == null)
            System.out.println("Replayblock ctor. BlockNo not found:" + blockNo);

    }

    private void loadGenesis() throws AddressDecodeException, HashDecodeException {

        org.ethereum.core.Repository snapshot = EthereumBean_DEL.getRepositoryImpl().getSnapshotTo(hash_decode("d7f8974fb5ac78d9ac099b9ad5018bedc2ce0a72dad1827a1709da30580f0544"));
        Block block = EthereumBean_DEL.getBlockByNumber(0);

        Set<byte[]> accountsKeys = snapshot.getAccountsKeys();

        int entryNo = 0;
        for (byte[] account : accountsKeys) {

            LedgerEntry entry = new LedgerEntry();
            ++entryNo;

            BigDecimal balance = new BigDecimal(snapshot.getBalance(account));

            System.out.println("acc " + entryNo + ":" + Hex.toHexString(account) + " = " + Convert2json.BD2ValStr(balance, false));

            entry.setTxNumber(entryNo);
            entry.setAccountDel(new LedgerAccount_del(account));
            entry.setTxhash(HashUtil.EMPTY_DATA_HASH);
            entry.setOffsetAccountDel(LedgerAccount_del.GenesisAccount());
            entry.setAmount(balance);
            entry.setBlock(this.block);
            entry.setBlockNumber(this.block.getNumber());
            entry.setBlockTimestamp(1438269973000L);
            entry.setDepth((byte) 0);
            entry.setGasUsed(0);
            entry.setEntryType(EntryType.Genesis);
            entry.setFee(BigDecimal.ZERO);
            entry.setGrossAmount(entry.getAmount());
            entry.setExtraData(Hex.toHexString(Genesis.getInstance().getExtraData()));
            entry.setEntryResult(EntryResult.Ok);

            entries.add(entry);
        }
    }

    public void run() throws HashDecodeException, AddressDecodeException {
        if (block == null) {

            return;
        }

        if (block.getNumber() == 0) {
            loadGenesis();
            return;
        }

        BlockchainImpl blockchain = EthereumBean_DEL.getBlockchainImpl();

        BlockStore blockStore = blockchain.getBlockStore();
        ProgramInvokeFactory programInvokeFactory = blockchain.getProgramInvokeFactory();

        Repository track = blockchain.getRepository();

        Repository snapshot;
        if (block.getNumber() == 0)
            snapshot = track.getSnapshotTo(null);
        else
            snapshot = track.getSnapshotTo(blockchain.getBlockByHash(block.getParentHash()).getStateRoot());

        int txNumber = 0;
        long totalGasUsed = 0;

        for (Transaction tx : block.getTransactionsList()) {

            TransactionExecutor executor = new TransactionExecutor(tx, block.getCoinbase(),
                    snapshot, blockStore,
                    programInvokeFactory, block, EthereumBean_DEL.getListener(), totalGasUsed);

            executor.setLocalCall(false);
            executor.init();
            executor.execute();
            executor.go();

            executor.finalization();

            totalGasUsed += executor.getGasUsed();
            ProgramResult result = executor.getResult();
            long gasRefund = Math.min(result.getFutureRefund(), result.getGasUsed() / 2);

            ++txNumber;
            boolean isFailed = result.getException() != null;


            //this.addTxEntries(tx,executor.getResult().getGasUsed(), txNumber,isFailed);
            final int f_txNumber = txNumber;


        }

        //printEntries();
    }

    public Block getBlock() {
        return this.block;
    }

    public static void setNullCurrent() {
        currentReplayBlock = null;
    }

    //    public BigInteger getCoinbaseDelta()
//    {
//        byte[] coinbase = block.getCoinbase();
//
//        return getAccountDelta(coinbase);
//
//    }

//    public BigInteger getUnclesDelta()
//    {
//        BigInteger delta = BigInteger.valueOf(0);
//        for (BlockHeader uncleHeader : block.getUncleList()) {
//            BigInteger accountDelta = getAccountDelta(uncleHeader.getCoinbase());
//            delta=delta.add(accountDelta);
//        }
//        return delta;
//    }

//    public BigInteger getAccountDelta(byte[] coinbase) {
//
//        BlockchainImpl blockchain = (BlockchainImpl) ethereum.getBlockchainImpl();
//        Repository track = blockchain.getRepository();
//
//        Repository snapshot = track.getSnapshotTo(block.getStateRoot());
//        BigInteger balance = snapshot.getBalance(coinbase);
//
//        Block blockPrev = blockchain.getBlockByHash(block.getParentHash());
//
//        //block.getTransactionsList().get(block.getTransactionsList().size()-1)
//        BlockHeader header = block.getHeader();
//
//        Repository snapshotPrev = track.getSnapshotTo(blockPrev.getStateRoot());
//        BigInteger balancePrev = snapshotPrev.getBalance(coinbase);
//
//        if (balancePrev==null)
//            return  balance;
//        else
//            return  balance.subtract(balancePrev);
//    }
}

