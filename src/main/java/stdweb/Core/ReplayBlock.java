package stdweb.Core;

import org.ethereum.core.*;
import org.ethereum.crypto.HashUtil;
import org.ethereum.db.BlockStore;
import org.ethereum.db.RepositoryImpl;
import org.ethereum.facade.Ethereum;
import org.ethereum.util.ByteUtil;
import org.ethereum.vm.program.InternalTransaction;
import org.ethereum.vm.program.ProgramResult;
import org.ethereum.vm.program.invoke.ProgramInvokeFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.util.Assert;
import stdweb.ethereum.EthereumBean;
import stdweb.ethereum.EthereumListener;

import javax.transaction.NotSupportedException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Created by bitledger on 24.09.15.
 */
public class ReplayBlock {

    private EthereumListener listener;
    private Ethereum ethereum;

    private static ReplayBlock currentReplayBlock;

    public static ReplayBlock GENESIS()
    {
        ReplayBlock replayBlock = new ReplayBlock(EthereumBean.getListener(), 0);
        replayBlock.loadGenesis();
        return replayBlock;
    }
    public static ReplayBlock CURRENT(Block _block)  {

        if (currentReplayBlock==null) {
            Assert.isTrue(_block!=null,"replayBlock.block cannot be null.1");
            currentReplayBlock = new ReplayBlock(EthereumBean.getListener(), _block);
        }
        else {
            Assert.isTrue(currentReplayBlock.block!=null,"replayBlock.block cannot be null.2");
            Assert.isTrue(currentReplayBlock.getBlock().getNumber() == _block.getNumber(), "if replayBlock is not null, then its number must be equal to param");
        }

        return currentReplayBlock;
    }

    public Ethereum getEthereum()
    {
        return ethereum;
    }
    private Block block;

    private final List<LedgerEntry> entries=new ArrayList<>();

    public List<LedgerEntry> getLedgerEntries()
    {
        return entries;
    }

    public void addRewardEntries()
    {
        LedgerAccount coinbase=new LedgerAccount(block.getCoinbase());
        HashMap<LedgerAccount,EntryType> accounts=new HashMap<>();

        accounts.put(coinbase, EntryType.CoinbaseReward);

        block.getUncleList().forEach(uncle ->
                accounts.put(new LedgerAccount(uncle.getCoinbase()), EntryType.UncleReward));

        BigInteger totalBlockReward = Block.BLOCK_REWARD;
        BigDecimal uncleReward=BigDecimal.ZERO;

        // Add extra rewards based on number of uncles
        if (block.getUncleList().size() > 0) {
            for (BlockHeader uncle : block.getUncleList()) {
                uncleReward = getUncleReward(uncle);

                totalBlockReward = totalBlockReward.add(Block.INCLUSION_REWARD);

                LedgerEntry uncleEntry=new LedgerEntry();
                uncleEntry.Account=new LedgerAccount(uncle.getCoinbase());
                uncleEntry.txhash= ByteUtil.ZERO_BYTE_ARRAY;
                uncleEntry.offsetAccount=LedgerAccount.GenesisAccount();
                uncleEntry.amount=uncleReward;
                uncleEntry.block=this.block;
                uncleEntry.blockNo=this.block.getNumber();
                uncleEntry.blockTimestamp=this.block.getTimestamp();
                uncleEntry.depth=0;
                uncleEntry.gasUsed=0;
                uncleEntry.entryType=EntryType.UncleReward;
                uncleEntry.fee= BigDecimal.ZERO;
                uncleEntry.grossAmount=uncleReward;
                uncleEntry.extraData= Hex.toHexString(ByteUtil.ZERO_BYTE_ARRAY);
                uncleEntry.entryResult=EntryResult.Ok;

                entries.add(uncleEntry);
            }
        }

        LedgerEntry coinbaseEntry=new LedgerEntry();
        coinbaseEntry.Account=coinbase;
        coinbaseEntry.txhash= ByteUtil.ZERO_BYTE_ARRAY;
        coinbaseEntry.offsetAccount=LedgerAccount.GenesisAccount();
        coinbaseEntry.amount=new BigDecimal( totalBlockReward);
        coinbaseEntry.block=this.block;
        coinbaseEntry.blockNo=this.block.getNumber();
        coinbaseEntry.blockTimestamp=this.block.getTimestamp();
        coinbaseEntry.depth=0;
        coinbaseEntry.gasUsed=0;
        coinbaseEntry.entryType=EntryType.CoinbaseReward;
        coinbaseEntry.fee= BigDecimal.ZERO;
        coinbaseEntry.grossAmount=coinbaseEntry.amount;
        coinbaseEntry.extraData= Hex.toHexString(ByteUtil.ZERO_BYTE_ARRAY);
        coinbaseEntry.entryResult=EntryResult.Ok;

        entries.add(coinbaseEntry);

        BigDecimal fee = entries.stream().map( x -> x.fee).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (!fee.equals(BigDecimal.ZERO))
        {
            coinbaseEntry = new LedgerEntry();
            coinbaseEntry.Account = coinbase;
            coinbaseEntry.txhash = ByteUtil.ZERO_BYTE_ARRAY;
            coinbaseEntry.offsetAccount = LedgerAccount.GenesisAccount();
            coinbaseEntry.amount = fee;
            coinbaseEntry.block = this.block;
            coinbaseEntry.blockNo = this.block.getNumber();
            coinbaseEntry.blockTimestamp = this.block.getTimestamp();
            coinbaseEntry.depth = 0;
            coinbaseEntry.gasUsed = 0;
            coinbaseEntry.entryType = EntryType.FeeReward;
            coinbaseEntry.fee = BigDecimal.ZERO;
            coinbaseEntry.grossAmount = coinbaseEntry.amount;
            coinbaseEntry.extraData = Hex.toHexString(ByteUtil.ZERO_BYTE_ARRAY);
            coinbaseEntry.entryResult=EntryResult.Ok;

            entries.add(coinbaseEntry);
        }
    }

