package DEL.Ledger_DEL;

import org.ethereum.core.Block;
import org.springframework.util.Assert;
import stdweb.Core.AddressDecodeException;
import stdweb.Core.EntryType;
import stdweb.Core.HashDecodeException;
import stdweb.Core.SyncStatus;

import java.math.BigDecimal;
import java.sql.*;
import java.util.Arrays;
import java.util.List;

//import org.ethereum.vm.program.InternalTransaction;

/**
 * Created by bitledger on 28.09.15.
 */
public class SqlDb {

    private static SqlDb sqlDb;
    //private final Ethereum ethereum;
    //private final EthereumListener listener;
    private int count;
    private ReplayBlock_DEL replayBlock;
    private PreparedStatement statInsertEntry;
    private PreparedStatement statInsertBlockHeader;
    //private PreparedStatement statInsertAccount;


    long nextSyncBlock;
    private java.lang.String connString="jdbc:h2:tcp://localhost:9092/~/git/stdweb/database/ledger";
            //"jdbc:h2:"+System.getProperty("user.dir")+"/database/ledger";
    //private java.lang.String connString="jdbc:h2:tcp://bitledger.net:9092/~/git/stdweb/database/ledger";

    public  LedgerQuery getQuery() {
        return query;
    }
    private final LedgerQuery query;

    public AccountStore getAccountStore() {
        return accountStore;
    }

    private final AccountStore accountStore;

    public LedgerBlockStore getBlockStore() {
        return blockStore;
    }

    private final LedgerBlockStore blockStore;

    public LedgerEntryStore getLedgerStore() {
        return ledgerStore;
    }

    private final LedgerEntryStore ledgerStore;


    public SyncStatus getSyncStatus() {
        return syncStatus;
    }
    public void setSyncStatus(SyncStatus syncStatus) {
        this.syncStatus = syncStatus;
    }
    SyncStatus syncStatus;
    public void setNextStatus(SyncStatus nextStatus) {
        this.nextStatus = nextStatus;
        if (EthereumBean_DEL.getBlockchainSyncStatus()==SyncStatus.stopped)
            this.syncStatus=nextStatus;

    }
    public SyncStatus getNextStatus() {
        return nextStatus;
    }
    SyncStatus nextStatus;
    private Thread syncLedgerThread;


    public void ledgerBulkLoad() throws SQLException, InterruptedException {
        this.ledgerBulkLoad(query.getSqlTopBlock() + 1);
    }

    public void ledgerBulkLoad(long _block) throws SQLException, InterruptedException {

        System.out.println("Ledger_DEL Start BulkLoad from :"+_block);
        syncStatus=SyncStatus.bulkLoading;

        this.nextSyncBlock =_block;
        deleteBlocksFrom(_block);

        if (syncLedgerThread==null || !syncLedgerThread.isAlive()) {
            createSyncLedgerThread();
            syncLedgerThread.start();
        }
    }

