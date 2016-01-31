package DEL.Ledger_DEL;

import org.ethereum.core.Block;
import org.ethereum.core.BlockHeader;
import org.spongycastle.util.encoders.Hex;
import stdweb.Core.AddressDecodeException;
import stdweb.Core.HashDecodeException;
import stdweb.Core.Sha3Hash;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;

import static stdweb.Core.Utils.hash_decode;

/**
 * Created by bitledger on 09.11.15.
 */
public class LedgerBlockStore extends SqlStore<del_LBlock> {

    static LedgerBlockStore store;

//    void commit() throws SQLException {
//        conn .commit();
//    }

    public static LedgerBlockStore getInstance() throws SQLException {
        store= store==null ? new LedgerBlockStore() :store;
        return store;
    }
    public void write(del_LBlock _block) throws SQLException {

            ResultSet rs = get_rs(_block.getNumber());
            if (!rs.isFirst())
            {
                insert(_block);
            //_block.load(rs);
            return;}


        String sql="UPDATE BLOCK SET " +
               // "ID,HASH, PARENTHASH, UNCLESHASH, COINBASE, STATEROOT, TXTRIEROOT, RECEIPTTRIEROOT, LOGSBLOOM, " +
               // "DIFFICULTY, TIMESTAMP, GASLIMIT, GASUSED, MIXHASH, EXTRADATA, NONCE, " +
                "FEE =? , TRIEBALANCE=?, REWARD=? ,UNCLESCOUNT=?" +
                "WHERE ID="+_block.getNumber();

        PreparedStatement st=conn.prepareStatement(sql);

        st.setBigDecimal(1, _block.getFee());
        st.setBigDecimal(2,_block.getBalance());
        st.setBigDecimal(3,_block.getReward());
        st.setInt(4,_block.getUncles_count());

        st.executeUpdate();

        commit();

        _block.reload(get_rs(_block.getNumber()));
    }

    @Override
    public del_LBlock create(String s) throws SQLException, AddressDecodeException {
        throw new SQLDataException("LedgerBlock.create(string ) not supported.");

    }

//    public  LedgerBlock getOrCreateLedgerAccount(byte[] address) throws SQLException {
//        ResultSet rs = getBlockRs(address);
//        if (rs.isFirst())
//            return new LedgerBlock(rs);
//            //INSERT INTO ACCOUNT (ADDRESS, NAME, NONCE, ISCONTRACT, LASTBLOCK, BALANCE, TXSTATEROOT) VALUES (?,?,?,?,?,?,?);
//        else {
//            return new LedgerBlock();
//        }
//    }

//    public  LedgerBlock getOrCreateLedgerBlock(String accStr) throws SQLException {
//
//        accStr= Utils.remove0x(accStr);
//        byte[] decode = Hex.decode(accStr);
//        return getOrCreateLedgerAccount(decode);
//    }

    public del_LBlock get(Long number) throws SQLException {
        ResultSet rs = get_rs(number);
        if (rs.isFirst())
            return del_LBlock.reload(rs);
        else
            return null;
    }
    public del_LBlock get(String blockId) throws SQLException, HashDecodeException {
        return get( hash_decode(blockId));
    }
    public del_LBlock get(byte[] hash) throws SQLException {
        ResultSet rs = get_rs(hash);
        if (rs.isFirst())
            return del_LBlock.reload(rs);
        else
            return null;
    }


