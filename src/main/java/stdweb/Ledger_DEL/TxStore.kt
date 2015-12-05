package stdweb.Ledger_DEL

import java.sql.ResultSet

/**
 * Created by bitledger on 14.11.15.
 */

class TxStore : SqlStore<Tx>()
{
    override fun initH2() {

        val statement = conn.createStatement()

        statement.execute("CREATE TABLE IF NOT EXISTS TX (  ID IDENTITY PRIMARY KEY , HASH BINARY(32) ")

        statement.execute("CREATE UNIQUE INDEX if not exists IDX_TX_HASH_ID ON TX (HASH, ID);")
        commit()
        val insBlockHeaderSql = "INSERT INTO TX ( HASH ) VALUES (?) "
        st_ins = conn.prepareStatement(insBlockHeaderSql)

        statement.close()
    }

    override fun write(o: Tx?) {
        throw UnsupportedOperationException()
    }

    override fun create(s: String?): Tx? {
        throw UnsupportedOperationException()
    }

    override fun get(id: String?): Tx? {
        throw UnsupportedOperationException()
    }

    override fun get(id: Long?): Tx? {
        throw UnsupportedOperationException()
    }

    override fun get(id: ByteArray?): Tx? {
        throw UnsupportedOperationException()
    }

    override fun get_rs(b: ByteArray?): ResultSet? {
        throw UnsupportedOperationException()
    }

    override fun get_rs(id: Long?): ResultSet? {
        throw UnsupportedOperationException()
    }

    override fun get_rs(id: String?): ResultSet? {
        throw UnsupportedOperationException()
    }

    override fun insert(o: Tx?): Tx? {
        throw UnsupportedOperationException()
    }


}