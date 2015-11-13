package stdweb.Ledger;

import org.ethereum.core.Block;
import org.ethereum.core.BlockchainImpl;
import org.ethereum.core.Repository;
import org.ethereum.db.ByteArrayWrapper;
import org.ethereum.db.ContractDetails;
import org.ethereum.db.RepositoryImpl;
import org.spongycastle.util.encoders.Hex;
import stdweb.Core.AddressDecodeException;
import stdweb.ethereum.EthereumBean;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;

import static stdweb.Core.Utils.address_decode;

/**
 * Created by bitledger on 09.10.15.
 */
public class LedgerAccount implements IAccount {

    boolean dirty;
    private long lastBlock;

    //private ContractDetails contractDetails;

    LedgerAccount(String addr) throws AddressDecodeException {
        this(address_decode(addr));
    }
    public boolean isNew()
    {
        return Id==null;
    }

    Long Id;
    ByteArrayWrapper addrWrapper;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;this.dirty=true;
    }

    String name;

    public long getNonce() {
        return nonce;
    }

    public void setNonce(long nonce) {
        this.nonce = nonce;
        this.dirty=true;
    }

    long nonce;
    boolean isContract;
    long lastBlockNumber;
    BigDecimal balance;
    byte[] stateRoot;
    public byte[] getAddress() { return addrWrapper.getData(); }

    public void reload(ResultSet rs) throws SQLException {
        //String sql="select ID,ADDRESS, NAME, NONCE, ISCONTRACT, LASTBLOCK, BALANCE, TXSTATEROOT from Account where address =X'" +"' ";
        if (rs.isFirst()) {
            this.Id = rs.getLong(1);
            this.addrWrapper = new ByteArrayWrapper(rs.getBytes(2));
            this.name = rs.getString(3);
            this.nonce = rs.getLong(4);
            this.isContract = rs.getBoolean(5);
            this.lastBlockNumber = rs.getLong(6);
            this.balance = rs.getBigDecimal(7);
            this.stateRoot = rs.getBytes(8);
            this.dirty=false;
        }
    }

    private void reload() throws SQLException {
        this.reload(AccountStore.getInstance().get_rs(getAddress()));
    }
    LedgerAccount(ResultSet rs) throws SQLException {
        this.reload(rs);
    }

    public static LedgerAccount GenesisAccount()  {
        try {
            return AccountStore.getInstance().get(address_decode("0000000000000000000000000000000000000000"));
        }
        catch (SQLException e) {
            return null;
        } catch (AddressDecodeException e) {
            //e.printStackTrace();
            return null;
        }
    }

    LedgerAccount(byte[] _address) {
        this.Id=null;
        this.addrWrapper=new ByteArrayWrapper(_address);
        this.dirty=false;
    }

    public boolean equals(Object other) {

        if (!(other instanceof LedgerAccount))
            return false;

        return ((LedgerAccount)other).addrWrapper.equals(this.addrWrapper);
    }

    @Override
    public int hashCode() {
        return this.addrWrapper.hashCode();
    }



    public boolean isContract() {
        return  AccountStore.isContract(addrWrapper.getData());
    }
    @Override
    public String toString()
    {
        return  Hex.toHexString(addrWrapper.getData());
    }


    public BigDecimal getBalance() throws SQLException {
        return balance;
    }

    public BigDecimal getLedgerBalance(long _block)
    {
        return null;
//        BlockchainImpl blockchain = (BlockchainImpl)EthereumBean.getBlockchainImpl();
//        Block blockByNumber = blockchain.getBlockByNumber(_block);
//        return blockByNumber==null ? null : getBalance(blockByNumber);
    }

    public BigDecimal getBalance(long _block)
    {
        BlockchainImpl blockchain = (BlockchainImpl)EthereumBean.getBlockchainImpl();
        Block blockByNumber = blockchain.getBlockByNumber(_block);
        return blockByNumber==null ? null : getBalance(blockByNumber);
    }

    public BigDecimal getBalance(Block block)
    {
        BlockchainImpl blockchain = (BlockchainImpl)EthereumBean.getBlockchainImpl();
        Repository track = blockchain.getRepository();

        Repository snapshot = track.getSnapshotTo(block.getStateRoot());

        BigInteger balance=BigInteger.valueOf(0);

        balance=balance.add(snapshot.getBalance(this.getAddress()));

        return new BigDecimal(balance);
    }

    public long getLastBlock() {
        return lastBlock;
    }

    public byte[] getStateRoot() {
        return this.stateRoot;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
}
