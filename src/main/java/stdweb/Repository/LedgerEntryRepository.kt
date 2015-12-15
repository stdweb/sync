package stdweb.Repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import stdweb.Entity.LedgerAccount
import stdweb.Entity.LedgerBlock
import stdweb.Entity.LedgerEntry

interface LedgerEntryRepository : PagingAndSortingRepository<LedgerEntry, Int>
{
    fun deleteByBlock(b : LedgerBlock?)

    @Modifying
    @Query("delete from LedgerEntry b where b.block.id=:bnumber")
    fun deleteByBlockNumber(@Param("bnumber") bnumber : Int)

    @Modifying
    @Query("delete from LedgerEntry b where b.block.id>=:bnumber")
    fun deleteByBlockNumberFrom(@Param("bnumber") bnumber : Int)

    @Query("select e from LedgerEntry e where e.block.id=:bnumber order by id")
    fun getByBlockId( @Param("bnumber") bnumber : Int? ) : List<LedgerEntry>

    @Query("select e from LedgerEntry e where e.account.id=:bnumber order by id ")
    fun getByAccountId( @Param("bnumber") bnumber : Int? ) : List<LedgerEntry>

    @Query("select e from LedgerEntry e where e.tx.id=:txid order by id")
    fun getByTxId(@Param("txid") txid : Int? ): List<LedgerEntry>?

    @Query(value="select * from Ledger_Entry e where e.account_id=:accid order by id limit 25 offset :offset",
            nativeQuery = true)
    fun getAccountLedgerPage(@Param("accid") accid : Int, @Param("offset") offset: Int) : List<LedgerEntry>

    @Query("select count(e) from LedgerEntry e where e.account.id=:accid")
    fun getEntriesCount(@Param("accid") accid : Int) : Int


}

