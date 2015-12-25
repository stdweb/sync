package stdweb.Entity

import org.ethereum.db.ByteArrayWrapper
import org.ethereum.util.ByteUtil
import org.hibernate.annotations.NaturalId
import org.spongycastle.util.encoders.Hex
import stdweb.Core.Utils

import java.math.BigDecimal
import javax.persistence.*

@Entity
class LedgerAccount {

    @Id @GeneratedValue
    var id: Int = -1

    @NaturalId
    @Column(length = 20)
    var address     : ByteArray = ByteUtil.EMPTY_BYTE_ARRAY
    var name        : String =""
    var nonce       : Long = -1
    var isContract  : Boolean = false


    //todo: del lastblock, firstblock but create indexes on thees fields
    @ManyToOne(fetch = FetchType.LAZY)
    var lastBlock   : LedgerBlock?  = null

    @ManyToOne(fetch = FetchType.LAZY)
    var firstBlock  : LedgerBlock?  = null


    @Column(precision = 31,scale = 0)
    var balance     : BigDecimal = BigDecimal.ZERO

    @Column(length = 32)
    var stateRoot   : ByteArray = ByteUtil.EMPTY_BYTE_ARRAY

    var txCount     : Int = 0

    var entrCnt     : Int = 0




    public override fun toString(): String {
        return "0x"+Hex.toHexString(address)
    }

    fun address_str() : String
    {
        return if (ByteArrayWrapper(address).equals(ByteArrayWrapper(Utils.ZERO_BYTE_ARRAY_20))) "" else toString()
    }


    public constructor(){}
    public constructor(addr: ByteArray)
    {
        this.address=addr
    }

//    companion  object   {
//        public fun ZeroAccount(): LedgerAccount = LedgerAccount(Utils.ZERO_BYTE_ARRAY_20)
//    }


}

