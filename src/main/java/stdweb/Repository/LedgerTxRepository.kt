package stdweb.Repository

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import stdweb.Entity.LedgerBlock
import stdweb.Entity.Tx

interface LedgerTxRepository : PagingAndSortingRepository<Tx, Int>
{
    public fun findByHash(hash : ByteArray) : Tx?


    @Modifying
    @Query("delete from Tx b where b.block.id=:bnumber")
    fun deleteByBlockNumber(@Param("bnumber") bnumber : Int)

    @Modifying
    @Query("delete from Tx b where b.block.id>=:bnumber")
    fun deleteByBlockNumberFrom(@Param("bnumber") bnumber : Int)
}

