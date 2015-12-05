package stdweb.Repository

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import stdweb.Entity.LedgerBlock
import stdweb.Entity.LedgerEntry

interface LedgerEntryRepository : PagingAndSortingRepository<LedgerEntry, Int>
{
    fun deleteByBlock(b : LedgerBlock?)

    @Modifying
    @Query("delete from LedgerEntry b where b.block.id=:bnumber")
    fun deleteByBlockNumber(@Param("bnumber") bnumber : Int)

}