    public del_LBlock create(Block block, BigDecimal fee, BigDecimal reward, BigDecimal trieBalance) throws SQLException {
        //super(parentHash,unclesHash,coinbase,logsBloom,difficulty,number,gasLimit,gasUsed,timestamp,extraData,mixHash,nonce);

        if (get_rs(block.getNumber()).isFirst())
            throw new SQLDataException("Block already exists: "+block.getNumber());

        BlockHeader header = block.getHeader();
        Sha3Hash blockHash = new Sha3Hash(block.getHash());

        del_LBlock ledgerBlockDel = new del_LBlock(
                header.getParentHash(), header.getUnclesHash(), header.getCoinbase(),
                header.getLogsBloom(),
                new BigInteger(1, header.getDifficulty()).toByteArray(), header.getNumber(),
                header.getGasLimit(),
                header.getGasUsed(), header.getTimestamp() ,
                header.getExtraData(), header.getMixHash(),
                new BigInteger(1, header.getNonce()).toByteArray());
        //header.getTxTrieRoot()
        //header.getReceiptsRoot()
        ledgerBlockDel.setBalance(trieBalance);
        ledgerBlockDel.setReward(reward);
        ledgerBlockDel.setFee(fee);
        ledgerBlockDel.setHash(blockHash);

        ledgerBlockDel.setTransactionsRoot(block.getTxTrieRoot());
        ledgerBlockDel.setReceiptsRoot(block.getReceiptsRoot());
        ledgerBlockDel.setStateRoot(block.getStateRoot());
        ledgerBlockDel.setTxcount(block.getTransactionsList().size());
        ledgerBlockDel.setSize(block.getEncoded().length);
        ledgerBlockDel.setUncles_count(block.getUncleList().size());


        ReplayBlock_DEL replayBlock = new ReplayBlock_DEL( block);
        BigInteger blockReward = replayBlock.getBlockReward();
        BigInteger totalUncleReward = replayBlock.getTotalUncleReward();
        BigInteger totalReward = blockReward.add(totalUncleReward);
        ledgerBlockDel.setReward(new BigDecimal(totalReward));

        ledgerBlockDel.setDirty(true);

        return ledgerBlockDel;

    }
//    public LedgerBlock create2(Block block, Sha3Hash blockHash, BigDecimal fee, BigDecimal reward, BigDecimal trieBalance) throws SQLException {
//
//        ResultSet rs = getBlockRs(block.getNumber());
//        if (rs.isFirst())
//            throw new SQLDataException("Block already exists. block#"+block.getNumber());
//
//        BlockHeader header = insert(block, blockHash, fee, reward, trieBalance);
//
//        return  LedgerBlock.reload(getBlockRs(header.getNumber()));
//    }



    //private BlockHeader insert(Block block, Sha3Hash blockHash, BigDecimal fee, BigDecimal reward, BigDecimal trieBalance) throws SQLException {
    protected del_LBlock insert(del_LBlock block) throws SQLException {
        //BlockHeader header=block.getHeader();
        //LedgerBlock header;//=new LedgerBlock();

        if (get_rs(block.getNumber()).isFirst())
            throw new SQLDataException("Block already exists: "+(block.getNumber()));

        int txcount = block.getTxcount();
        int blocksize = block.getSize();

        PreparedStatement st = st_ins;
        //ID,HASH, PARENTHASH, UNCLESHASH, COINBASE, STATEROOT, TXTRIEROOT,
        // RECEIPTTRIEROOT, LOGSBLOOM, DIFFICULTY, TIMESTAMP, GASLIMIT, GASUSED, MIXHASH, EXTRADATA, NONCE, FEE, TRIEBALANCE
        st.setLong(1,block.getNumber());
        st.setBytes(2,block.getHash());
        st.setBytes(3,block.getParentHash());
        st.setBytes(4,block.getUnclesHash());
        st.setBytes(5,block.getCoinbase());
        st.setBytes(6,block.getStateRoot());

        st.setBytes(7,block.getTxTrieRoot());
        st.setBytes(8,block.getReceiptsRoot());
        st.setBytes(9,block.getLogsBloom());
        st.setLong(10, new BigInteger(1,block.getDifficulty()).longValue());
        st.setTimestamp(11, new Timestamp(block.getTimestamp()));
        st.setLong(12, block.getGasLimit());
        st.setLong(13,block.getGasUsed());
        st.setBytes(14,block.getMixHash());
        st.setBytes(15,block.getExtraData());
        st.setLong(16,new BigInteger(1,block.getNonce()).longValue());
        st.setBigDecimal(17,block.getFee());

        st.setBigDecimal(18,block.getBalance());
        st.setBigDecimal(19,block.getReward());
        st.setInt(20,txcount);
        st.setInt(21,blocksize);
        st.setInt(22,block.getUncles_count());

        st.executeUpdate();
        commit();

        return block;
    }

