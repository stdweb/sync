package DEL.Ledger_DEL

import java.sql.ResultSet
import java.sql.SQLException

interface ITableEntry {

    @Throws(SQLException::class)
    fun reload(rs : ResultSet)
}
/**
 * Created by bitledger on 16.11.15.
 */
