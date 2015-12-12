package stdweb.Entity

import org.ethereum.util.ByteUtil
import org.hibernate.annotations.NaturalId
import org.spongycastle.util.encoders.Hex

import java.math.BigDecimal
import javax.persistence.*

@Entity
class LedgerAccount {

    @Id @GeneratedValue
    var id: Int = -1

    @NaturalId
    @Column(length = 20)
    var address : ByteArray = ByteUtil.EMPTY_BYTE_ARRAY
    var name : String =""
    var nonce : Long = -1
    var isContract : Boolean = false

//    @ManyToOne(fetch = FetchType.LAZY)
//    var lastBlock : LedgerBlock?  = null

    @Column(precision = 31,scale = 0)
    var balance : BigDecimal = BigDecimal.ZERO

    @Column(length = 32)
    var stateRoot : ByteArray = ByteUtil.EMPTY_BYTE_ARRAY


    public override fun toString(): String {
        return "0x"+Hex.toHexString(address)
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

