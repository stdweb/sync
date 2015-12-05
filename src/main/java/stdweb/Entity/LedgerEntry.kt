package stdweb.Entity

import stdweb.Core.EntryResult
import stdweb.Core.EntryType
import java.math.BigDecimal
import javax.persistence.*

@Entity
class LedgerEntry {

    @Id @GeneratedValue var id: Int = 0

    @ManyToOne(fetch = FetchType.LAZY)
    var tx: Tx? = null//

    @ManyToOne(fetch = FetchType.LAZY)
    var Account: LedgerAccount? = null

    @Column(precision = 31,scale = 0)
    var amount: BigDecimal = BigDecimal.ZERO

    @ManyToOne(fetch = FetchType.LAZY)
    var block: LedgerBlock? = null

    var blockTimestamp: Long = 0
    var depth: Byte = 0
    var gasUsed: Long = 0

    @Column(precision = 31,scale = 0)
    var fee: BigDecimal = BigDecimal.ZERO

    var entryType: EntryType = EntryType.NA
    var entryResult: EntryResult = EntryResult.Ok

    @ManyToOne(fetch = FetchType.LAZY)
    var offsetAccount: LedgerAccount? = null//



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
}









