package stdweb.ethereum

import org.ethereum.core.Block
import org.ethereum.core.TransactionExecutionSummary
import org.spongycastle.util.encoders.Hex
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import stdweb.Repository.*

@Service
open class DbBean {

    @Autowired open var ledgService      : LedgerSyncService? =null

    @Autowired open var blockRepo        : LedgerBlockRepository? = null
    @Autowired open var accRepo          : LedgerAccountRepository? = null
    @Autowired open var ledgerRepo       : LedgerEntryRepository? = null
    @Autowired open var txRepo           : LedgerTxRepository? = null
    @Autowired open var logRepo          : LedgerTxLogRepository? = null
    @Autowired open var receiptRepo      : LedgerTxReceiptRepository? = null
    @Autowired open var ethereumBean     : EthereumBean? = null

    @Transactional//(propagation = Propagation.REQUIRES_NEW)
    open fun saveBlockData(block : Block, summaries: List<TransactionExecutionSummary>?)
    {
        val replayBlock = ReplayBlockWrite(
                ledgService!!,block,
                blockRepo !!,
                accRepo !!,
                ledgerRepo!!,
                txRepo!!,
                logRepo!!,
                receiptRepo!!
        )

        if (summaries==null)
            replayBlock.run()
        else
            replayBlock.summaries = summaries

        replayBlock.write()
    }


    open fun testGetFirstBlock()
    {
        val acc=accRepo!!.findByAddress(Hex.decode("2a65aca4d5fc5b5c859090a6c34d164135398226"))!!
        val found=ledgerRepo!!.getFirstAccountBlock(acc.id)

        val block=blockRepo!!.findOne(found)

        println(block)
    }



    @Transactional//(propagation = Propagation.REQUIRES_NEW)
    open fun deleteTopBlocksData(id : Int)
    {
        //accRepo?.updAccFirstBlock2null(id)
        //accRepo?.updAccLastBlock2null(id)

        //deletes blocks from SqlTop down to id
        val sqltopId=blockRepo!!.topBlockId()

        println ("delete blocks from ${sqltopId} downto ${id}")
        (sqltopId downTo id) .forEach { blockId ->

            //val accList=ledgerRepo!!.getAccountsByBlock(blockId)
            blockRepo !!.deleteBlockWithEntries(blockId)

//            accList?.forEach {
//                val maxId = ledgerRepo?.getMaxAccEntryInd(it.id) ?: 0
//                it.entrCnt=maxId
//                accRepo!!.save( it )
//                //accRepo?.updAccEntryInd(it.id, maxId ?: 0)
//            }

            println("block ${blockId} deleted")
            println("new sqlTop ${blockRepo!!.topBlockId()}")
        }
        println ("deletion completed")
        println("SqlTop ${blockRepo!!.topBlockId()}")
    }
}