    public   BigInteger getTotalUncleReward() {
        BigInteger totalUncleReward=BigInteger.ZERO;
        for (BlockHeader blockHeader : this.getBlock().getUncleList()) {
            totalUncleReward=totalUncleReward.add(this.getUncleReward(blockHeader).toBigInteger());
        }
        return totalUncleReward;
    }
    public BigDecimal getUncleReward(BlockHeader uncle) {
        BigDecimal uncleReward;
        uncleReward= new BigDecimal(block.BLOCK_REWARD)
                .multiply(BigDecimal.valueOf(8 + uncle.getNumber() - block.getNumber()).divide(new BigDecimal(8)));
        return uncleReward;
    }

    public BigInteger getBlockFee() {

        BigInteger fee = BigInteger.ZERO;
        for (Transaction tx : block.getTransactionsList()) {
            if (tx.getGasPrice() == null)
                continue;
            fee = fee.add(BigInteger.valueOf(tx.transactionCost()) .multiply (new BigInteger(1, tx.getGasPrice())));
        }
        return  fee;
    }

    public BigInteger getBlockReward() {
        BigInteger totalBlockReward = Block.BLOCK_REWARD;
        totalBlockReward = totalBlockReward.add(Block.INCLUSION_REWARD.multiply(BigInteger.valueOf(block.getUncleList().size() )));
        return totalBlockReward;
    }

    public void printEntries()
    {
        for (LedgerEntry entry : entries)
        {
            System.out.println(entry);
        }
    }

