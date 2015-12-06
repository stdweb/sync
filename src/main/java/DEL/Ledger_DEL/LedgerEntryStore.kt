package DEL.Ledger_DEL

import stdweb.Core.AddressDecodeException
import stdweb.Core.HashDecodeException
import stdweb.Core.Utils.address_decode
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Timestamp

/**
 * Created by bitledger on 13.11.15.
 */
class LedgerEntryStore  : SqlStore<LedgerEntry>() {


    override fun create(s: String): LedgerEntry? {
        return null
    }

    @Throws(SQLException::class)
    override fun write(entry: LedgerEntry) {
        val rs = get_rs(entry.id?.toLong())
        if (!rs.isFirst) {
            insert(entry)
            //_block.load(rs);
            return
        }
    }

    override fun get(id: Long?): LedgerEntry? {
        val rs = get_rs(id)
        if (rs.isFirst) {
            var e = LedgerEntry()
            e.reload(rs)
            return e
        }
        else
            return null
    }

    override fun get(s: String): LedgerEntry? {
        throw UnsupportedOperationException()
    }

    override fun get(b: ByteArray): LedgerEntry? {
        throw UnsupportedOperationException()
    }


    @Throws(SQLException::class)
    override fun get_rs(id: Long?): ResultSet {
        val sql = "select ID, TX, ADDRESS, AMOUNT, BLOCK, BLOCKTIMESTAMP, DEPTH, GASUSED, FEE, ENTRYTYPE, ENTRYRESULT, OFFSETACCOUNT, DESCR, GROSSAMOUNT " + "from LEDGERENTRY where id=" + id

        val st = conn.createStatement()
        val rs = st.executeQuery(sql)
        rs.first()
        return rs
    }

    @Throws(SQLException::class, AddressDecodeException::class, HashDecodeException::class)
    override fun get_rs(addrStr: String): ResultSet {
        return get_rs(address_decode(addrStr))
    }

    @Throws(SQLException::class, AddressDecodeException::class)
    override fun get_rs(addr: ByteArray): ResultSet {
            throw UnsupportedOperationException()
//        val addrStr = Hex.toHexString(addr)
//        val sql = "select ID, TX, ADDRESS, AMOUNT, BLOCK, BLOCKTIMESTAMP, DEPTH, GASUSED, FEE, ENTRYTYPE, ENTRYRESULT, OFFSETACCOUNT, DESCR, GROSSAMOUNT ,BALANCE from LEDGERENTRY where ADDRESS=X'$addrStr;"
//
//        val st = conn.createStatement()
//        val rs = st.executeQuery(sql)
//        rs.first()
//        return rs
    }

    @Throws(SQLException::class)
    override fun insert(entry: LedgerEntry): LedgerEntry {

        val st = st_ins

        st.setInt(1, entry.tx?.id ?: 0)
        st.setInt(2, entry.AccountDel?.id ?: 0)
        st.setBigDecimal(3, entry.amount)
        st.setLong(4, entry.blockNumber)
        st.setTimestamp(5, Timestamp(entry.blockTimestamp * 1000))
        st.setByte(6, entry.depth)
        st.setLong(7, entry.gasUsed)
        st.setBigDecimal(8, entry.fee)
        st.setByte(9, entry.entryType.ordinal.toByte())
        st.setByte(10, entry.entryResult.ordinal.toByte())
        st.setInt(11, entry.offsetAccountDel?.id ?: 0)
        //st.setString(11, entry.extraData);
        st.setString(12, "")
        st.setBigDecimal(13, entry.grossAmount)

        st.setBigDecimal(14, entry.balance)

        st.executeUpdate()

        return entry
    }

    @Throws(SQLException::class)
    override fun initH2() {
        val statement = conn.createStatement()

        statement.execute("create table if not exists ledgerentry(id identity primary key, tx  int,address int,"
                + "amount decimal(31,0),block bigint,blocktimestamp timestamp," + "depth tinyint,gasused bigint,fee decimal(31,0)," +
                "entryType tinyint," +
                "entryResult tinyint,offsetaccount int ,descr varchar(32)," +
                "GrossAmount decimal(31,0) ,balance decimal(31,0))")

        statement.execute("create index if not exists idx_ledgerentry_address_tx on ledgerentry(address,tx)")

        statement.execute("create index if not exists idx_ledgerentry_address on ledgerentry(address,id,entryType)")
        statement.execute("create index if not exists idx_ledgerentry_tx on ledgerentry(tx)")
        statement.execute("create index if not exists idx_ledgerentry_block_id on ledgerentry(block,id)")
        commit()
        val sql = "INSERT INTO LEDGERENTRY (TX, ADDRESS, AMOUNT, BLOCK, BLOCKTIMESTAMP, DEPTH, GASUSED, " +
                "FEE, ENTRYTYPE, " +
                "ENTRYRESULT, OFFSETACCOUNT," +
                " DESCR, GROSSAMOUNT,BALANCE) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)"
        st_ins = conn.prepareStatement(sql)

        statement.close()
    }

}
