package stdweb.Core;

import org.ethereum.core.*;
import org.ethereum.crypto.HashUtil;
import org.ethereum.db.ByteArrayWrapper;
import org.ethereum.db.ContractDetails;
import org.ethereum.db.RepositoryImpl;
import org.ethereum.facade.Ethereum;
import org.ethereum.util.ByteUtil;
//import org.ethereum.vm.program.InternalTransaction;
import org.ethereum.vm.program.InternalTransaction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.spongycastle.util.encoders.Hex;
import stdweb.ethereum.EthereumBean;
import stdweb.ethereum.EthereumListener;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by bitledger on 28.09.15.
 */
public class LedgerStore {

    private static LedgerStore ledgerStore;
    private final Ethereum ethereum;
    private final EthereumListener listener;
    private int count;
    private ReplayBlock replayBlock;
    private PreparedStatement statInsertEntry;


    long lastSyncBlock;

    public SyncStatus getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(SyncStatus syncStatus) {
        this.syncStatus = syncStatus;
    }

    SyncStatus syncStatus;

    public void setNextStatus(SyncStatus nextStatus) {
        this.nextStatus = nextStatus;
    }

    public SyncStatus getNextStatus() {
        return nextStatus;
    }

    SyncStatus nextStatus;

    private Thread syncLedgerThread;

    public void setLastBlock() throws SQLException {
        this.lastSyncBlock = getSqlTopBlock();
        deleteBlocksFrom(lastSyncBlock +1);
        syncStatus=SyncStatus.stopped;

    }
    public void reloadFrom(long _from) throws SQLException {
        deleteBlocksFrom(_from);
        this.lastSyncBlock = _from;
        syncStatus=SyncStatus.bulkLoading;

    }



    public void ledgerBulkLoad() throws SQLException, InterruptedException {
        this.ledgerBulkLoad(getSqlTopBlock() + 1);
    }


    public void ledgerBulkLoad(long _block) throws SQLException, InterruptedException {

        System.out.println("Ledger Start BulkLoad from :"+_block);


        this.lastSyncBlock=_block;
        if (!syncLedgerThread.isAlive()) {
            createSyncLedgerThread();
            syncLedgerThread.start();
        }
    }

    public void stopSync()
    {
        System.out.println("stop BulkLoad");
        lastSyncBlock =1_000_000_000;
    }
    public synchronized  void  createSyncLedgerThread() throws SQLException, InterruptedException {

        System.out.println("create Ledger BulkLoadThread");
        EthereumBean.blockchainStopSync();
        LedgerStore ledgerStore = LedgerStore.getLedgerStore(listener);

        syncLedgerThread = new Thread(() -> {
            while (true)
            {
                syncStatus=SyncStatus.bulkLoading;
                if (lastSyncBlock <= ethereum.getBlockchain().getBestBlock().getNumber())
                    try {
                        ledgerStore.insertBlock(lastSyncBlock);
                        ++lastSyncBlock;

                    } catch (SQLException e) {
                        System.out.println("Error inserting block:"+ lastSyncBlock);
                        e.printStackTrace();
                    }
                else {
                    syncStatus = nextStatus;
                    break;
                }
//                    try {
//                        Thread.sleep(1000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }

            }
        });
    }
//    public void updateBatch() throws SQLException {
//
////        count+=stat.executeBatch().length;
////        conn.commit();
//    }

    public int getCount() {
        return count;
    }


    public String getBalance(Block block) throws SQLException {

        BigDecimal bigDecimal = getLedgerBlockBalance(block);


        BigInteger bi=BigInteger.valueOf(0);

        if (bigDecimal!= null)
            bi = bigDecimal.toBigInteger();

        //Long count=rs.getLong(2);

        BigInteger trieBalance = getTrieBalance(block).toBigInteger();


        return "{'balance' "+Convert2json.BI2ValStr(bi,false) +", count "+count+", 'triebalance'" +Convert2json.BI2ValStr(trieBalance,false)+ " }";
    }

    public BigDecimal getLedgerBlockDelta(Block block) throws SQLException {
        ResultSet rs;
        Statement statement = conn.createStatement();

        //String accStr = Hex.toHexString(account);
        //'f0134ff161a5c8f7c4f8cc33d3e1a7ae088594a9'
        String sql="select  sum(grossamount) amo, count(*) c from ledger  where block="+block.getNumber();
        rs = statement.executeQuery(sql);
        rs.first();

        return rs.getBigDecimal(1);
    }
    public BigDecimal getLedgerBlockBalance(Block block) throws SQLException {
        ResultSet rs;
        Statement statement = conn.createStatement();

        //String accStr = Hex.toHexString(account);
        //'f0134ff161a5c8f7c4f8cc33d3e1a7ae088594a9'
        String sql="select  sum(grossamount) amo, count(*) c from ledger  where block<="+block.getNumber();
        rs = statement.executeQuery(sql);
        rs.first();

        return rs.getBigDecimal(1);
    }


