package stdweb.Entity

import com.fasterxml.jackson.annotation.JsonIgnore
import org.spongycastle.util.encoders.Hex
import stdweb.Core.Convert2json
import stdweb.Core.EntryResult
import stdweb.Core.EntryType
import stdweb.Core.Utils
import java.math.BigDecimal
import javax.persistence.*

@Entity
class LedgerEntry {

    @Id @GeneratedValue var id: Int = 0

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    var tx: Tx? = null//


    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    var account: LedgerAccount? = null

    @Column(precision = 31,scale = 0)
    var amount: BigDecimal = BigDecimal.ZERO

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    var block: LedgerBlock? = null

    var blockTimestamp: Long = 0
    var depth: Byte = 0
    var gasUsed: Long = 0

    @Column(precision = 31,scale = 0)
    var fee: BigDecimal = BigDecimal.ZERO

    var entryType: EntryType = EntryType.NA
    var entryResult: EntryResult = EntryResult.Ok

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    var offsetAccount: LedgerAccount? = null

    @Column(precision = 31,scale = 0)
    var grossAmount: BigDecimal = BigDecimal.ZERO

    @Column(precision = 31,scale = 0)
    var balance: BigDecimal = BigDecimal.ZERO

    @ManyToOne(fetch = FetchType.LAZY)
    var receipt: TxReceipt? = null

    public constructor() {
    }

    override fun toString(): String {
        return "entry#$id"
//        "%-70s%-43s%-16s%-20s%-43s%n".format(
//                Hex.toHexString(tx?.txhash ?: ByteUtil.EMPTY_BYTE_ARRAY),
//                Account, entryType,
//                Convert2json.BD2ValStr(amount, true), offsetAccount)
    }


    @Transient var blockId: Int? = null
        get() = block?.id

    @Transient var BlockDateTime: String =""
        //get() = Convert2json.convertTimestamp2str(if (id == 0) 1438269973 else blockTimestamp)
        get() = Convert2json.convertTimestamp2str( blockTimestamp )

    @Transient var fee_str : String = ""
        get()= Convert2json.BD2ValStr(fee,true)

    @Transient var amount_str : String = ""
        get()= Convert2json.BD2ValStr(amount,true)

    @Transient var sent_str : String = ""
        get()= if (amount.signum()==-1)  Convert2json.BD2ValStr(amount.negate(),true) else Convert2json.BD2ValStr(BigDecimal.ZERO,true)

    @Transient var received_str : String = ""
        get()= if (amount.signum()==1)  Convert2json.BD2ValStr(amount,true) else Convert2json.BD2ValStr(BigDecimal.ZERO,true)

    @Transient var grossamount_str : String = ""
        get()= Convert2json.BD2ValStr(grossAmount,true)

    @Transient var balance_str : String = ""
        get()= Convert2json.BD2ValStr(balance,true)

    @Transient var hash_str: String =""
        get() =tx?.hash_str() ?: ""

    @Transient
    var addr_str: String =""
        //get() ="0x"+ Hex.toHexString(account?.address ?: byteArrayOf(0,0))
        get() =this.account?.address_str() ?: ""

    @Transient
    var offsetAddr_str: String =""
        get() =this.offsetAccount?.address_str() ?: ""


}









