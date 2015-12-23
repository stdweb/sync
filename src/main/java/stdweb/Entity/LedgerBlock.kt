package stdweb.Entity


import com.fasterxml.jackson.annotation.JsonIgnore
import org.ethereum.util.ByteUtil
import org.hibernate.annotations.NaturalId
import org.spongycastle.util.encoders.Hex
import stdweb.Core.Convert2json
import stdweb.Core.Utils
import java.math.BigDecimal
import java.util.*
import javax.persistence.*

@Entity
class LedgerBlock
{
    @Id var id: Int = 0

    @JsonIgnore
    @NaturalId
    @Column(length = 32)    var hash  = ByteUtil.EMPTY_BYTE_ARRAY

    var timestamp : Long = 0

    @JsonIgnore
    @Column(length = 32)    var parentHash :        ByteArray = ByteUtil.EMPTY_BYTE_ARRAY

    @Column(length = 32)    var stateRoot :         ByteArray = ByteUtil.EMPTY_BYTE_ARRAY
    @Column(length = 32)    var txTrieRoot :        ByteArray = ByteUtil.EMPTY_BYTE_ARRAY
    @Column(length = 32)    var receiptTrieRoot :   ByteArray = ByteUtil.EMPTY_BYTE_ARRAY
    @Column(length = 32)    var unclesHash      :   ByteArray = ByteUtil.EMPTY_BYTE_ARRAY

    @Column(length = 32)    var mixHash  :          ByteArray = ByteUtil.EMPTY_BYTE_ARRAY
    @Column()               var EXTRADATA  :        ByteArray = ByteUtil.EMPTY_BYTE_ARRAY

    @Column()               var logsBloom  :        ByteArray = ByteUtil.EMPTY_BYTE_ARRAY

    var difficulty  : Long =0
    var gasLimit    : Long =0
    var gasUsed     : Long =0
    var nonce       : Long =0

    var txCount         : Int =0
    var unclesCount     : Int =0
    var blockSize       : Int =0

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    var coinbase : LedgerAccount? = null

    @Column(precision = 31,scale = 0)    var fee:       BigDecimal = BigDecimal.ZERO
    @Column(precision = 31,scale = 0)    var balance:   BigDecimal = BigDecimal.ZERO
    @Column(precision = 31,scale = 0)    var reward:    BigDecimal = BigDecimal.ZERO

    @Transient
    var BlockDateTime: String =""
        get() = Convert2json.convertTimestamp2str(if (id == 0) 1438269973 else timestamp)

    @Transient
    var hash_str: String =""
        get() ="0x"+ Hex.toHexString(hash)

    @Transient
    var fee_str : String = ""
        get()=Convert2json.BD2ValStr(fee,true)

    @Transient
    var reward_str : String = ""
        get()=Convert2json.BD2ValStr(reward,true)

    @Transient
    var coinbase_str : String = ""
        get()="0x"+Hex.toHexString(coinbase?.address ?: Utils.ZERO_BYTE_ARRAY_20 )

    @Transient
    var stateroot_str : String = ""
        get()="0x"+Hex.toHexString( stateRoot )

    @Transient
    var txroot_str : String = ""
        get()="0x"+Hex.toHexString(txTrieRoot  )

    @Transient
    var receiptroot_str : String = ""
        get()="0x"+Hex.toHexString(receiptTrieRoot  )

    @Transient
    var parent_str : String = ""
        get()="0x"+Hex.toHexString(parentHash  )


    public fun toJSON() : String
    {
        ByteUtil.ZERO_BYTE_ARRAY
        throw NotImplementedError()
    }


}