    public BigDecimal getTrieDelta(Block block) throws SQLException {

        BlockchainImpl blockchain = (BlockchainImpl) ethereum.getBlockchain();
        Repository track = blockchain.getRepository();

        Repository snapshot = track.getSnapshotTo(block.getStateRoot());

        BigInteger balance=BigInteger.valueOf(0);
        for (byte[] acc : snapshot.getAccountsKeys()) {
            balance=balance.add(snapshot.getBalance(acc));
        }

        if (block.getNumber()>0) {
            Block blockPrev = blockchain.getBlockByHash(block.getParentHash());
            Repository snapshotPrev = track.getSnapshotTo(blockPrev.getStateRoot());

            for (byte[] acc : snapshotPrev.getAccountsKeys()) {
                BigInteger balancePrev = snapshotPrev.getBalance(acc);
                balance = balance.subtract(balancePrev);
            }
        }

        return new BigDecimal(balance);
    }

    public BigDecimal getTrieBalance(Block block) throws SQLException {

        BlockchainImpl blockchain = (BlockchainImpl) ethereum.getBlockchain();
        Repository track = blockchain.getRepository();

        Repository snapshot = track.getSnapshotTo(block.getStateRoot());

        BigInteger balance=BigInteger.valueOf(0);
        for (byte[] acc : snapshot.getAccountsKeys()) {
            balance=balance.add(snapshot.getBalance(acc));
        }
        return new BigDecimal(balance);
    }

    private Connection conn;
    private PreparedStatement stat;

    public static JSONArray getJson(ResultSet resultSet,boolean calcBalance) throws Exception {
        JSONArray jsonArray = new JSONArray();

        BigDecimal balance=new BigDecimal(0);
        BigDecimal fee=new BigDecimal(0);
        BigDecimal received=new BigDecimal(0);
        BigDecimal sent=new BigDecimal(0);

        while (resultSet.next()) {

            int total_cols = resultSet.getMetaData().getColumnCount();
            JSONObject obj = new JSONObject();

            if (calcBalance) {
                balance = balance.add(resultSet.getBigDecimal("RECEIVED")).subtract(resultSet.getBigDecimal("SENT"));
                obj.put("BALANCE", Convert2json.BI2ValStr(balance.toBigInteger(), true));

                received = received.add(resultSet.getBigDecimal("RECEIVED"));
                sent = sent.add(resultSet.getBigDecimal("SENT"));
            }

            //fee

            for (int i = 1; i <= total_cols; i++) {
                //String columnTypeName = resultSet.getMetaData().getColumnTypeName(i);
                String columnLabel = resultSet.getMetaData().getColumnLabel(i);

                DataItem item= new DataItem("ledger", columnLabel, resultSet.getObject(i));
                obj.put(item.getKey(),item.getValue());
            }
            jsonArray.add(obj);
        }
        //add total row to json
        if (calcBalance) {
            JSONObject obj = new JSONObject();
            obj.put("BLOCK", "Total:");

            obj.put("RECEIVED", Convert2json.BI2ValStr(received.toBigInteger(), true));
            obj.put("RECEIVED", Convert2json.BI2ValStr(received.toBigInteger(), true));
            obj.put("SENT", Convert2json.BI2ValStr(sent.toBigInteger(), true));
            obj.put("FEE", Convert2json.BI2ValStr(fee.toBigInteger(), true));
            obj.put("BALANCE", Convert2json.BI2ValStr(balance.toBigInteger(), true));
            jsonArray.add(obj);
        }

        return jsonArray;

    }