    public void stopSync()
    {
        System.out.println("stop BulkLoad");
        syncStatus=SyncStatus.stopped;
        nextSyncBlock =1_000_000_000;
    }
    public synchronized  void  createSyncLedgerThread() throws SQLException, InterruptedException {

        System.out.println("create Ledger_DEL BulkLoadThread");
        EthereumBean_DEL.blockchainStopSync();
        SqlDb sqlDb = SqlDb.getSqlDb();

        syncLedgerThread = new Thread(() -> {
            while (syncStatus==SyncStatus.bulkLoading)
            {
                if (nextSyncBlock <= EthereumBean_DEL.getBlockchainImpl().getBestBlock().getNumber())
                    try {
                        try {
                            sqlDb.replayAndInsertBlock(nextSyncBlock);
                        } catch (HashDecodeException e) {
                            e.printStackTrace();
                        } catch (AddressDecodeException e) {
                            e.printStackTrace();
                        }
                        nextSyncBlock++;
                    } catch (SQLException e) {
                        System.out.println("Error inserting  block :"+ (nextSyncBlock));
                        e.printStackTrace();
                    }
                else {
                    syncStatus = nextStatus;
                    System.out.println("finished bulkloading . Block:"+ EthereumBean_DEL.getBlockchainImpl().getBestBlock().getNumber());
                    System.out.println("Snc status set to: "+syncStatus);
                    break;
                }
              }

            try {
                        flush(1);

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public BigDecimal getLedgerBlockTxFee(Block block) throws SQLException {
        ResultSet rs;
        Statement statement = conn.createStatement();

        //String accStr = Hex.toHexString(account);
        //'f0134ff161a5c8f7c4f8cc33d3e1a7ae088594a9'
        String sql="select  case when sum(fee) is null then 0 else sum(fee) end  amo, count(*) c from ledger  where  block="+block.getNumber();
        //sql+=" and entrytype="+EntryType.TxFee.ordinal();
        rs = statement.executeQuery(sql);
        rs.first();

        return rs.getBigDecimal(1);
    }


    public Connection getConn() {
        return conn;
    }

    private Connection conn;



    private PreparedStatement getInsertEntryStatement() throws SQLException {
        ensureConnection();

        return statInsertEntry;
    }

    public synchronized void deleteBlock(long blockNo) throws SQLException {

        ensureConnection();

        String s="delete from ledger where block="+blockNo;
        Statement statement = conn.createStatement();
        statement.execute(s);

        s="delete from block where id="+blockNo;
        statement = conn.createStatement();
        statement.execute(s);
        conn.commit();
    }

    public synchronized void deleteBlocksFrom(long blockNo) throws SQLException {

        ensureConnection();

        String s="delete from ledger where block>="+blockNo;
        Statement statement = conn.createStatement();
        statement.execute(s);

        s="delete from block where id>="+blockNo;
        statement = conn.createStatement();
        statement.execute(s);
        conn.commit();
    }

    private void ensureConnection() throws SQLException {
        if (conn==null || conn.isClosed()) {
            //System.out.println("ensure connection - Restore!");
            conn = DriverManager.getConnection(connString, "sa", "");
            String insEntrySql="insert into ledger (tx ,address ,amount ,block ,blocktimestamp,depth ,gasused ,fee ,entryType,offsetAccount,descr,GrossAmount,EntryResult) values(?,?,?,?,?,?,?,?,?,?,?,?,?)";
            statInsertEntry = conn.prepareStatement(insEntrySql);

//            String insBlockHeaderSql="INSERT INTO BLOCK ( ID,HASH, PARENTHASH, UNCLESHASH, COINBASE, STATEROOT, TXTRIEROOT, RECEIPTTRIEROOT, LOGSBLOOM, DIFFICULTY, TIMESTAMP, GASLIMIT, GASUSED, MIXHASH, EXTRADATA, NONCE, FEE, TRIEBALANCE)\n" +
//                    "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ";
//            statInsertBlockHeader = conn.prepareStatement(insBlockHeaderSql);

//            String insAccountSql="INSERT INTO ACCOUNT (ADDRESS, NAME, NONCE, ISCONTRACT, LASTBLOCK, BALANCE, STATEROOT) VALUES (?,?,?,?,?,?,?)";
//            statInsertAccount= conn.prepareStatement(insAccountSql);
        }
        //else
        //    System.out.println("ensure connection - Ok!");
    }

    public void truncateLedger() throws SQLException {
        String s="truncate table ledger";
        Statement statement = conn.createStatement();
        statement.execute(s);
        conn.commit();
    }


//    private void checkBalance() throws SQLException {
//        BigDecimal trieBalance = BigDecimal.valueOf(0);
//        BigDecimal ledgerBlockBalance = BigDecimal.valueOf(0);
//        Block block = replayBlock.getBlock();
//
//        trieBalance=BlockchainQuery.getTrieBalance(replayBlock.getBlock());
//        ledgerBlockBalance=query.getLedgerBlockBalance(block.getNumber());
//        long number = block.getNumber();
//
//
//        //System.out.println("trieBalance");
//        if (trieBalance.equals(ledgerBlockBalance))
//            System.out.println("Block balance correct:" + number + ": " + Convert2json.BI2ValStr(trieBalance.toBigInteger(), false));
//        else {
//            System.out.println("Block balance incorrect:" + number + ", trie - ledger: " + Convert2json.BI2ValStr(trieBalance.toBigInteger(), false) + " - " + Convert2json.BI2ValStr(ledgerBlockBalance.toBigInteger(), false));
//            //throw (new SQLException("Block balance incorrect:" + number + ", trie - ledger: " + Convert2json.BI2ValStr(trieBalance.toBigInteger(), false) + " - " + Convert2json.BI2ValStr(ledgerBlockBalance.toBigInteger(), false)));
//        }
//    }

//    public void checkDelta() throws SQLException {
//        BigDecimal trieDelta = BigDecimal.valueOf(0);
//        BigDecimal ledgerBlockDelta = BigDecimal.valueOf(0);
//
//        Block block = replayBlock.getBlock();
//        trieDelta=BlockchainQuery.getTrieDelta(block);
//        ledgerBlockDelta=query. getLedgerBlockDelta(block);
//        long number = block.getNumber();
//
//        //System.out.println("trieBalance");
//        if (trieDelta.equals(ledgerBlockDelta))
//            System.out.println("Block delta correct:" + number + ": " + Convert2json.BI2ValStr(trieDelta.toBigInteger(), false));
//        else {
//            System.out.println("Block Delta incorrect:" + number + ", trie - ledger: " + Convert2json.BI2ValStr(trieDelta.toBigInteger(), false) + " - " + Convert2json.BI2ValStr(ledgerBlockDelta.toBigInteger(), false));
//            //throw (new SQLException("Block balance incorrect:" + number + ", trie - ledger: " + Convert2json.BI2ValStr(trieBalance.toBigInteger(), false) + " - " + Convert2json.BI2ValStr(ledgerBlockBalance.toBigInteger(), false)));
//        }
//    }

    int blockCount2flush=0;
    public synchronized void flush(int n) throws SQLException {
        if (syncStatus==SyncStatus.bulkLoading)
            if (nextSyncBlock% 200==0)
            {
                System.out.println("System GC at "+nextSyncBlock);
                System.gc();
            }

        blockCount2flush++;
        if (blockCount2flush>=n) {
            conn.commit();
            blockCount2flush=0;
            //System.out.println("Ledger_DEL - block inserted:"+query.getSqlTopBlock());
        }
    }

    public synchronized int replayAndInsertBlock(long blockNumber) throws SQLException, HashDecodeException, AddressDecodeException {

        if (EthereumBean_DEL.getBlockchainImpl().getBestBlock().getNumber()<blockNumber) {
            System.out.println("cannot insert block.Top block is: " + EthereumBean_DEL.getBlockchainImpl().getBestBlock().getNumber());
            return -1;
        }

        Block blockByNumber = EthereumBean_DEL.getBlockchainImpl().getBlockByNumber(blockNumber);
        Assert.isTrue(blockByNumber!=null,"blockBy number is null:"+blockNumber);

        ReplayBlock_DEL current = new ReplayBlock_DEL(blockByNumber);
        //ReplayBlock current = ReplayBlock.CURRENT(blockByNumber);
        current.run();

        return  write(current);
    }




    public synchronized int write(ReplayBlock_DEL _replayBlock) throws SQLException{

            //getOrCreateAddresses(_replayBlock);

            //this.replayBlock = new ReplayBlock(listener, blockNumber);
            //replayBlock.run();

            this.replayBlock=_replayBlock;
            //deleteBlocksFrom(replayBlock.getBlock().getNumber());
            deleteBlock(replayBlock.getBlock().getNumber());

            if (_replayBlock.getBlock().getNumber()!=0) //no reward entry for genesis
                replayBlock.addRewardEntries();

            List<EntryType> rewardEntryTypes = Arrays.asList(EntryType.CoinbaseReward, EntryType.UncleReward, EntryType.FeeReward);

            //reward entries first
            replayBlock.getLedgerEntries()
                    .stream().filter(e -> rewardEntryTypes.contains(e.getEntryType()))
                    .forEach(e -> {
                        try {
                            ledgerStore.write(e);
                        } catch (SQLException e1) {
                            e1.printStackTrace();
                        }
                    });

            replayBlock.getLedgerEntries()
                    .stream().filter(e -> !rewardEntryTypes.contains(e.getEntryType()))
                    .forEach(e -> {
                        try {
                            ledgerStore.write(e);
                        } catch (SQLException e1) {
                            e1.printStackTrace();
                        }
                    });

        flush(1);
        this.replayBlock=null;

        ReplayBlock_DEL.setNullCurrent();

        int sqlTopBlock = query.getSqlTopBlock();

        return sqlTopBlock;

    }




    private void initMsSql() throws Exception
    {
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        String connstr="jdbc:sqlserver://ledg.database.windows.net:1433;database=ledgerdb;user=std;password={};encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;";
        Connection conn = DriverManager.getConnection(connstr);

        Statement statement = conn.createStatement();

        //stat.execute("drop table if exists LEDGER");
        String delTable=
                "IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[ledger]') AND type in (N'U')) "+
                        "DROP TABLE [dbo].[ledger]";
        String createTable="CREATE TABLE [dbo].[ledger]" +
                "( [id] [bigint] IDENTITY(1,1) NOT NULL, " +
                "  [tx] [binary](50) NULL," +
                " CONSTRAINT [PK_ledger] PRIMARY KEY CLUSTERED " +
                "( [id] ASC )" +
                "WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON) ON [PRIMARY]            ) ON [PRIMARY]";
        String dropInd="IF  EXISTS (SELECT * FROM sys.indexes WHERE object_id = OBJECT_ID(N'[dbo].[ledger]') AND name = N'IX_ledger')\n" +
                "DROP INDEX [IX_ledger] ON [dbo].[ledger] WITH ( ONLINE = OFF )";

        String createInd="CREATE NONCLUSTERED INDEX [IX_ledger] ON [dbo].[ledger] ( [tx] ASC )";
    }
    private void initH2()  throws Exception
    {

        Class.forName("org.h2.Driver");

        Connection conn1 = DriverManager.getConnection(connString, "sa", "");
        Statement statement = conn1.createStatement();
        statement.execute("create table if not exists ledger(id identity primary key, tx binary(32),address binary(20),amount decimal(31,0),block bigint,blocktimestamp timestamp," +
                "depth tinyint,gasused bigint,fee decimal(31,0),entryType tinyint,entryResult tinyint,offsetaccount binary(20),descr varchar(32),GrossAmount decimal(31,0) ,balance decimal(31,0))");
        statement.execute("create index if not exists idx_ledger_address_tx on ledger(address,tx)");

        //statement.execute("drop index if exists idx_ledger_address_id ");
        //statement.execute("drop index if exists idx_ledger_address ");
        statement.execute("create index if not exists idx_ledger_address on ledger(address,id,entryType)");
        statement.execute("create index if not exists idx_ledger_tx on ledger(tx)");
        statement.execute("create index if not exists idx_ledger_block_id on ledger(block,id)");

//        statement.execute("create table if not exists Block(id bigint primary key, TrieBalance decimal(31,0),blocktimestamp timestamp," +
//                "hash binary(32),prevhash binary(32),stateRoot binary(32)," +
//                "gasused bigint,fee decimal(31,0))");
//
//        statement.execute("create index if not exists idx_block_hash_id on block(hash,id)");

        statement.close();
        conn1.close();

        //ensureConnection();

    }
    private void init(String sqltype) throws Exception {
        switch (sqltype.toLowerCase())
        {
            case "mssql":
                initMsSql();break;
            case "h2":
                initH2();break;
        }
        ensureConnection();

    }

    private void ensureGenesis() throws SQLException, AddressDecodeException, HashDecodeException {
        if (query.getSqlTopBlock()==0 && query.ledgerCount(0)==0)
        {
            //deleteBlocksFrom(0);
            write(ReplayBlock_DEL.GENESIS());
            conn.commit();
            //flush(1);
        }
    }

    public static SqlDb getSqlDb()
    {

        if (sqlDb ==null)
            try {
                sqlDb =new SqlDb();

            } catch (SQLException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } catch (AddressDecodeException e) {
                e.printStackTrace();
            }
        return sqlDb;
    }

    private SqlDb() throws Exception, AddressDecodeException {
        //this.listener=EthereumBean.getListener();
        //this.ethereum=listener.getEthereum();
        init("h2");
        ensureConnection();
        conn.setAutoCommit(false);
        count=0;
        syncStatus=SyncStatus.stopped;
        nextStatus=SyncStatus.stopped;


        query=LedgerQuery.getQuery(this);
        accountStore=AccountStore.getInstance();
        accountStore.setConnection(conn);

        blockStore= LedgerBlockStore.getInstance();
        blockStore.setConnection(conn);

        ledgerStore= new LedgerEntryStore();
        //ledgerStore.setConnection(conn);

        ensureGenesis();
    }
//
//    public void del_insertLedgerEntries(ReplayBlock _replayBlock) throws Exception {
//
//        this.replayBlock=_replayBlock;
//
//        deleteBlocksFrom(replayBlock.getBlock().getNumber());
//
//        String sql="insert into ledger (tx ,address ,amount ,block ,blocktimestamp,depth ,gasused ,fee ,entryType,offsetAccount,descr,GrossAmount) values(?,?,?,?,?,?,?,?,?,?,?,?)";
//
//            stat = conn.prepareStatement(sql);
//
//        if (replayBlock.getBlock().getNumber()==0) {
//            del_loadGenesis();
//            del_insertBlock(_replayBlock);
//            return;
//        }
//        replayBlock.run();
//        del_insertCoinbaseEntry();
//
//            List<Transaction> txList = replayBlock.getTxList();
//            int ind=-1;
//            for (Transaction tx:txList)
//            {
//                ind++;
//                del_insertLedgerEntry(tx, replayBlock, ind);
//            }
//
//        del_insertBlock(_replayBlock);
//
//        conn.commit();
//
//        //System.out.println("insert ledger block#"+replayBlock.getBlock().getNumber());
//
//
//    }
//
//
//
//    private void del_loadGenesis() throws SQLException {
//
//        org.ethereum.core.Repository snapshot = ((RepositoryImpl) ethereum.getRepository()).getSnapshotTo(Hex.decode("d7f8974fb5ac78d9ac099b9ad5018bedc2ce0a72dad1827a1709da30580f0544"));
//        Block block = ethereum.getBlockchainImpl().getBlockByNumber(0);
//
//
//        Set<byte[]> accountsKeys = snapshot.getAccountsKeys();
//
//        for (byte[] account : accountsKeys)
//        {
//
//            System.out.println("acc:"+Hex.toHexString(account));
//
//            BigDecimal balance = new BigDecimal(snapshot.getBalance(account));
//
//            stat.setBytes(1, HashUtil.EMPTY_DATA_HASH);
//            stat.setBytes(2,account);
//            stat.setBigDecimal(3,balance);
//            stat.setLong(4,block.getNumber()); //genesis blockno 0
//            stat.setTimestamp(5, new Timestamp(1438269973000l)); //genesis timestamp
//            stat.setByte(6, (byte) 0);//depth
//            stat.setLong(7,0);//gasused
//
//            stat.setBigDecimal(8, new BigDecimal(0));
//            stat.setByte(9,(byte)EntryType.Genesis.ordinal());
//            stat.setBytes(10, ByteUtil.ZERO_BYTE_ARRAY);//offset account - genesis
//            stat.setString(11,"Genesis balance entry");
//            stat.setBigDecimal(12,balance  );
//
//           // stat.addBatch();
//            stat.executeUpdate();
//        }
//    }
//    private EntryType del_getEntryType(Transaction tx, String entryType) {
//        EntryType entry_type= EntryType.NA;
//
//        if (tx.isContractCreation())
//            if (entryType.equals("send"))
//                return EntryType.ContractCreation;
//            else
//                return EntryType.ContractCreated;
//
//        if (tx instanceof InternalTransaction)
//            return EntryType.InternalCall;
//
//        if (entryType.equals("send") &&tx.getContractAddress()==null)
//        {
//            byte[] receiveAddress = tx.getReceiveAddress();
//            RepositoryImpl repository = (RepositoryImpl)ethereum.getRepository();
//            AccountState accountState = repository.getAccountState(receiveAddress);
//            ContractDetails contractDetails = repository.getContractDetails(receiveAddress);
//
//            if (contractDetails.getCode().length==0)
//                return EntryType.Send;
//            else
//                return EntryType.Call;
//
//        }
//
//
//        if (entryType.equals("receive"))
//            entry_type= EntryType.Receive;
//
//        return entry_type;
//    }
//
//    private void del_insertBlock(ReplayBlock replayBlock) throws SQLException {
//
//        Block block = replayBlock.getBlock();
//
//
//        //String sql="insert into block (id ,TrieBalance, blocktimestamp,hash,prevhash,stateroot,gasused,fee) values(?,?,?,?,?,?,?,?)";
//        //stat = conn.prepareStatement(sql);
//
//        BigDecimal trieBalance = BigDecimal.valueOf(0);
//        BigDecimal ledgerBlockBalance = BigDecimal.valueOf(0);
//
//        BigDecimal trieDelta = BigDecimal.valueOf(0);
//        BigDecimal ledgerBlockDelta = BigDecimal.valueOf(0);
//
//        long number = block.getNumber();
//
//        //46859
//        if (number >= 46147) {
//            trieBalance=getTrieBalance(replayBlock.getBlock());
//            ledgerBlockBalance=getLedgerBlockBalance(block);
//
//            trieDelta=getTrieDelta(replayBlock.getBlock());
//            ledgerBlockDelta=getLedgerBlockDelta(block);
//
//            //System.out.println("trieBalance");
//            if (trieDelta.equals(ledgerBlockDelta))
//                System.out.println("Block delta correct:" + number + ": " + Convert2json.BI2ValStr(trieDelta.toBigInteger(), false));
//            else {
//                System.out.println("Block Delta incorrect:" + number + ", trie - ledger: " + Convert2json.BI2ValStr(trieDelta.toBigInteger(), false) + " - " + Convert2json.BI2ValStr(ledgerBlockDelta.toBigInteger(), false));
//                //throw (new SQLException("Block balance incorrect:" + number + ", trie - ledger: " + Convert2json.BI2ValStr(trieBalance.toBigInteger(), false) + " - " + Convert2json.BI2ValStr(ledgerBlockBalance.toBigInteger(), false)));
//            }
//
//            //System.out.println("trieBalance");
//            if (trieBalance.equals(ledgerBlockBalance))
//                System.out.println("Block balance correct:" + number + ": " + Convert2json.BI2ValStr(trieBalance.toBigInteger(), false));
//            else {
//                System.out.println("Block balance incorrect:" + number + ", trie - ledger: " + Convert2json.BI2ValStr(trieBalance.toBigInteger(), false) + " - " + Convert2json.BI2ValStr(ledgerBlockBalance.toBigInteger(), false));
//                //throw (new SQLException("Block balance incorrect:" + number + ", trie - ledger: " + Convert2json.BI2ValStr(trieBalance.toBigInteger(), false) + " - " + Convert2json.BI2ValStr(ledgerBlockBalance.toBigInteger(), false)));
//            }
//
//            try {
//                Thread.sleep(000);;//2000
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//
//
//
////        stat.setLong(1, replayBlock.getBlock().getNumber());
////        stat.setBigDecimal(2, trieBalance);
////        stat.setTimestamp(3, new Timestamp(block.getTimestamp()*1000));
////        stat.setBytes(4,block.getHash());
////        stat.setBytes(5,block.getParentHash());
////        stat.setBytes(6,block.getStateRoot());
////        stat.setLong(7,block.getGasUsed());
////        stat.setLong(8,0);
////
////        stat.executeUpdate();
//
//
//        // conn.commit();
//
//    }
//
//    private BigInteger del_getFee(Transaction tx, long gasUsed, EntryType entry_type, String entryType) {
//        //if (entry_type==EntryType.Receive)
//        if (entryType=="receive")
//            return BigInteger.valueOf(0);
//        else
//            return (new BigInteger(1, tx.getGasPrice())).multiply(BigInteger.valueOf(gasUsed));
//    }
//    public void  del_insertLedgerEntry(Transaction tx,String entryType,ReplayBlock replayBlock, int ind) throws SQLException {
//
//        Block block = replayBlock.getBlock();
//
//
//
//
//        if (tx instanceof InternalTransaction)
//            stat.setBytes(1,((InternalTransaction)tx).getParentHash());
//        else
//            stat.setBytes(1,tx.getHash());
//
//
//        BigInteger biVal=new BigInteger(1,tx.getValue());
//        if (entryType.equals("send")) {
//            stat.setBytes(2, tx.getSender());
//
//            stat.setBytes(10,(tx.getContractAddress()==null? tx.getReceiveAddress():tx.getContractAddress())); //offset account
//            biVal=biVal.negate();
//        }
//        else {
//            stat.setBytes(10,tx.getSender());//offset account
//
//            if (tx.getReceiveAddress()==null)
//                stat.setBytes(2,tx.getContractAddress());
//            else
//                stat.setBytes(2, tx.getReceiveAddress());
//        }
//
//
//        stat.setBigDecimal(3, new BigDecimal(biVal));
//
//
//        stat.setLong(4, block.getNumber());
//
//        stat.setTimestamp(5, new Timestamp(block.getTimestamp()*1000));
//
//
//        String indStr=String.valueOf(ind);
//        byte depth=0;
//        if (tx instanceof InternalTransaction)
//        {
//            depth=(byte)(((InternalTransaction) tx).getDeep()+1);
//        }
//        if (tx instanceof InternalTransaction) {
//            indStr += ":" + ((InternalTransaction) tx).getIndex();
//            if (((InternalTransaction) tx).getDeep()!=0)
//                indStr+= " (deep " + ((InternalTransaction) tx).getDeep() + ")";
//        }
//
//
//        stat.setByte(6, depth);
//
//        long gasUsed=0;
//        if (replayBlock.getTxGasUsedList().containsKey(tx))
//            gasUsed=replayBlock.getTxGasUsedList().get(tx);
//
//        stat.setLong(7,gasUsed);
//
//
//        EntryType entry_type = del_getEntryType(tx, entryType);
//
//        BigInteger fee = del_getFee(tx, gasUsed, entry_type, entryType);
//        stat.setBigDecimal(8, new BigDecimal(fee));
//
//
//        stat.setByte(9,(byte)entry_type.ordinal());
//
//        stat.setString(11,"Descr ");
//
//
//        BigDecimal bigDecimal = new BigDecimal(biVal.subtract(fee));
//        stat.setBigDecimal(12,bigDecimal  );
//
//        //stat.executeUpdate();
//
//
//        count+=stat.executeUpdate();
//        //System.out.println("insertion status :" + stat.executeUpdate());
//
//    }
//    public void  del_insertLedgerEntry(Transaction tx,ReplayBlock replayBlock,int ind) throws SQLException {
//
//        del_insertLedgerEntry(tx, "send", replayBlock, ind);
//        del_insertLedgerEntry(tx, "receive", replayBlock, ind);
//
//    }
//
//    void del_insertCoinbaseEntry() throws SQLException {
//
//        Block block = replayBlock.getBlock();
//        byte[] coinbase = block.getCoinbase();
//
//        HashSet<ByteArrayWrapper> accounts=new HashSet<>();
//        accounts.add(new ByteArrayWrapper(coinbase));
//
//
//        block.getUncleList().forEach(uncle -> accounts.add(new ByteArrayWrapper(uncle.getCoinbase())));
//
//        for (ByteArrayWrapper acc : accounts) {
//
//            BigDecimal delta = new BigDecimal(replayBlock.getAccountDelta(acc.getData()));
//
//
//            stat.setBytes(1, HashUtil.EMPTY_DATA_HASH);
//            stat.setBytes(2,acc.getData());//address
//            stat.setBigDecimal(3,delta);
//            stat.setLong(4,block.getNumber()); //genesis blockno 0
//            stat.setTimestamp(5, new Timestamp(block.getTimestamp()*1000)); //genesis timestamp
//            stat.setByte(6, (byte) 0);//depth
//            stat.setLong(7,0);//gasused
//
//            stat.setBigDecimal(8, new BigDecimal(0));//fee
//
//            if (acc.equals(new ByteArrayWrapper(coinbase))) {
//                stat.setByte(9, (byte) EntryType.CoinbaseReward.ordinal());
//                stat.setString(11,"Coinbase reward entry");
//            }
//            else {
//                stat.setByte(9, (byte) EntryType.UncleReward.ordinal());
//                stat.setString(11,"Uncle reward entry");
//            }
//
//            stat.setBytes(10, ByteUtil.ZERO_BYTE_ARRAY);//offset account - genesis
//
//            stat.setBigDecimal(12,delta  );
//
//            // stat.addBatch();
//            stat.executeUpdate();
//        }
//    }

    //    public String LedgerSelect1(String accStr) throws SQLException {
//
//        if (accStr.startsWith("0x"))
//            accStr=accStr.substring(2);
//
//        ResultSet rs;
//        Statement statement = conn.createStatement();
//
//        //String accStr = Hex.toHexString(account);
//        //'f0134ff161a5c8f7c4f8cc33d3e1a7ae088594a9'
//        String sql="select  id   , tx ,address ,amount ,block ,blocktimestamp ,depth ,gasused ,fee ,entryType , offsetaccount, descr ," +
//                " GrossAmount from ledger  where address =X'" +accStr+"' "+
//                "order by id";
//        rs = statement.executeQuery(sql);
//
//        try {
//            JSONArray jsonArray = RsToJSON(rs);
//            System.out.println(jsonArray.toJSONString());
//            return jsonArray.toJSONString();
//        } catch (Exception e) {
//            e.printStackTrace();
//            return "{error :"+e.toString()+"}";
//        }
//    }

//    public String LedgerSelectTx(String txStr) throws SQLException {
//
//        if (txStr.startsWith("0x"))
//            txStr=txStr.substring(2);
//
//        ResultSet rs;
//        Statement statement = conn.createStatement();
//
//        String sql="select  id   , tx ,address, case when amount>0 then amount else 0 end as Received ,case when amount<0 then -amount else 0 end as sent, block ,blocktimestamp ,depth ,gasused ,fee ,entryType , offsetaccount, descr ," +
//                " GrossAmount from ledger  where tx =X'" +txStr+"' "
//                +                "order by id";
//
//        rs = statement.executeQuery(sql);
//
//        try {
//            //JSONArray jsonArray = RsToJSON(rs);
//            JSONArray jsonArray = getJson(rs,true);
//            return jsonArray.toJSONString();
//        } catch (Exception e) {
//            e.printStackTrace();
//            return "{error :"+e.toString()+"}";
//        }
//    }

    //public static JSONArray getJson(ResultSet resultSet,boolean calcBalance) throws Exception {
//        JSONArray jsonArray = new JSONArray();
//
//        BigDecimal balance=new BigDecimal(0);
//        BigDecimal fee=new BigDecimal(0);
//        BigDecimal received=new BigDecimal(0);
//        BigDecimal sent=new BigDecimal(0);
//
//        while (resultSet.next()) {
//
//            int total_cols = resultSet.getMetaData().getColumnCount();
//            JSONObject obj = new JSONObject();
//
//            if (calcBalance) {
//                balance = balance.add(resultSet.getBigDecimal("RECEIVED")).subtract(resultSet.getBigDecimal("SENT"));
//                obj.put("BALANCE", Convert2json.BI2ValStr(balance.toBigInteger(), true));
//
//                received = received.add(resultSet.getBigDecimal("RECEIVED"));
//                sent = sent.add(resultSet.getBigDecimal("SENT"));
//            }
//
//            //fee
//
//            for (int i = 1; i <= total_cols; i++) {
//                //String columnTypeName = resultSet.getMetaData().getColumnTypeName(i);
//                String columnLabel = resultSet.getMetaData().getColumnLabel(i);
//
//                DataItem item= new DataItem("ledger", columnLabel, resultSet.getObject(i));
//                obj.put(item.getKey(),item.getValue());
//            }
//            jsonArray.add(obj);
//        }
//        //add total row to json
//        if (calcBalance) {
//            JSONObject obj = new JSONObject();
//            obj.put("BLOCK", "Total:");
//
//            obj.put("RECEIVED", Convert2json.BI2ValStr(received.toBigInteger(), true));
//            obj.put("RECEIVED", Convert2json.BI2ValStr(received.toBigInteger(), true));
//            obj.put("SENT", Convert2json.BI2ValStr(sent.toBigInteger(), true));
//            obj.put("FEE", Convert2json.BI2ValStr(fee.toBigInteger(), true));
//            obj.put("BALANCE", Convert2json.BI2ValStr(balance.toBigInteger(), true));
//            jsonArray.add(obj);
//        }
//
//        return jsonArray;
//    }
//
//
//
//    public static JSONArray RsToJSON(ResultSet resultSet)
//            throws Exception {
//        JSONArray jsonArray = new JSONArray();
//
//
//        BigDecimal balance=new BigDecimal(0);
//        BigDecimal fee=new BigDecimal(0);
//        BigDecimal received=new BigDecimal(0);
//        BigDecimal sent=new BigDecimal(0);
//
//
//        while (resultSet.next()) {
//
//            long block = resultSet.getLong("BLOCK");
//
//            int total_cols = resultSet.getMetaData().getColumnCount();
//            JSONObject obj = new JSONObject();
//
//            fee=fee.add(resultSet.getBigDecimal("FEE"));
//
//            if (resultSet.getBigDecimal("AMOUNT").signum()==1) {
//                received = received.add(resultSet.getBigDecimal("AMOUNT"));
//                obj.put("RECEIVED",Convert2json.BI2ValStr(resultSet.getBigDecimal("AMOUNT").toBigInteger(),true));
//            }
//            else {
//                sent = sent.add(resultSet.getBigDecimal("AMOUNT"));
//                obj.put("SENT",Convert2json.BI2ValStr(resultSet.getBigDecimal("AMOUNT").toBigInteger(),true));
//            }
//
//            balance=balance.add(resultSet.getBigDecimal("GROSSAMOUNT"));
//            obj.put("BALANCE",Convert2json.BI2ValStr(balance.toBigInteger(),true));
//
//
//            String e=EntryType.values()[resultSet.getInt("ENTRYTYPE")].toString();
//            obj.put("ENTRYTYPE",e);
//
//            for (int i = 1; i <= total_cols; i++) {
//
//                String columnTypeName = resultSet.getMetaData().getColumnTypeName(i);
//
//                String columnLabel = resultSet.getMetaData().getColumnLabel(i);
//
//                switch (columnTypeName)
//                {
//                    case "DECIMAL":
//
//                        obj.put(columnLabel, Convert2json.BI2ValStr(resultSet.getBigDecimal(i).toBigInteger(), true));
//                        break;
//                    case "VARBINARY":
//                        obj.put(columnLabel,"0x"+Hex.toHexString(resultSet.getBytes(i)));break;
//                    case "TINYINT":
////                        if (columnLabel.equals("ENTRYTYPE"))
////                            obj.put(columnLabel,EntryType.values()[resultSet.getInt(i)].toString());
////                        else
////                            obj.put(columnLabel,resultSet.getLong(i));
//
//                        break;
//                    case "TIMESTAMP":
//                        obj.put(columnLabel,resultSet.getTimestamp(i).toString());break;
//                    default:
//                        obj.put(columnLabel,resultSet.getObject(i));
//                }
//                //obj.put(resultSet.getMetaData().getColumnLabel(i + 1).toLowerCase(), resultSet.getObject(i + 1));
//            }
//
//
//            jsonArray.add(obj);
//        }
//        //add total row to json
//        JSONObject obj = new JSONObject();
//        obj.put("BLOCK", "Total:");
//
//        obj.put("RECEIVED",Convert2json.BI2ValStr(received.toBigInteger(),true));
//        obj.put("RECEIVED",Convert2json.BI2ValStr(received.toBigInteger(),true));
//        obj.put("SENT",Convert2json.BI2ValStr(sent.toBigInteger(),true));
//        obj.put("FEE", Convert2json.BI2ValStr(fee.toBigInteger(), true));
//        obj.put("BALANCE",Convert2json.BI2ValStr(balance.toBigInteger(), true));
//        jsonArray.add(obj);
//
//        return jsonArray;
//
//    }
    //    public  void insertBlockHeader(BlockHeader header,Sha3Hash blockHash,BigDecimal fee, BigDecimal reward, BigDecimal trieBalance) throws SQLException {
//        ensureConnection();
//        PreparedStatement st = statInsertBlockHeader;
//        //ID,HASH, PARENTHASH, UNCLESHASH, COINBASE, STATEROOT, TXTRIEROOT,
//        // RECEIPTTRIEROOT, LOGSBLOOM, DIFFICULTY, TIMESTAMP, GASLIMIT, GASUSED, MIXHASH, EXTRADATA, NONCE, FEE, TRIEBALANCE
//        st.setLong(1,header.getNumber());
//        st.setBytes(2,blockHash.getBytes());
//        st.setBytes(3,header.getParentHash());
//        st.setBytes(4,header.getUnclesHash());
//        st.setBytes(5,header.getCoinbase());
//        st.setBytes(6,header.getStateRoot());
//
//        st.setBytes(7,header.getTxTrieRoot());
//        st.setBytes(8,header.getReceiptsRoot());
//        st.setBytes(9,header.getLogsBloom());
//        st.setLong(10, new BigInteger(1,header.getDifficulty()).longValue());
//        st.setTimestamp(11, new Timestamp(header.getTimestamp()*1000));
//        st.setLong(12, header.getGasLimit());
//        st.setLong(13,header.getGasUsed());
//        st.setBytes(14,header.getMixHash());
//        st.setBytes(15,header.getExtraData());
//        st.setLong(16,new BigInteger(1,header.getNonce()).longValue());
//        st.setBigDecimal(17,fee);
//
//        st.setBigDecimal(18,trieBalance);
//        st.setBigDecimal(19,reward);
//
//        count+= st.executeUpdate();
//
//    }

    //    private void getOrCreateAddresses(ReplayBlock replayBlock) {
//        List<LedgerEntry> ledgerEntries = replayBlock.getLedgerEntries();
//
//    }


    //    private PreparedStatement stat;

//    public void write(LedgerAccount account) throws SQLException {
//
//        if (account.isNew()) {
//            ResultSet rs = getAccountRs(account.getBytes());
//            if (!rs.isFirst())
//                createAccount(account.getBytes());
//
//            account.load(rs);
//            return;
//        }
//
//        String sql="UPDATE account set name=?, nonce=?,isContract=?,lastblock=?,balance=?,txstateroot=? " +
//                "where id="+ account.Id;
//
//        PreparedStatement st=conn.prepareStatement(sql);
//
//        st.setString(1,account.name);
//        st.setLong(2, account.nonce);
//        st.setBoolean(3,account.isContract);
//        st.setLong(4,account.lastBlockNumber);
//        st.setBigDecimal(5, account.balance);
//        st.setBytes(6,account.txStateRoot);
//        st.executeUpdate();
//
//        conn.commit();
//
//        account.load(getAccountRs(account.getBytes()));
//    }
//
//    public  LedgerAccount getOrCreateLedgerAccount(byte[] address) throws SQLException {
//        ResultSet rs = getAccountRs(address);
//        if (rs.isFirst())
//            return new LedgerAccount(rs);
//
//            //INSERT INTO ACCOUNT (ADDRESS, NAME, NONCE, ISCONTRACT, LASTBLOCK, BALANCE, TXSTATEROOT) VALUES (?,?,?,?,?,?,?);
//        else {
//            return createAccount(address);
//
//        }
//    }
//
//    public  LedgerAccount getOrCreateLedgerAccount(String accStr) throws SQLException {
//
//        accStr=Utils.remove0x(accStr);
//        byte[] decode = Hex.decode(accStr);
//        return getOrCreateLedgerAccount(decode);
//
//    }
//
//    private LedgerAccount createAccount(byte[] addr) throws SQLException {
//
//        PreparedStatement stat=statInsertAccount;
//        RepositoryImpl repo = EthereumBean.getRepositoryImpl();
//        String accStr=Hex.toHexString(addr);
//
//        String name="";
//        long nonce = repo.getNonce(Hex.decode(accStr)).longValue();
//
//        stat.setBytes(1,addr);
//        stat.setString(2, name);
//        stat.setLong(3,nonce);
//        stat.setBoolean(4, LedgerAccount.isContract(addr));
//        stat.setLong(5,0);
//        stat.setBigDecimal(6, BigDecimal.ZERO);
//        stat.setBytes(7, ByteUtil.EMPTY_BYTE_ARRAY);
//
//        stat.executeUpdate();
//        conn.commit();
//        return new LedgerAccount(getAccountRs(addr));
//    }
//
//    ResultSet getAccountRs(byte[] addr) throws SQLException {
//
//        String accStr=Hex.toHexString(addr);
//
//        String sql="select ID,ADDRESS, NAME, NONCE, ISCONTRACT, LASTBLOCK, BALANCE, TXSTATEROOT from Account where address =X'" +accStr+"' ";
//        Statement st = conn.createStatement();
//        ResultSet rs = st.executeQuery(sql);
//        rs.first();
//        return rs;
//    }


//    public void write(LedgerEntry entry) throws SQLException {
//        write_sql(entry);
//    }
//    public void write_sql(LedgerEntry entry) throws SQLException {
//        Block block = replayBlock.getBlock();
//
//        PreparedStatement st = getInsertEntryStatement();
//
//        //String insEntrySql="insert into ledger
//        // (tx ,address ,amount ,block ,blocktimestamp,depth ,gasused ,fee ,entryType,offsetAccount,descr,GrossAmount)
//        // values(?,?,?,?,?,?,?,?,?,?,?,?)";
//
//        st.setBytes(1, entry.txhash);
//        st.setBytes(2, entry.Account.getAddress());
//        st.setBigDecimal(3, entry.amount);
//        st.setLong(4,block.getNumber());
//        st.setTimestamp(5, new Timestamp(block.getTimestamp() * 1000));
//        st.setByte(6, entry.depth);
//        st.setLong(7, entry.gasUsed);
//        st.setBigDecimal(8, entry.fee);
//        st.setByte(9, (byte) entry.entryType.ordinal());
//        st.setBytes(10,entry.offsetAccount.getAddress());
//        //st.setString(11, entry.extraData);
//        st.setString(11, "");
//        st.setBigDecimal(12, entry.grossAmount);
//        st.setByte(13,(byte) entry.entryResult.ordinal());
//
//        count+= st.executeUpdate();
//
//    }


}
