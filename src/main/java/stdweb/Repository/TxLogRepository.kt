package stdweb.Repository

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
import stdweb.Entity.TxLog

interface LedgerTxLogRepository : PagingAndSortingRepository<TxLog, Int>
{
    @Modifying
    @Query("delete from TxLog b where b.block.id=:bnumber")
    fun deleteByBlockNumber(@Param("bnumber") bnumber : Int)

    @Modifying
    @Query("delete from TxLog b where b.block.id>=:bnumber")
    fun deleteByBlockNumberFrom(@Param("bnumber") bnumber : Int)
}
