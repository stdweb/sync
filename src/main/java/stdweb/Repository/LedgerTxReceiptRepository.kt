package stdweb.Repository

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
import stdweb.Entity.TxReceipt


interface LedgerTxReceiptRepository : PagingAndSortingRepository<TxReceipt, Int>
{
    @Modifying
    @Query("delete from TxReceipt b where b.block.id=:bnumber")
    fun deleteByBlockNumber(@Param("bnumber") bnumber : Int)

    @Modifying
    @Query("delete from TxReceipt b where b.block.id>=:bnumber")
    fun deleteByBlockNumberFrom(@Param("bnumber") bnumber : Int)
}