    public void addTxEntries(TransactionExecutionSummary summary) {

        Transaction tx = summary.getTransaction();

        //BigInteger gasRefund = summary.getGasRefund();
        //BigInteger gasUsed = summary.getGasUsed();

        long calcGasUsed = summary.getGasLimit().subtract(summary.getGasLeftover().add(summary.getGasRefund())).longValue();

        addTxEntries(tx,calcGasUsed,summary.getEntryNumber(),summary.isFailed());

        summary.getInternalTransactions()
                .forEach(t -> addTxEntries(t, 0,
                        summary.getEntryNumber(),t.isRejected()));

    }
    //public void addTxEntries(Transaction tx, TransactionExecutor executor, int entryNo)
    public void addTxEntries(Transaction tx, long gasUsed, int _txNumber, boolean isFailed)
    {

        LedgerEntry ledgerEntrySend = new LedgerEntry();
        LedgerEntry ledgerEntryRecv = new LedgerEntry();


        ledgerEntryRecv.tx=tx;
        ledgerEntrySend.tx=tx;
        //ledgerEntryRecv.receipt=receipt;
        //ledgerEntrySend.receipt=receipt;

        ledgerEntryRecv.txNumber =_txNumber;
        ledgerEntrySend.txNumber =_txNumber;

        byte[] txhash=(tx instanceof InternalTransaction)
                ? ((InternalTransaction) tx).getParentHash() : tx.getHash();

        ledgerEntrySend.txhash=txhash;
        ledgerEntryRecv.txhash=txhash;

        ledgerEntrySend.Account=new LedgerAccount(tx.getSender());
        ledgerEntrySend.offsetAccount=new LedgerAccount(tx.getContractAddress()==null
                        ? tx.getReceiveAddress() : tx.getContractAddress());

        ledgerEntryRecv.offsetAccount=new LedgerAccount(tx.getSender());
        ledgerEntryRecv.Account=new LedgerAccount(tx.getContractAddress()==null
                ? tx.getReceiveAddress() : tx.getContractAddress());


        ledgerEntrySend.amount = Convert2json.val2BigDec(tx.getValue()).negate();
        ledgerEntryRecv.amount = Convert2json.val2BigDec(tx.getValue());

        EntryResult entryResult=isFailed ? EntryResult.Failed : EntryResult.Ok;
        //EntryResult entryResult= EntryResult.Ok;
        ledgerEntryRecv.entryResult=entryResult;
        ledgerEntrySend.entryResult=entryResult;


        ledgerEntryRecv.block=this.block;
        ledgerEntrySend.block=this.block;

        ledgerEntryRecv.blockNo=this.block.getNumber();
        ledgerEntrySend.blockNo=this.block.getNumber();

        ledgerEntryRecv.blockTimestamp=this.block.getTimestamp();
        ledgerEntrySend.blockTimestamp=this.block.getTimestamp();

        //call depth
        if (tx instanceof InternalTransaction)
        {
            ledgerEntryRecv.depth=(byte)(((InternalTransaction) tx).getDeep()+1);
            ledgerEntrySend.depth=(byte)(((InternalTransaction) tx).getDeep()+1);
        }

        if (tx instanceof InternalTransaction)
            ledgerEntrySend.gasUsed=0;
        else {

            ledgerEntrySend.gasUsed = gasUsed;
        }

        ledgerEntryRecv.gasUsed=0;

        ledgerEntryRecv.entryType=getRecvEntryType(tx,ledgerEntryRecv.Account);
        ledgerEntrySend.entryType=getSendEntryType(tx,ledgerEntrySend.Account,ledgerEntrySend.offsetAccount);

        ledgerEntryRecv.fee= BigDecimal.valueOf(0);

        if (tx instanceof InternalTransaction)
            ledgerEntrySend.fee= BigDecimal.valueOf(0);
        else
            ledgerEntrySend.fee=new BigDecimal((new BigInteger(1, tx.getGasPrice())).multiply(BigInteger.valueOf(ledgerEntrySend.gasUsed)));

//        if (isFailed)
//        {
//            ledgerEntryRecv.grossAmount=BigDecimal.ZERO;
//            ledgerEntrySend.grossAmount=BigDecimal.ZERO.subtract(ledgerEntrySend.fee);
//        }
//        else {
            ledgerEntryRecv.grossAmount = ledgerEntryRecv.amount;
            ledgerEntrySend.grossAmount = ledgerEntrySend.amount.subtract(ledgerEntrySend.fee);
        //}

        byte[] data = (tx.getData()==null)? ByteUtil.ZERO_BYTE_ARRAY : tx.getData();
        ledgerEntryRecv.extraData= Hex.toHexString(data);
        ledgerEntrySend.extraData=Hex.toHexString(data);

        entries.add(ledgerEntrySend);
        entries.add(ledgerEntryRecv);

    }

    private EntryType getSendEntryType(Transaction tx,LedgerAccount sendAcc,LedgerAccount recvAcc) {

        //EntryType entry_type=

        //LedgerAccount recvAcc = new LedgerAccount(tx.getReceiveAddress(),tx.getContractAddress());

        if (tx.isContractCreation())
            return EntryType.ContractCreation;

//        if (tx instanceof InternalTransaction)
//            return EntryType.InternalCall;

        if (recvAcc.isContract())
            return EntryType.Call;
        else
            return  EntryType.Send;


        //return entry_type;
    }

    private EntryType getRecvEntryType(Transaction tx,LedgerAccount account) {

        if (tx.isContractCreation())
            return EntryType.ContractCreated;


        if (account.isContract())
            return EntryType.CallReceive;
        else
            return EntryType.Receive;

//        EntryType entry_type;
//        if (tx instanceof InternalTransaction)
//            return EntryType.CallReceive;
//
//        return entry_type;
    }

    public ReplayBlock(EthereumListener _listener, Block _block) {
        this.listener=_listener;
        this.ethereum=_listener.getEthereum();
        this.block=_block;
    }

//    public ReplayBlock(EthereumListener _listener) {
//        this.listener=_listener;
//        this.ethereum=_listener.getEthereum();
//    }

    public ReplayBlock(EthereumListener _listener, long blockNo) {
        this.listener=_listener;
        this.ethereum=_listener.getEthereum();
        this.block = ethereum.getBlockchain().getBlockByNumber(blockNo);
        if (block==null)
            System.out.println("Replayblock ctor. BlockNo not found:"+blockNo);

    }

    public ReplayBlock(EthereumListener _listener, byte[] blockHash) {
        this.listener=_listener;
        this.ethereum=_listener.getEthereum();
        this.block=ethereum.getBlockchain().getBlockByHash(blockHash);;
    }

    public BigInteger getCoinbaseDelta()
    {
        byte[] coinbase = block.getCoinbase();

        return getAccountDelta(coinbase);

    }

