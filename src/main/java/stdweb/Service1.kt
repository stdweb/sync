package stdweb

import org.ethereum.core.Block
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import stdweb.Repository.LedgerAccountRepository
import stdweb.Repository.LedgerBlockRepository
import stdweb.Repository.LedgerEntryRepository
import stdweb.ethereum.EthereumBean
import stdweb.ethereum.GenesisBlockWrite
import stdweb.ethereum.LedgerSyncService

/**
 * Created by bitledger on 06.12.15.
 */
//@Service
class Service1 {

    @Autowired
    open var blockRepo: LedgerBlockRepository? = null
    @Autowired
    open var accRepo: LedgerAccountRepository? = null
    @Autowired
    open var ledgerRepo: LedgerEntryRepository? = null
    @Autowired
    open var ethereumBean: EthereumBean? = null

    @Transactional
    open fun ensureGenesis(bean: LedgerSyncService) {

        if (blockRepo!!.findOne(0) == null) {
            println("ensure genesis service1")
            val block0 = ethereumBean!!.blockchain.getBlockByNumber(0)
            val genesis = GenesisBlockWrite(bean, block0, blockRepo!!, ledgerRepo!!)
            genesis.writeGenesis()
        }
    }
}
