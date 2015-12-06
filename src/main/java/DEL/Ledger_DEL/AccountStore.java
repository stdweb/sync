package DEL.Ledger_DEL;

import org.ethereum.db.ContractDetails;
import org.ethereum.db.RepositoryImpl;
import org.spongycastle.util.encoders.Hex;
import stdweb.Core.AddressDecodeException;
import stdweb.ethereum.EthereumBean_DEL;

import java.sql.*;

import static stdweb.Core.Utils.address_decode;

/**
 * Created by bitledger on 09.11.15.
 */
public class AccountStore extends SqlStore<LedgerAccount_del> {
    static AccountStore store;

    PreparedStatement st_ins;
    //Connection conn;

//    void commit() throws SQLException {
//        conn .commit();
//    }

    public void write(LedgerAccount_del account) throws SQLException {

        if (account.isNew()) {
                insert(account);
            //account.reload(getAccountRs(account.getAddress()));
            account.setDirty(false);
            return;
        }

        String sql="UPDATE account set name=?, nonce=?,isContract=?,lastblock=?,balance=?,stateroot=? " +
                "where id="+ account.Id;

        PreparedStatement st=conn.prepareStatement(sql);

        st.setString(1,account.name);
        st.setLong(2, account.nonce);
        st.setBoolean(3,account.isContract);
        st.setLong(4,account.lastBlockNumber);
        st.setBigDecimal(5, account.balance);
        st.setBytes(6,account.stateRoot);
        st.executeUpdate();

        commit();
        account.setDirty(false);

        account.reload(get_rs(account.getAddress()));
    }

    public LedgerAccount_del get(Long id) throws SQLException {
        ResultSet rs = get_rs(id.longValue());
        if (rs.isFirst())
            return new LedgerAccount_del(rs);
        else
            return null;
    }
    public LedgerAccount_del get(byte[] address) throws SQLException {
        ResultSet rs = get_rs(address);
        if (rs.isFirst())
            return new LedgerAccount_del(rs);
        else
            return null;
    }

    public LedgerAccount_del get(String accStr) throws SQLException, AddressDecodeException {
        byte[] addr = address_decode(accStr);
        return get(addr);
    }


    public LedgerAccount_del create(byte[] addr) throws SQLException {
        if (get_rs(addr).isFirst())
            throw new SQLDataException("Account already exists: "+"0x"+Hex.toHexString(addr));

        return new LedgerAccount_del(addr);
    }
    public LedgerAccount_del create(String accStr) throws SQLException, AddressDecodeException {
        return create(address_decode(accStr));
    }

    protected LedgerAccount_del insert(LedgerAccount_del account) throws SQLException {

        PreparedStatement stat=st_ins;
        byte[] addr = account.getAddress();
        if (get_rs(addr).isFirst())
            throw new SQLDataException("Account already exists: "+"0x"+Hex.toHexString(addr));
       // RepositoryImpl repo = EthereumBean.getRepositoryImpl();
        //String accStr=Hex.toHexString(addr);

        String name=account.getName();

        long nonce = account.getNonce();

        stat.setBytes(1,addr);
        stat.setString(2, name);
        stat.setLong(3,nonce);
        stat.setBoolean(4, AccountStore.isContract(addr));
        stat.setLong(5,account.getLastBlock());
        stat.setBigDecimal(6, account.getBalance());
        stat.setBytes(7, account.getStateRoot());


        stat.executeUpdate();
        commit();
        return account;
    }

    protected ResultSet get_rs(Long id) throws SQLException {
        String sql="select ID,ADDRESS, NAME, NONCE, ISCONTRACT, LASTBLOCK, BALANCE, STATEROOT from Account where ID =" +id;
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(sql);
        rs.first();
        return rs;
    }

    protected ResultSet get_rs(byte[] addr) throws SQLException {
        String accStr=Hex.toHexString(addr);
        String sql="select ID,ADDRESS, NAME, NONCE, ISCONTRACT, LASTBLOCK, BALANCE, STATEROOT from Account where address =X'" +accStr+"' ";
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(sql);
        rs.first();
        return rs;
    }
    protected ResultSet get_rs(String addr) throws SQLException, AddressDecodeException {
        return get_rs(address_decode(addr));
    }


    public static AccountStore getInstance() throws SQLException {
        store= store==null ? new AccountStore() :store;
        return store;
    }


    public static boolean isContract(byte[] addr)
    {
        RepositoryImpl repository = (RepositoryImpl) EthereumBean_DEL.ethereum.getRepository();
        ContractDetails contractDetails = repository.getContractDetails(addr);

        if (contractDetails==null)
            return false;
        if (contractDetails.getCode()==null)
            return false;

        return  (contractDetails.getCode().length!=0);
    }



    protected void initH2() throws SQLException, AddressDecodeException {
        Statement statement = conn.createStatement();

        statement.execute("create TABLE IF NOT EXISTS ACCOUNT  (\n" +
                "    ID BIGINT not null  AUTO_INCREMENT,\n" +
                "    ADDRESS VARBINARY,\n" +
                "    NAME VARCHAR(32),\n" +
                "    NONCE BIGINT,\n" +
                "    ISCONTRACT BOOLEAN,\n" +
                "    LASTBLOCK BIGINT,\n" +
                "    BALANCE DECIMAL(31, 0),\n" +
                "    STATEROOT VARBINARY,\n" +
                "    PRIMARY KEY (ID)\n" +
                ")");

        statement.execute("CREATE UNIQUE INDEX IF NOT EXISTS ACCOUNT_ADDRESS_UINDEX ON ACCOUNT (ADDRESS)");
        commit();
        String sql="INSERT INTO ACCOUNT (ADDRESS, NAME, NONCE, ISCONTRACT, LASTBLOCK, BALANCE, STATEROOT) VALUES (?,?,?,?,?,?,?)";
        st_ins=conn.prepareStatement(sql);

        if (LedgerAccount_del.GenesisAccount()==null) {
            LedgerAccount_del genesisaccount = create(address_decode("0000000000000000000000000000000000000000"));
            genesisaccount.setName("Genesis");
            write(genesisaccount);
            commit();
        }
        statement.close();
    }
}
