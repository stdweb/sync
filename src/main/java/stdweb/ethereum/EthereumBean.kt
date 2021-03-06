package stdweb.ethereum

import org.ethereum.core.BlockchainImpl
import org.ethereum.core.CallTransaction
import org.ethereum.db.RepositoryImpl
import org.ethereum.facade.Ethereum
import org.ethereum.facade.EthereumFactory
//import org.ethereum.net.eth.sync.SyncState
import org.spongycastle.util.encoders.Hex
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import stdweb.Core.Amount
import stdweb.Core.SyncStatus
import java.math.BigInteger
import javax.annotation.PostConstruct

@Service
public class EthereumBean
{
    val ethereum    : Ethereum
    val listener    : EthereumListener
    val blockchain  : BlockchainImpl
    val repo        : RepositoryImpl

    @Autowired
    var ledgerSync : LedgerSyncService? = null

    @Autowired
    var dbBean : DbBean? = null

    var blockchainSyncStatus : SyncStatus = SyncStatus.stopped


//    fun blockchainStartSync() {
//        blockchain.stopOn       = java.lang.Long.MAX_VALUE
//        blockchainSyncStatus    = SyncStatus.onBlockSync
//        println("blockchain started")
//    }
//
//    fun blockchainStopSync() {
//        blockchainSyncStatus= SyncStatus.stopped
//        blockchain.stopOn = 0
//        println("blockchain stopped")
//        Thread.sleep(1000)
//    }

    @PostConstruct
    public  fun initService()
    {
        this.listener.ledgerSync=this.ledgerSync!!
        //blockchainStopSync()

        //ledgerSync!!.replayAndSaveBlock(758586);

        //dbBean!!.deleteTopBlocksData(1000994)
        //val block = blockchain.getBlockByNumber(1000994);
        //println ("found  ${block.number} -> ${block.shortHash}")

        //blockchainStartSync()
        ledgerSync?.syncStatus = SyncStatus.onBlockSync
        ledgerSync?.nextStatus = SyncStatus.onBlockSync

        println("EthereumBean initService")
    }

    private fun checkBlockNumber() {
        val b326= ethereum.blockchain.getBlockByNumber(326)
        val b323= ethereum.blockchain.getBlockByNumber(323)

        val uncle=ethereum.blockchain.getBlockByHash(Hex.decode("55b44267d6e07e845edcf2ef3020ec4b889e13eba463e46ec9539371758e81a8"))

        println("uncle : ${uncle.number}")
    }

    constructor()
    {
        this.ethereum=EthereumFactory.createEthereum()
        this.listener = EthereumListener(ethereum)

        this.ethereum.addListener(this.listener)
        this.blockchain=this.ethereum.blockchain as BlockchainImpl
        this.repo=this.ethereum.repository as RepositoryImpl

        println ("bestblock on start:${this.blockchain.bestBlock.number} hash: ${Hex.toHexString(this.blockchain.bestBlock.hash)}");
    }
}


