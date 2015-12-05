package stdweb.Ledger_DEL

import org.ethereum.core.Transaction
import org.ethereum.util.ByteUtil
import stdweb.Core.EntryResult
import stdweb.Core.EntryType
import java.sql.ResultSet
import java.sql.SQLException

/**
 * Created by bitledger on 14.11.15.
 */
class Tx{


    public var txhash: ByteArray = ByteUtil.EMPTY_BYTE_ARRAY
    public var id : Int? = null

    @Throws(SQLException::class)
    fun reload(rs: ResultSet) {

        if (rs.isFirst) {
            this.id = rs.getInt("ID")
            this.txhash = rs.getBytes("TX")
            //this.dirty = false
        }
    }
    constructor( )
    constructor( tx : Transaction)
    {
        txhash=tx.hash
        id=0
    }
}