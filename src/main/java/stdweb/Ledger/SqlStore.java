package stdweb.Ledger;

import stdweb.Core.AddressDecodeException;
import stdweb.Core.HashDecodeException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by bitledger on 13.11.15.
 */
public abstract class SqlStore<T> {


    protected Connection conn;
    PreparedStatement st_ins;


    protected void setConnection(Connection connection) throws SQLException, AddressDecodeException {
        this.conn = connection;
        initH2();
    }

    protected abstract void initH2() throws SQLException, AddressDecodeException;

    public abstract void write(T o) throws SQLException;
    public abstract T create(String s) throws SQLException, AddressDecodeException;

    public abstract T get(String id) throws SQLException, AddressDecodeException, HashDecodeException;
    public abstract T get(Long id) throws SQLException;
    public abstract T get(byte[] id)throws SQLException;

    protected abstract ResultSet get_rs(byte[] b)   throws SQLException;
    protected abstract ResultSet get_rs(Long id)    throws SQLException;
    protected abstract ResultSet get_rs(String id) throws SQLException, AddressDecodeException, HashDecodeException;


    protected abstract T insert(T o) throws SQLException;




}
