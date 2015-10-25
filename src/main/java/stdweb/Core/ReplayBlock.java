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
import stdweb.ethereum.EthereumListener;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
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


    public Ethereum getEthereum()
    {
        return ethereum;
    }
    Block block;
    List<Transaction> txlist;
    HashMap<Transaction,Long> gasUsedList;
    HashMap<Transaction, Long> intTxCount;


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

        entries.add(coinbaseEntry);

        BigDecimal fee = entries.stream().map(x -> x.fee).reduce(BigDecimal.ZERO, BigDecimal::add);

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

    public void addTxEntries(Transaction tx, TransactionExecutor executor, int entryNo)
    {

        LedgerEntry ledgerEntrySend = new LedgerEntry();
        LedgerEntry ledgerEntryRecv = new LedgerEntry();


        ledgerEntryRecv.tx=tx;
        ledgerEntrySend.tx=tx;
        ledgerEntryRecv.receipt=executor.getReceipt();
        ledgerEntrySend.receipt=executor.getReceipt();

        ledgerEntryRecv.txNo=entryNo;
        ledgerEntrySend.txNo=entryNo;

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

        ledgerEntrySend.amount=Convert2json.val2BigDec(tx.getValue()).negate();
        ledgerEntryRecv.amount=Convert2json.val2BigDec(tx.getValue());

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
            long gasRefund = Math.min(executor.getResult().getFutureRefund(), executor.getResult().getGasUsed() / 2);
            ledgerEntrySend.gasUsed = executor.getGasUsed()-gasRefund;
        }

        ledgerEntryRecv.gasUsed=0;

        ledgerEntryRecv.entryType=getRecvEntryType(tx);
        ledgerEntrySend.entryType=getSendEntryType(tx);

        ledgerEntryRecv.fee= BigDecimal.valueOf(0);

        if (tx instanceof InternalTransaction)
            ledgerEntrySend.fee= BigDecimal.valueOf(0);
        else
            ledgerEntrySend.fee=new BigDecimal((new BigInteger( tx.getGasPrice())).multiply(BigInteger.valueOf(ledgerEntrySend.gasUsed)));

        ledgerEntryRecv.grossAmount=ledgerEntryRecv.amount;
        ledgerEntrySend.grossAmount=ledgerEntrySend.amount.subtract(ledgerEntrySend.fee);

        byte[] data = (tx.getData()==null)? ByteUtil.ZERO_BYTE_ARRAY : tx.getData();
        ledgerEntryRecv.extraData= Hex.toHexString(data);
        ledgerEntrySend.extraData=Hex.toHexString(data);

        entries.add(ledgerEntrySend);
        entries.add(ledgerEntryRecv);

    }

    private EntryType getSendEntryType(Transaction tx) {
        EntryType entry_type= EntryType.Send;

        LedgerAccount recvAcc = new LedgerAccount(tx.getReceiveAddress(),tx.getContractAddress());

        if (tx.isContractCreation())
            return EntryType.ContractCreation;

        if (recvAcc.isContract())
            return EntryType.Call;

        if (tx instanceof InternalTransaction)
            return EntryType.InternalCall;

        return entry_type;
    }

    private EntryType getRecvEntryType(Transaction tx) {

        EntryType entry_type= EntryType.Receive;

        if (tx.isContractCreation())
            entry_type= EntryType.ContractCreated;

        if (tx instanceof InternalTransaction)
            return EntryType.CallReceive;


        return entry_type;
    }



    public HashMap<Transaction, Long> getInternalTxCount() {
        return intTxCount;
    }

    public List<Transaction> getTxList()
    {
        return txlist;
    }
    public HashMap<Transaction,Long> getTxGasUsedList()
    {
        return gasUsedList;
    }

    public ReplayBlock(EthereumListener _listener, Block _block) {
        this.listener=_listener;
        this.ethereum=_listener.getEthereum();
        this.block=_block;
    }

    public ReplayBlock(EthereumListener _listener, long blockNo) {
        this.listener=_listener;
        this.ethereum=_listener.getEthereum();
        this.block = ethereum.getBlockchain().getBlockByNumber(blockNo);
        if (block==null)
            System.out.println("Replayblock ctor. BlockNo not found:"+blockNo);


        //this.block.getCoinbase();
        //this.block.getUncleList();
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

            entry.txNo=entryNo;
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


        txlist = new ArrayList<>();
        gasUsedList=new HashMap<>();
        intTxCount = new HashMap<Transaction,Long>();
        //EthereumListener listener = new EthereumListener(ethereum);

        BlockchainImpl blockchain = (BlockchainImpl) ethereum.getBlockchain();

        BlockStore blockStore = blockchain.getBlockStore();
        ProgramInvokeFactory programInvokeFactory = blockchain.getProgramInvokeFactory();

        Repository track = blockchain.getRepository();

        Repository snapshot;
        if (block.getNumber()==0)
            snapshot=track.getSnapshotTo(null);
        else
            snapshot = track.getSnapshotTo(blockchain.getBlockByHash(block.getParentHash()).getStateRoot());

        int entryNo=0;
        long totalGasUsed = 0;


        for (Transaction tx : block.getTransactionsList()) {


            TransactionExecutor executor = new TransactionExecutor(tx, block.getCoinbase(),
                    snapshot, blockStore,
                    programInvokeFactory, block, listener, totalGasUsed);

            executor.setLocalCall(false);
            executor.init();
            executor.execute();
            executor.go();
            //executor.finalization();

            //executor.getResult().refundGas();

            totalGasUsed += executor.getGasUsed();
            ProgramResult result = executor.getResult();
            long gasRefund = Math.min(result.getFutureRefund(), result.getGasUsed() / 2);
            gasUsedList.put(tx,executor.getGasUsed()-gasRefund);

            ++entryNo;
            txlist.add(tx);
            this.addTxEntries(tx,executor,entryNo);
            final int f_entryNo=entryNo;

            txlist.addAll(executor.getResult().getInternalTransactions());//call before executor.finalization

//            for (InternalTransaction t : executor.getResult().getInternalTransactions())
//            {
//                addTxEntries(t,executor,f_entryNo);
//            }



            executor.getResult().getInternalTransactions()
                    .forEach(t -> addTxEntries(t, executor, f_entryNo));

            intTxCount.put(tx,Long.valueOf(executor.getResult().getInternalTransactions().size()));

        }

        addRewardEntries();
        //printEntries();
    }

    public Block getBlock() {
        return this.block;
    }
}
