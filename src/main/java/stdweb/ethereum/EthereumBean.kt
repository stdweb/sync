package stdweb.ethereum

import org.ethereum.core.BlockchainImpl
import org.ethereum.db.RepositoryImpl
import org.ethereum.facade.Ethereum
import org.ethereum.facade.EthereumFactory
import org.ethereum.net.eth.sync.SyncState
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import stdweb.Core.Amount
import stdweb.Core.SyncStatus
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

    var blockchainSyncStatus : SyncStatus = SyncStatus.stopped

    fun blockchainStartSync() {
        blockchain.stopOn       = java.lang.Long.MAX_VALUE
        blockchainSyncStatus    = SyncStatus.onBlockSync
        println("blockchain started")
    }

    fun blockchainStopSync() {
        blockchainSyncStatus= SyncStatus.stopped
        blockchain.stopOn = 0
        println("blockchain stopped")
        Thread.sleep(1000)
    }

    @PostConstruct
    public  fun initService()
    {
        this.listener.ledgerSync=this.ledgerSync!!
//        blockchainStopSync()
        blockchainStartSync()
        ledgerSync?.syncStatus = SyncStatus.onBlockSync
        ledgerSync?.nextStatus = SyncStatus.onBlockSync

        println("EtheteumBean initService")
    }

    constructor()
    {
        this.ethereum=EthereumFactory.createEthereum()
        this.listener = EthereumListener(ethereum)

        this.ethereum.addListener(this.listener)
        this.blockchain=this.ethereum.blockchain as BlockchainImpl
        this.repo=this.ethereum.repository as RepositoryImpl

        println ("bestblock on start:"+this.blockchain.bestBlock.number);
    }
}


