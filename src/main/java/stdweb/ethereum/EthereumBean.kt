package stdweb.ethereum

import org.ethereum.core.BlockchainImpl
import org.ethereum.db.RepositoryImpl
import org.ethereum.facade.Ethereum
import org.ethereum.facade.EthereumFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
public class EthereumBean
{
    val ethereum : Ethereum
    val listener : EthereumListener
    val blockchain : BlockchainImpl
    val repo : RepositoryImpl

    @Autowired
    var ledgerSync : LedgerSyncService? = null


    @PostConstruct
    private fun initService()
    {
        this.listener.ledgerSync=this.ledgerSync!!
        println("EtheteumBean initService")
    }

    constructor()
    {
        this.ethereum=EthereumFactory.createEthereum()
        this.listener = EthereumListener(ethereum)

        this.ethereum.addListener(this.listener)
        this.blockchain=this.ethereum.blockchain as BlockchainImpl
        this.repo=this.ethereum.repository as RepositoryImpl
        blockchainStartSync()
    }

    fun blockchainStartSync() {
        //var ret = ""
        blockchain.stopOn = java.lang.Long.MAX_VALUE
//        if (blockchainSyncStatus == SyncStatus.onBlockSync)
//            ret = "blockchain sync is already started"
//        else {
//            ret = "blockchain sync is started"
//        }
//        blockchainSyncStatus = SyncStatus.onBlockSync
//

        //println(ret)
    }

    fun blockchainStopSync() {
        //var ret = ""
        //        if (blockchainSyncStatus==SyncStatus.stopped)
        //            ret="blockchain sync is already stopped";
        //        else
        //ret = "blockchain sync is stopped"

        blockchain.stopOn = 0
        Thread.sleep(1000)
        //blockchainSyncStatus = SyncStatus.stopped

//        try {
//            Thread.sleep(1000)
//        } catch (e: InterruptedException) {
//            e.printStackTrace()
//        }
//
//        println(ret)
    }
}
