package stdweb.Entity

import org.ethereum.util.ByteUtil
import org.hibernate.annotations.NaturalId
import org.spongycastle.util.encoders.Hex
import java.math.BigDecimal
import javax.persistence.*

@Entity
class Tx{

    @Id @GeneratedValue var id: Int = 0

    @NaturalId @Column(length = 32)
    var hash : ByteArray? = null

    @ManyToOne(fetch = FetchType.LAZY)
    var block : LedgerBlock? = null

    @ManyToOne(fetch = FetchType.LAZY)
    var from : LedgerAccount?=null

    @ManyToOne(fetch = FetchType.LAZY)
    var to : LedgerAccount?=null

    @Column(precision = 31,scale = 0)
    var value : BigDecimal = BigDecimal.ZERO

    @Column(precision = 31,scale = 0)
    var fee : BigDecimal = BigDecimal.ZERO


    var gas : Long = 0
    var gasPrice : BigDecimal = BigDecimal.ZERO

    var nonce : Long = 0
    var txindex : Int = 0

    @Lob
    var extradata : ByteArray? = null
    var contractCreate : Boolean = false

    //todo: invariant calc tx hash

    override  fun toString(): String {
        return "tx: 0x"+Hex.toHexString(hash)
    }

    fun hash_str() : String {
        return "0x"+Hex.toHexString(hash)
    }

    constructor(_hash: ByteArray?) {
        this.hash=_hash
    }
    constructor(){}
}