package stdweb.Repository

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import stdweb.Core.HashDecodeException
import stdweb.Core.Utils
import stdweb.Entity.LedgerBlock

interface  ILedgerBlockRepositoryCustom
{
    public fun topBlock() : LedgerBlock?
    public fun get(b : Int ) : LedgerBlock?
    public fun get(b : String ) : LedgerBlock?
    public fun get(b : ByteArray ) : LedgerBlock?

    @Transactional
    public fun deleteBlockWithEntries(b : LedgerBlock)
    @Transactional
    public fun deleteBlockWithEntries(id : Int)

    @Transactional
    public fun deleteBlockWithEntriesFrom(b : LedgerBlock)
    @Transactional
    public fun deleteBlockWithEntriesFrom(id : Int)

    //public fun getPage(blockId: String) : List<LedgerBlock?>

}

class LedgerBlockRepositoryImpl : ILedgerBlockRepositoryCustom
{
    @Autowired
    var blockRepo : LedgerBlockRepository? = null
    @Autowired
    var ledgerRepo : LedgerEntryRepository? = null
    @Autowired
    var txRepo : LedgerTxRepository? = null
    @Autowired
    var logRepo : LedgerTxLogRepository? = null
    @Autowired
    var receiptRepo : LedgerTxReceiptRepository? = null


//    override fun getPage(blockId: String): List<LedgerBlock?> {
//        val top_block=this.topBlock()
//        return this.getPage(top_block?.id)
//
//    }
//    @Query("FROM LedgerBlock where id between  ?1 - 10 and ?1 ORDER BY id desc")
//    fun getPage(blockId: Int?): List<LedgerBlock?> {
//    }


    override fun deleteBlockWithEntriesFrom(id : Int)
    {
        ledgerRepo  !!   .deleteByBlockNumberFrom(id)
        receiptRepo !!   .deleteByBlockNumberFrom(id)
        logRepo     !!   .deleteByBlockNumberFrom(id)
        txRepo      !!   .deleteByBlockNumberFrom(id)

        blockRepo   !!   .deleteByBlockNumberFrom(id)

    }
    override fun deleteBlockWithEntriesFrom(b: LedgerBlock)
    {
        deleteBlockWithEntriesFrom(b.id)
    }

    override fun deleteBlockWithEntries(id : Int)
    {
        ledgerRepo  !!   .deleteByBlockNumber(id)
        receiptRepo !!   .deleteByBlockNumber(id)
        logRepo     !!   .deleteByBlockNumber(id)
        txRepo      !!   .deleteByBlockNumber(id)

        blockRepo   !!   .deleteByBlockNumber(id)

    }


    override fun deleteBlockWithEntries(b: LedgerBlock)
    {
        deleteBlockWithEntries(b.id)
    }

    override fun topBlock(): LedgerBlock? {
        
        return blockRepo?.findTopByOrderByIdDesc()
    }

    public override fun get(b : Int ) : LedgerBlock?
    {
        return blockRepo?.findOne(b)
    }
    public override  fun get(b : String ) : LedgerBlock?
    {
        try {
            var i: Int =b.toInt()
            return get(i)
        }
        catch ( e : NumberFormatException )
        {
            try {
                return get(Utils.hash_decode(b))
            }
            catch (e : HashDecodeException )
            {
                return  null
            }
        }
    }

    public override  fun get(b : ByteArray ) : LedgerBlock?
    {
        return blockRepo?.findByHash(b)
    }
    constructor()
    {
        println("ctor LedgerBlockRepository")
    }
}

interface LedgerBlockRepository : PagingAndSortingRepository<LedgerBlock, Int> , ILedgerBlockRepositoryCustom {

    public fun findTopByOrderByIdDesc() :LedgerBlock?
    public fun findByHash(hash : ByteArray) : LedgerBlock?

    @Modifying
    @Query("delete from LedgerBlock b where b.id=:bnumber")
    public fun deleteByBlockNumber( @Param("bnumber") bnumber : Int)

    @Modifying
    @Query("delete from LedgerBlock b where b.id>=:bnumber")
    public fun deleteByBlockNumberFrom( @Param("bnumber") bnumber : Int)



//
//    public fun get ( blockId : String ) : LedgerBlock?
//    {
//        return this.findByHash(blockId)
//    }
//    public fun get(i : Int ) : LedgerBlock?
//    {
//        return this.findOne( i )
//    }

}
