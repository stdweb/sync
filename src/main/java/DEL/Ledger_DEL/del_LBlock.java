package DEL.Ledger_DEL;

import org.ethereum.core.BlockHeader;
import org.spongycastle.util.encoders.Hex;
import stdweb.Core.Sha3Hash;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.util.HashMap;

import static stdweb.Core.Convert2json.*;

/**
 * Created by bitledger on 08.11.15.
 */
public class del_LBlock extends BlockHeader {
    private Sha3Hash hash;
    private BigDecimal fee;
    private BigDecimal balance;
    private BigDecimal reward;
    private int size;

    public int getUncles_count() {
        return uncles_count;
    }

    public void setUncles_count(int uncles_count) {
        this.uncles_count = uncles_count;
    }

    private int uncles_count;

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    private boolean dirty;

    public int getTxcount() {
        return txcount;
    }

    public void setTxcount(int txcount) {
        this.txcount = txcount;
    }

    private int txcount;

    protected del_LBlock(byte[] parentHash, byte[] unclesHash, byte[] coinbase,
                         byte[] logsBloom, byte[] difficulty, long number,
                         long gasLimit, long gasUsed, long timestamp,
                         byte[] extraData, byte[] mixHash, byte[] nonce) {
        super(parentHash,unclesHash,coinbase,logsBloom,difficulty,number,
                null,//gasLimit,
                gasUsed,timestamp,extraData,mixHash,nonce);
//        this.parentHash = parentHash;
//        this.unclesHash = unclesHash;
//        this.coinbase = coinbase;
//        this.logsBloom = logsBloom;
//        this.difficulty = difficulty;
//        this.number = number;
//        this.gasLimit = gasLimit;
//        this.gasUsed = gasUsed;
//        this.timestamp = timestamp;
//        this.extraData = extraData;
//        this.mixHash = mixHash;
//        this.nonce = nonce;
//        this.stateRoot = HashUtil.EMPTY_TRIE_HASH;
        this.setDirty(true);
    }


    public void setParentHash(byte[] b)
    {
        throw new UnsupportedClassVersionError("parent hash cannot be changed");
    }

    public static del_LBlock reload(ResultSet rs) throws SQLException {
        del_LBlock lb = new del_LBlock(
//rs.getLong(1, header.getNumber());
//rs.getBytes(2, blockHash.getBytes());
//ID,HASH, PARENTHASH, UNCLESHASH, COINBASE, STATEROOT, TXTRIEROOT,
// RECEIPTTRIEROOT, LOGSBLOOM, DIFFICULTY, TIMESTAMP, GASLIMIT, GASUSED, MIXHASH, EXTRADATA, NONCE, FEE, TRIEBALANCE
                rs.getBytes("PARENTHASH"),
                rs.getBytes("UNCLESHASH"),
                rs.getBytes("COINBASE"),
                rs.getBytes("LOGSBLOOM"),
                BigInteger.valueOf(rs.getLong("DIFFICULTY")).toByteArray(),
                rs.getLong("ID"),
                rs.getLong("GASLIMIT"),
                rs.getLong("GASUSED"),
                rs.getTimestamp("TIMESTAMP").getTime(),
                rs.getBytes("EXTRADATA"),
                rs.getBytes("MIXHASH"),
                BigInteger.valueOf(rs.getLong("NONCE")).toByteArray());

        lb.setHash(new Sha3Hash(rs.getBytes("HASH")));
        lb.setStateRoot(rs.getBytes("STATEROOT"));

        lb.setTransactionsRoot(rs.getBytes("TXTRIEROOT"));
        lb.setReceiptsRoot(rs.getBytes("RECEIPTTRIEROOT"));

        lb.setFee(rs.getBigDecimal("FEE"));
        lb.setBalance(rs.getBigDecimal("TRIEBALANCE"));
        lb.setReward(rs.getBigDecimal("REWARD"));
        lb.setTxcount(rs.getInt("TXCOUNT"));
        lb.setSize(rs.getInt("BLOCKSIZE"));
        lb.setUncles_count(rs.getInt("UNCLESCOUNT"));
        lb.setDirty(false);

        return lb;
    }


