package stdweb.Entity


import org.ethereum.util.ByteUtil
import javax.persistence.*

@Entity
class TxLog
{
    @Id @GeneratedValue var id: Int = 0

    @ManyToOne(fetch = FetchType.LAZY)
    var block : LedgerBlock? = null

    @ManyToOne(fetch = FetchType.LAZY)
    var tx : Tx? = null
    var ind : Int = 0

    @ManyToOne(fetch = FetchType.LAZY)
    var address : LedgerAccount? = null
    var data : ByteArray = ByteUtil.EMPTY_BYTE_ARRAY

    @Column(length = 32) var topic1 = ByteUtil.EMPTY_BYTE_ARRAY
    @Column(length = 32) var topic2 = ByteUtil.EMPTY_BYTE_ARRAY
    @Column(length = 32) var topic3 = ByteUtil.EMPTY_BYTE_ARRAY
    @Column(length = 32) var topic4 = ByteUtil.EMPTY_BYTE_ARRAY



}