package stdweb.Entity

import org.ethereum.util.ByteUtil

import org.hibernate.annotations.NaturalId
import javax.persistence.*

@Entity
class TxReceipt
{

    @Id var id: Int = 0

    @ManyToOne(fetch = FetchType.LAZY)
    var block : LedgerBlock? = null

    @OneToOne(fetch = FetchType.LAZY)
    var tx : Tx? = null

    @NaturalId @Column(length = 32)
    var postTxState     = ByteUtil.EMPTY_BYTE_ARRAY
    var cumulativeGas   = ByteUtil.EMPTY_BYTE_ARRAY

    @Lob
    var bloomFilter     = ByteUtil.EMPTY_BYTE_ARRAY
}