    public static JSONArray RsToJSON(ResultSet resultSet)
            throws Exception {
        JSONArray jsonArray = new JSONArray();


        BigDecimal balance=new BigDecimal(0);
        BigDecimal fee=new BigDecimal(0);
        BigDecimal received=new BigDecimal(0);
        BigDecimal sent=new BigDecimal(0);


        while (resultSet.next()) {

            long block = resultSet.getLong("BLOCK");

            int total_cols = resultSet.getMetaData().getColumnCount();
            JSONObject obj = new JSONObject();

            fee=fee.add(resultSet.getBigDecimal("FEE"));

            if (resultSet.getBigDecimal("AMOUNT").signum()==1) {
                received = received.add(resultSet.getBigDecimal("AMOUNT"));
                obj.put("RECEIVED",Convert2json.BI2ValStr(resultSet.getBigDecimal("AMOUNT").toBigInteger(),true));
            }
            else {
                sent = sent.add(resultSet.getBigDecimal("AMOUNT"));
                obj.put("SENT",Convert2json.BI2ValStr(resultSet.getBigDecimal("AMOUNT").toBigInteger(),true));
            }

            balance=balance.add(resultSet.getBigDecimal("GROSSAMOUNT"));
            obj.put("BALANCE",Convert2json.BI2ValStr(balance.toBigInteger(),true));


            String e=EntryType.values()[resultSet.getInt("ENTRYTYPE")].toString();
            obj.put("ENTRYTYPE",e);

            for (int i = 1; i <= total_cols; i++) {

                String columnTypeName = resultSet.getMetaData().getColumnTypeName(i);

                String columnLabel = resultSet.getMetaData().getColumnLabel(i);

                switch (columnTypeName)
                {
                    case "DECIMAL":

                        obj.put(columnLabel, Convert2json.BI2ValStr(resultSet.getBigDecimal(i).toBigInteger(), true));
                        break;
                    case "VARBINARY":
                        obj.put(columnLabel,"0x"+Hex.toHexString(resultSet.getBytes(i)));break;
                    case "TINYINT":
//                        if (columnLabel.equals("ENTRYTYPE"))
//                            obj.put(columnLabel,EntryType.values()[resultSet.getInt(i)].toString());
//                        else
//                            obj.put(columnLabel,resultSet.getLong(i));

                        break;
                    case "TIMESTAMP":
                        obj.put(columnLabel,resultSet.getTimestamp(i).toString());break;
                    default:
                        obj.put(columnLabel,resultSet.getObject(i));
                }
                //obj.put(resultSet.getMetaData().getColumnLabel(i + 1).toLowerCase(), resultSet.getObject(i + 1));
            }


            jsonArray.add(obj);
        }
        //add total row to json
        JSONObject obj = new JSONObject();
        obj.put("BLOCK", "Total:");

        obj.put("RECEIVED",Convert2json.BI2ValStr(received.toBigInteger(),true));
        obj.put("RECEIVED",Convert2json.BI2ValStr(received.toBigInteger(),true));
        obj.put("SENT",Convert2json.BI2ValStr(sent.toBigInteger(),true));
        obj.put("FEE", Convert2json.BI2ValStr(fee.toBigInteger(), true));
        obj.put("BALANCE",Convert2json.BI2ValStr(balance.toBigInteger(), true));
        jsonArray.add(obj);

        return jsonArray;

    }


    public void insertLedgerEntry(LedgerEntry entry) throws SQLException {
        Block block = replayBlock.getBlock();

        PreparedStatement st = getInsertEntryStatement();

        //String insEntrySql="insert into ledger
        // (tx ,address ,amount ,block ,blocktimestamp,depth ,gasused ,fee ,entryType,offsetAccount,descr,GrossAmount)
        // values(?,?,?,?,?,?,?,?,?,?,?,?)";

        st.setBytes(1, entry.txhash);
        st.setBytes(2, entry.Account.getBytes());
        st.setBigDecimal(3, entry.amount);
        st.setLong(4,block.getNumber());
        st.setTimestamp(5, new Timestamp(block.getTimestamp() * 1000));
        st.setByte(6, entry.depth);
        st.setLong(7, entry.gasUsed);
        st.setBigDecimal(8, entry.fee);
        st.setByte(9, (byte) entry.entryType.ordinal());
        st.setBytes(10,entry.offsetAccount.getBytes());
        //st.setString(11, entry.extraData);
        st.setString(11, "");
        st.setBigDecimal(12, entry.grossAmount);

        count+= st.executeUpdate();

    }

