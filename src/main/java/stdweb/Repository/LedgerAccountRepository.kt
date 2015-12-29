package stdweb.Repository

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
import stdweb.Entity.LedgerAccount

public interface LedgerAccountRepository : PagingAndSortingRepository<LedgerAccount, Int>
{
    public fun findByAddress(address : ByteArray?) : LedgerAccount?

//    @Modifying
//    @Query("update  LedgerAccount set firstBlock.id  = null where firstBlock.id =:previd")
//    public fun updAccFirstBlock2null(@Param("previd") previd : Int)
//
//
//    @Modifying
//    @Query("update  LedgerAccount set lastBlock.id = null where lastBlock.id =:previd")
//    public fun updAccLastBlock2null(@Param("previd") previd : Int)


    @Modifying
    @Query("update LedgerAccount set entrCnt = :entryind where id=:accid")
    public fun updAccEntryInd(@Param("accid") accid : Int,@Param("entryind") entryind : Int)


    @Modifying
    @Query(nativeQuery = true,value = "update LEDGER_ACCOUNT a " +
            "set BALANCE=BALANCE-(select sum(GROSS_AMOUNT) balance_delta from LEDGER_ENTRY where BLOCK_ID=:blockid and ACCOUNT_ID =a.id),    " +
            "ENTR_CNT=ENTR_CNT-(select count(*)cnt_delta from LEDGER_ENTRY where BLOCK_ID=:blockid and ACCOUNT_ID =a.id),    " +
            "FIRST_BLOCK_ID = (select min(block_id) from LEDGER_ENTRY where BLOCK_ID<:blockid and ACCOUNT_ID =a.id), " +
            "LAST_BLOCK_ID = (select max(block_id) from LEDGER_ENTRY where BLOCK_ID<:blockid and ACCOUNT_ID =a.id) " +
            "where id in (select DISTINCT ACCOUNT_ID from LEDGER_ENTRY where BLOCK_ID=:blockid)")
    public fun updBalanceAndEntryCount(@Param("blockid") blockid : Int)

}