    public BigInteger getUnclesDelta()
    {
        BigInteger delta = BigInteger.valueOf(0);
        for (BlockHeader uncleHeader : block.getUncleList()) {
            BigInteger accountDelta = getAccountDelta(uncleHeader.getCoinbase());
            delta=delta.add(accountDelta);
        }
        return delta;
    }

    public BigInteger getAccountDelta(byte[] coinbase) {

        BlockchainImpl blockchain = (BlockchainImpl) ethereum.getBlockchain();
        Repository track = blockchain.getRepository();

        Repository snapshot = track.getSnapshotTo(block.getStateRoot());
        BigInteger balance = snapshot.getBalance(coinbase);

        Block blockPrev = blockchain.getBlockByHash(block.getParentHash());

        //block.getTransactionsList().get(block.getTransactionsList().size()-1)
        BlockHeader header = block.getHeader();

        Repository snapshotPrev = track.getSnapshotTo(blockPrev.getStateRoot());
        BigInteger balancePrev = snapshotPrev.getBalance(coinbase);

        if (balancePrev==null)
            return  balance;
        else
            return  balance.subtract(balancePrev);
    }


    private void loadGenesis()  {

        org.ethereum.core.Repository snapshot = ((RepositoryImpl) ethereum.getRepository()).getSnapshotTo(Hex.decode("d7f8974fb5ac78d9ac099b9ad5018bedc2ce0a72dad1827a1709da30580f0544"));
        Block block = ethereum.getBlockchain().getBlockByNumber(0);

        Set<byte[]> accountsKeys = snapshot.getAccountsKeys();

        int entryNo=0;
        for (byte[] account : accountsKeys)
        {

            LedgerEntry entry = new LedgerEntry();
            ++entryNo;

            BigDecimal balance = new BigDecimal(snapshot.getBalance(account));

            System.out.println("acc "+entryNo+":"+Hex.toHexString(account)+" = "+Convert2json.BD2ValStr(balance,false));

            entry.txNumber =entryNo;
            entry.Account=new LedgerAccount(account);
            entry.txhash= HashUtil.EMPTY_DATA_HASH;
            entry.offsetAccount=LedgerAccount.GenesisAccount();
            entry.amount=balance;
            entry.block=this.block;
            entry.blockNo=this.block.getNumber();
            entry.blockTimestamp=1438269973000L;
            entry.depth=0;
            entry.gasUsed=0;
            entry.entryType=EntryType.Genesis;
            entry.fee= BigDecimal.ZERO;
            entry.grossAmount=entry.amount;
            entry.extraData= Hex.toHexString(Genesis.getInstance().getExtraData());
            entry.entryResult=EntryResult.Ok;

            entries.add(entry);
        }
    }

    public void   run()  {
        if (block==null) {

            return;
        }

        if (block.getNumber()==0) {
            loadGenesis();
            return;
        }

        BlockchainImpl blockchain = (BlockchainImpl) ethereum.getBlockchain();

        BlockStore blockStore = blockchain.getBlockStore();
        ProgramInvokeFactory programInvokeFactory = blockchain.getProgramInvokeFactory();

        Repository track = blockchain.getRepository();

        Repository snapshot;
        if (block.getNumber()==0)
            snapshot=track.getSnapshotTo(null);
        else
            snapshot = track.getSnapshotTo(blockchain.getBlockByHash(block.getParentHash()).getStateRoot());

        int txNumber=0;
        long totalGasUsed = 0;

        for (Transaction tx : block.getTransactionsList()) {

            TransactionExecutor executor = new TransactionExecutor(tx, block.getCoinbase(),
                    snapshot, blockStore,
                    programInvokeFactory, block, listener, totalGasUsed);

            executor.setLocalCall(false);
            executor.init();
            executor.execute();
            executor.go();

            executor.finalization();

            totalGasUsed += executor.getGasUsed();
            ProgramResult result = executor.getResult();
            long gasRefund = Math.min(result.getFutureRefund(), result.getGasUsed() / 2);

            ++txNumber;
            boolean isFailed=result.getException()!=null;


            //this.addTxEntries(tx,executor.getResult().getGasUsed(), txNumber,isFailed);
            final int f_txNumber=txNumber;

            executor.getResult().getInternalTransactions()
                    .forEach(t -> addTxEntries(t,0,
                            f_txNumber,t.isRejected()));
        }

        //printEntries();
    }

    public Block getBlock() {
        return this.block;
    }

    public static void setNullCurrent() {
        currentReplayBlock=null;
    }


    //    public HashMap<Transaction, Long> getInternalTxCount() {
//        return intTxCount;
//    }

//    public List<Transaction> getTxList()
//    {
//        return txlist;
//    }
//    public HashMap<Transaction,Long> getTxGasUsedList()
//    {
//        return gasUsedList;
//    }
}