    private PreparedStatement getInsertEntryStatement() throws SQLException {
        ensureConnection();

        return statInsertEntry;
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
            System.out.println("ensure connection - Restore!");
            conn = DriverManager.getConnection("jdbc:h2:~/testh2db", "sa", "");
            String insEntrySql="insert into ledger (tx ,address ,amount ,block ,blocktimestamp,depth ,gasused ,fee ,entryType,offsetAccount,descr,GrossAmount) values(?,?,?,?,?,?,?,?,?,?,?,?)";
            statInsertEntry = conn.prepareStatement(insEntrySql);
        }
        else
            System.out.println("ensure connection - Ok!");
    }

    public void truncateLedger() throws SQLException {
        String s="truncate table ledger";
        Statement statement = conn.createStatement();
        statement.execute(s);
        conn.commit();
    }

    public int getSqlTopBlock() throws SQLException {
        ResultSet rs;
        Statement statement = conn.createStatement();

        //String accStr = Hex.toHexString(account);
        //'f0134ff161a5c8f7c4f8cc33d3e1a7ae088594a9'
        String sql = "select max(block) as topblock from ledger";

        rs = statement.executeQuery(sql);

        boolean first = rs.first();

        return rs.getInt("topblock");

    }

    public int ledgerCount() throws SQLException {
        ResultSet rs;
        Statement statement = conn.createStatement();

        //String accStr = Hex.toHexString(account);
        //'f0134ff161a5c8f7c4f8cc33d3e1a7ae088594a9'
        String sql="select count(*) as c from ledger";

        rs = statement.executeQuery(sql);
        rs.first();
        return  rs.getInt("c");
    }

    public String LedgerSelectByBlock(String blockStr) throws SQLException {

        if (blockStr.startsWith("0x"))
            blockStr=blockStr.substring(2);

        ResultSet rs;
        Statement statement = conn.createStatement();

        //String accStr = Hex.toHexString(account);
        //'f0134ff161a5c8f7c4f8cc33d3e1a7ae088594a9'


        String sql="select  id   , tx ,address Receiver ,amount ,block ,blocktimestamp ,depth ,gasused ,fee ,entryType , offsetaccount sender, descr ," +
                " GrossAmount from ledger  where block =" +blockStr+
                " and entryType in ("+EntryType.FeeReward.ordinal()+","+EntryType.CoinbaseReward.ordinal()+", "+EntryType.UncleReward.ordinal()+")"+
                " order by id";



        String sql1="select  id   , tx ,address sender,abs(amount) amount ,block ,blocktimestamp ,depth ,gasused ,fee ,entryType , offsetaccount receiver, descr ," +
                " GrossAmount from ledger  where block =" +blockStr+
                " and entryType  in ("+EntryType.Send.ordinal()+","
                +EntryType.InternalCall.ordinal()
                +","+EntryType.Call.ordinal()
                +","+EntryType.NA.ordinal()
                +", "+EntryType.ContractCreation.ordinal()+")"+
                " order by id";
        try {
            System.out.println(sql1);
            rs = statement.executeQuery(sql);
            JSONArray jsonArray = getJson(rs,false);
            rs = statement.executeQuery(sql1);
            jsonArray.addAll(getJson(rs,false));


            System.out.println(jsonArray.toJSONString());
            return jsonArray.toJSONString();
        } catch (Exception e) {
            e.printStackTrace();
            return "{error :"+e.toString()+"}";
        }
    }

    public String LedgerSelect1(String accStr) throws SQLException {

        if (accStr.startsWith("0x"))
            accStr=accStr.substring(2);

        ResultSet rs;
        Statement statement = conn.createStatement();

        //String accStr = Hex.toHexString(account);
        //'f0134ff161a5c8f7c4f8cc33d3e1a7ae088594a9'
        String sql="select  id   , tx ,address ,amount ,block ,blocktimestamp ,depth ,gasused ,fee ,entryType , offsetaccount, descr ," +
                " GrossAmount from ledger  where address =X'" +accStr+"' "+
                "order by id";
        rs = statement.executeQuery(sql);

        try {
            JSONArray jsonArray = RsToJSON(rs);
            System.out.println(jsonArray.toJSONString());
            return jsonArray.toJSONString();
        } catch (Exception e) {
            e.printStackTrace();
            return "{error :"+e.toString()+"}";
        }
    }
    public String LedgerSelect(String accStr) throws SQLException {

        if (accStr.startsWith("0x"))
            accStr=accStr.substring(2);

        ResultSet rs;
        Statement statement = conn.createStatement();

        //String accStr = Hex.toHexString(account);
        //'f0134ff161a5c8f7c4f8cc33d3e1a7ae088594a9'
        String sql="select  id   , tx ,address, case when amount>0 then amount else 0 end as Received ,case when amount<0 then -amount else 0 end as sent, block ,blocktimestamp ,depth ,gasused ,fee ,entryType , offsetaccount, descr ," +
                " GrossAmount from ledger  where address =X'" +accStr+"' "
                ;//+                "order by id"; //"+EntryType.TxFee.ordinal()+" as
        sql +=" union all ";
        sql+=" select  id   , X'00' as tx ,address ,0 as received,fee as sent ,block ,blocktimestamp ,depth ,0 gasused, fee, "+EntryType.TxFee.ordinal()+" as  entryType , X'00' as offsetaccount, descr ," +
                " GrossAmount from ledger  where fee<>0 and address =X'" +accStr+"' "
                +                "order by id,entryType";
        rs = statement.executeQuery(sql);

        try {
            //JSONArray jsonArray = RsToJSON(rs);
            JSONArray jsonArray = getJson(rs,true);
            System.out.println(jsonArray.toJSONString());
            return jsonArray.toJSONString();
        } catch (Exception e) {
            e.printStackTrace();
            return "{error :"+e.toString()+"}";
        }
    }

    private void checkBalance() throws SQLException {
        BigDecimal trieBalance = BigDecimal.valueOf(0);
        BigDecimal ledgerBlockBalance = BigDecimal.valueOf(0);
        Block block = replayBlock.getBlock();

        trieBalance=getTrieBalance(replayBlock.getBlock());
        ledgerBlockBalance=getLedgerBlockBalance(block);
        long number = block.getNumber();


        //System.out.println("trieBalance");
        if (trieBalance.equals(ledgerBlockBalance))
            System.out.println("Block balance correct:" + number + ": " + Convert2json.BI2ValStr(trieBalance.toBigInteger(), false));
        else {
            System.out.println("Block balance incorrect:" + number + ", trie - ledger: " + Convert2json.BI2ValStr(trieBalance.toBigInteger(), false) + " - " + Convert2json.BI2ValStr(ledgerBlockBalance.toBigInteger(), false));
            //throw (new SQLException("Block balance incorrect:" + number + ", trie - ledger: " + Convert2json.BI2ValStr(trieBalance.toBigInteger(), false) + " - " + Convert2json.BI2ValStr(ledgerBlockBalance.toBigInteger(), false)));
        }
    }

    public void checkDelta() throws SQLException {
        BigDecimal trieDelta = BigDecimal.valueOf(0);
        BigDecimal ledgerBlockDelta = BigDecimal.valueOf(0);

        Block block = replayBlock.getBlock();
        trieDelta=getTrieDelta(block);
        ledgerBlockDelta=getLedgerBlockDelta(block);
        long number = block.getNumber();

        //System.out.println("trieBalance");
        if (trieDelta.equals(ledgerBlockDelta))
            System.out.println("Block delta correct:" + number + ": " + Convert2json.BI2ValStr(trieDelta.toBigInteger(), false));
        else {
            System.out.println("Block Delta incorrect:" + number + ", trie - ledger: " + Convert2json.BI2ValStr(trieDelta.toBigInteger(), false) + " - " + Convert2json.BI2ValStr(ledgerBlockDelta.toBigInteger(), false));
            //throw (new SQLException("Block balance incorrect:" + number + ", trie - ledger: " + Convert2json.BI2ValStr(trieBalance.toBigInteger(), false) + " - " + Convert2json.BI2ValStr(ledgerBlockBalance.toBigInteger(), false)));
        }
    }

    public synchronized void insertBlock(long blockNo) throws SQLException{
        this.replayBlock=new ReplayBlock(listener,blockNo);
        replayBlock.run();

        deleteBlocksFrom(blockNo);

        List<EntryType> rewardEntryTypes = Arrays.asList(EntryType.CoinbaseReward, EntryType.UncleReward, EntryType.FeeReward);

        //reward entries first
        replayBlock.getLedgerEntries()
                .stream().filter( e -> rewardEntryTypes.contains(e.entryType))
                .forEach(e -> {
                    try {
                        insertLedgerEntry(e);
                    } catch (SQLException e1) {
                        e1.printStackTrace();
                    }
                });

        replayBlock.getLedgerEntries()
                .stream().filter( e -> !rewardEntryTypes.contains(e.entryType))
                .forEach(e -> {
                    try {
                        insertLedgerEntry(e);
                    } catch (SQLException e1) {
                        e1.printStackTrace();
                    }
                });

        conn.commit();
        System.out.println("Ledger - block inserted:"+blockNo);
       //checkDelta();
    }

    public static LedgerStore getLedgerStore(EthereumListener listener)
    {

        if (ledgerStore==null)
            try {
                ledgerStore=new LedgerStore(listener);
                ledgerStore.init("h2");
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        return ledgerStore;
    }

    private LedgerStore(EthereumListener listener) throws SQLException {
        this.listener=listener;
        this.ethereum=listener.getEthereum();
        ensureConnection();
        conn.setAutoCommit(false);
        count=0;
        syncStatus=SyncStatus.stopped;
        nextStatus=SyncStatus.stopped;
    }

    private void initMsSql() throws Exception
    {
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        String connstr="jdbc:sqlserver://ledg.database.windows.net:1433;database=ledgerdb;user=std;password={Str,.ul11};encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;";
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
        //Connection conn = DriverManager.getConnection("jdbc:h2:~/testh2db", "sa", "");
        //System.out.println(conn);

        Statement statement = conn.createStatement();

        //stat.execute("drop table if exists LEDGER");
        statement.execute("create table if not exists ledger(id identity primary key, tx binary(32),address binary(20),amount decimal(31,0),block bigint,blocktimestamp timestamp," +
                "depth tinyint,gasused bigint,fee decimal(31,0),entryType tinyint,offsetaccount binary(20),descr varchar(32),GrossAmount decimal(31,0))");
        statement.execute("create index if not exists idx_ledger_address_tx on ledger(address,tx)");
        statement.execute("create index if not exists idx_ledger_tx on ledger(tx)");
        statement.execute("create index if not exists idx_ledger_block_id on ledger(block,id)");

        statement.execute("create table if not exists Block(id bigint primary key, TrieBalance decimal(31,0),blocktimestamp timestamp," +
                "hash binary(32),prevhash binary(32),stateRoot binary(32)," +
                "gasused bigint,fee decimal(31,0))");

        statement.execute("create index if not exists idx_block_hash_id on block(hash,id)");

        statement.close();


    }
    private void init(String sqltype) throws Exception {


        System.out.println("Init");

        switch (sqltype.toLowerCase())
        {
            case "mssql":
                initMsSql();break;
            case "h2":
                initH2();break;

        }






        //statInsertEntry = conn.prepareStatement(insEntrySql);
        ensureConnection();

        createSyncLedgerThread();



        ensureGenesis();

        //conn.close();
    }

    private void ensureGenesis() throws SQLException {
        if (getSqlTopBlock()==0 && ledgerCount()==0)
        {
            insertBlock(0);
        }
    }


    //______________________________________________________________________________________________________//
    //______________________________________________________________________________________________________//
    //______________________________________________________________________________________________________//
    //______________________________________________________________________________________________________//

    public void del_insertLedgerEntries(ReplayBlock _replayBlock) throws Exception {

        this.replayBlock=_replayBlock;

        deleteBlocksFrom(replayBlock.getBlock().getNumber());

        String sql="insert into ledger (tx ,address ,amount ,block ,blocktimestamp,depth ,gasused ,fee ,entryType,offsetAccount,descr,GrossAmount) values(?,?,?,?,?,?,?,?,?,?,?,?)";

            stat = conn.prepareStatement(sql);

        if (replayBlock.getBlock().getNumber()==0) {
            del_loadGenesis();
            del_insertBlock(_replayBlock);
            return;
        }
        replayBlock.run();
        del_insertCoinbaseEntry();

            List<Transaction> txList = replayBlock.getTxList();
            int ind=-1;
            for (Transaction tx:txList)
            {
                ind++;
                del_insertLedgerEntry(tx, replayBlock, ind);
            }

        del_insertBlock(_replayBlock);

        conn.commit();

        //System.out.println("insert ledger block#"+replayBlock.getBlock().getNumber());


    }



    private void del_loadGenesis() throws SQLException {

        org.ethereum.core.Repository snapshot = ((RepositoryImpl) ethereum.getRepository()).getSnapshotTo(Hex.decode("d7f8974fb5ac78d9ac099b9ad5018bedc2ce0a72dad1827a1709da30580f0544"));
        Block block = ethereum.getBlockchain().getBlockByNumber(0);


        Set<byte[]> accountsKeys = snapshot.getAccountsKeys();

        for (byte[] account : accountsKeys)
        {

            System.out.println("acc:"+Hex.toHexString(account));

            BigDecimal balance = new BigDecimal(snapshot.getBalance(account));

            stat.setBytes(1, HashUtil.EMPTY_DATA_HASH);
            stat.setBytes(2,account);
            stat.setBigDecimal(3,balance);
            stat.setLong(4,block.getNumber()); //genesis blockno 0
            stat.setTimestamp(5, new Timestamp(1438269973000l)); //genesis timestamp
            stat.setByte(6, (byte) 0);//depth
            stat.setLong(7,0);//gasused

            stat.setBigDecimal(8, new BigDecimal(0));
            stat.setByte(9,(byte)EntryType.Genesis.ordinal());
            stat.setBytes(10, ByteUtil.ZERO_BYTE_ARRAY);//offset account - genesis
            stat.setString(11,"Genesis balance entry");
            stat.setBigDecimal(12,balance  );

           // stat.addBatch();
            stat.executeUpdate();
        }
    }
    private EntryType del_getEntryType(Transaction tx, String entryType) {
        EntryType entry_type= EntryType.NA;

        if (tx.isContractCreation())
            if (entryType.equals("send"))
                return EntryType.ContractCreation;
            else
                return EntryType.ContractCreated;

        if (tx instanceof InternalTransaction)
            return EntryType.InternalCall;

        if (entryType.equals("send") &&tx.getContractAddress()==null)
        {
            byte[] receiveAddress = tx.getReceiveAddress();
            RepositoryImpl repository = (RepositoryImpl)ethereum.getRepository();
            AccountState accountState = repository.getAccountState(receiveAddress);
            ContractDetails contractDetails = repository.getContractDetails(receiveAddress);

            if (contractDetails.getCode().length==0)
                return EntryType.Send;
            else
                return EntryType.Call;

        }


        if (entryType.equals("receive"))
            entry_type= EntryType.Receive;

        return entry_type;
    }

    private void del_insertBlock(ReplayBlock replayBlock) throws SQLException {

        Block block = replayBlock.getBlock();


        //String sql="insert into block (id ,TrieBalance, blocktimestamp,hash,prevhash,stateroot,gasused,fee) values(?,?,?,?,?,?,?,?)";
        //stat = conn.prepareStatement(sql);

        BigDecimal trieBalance = BigDecimal.valueOf(0);
        BigDecimal ledgerBlockBalance = BigDecimal.valueOf(0);

        BigDecimal trieDelta = BigDecimal.valueOf(0);
        BigDecimal ledgerBlockDelta = BigDecimal.valueOf(0);

        long number = block.getNumber();

        //46859
        if (number >= 46147) {
            trieBalance=getTrieBalance(replayBlock.getBlock());
            ledgerBlockBalance=getLedgerBlockBalance(block);

            trieDelta=getTrieDelta(replayBlock.getBlock());
            ledgerBlockDelta=getLedgerBlockDelta(block);

            //System.out.println("trieBalance");
            if (trieDelta.equals(ledgerBlockDelta))
                System.out.println("Block delta correct:" + number + ": " + Convert2json.BI2ValStr(trieDelta.toBigInteger(), false));
            else {
                System.out.println("Block Delta incorrect:" + number + ", trie - ledger: " + Convert2json.BI2ValStr(trieDelta.toBigInteger(), false) + " - " + Convert2json.BI2ValStr(ledgerBlockDelta.toBigInteger(), false));
                //throw (new SQLException("Block balance incorrect:" + number + ", trie - ledger: " + Convert2json.BI2ValStr(trieBalance.toBigInteger(), false) + " - " + Convert2json.BI2ValStr(ledgerBlockBalance.toBigInteger(), false)));
            }

            //System.out.println("trieBalance");
            if (trieBalance.equals(ledgerBlockBalance))
                System.out.println("Block balance correct:" + number + ": " + Convert2json.BI2ValStr(trieBalance.toBigInteger(), false));
            else {
                System.out.println("Block balance incorrect:" + number + ", trie - ledger: " + Convert2json.BI2ValStr(trieBalance.toBigInteger(), false) + " - " + Convert2json.BI2ValStr(ledgerBlockBalance.toBigInteger(), false));
                //throw (new SQLException("Block balance incorrect:" + number + ", trie - ledger: " + Convert2json.BI2ValStr(trieBalance.toBigInteger(), false) + " - " + Convert2json.BI2ValStr(ledgerBlockBalance.toBigInteger(), false)));
            }

            try {
                Thread.sleep(000);;//2000
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }



//        stat.setLong(1, replayBlock.getBlock().getNumber());
//        stat.setBigDecimal(2, trieBalance);
//        stat.setTimestamp(3, new Timestamp(block.getTimestamp()*1000));
//        stat.setBytes(4,block.getHash());
//        stat.setBytes(5,block.getParentHash());
//        stat.setBytes(6,block.getStateRoot());
//        stat.setLong(7,block.getGasUsed());
//        stat.setLong(8,0);
//
//        stat.executeUpdate();


        // conn.commit();

    }

    private BigInteger del_getFee(Transaction tx, long gasUsed, EntryType entry_type, String entryType) {
        //if (entry_type==EntryType.Receive)
        if (entryType=="receive")
            return BigInteger.valueOf(0);
        else
            return (new BigInteger(1, tx.getGasPrice())).multiply(BigInteger.valueOf(gasUsed));
    }
    public void  del_insertLedgerEntry(Transaction tx,String entryType,ReplayBlock replayBlock, int ind) throws SQLException {

        Block block = replayBlock.getBlock();




        if (tx instanceof InternalTransaction)
            stat.setBytes(1,((InternalTransaction)tx).getParentHash());
        else
            stat.setBytes(1,tx.getHash());


        BigInteger biVal=new BigInteger(1,tx.getValue());
        if (entryType.equals("send")) {
            stat.setBytes(2, tx.getSender());

            stat.setBytes(10,(tx.getContractAddress()==null? tx.getReceiveAddress():tx.getContractAddress())); //offset account
            biVal=biVal.negate();
        }
        else {
            stat.setBytes(10,tx.getSender());//offset account

            if (tx.getReceiveAddress()==null)
                stat.setBytes(2,tx.getContractAddress());
            else
                stat.setBytes(2, tx.getReceiveAddress());
        }


        stat.setBigDecimal(3, new BigDecimal(biVal));


        stat.setLong(4, block.getNumber());

        stat.setTimestamp(5, new Timestamp(block.getTimestamp()*1000));


        String indStr=String.valueOf(ind);
        byte depth=0;
        if (tx instanceof InternalTransaction)
        {
            depth=(byte)(((InternalTransaction) tx).getDeep()+1);
        }
        if (tx instanceof InternalTransaction) {
            indStr += ":" + ((InternalTransaction) tx).getIndex();
            if (((InternalTransaction) tx).getDeep()!=0)
                indStr+= " (deep " + ((InternalTransaction) tx).getDeep() + ")";
        }


        stat.setByte(6, depth);

        long gasUsed=0;
        if (replayBlock.getTxGasUsedList().containsKey(tx))
            gasUsed=replayBlock.getTxGasUsedList().get(tx);

        stat.setLong(7,gasUsed);


        EntryType entry_type = del_getEntryType(tx, entryType);

        BigInteger fee = del_getFee(tx, gasUsed, entry_type, entryType);
        stat.setBigDecimal(8, new BigDecimal(fee));


        stat.setByte(9,(byte)entry_type.ordinal());

        stat.setString(11,"Descr ");


        BigDecimal bigDecimal = new BigDecimal(biVal.subtract(fee));
        stat.setBigDecimal(12,bigDecimal  );

        //stat.executeUpdate();


        count+=stat.executeUpdate();
        //System.out.println("insertion status :" + stat.executeUpdate());

    }
    public void  del_insertLedgerEntry(Transaction tx,ReplayBlock replayBlock,int ind) throws SQLException {

        del_insertLedgerEntry(tx, "send", replayBlock, ind);
        del_insertLedgerEntry(tx, "receive", replayBlock, ind);

    }

    void del_insertCoinbaseEntry() throws SQLException {

        Block block = replayBlock.getBlock();
        byte[] coinbase = block.getCoinbase();

        HashSet<ByteArrayWrapper> accounts=new HashSet<>();
        accounts.add(new ByteArrayWrapper(coinbase));


        block.getUncleList().forEach(uncle -> accounts.add(new ByteArrayWrapper(uncle.getCoinbase())));

        for (ByteArrayWrapper acc : accounts) {

            BigDecimal delta = new BigDecimal(replayBlock.getAccountDelta(acc.getData()));


            stat.setBytes(1, HashUtil.EMPTY_DATA_HASH);
            stat.setBytes(2,acc.getData());//address
            stat.setBigDecimal(3,delta);
            stat.setLong(4,block.getNumber()); //genesis blockno 0
            stat.setTimestamp(5, new Timestamp(block.getTimestamp()*1000)); //genesis timestamp
            stat.setByte(6, (byte) 0);//depth
            stat.setLong(7,0);//gasused

            stat.setBigDecimal(8, new BigDecimal(0));//fee

            if (acc.equals(new ByteArrayWrapper(coinbase))) {
                stat.setByte(9, (byte) EntryType.CoinbaseReward.ordinal());
                stat.setString(11,"Coinbase reward entry");
            }
            else {
                stat.setByte(9, (byte) EntryType.UncleReward.ordinal());
                stat.setString(11,"Uncle reward entry");
            }

            stat.setBytes(10, ByteUtil.ZERO_BYTE_ARRAY);//offset account - genesis

            stat.setBigDecimal(12,delta  );

            // stat.addBatch();
            stat.executeUpdate();
        }
    }
}