    public String toJSON() throws SQLException {

        del_LBlock block=this;
        HashMap<String, String> hashMap = new HashMap<>();

        hashMap.put("height",String.valueOf(block.getNumber()) );
        hashMap.put("hash",addParentheses("0x"+ Hex.toHexString(block.getHash())));
        hashMap.put("parenthash",addParentheses("0x"+Hex.toHexString(block.getParentHash())));
        hashMap.put("stateroot",addParentheses("0x"+Hex.toHexString(block.getStateRoot())));
        hashMap.put("receiptroot",addParentheses("0x"+Hex.toHexString(block.getReceiptsRoot())));
        hashMap.put("txtrieroot",addParentheses("0x"+Hex.toHexString(block.getTxTrieRoot())));

        hashMap.put("difficulty",String.valueOf(new BigInteger(block.getDifficulty()) ));
        hashMap.put("coinbase",addParentheses("0x"+Hex.toHexString(block.getCoinbase())));


        hashMap.put("gasused",addParentheses(Num2ValStr(block.getGasUsed(),true )));
        hashMap.put("uncles", String.valueOf(block.getUncles_count()));
        hashMap.put("timestamp",addParentheses(convertTimestamp2str(
                block.getNumber()==0 ? 1438269973 :block.getTimestamp())));

        hashMap.put("txcount",addParentheses(Num2ValStr(block.getTxcount(), true) ));
        hashMap.put("size",addParentheses(Num2ValStr(block.getSize(), true) ));
        hashMap.put("ENTRYRESULT",addParentheses("Ok"));

        //BigInteger blockFee = new ReplayBlock(listener, block).getBlockFee();
        //BigDecimal ledgerBlockTxFee = LedgerStore.getLedgerStore().getLedgerBlockTxFee(block);

        hashMap.put("txfee",addParentheses(BD2ValStr(block.getFee(), true)));

        //ReplayBlock replayBlock = new ReplayBlock( block);
        //BigInteger blockReward = replayBlock.getBlockReward();
        //BigInteger totalUncleReward = replayBlock.getTotalUncleReward();


        hashMap.put("reward",addParentheses(BD2ValStr(block.getReward(),true)));
        //hashMap.put("UncleReward",addParentheses(BI2ValStr(totalUncleReward,true)));


        return map2json(hashMap);
    }



    public void setHash(Sha3Hash hash) {
        this.hash = hash;
        this.setDirty(true);
    }

//    public Sha3Hash getHash() {
//        return hash;
//    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
        this.setDirty(true);
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
        this.setDirty(true);
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setReward(BigDecimal reward) {
        this.reward = reward;
        this.setDirty(true);
    }

    public BigDecimal getReward() {
        return reward;
    }

    public void setSize(int size) {
        this.size = size;
        this.setDirty(true);
    }

    public int getSize() {
        return size;
    }

//    public void load(ResultSet rs) throws SQLException {
//        //String sql="select ID,ADDRESS, NAME, NONCE, ISCONTRACT, LASTBLOCK, BALANCE, TXSTATEROOT from Account where address =X'" +"' ";
//        if (rs.isFirst()) {
//            rs.getBytes("PARENTHASH"),
//                    rs.getBytes("UNCLESHASH"),
//                    rs.getBytes("COINBASE"),
//                    rs.getBytes("LOGSBLOOM"),
//                    BigInteger.valueOf(rs.getLong("DIFFICULTY")).toByteArray(),
//                    rs.getLong("ID"),
//                    rs.getLong("GASLIMIT"),
//                    rs.getLong("GASUSED"),
//                    rs.getTimestamp("TIMESTAMP").getTime(),
//                    rs.getBytes("EXTRADATA"),
//                    rs.getBytes("MIXHASH"),
//                    BigInteger.valueOf(rs.getLong("NONCE")).toByteArray());
//        }
//    }
}