    protected ResultSet get_rs(Long blockNumber) throws SQLException {

        String sql="SELECT ID,HASH, PARENTHASH, UNCLESHASH, COINBASE, STATEROOT, TXTRIEROOT, RECEIPTTRIEROOT, LOGSBLOOM, DIFFICULTY, " +
                "TIMESTAMP, GASLIMIT, GASUSED, MIXHASH, EXTRADATA, NONCE, FEE, TRIEBALANCE,REWARD,TXCOUNT,BLOCKSIZE ,UNCLESCOUNT " +
                "FROM BLOCK where id="+blockNumber;
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(sql);
        rs.first();
        return rs;
    }
    protected ResultSet get_rs(byte[] hash) throws SQLException {

        String sql="SELECT ID,HASH, PARENTHASH, UNCLESHASH, COINBASE, STATEROOT, TXTRIEROOT, RECEIPTTRIEROOT, LOGSBLOOM, DIFFICULTY, " +
                "TIMESTAMP, GASLIMIT, GASUSED, MIXHASH, EXTRADATA, NONCE, FEE, TRIEBALANCE,REWARD,TXCOUNT,BLOCKSIZE ,UNCLESCOUNT " +
                "FROM BLOCK where hash=X'"+Hex.toHexString(hash)+"'";
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(sql);
        rs.first();
        return rs;
    }
    protected ResultSet get_rs(String hash) throws SQLException, HashDecodeException {
        return get_rs(hash_decode(hash));
    }



    LedgerBlockStore() throws SQLException {
        //conn=LedgerStore.getLedgerStore().getConn();


    }



    protected void initH2() throws SQLException, AddressDecodeException {
        Statement statement = conn.createStatement();

        statement.execute("create TABLE if not exists BLOCK (\n" +
                "    ID INTEGER not null,\n" +
                "    HASH VARBINARY,\n" +
                "    PARENTHASH VARBINARY,\n" +
                "    UNCLESHASH VARBINARY,\n" +
                "    COINBASE VARBINARY,\n" +
                "    STATEROOT VARBINARY,\n" +
                "    TXTRIEROOT VARBINARY,\n" +
                "    RECEIPTTRIEROOT VARBINARY,\n" +
                "    LOGSBLOOM VARBINARY,\n" +
                "    DIFFICULTY BIGINT,\n" +
                "    \"TIMESTAMP\" TIMESTAMP,\n" +
                "    GASLIMIT BIGINT,\n" +
                "    GASUSED BIGINT,\n" +
                "    MIXHASH VARBINARY,\n" +
                "    EXTRADATA VARBINARY,\n" +
                "    NONCE BIGINT,\n" +
                "    FEE DECIMAL(31, 0),\n" +
                "    TRIEBALANCE DECIMAL(31, 0),\n" +
                "    REWARD DECIMAL(31, 0),\n" +
                "    TXCOUNT INTEGER,\n" +
                "    BLOCKSIZE INTEGER,\n" +
                "    ENCODED VARBINARY,\n" +
                "    UNCLESCOUNT INTEGER,\n"+
                "    PRIMARY KEY (ID)\n" +
                ");\n"
                );

        statement.execute("CREATE UNIQUE INDEX if not exists IDX_BLOCK_HASH_ID ON BLOCK (HASH, ID);");
        commit();
        String insBlockHeaderSql="INSERT INTO BLOCK ( ID,HASH, PARENTHASH, UNCLESHASH, COINBASE, " +
                "STATEROOT, TXTRIEROOT, RECEIPTTRIEROOT, LOGSBLOOM, DIFFICULTY, TIMESTAMP, GASLIMIT, " +
                "GASUSED, MIXHASH, EXTRADATA, NONCE, FEE, TRIEBALANCE,REWARD," +
                "TXCOUNT, BLOCKSIZE, UNCLESCOUNT )\n" +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ";
        st_ins = conn.prepareStatement(insBlockHeaderSql);


//        if (LedgerAccount.GenesisAccount()==null) {
//            LedgerAccount genesisaccount = create(address_decode("0000000000000000000000000000000000000000"));
//            genesisaccount.setName("Genesis");
//            write(genesisaccount);
//            commit();
//        }

        statement.close();
    }

    public del_LBlock getTopBlock() throws SQLException {
        String sql="select max(id) from block";
        Statement statement = conn.createStatement();
        ResultSet rs = statement.executeQuery(sql);

        rs.first();
        if (rs.isFirst())
            return get(rs.getLong(1));
        else
            return null;
    }


}
