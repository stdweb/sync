package stdweb.Ledger;

import stdweb.Core.AddressDecodeException;
import stdweb.Core.HashDecodeException;

import java.sql.*;

import static stdweb.Core.Utils.address_decode;

/**
 * Created by bitledger on 13.11.15.
 */
public class LedgerEntryStore extends SqlStore<LedgerEntry> {

    static LedgerEntryStore store;


    public LedgerEntry create(String s)
    {
        return  null;
    }
    public void write(LedgerEntry entry)
    {

    }
    public LedgerEntry get(Long id)
    {
        return null;
    }
    public LedgerEntry get(String s)
    {
        return null;
    }
    public LedgerEntry get(byte[] b)
    {
        return null;
    }

    @Override
    protected ResultSet get_rs(byte[] b) throws SQLException {
        return null;
    }

    @Override
    protected ResultSet get_rs(Long id) throws SQLException {
        return null;
    }

    @Override
    protected ResultSet get_rs(String id) throws SQLException, AddressDecodeException, HashDecodeException {
        return null;
    }


    void commit()
    {

    }
    protected LedgerEntry insert (LedgerEntry e)
    {
        return null;
    }
    LedgerEntryStore getInstance()
    {
        store= store==null ? new LedgerEntryStore() :store;
        return store;
    }
    LedgerEntryStore()
    {

    }
    protected void setConnection(Connection connection) throws SQLException, AddressDecodeException {
        this.conn = connection;
        initH2();
    }

    protected void initH2() throws SQLException {
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

//        if (LedgerAccount.GenesisAccount()==null) {
//            LedgerAccount genesisaccount = create(address_decode("0000000000000000000000000000000000000000"));
//            genesisaccount.setName("Genesis");
//            write(genesisaccount);
//            commit();
//        }

        statement.close();
    }
}
