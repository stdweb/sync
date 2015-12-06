package stdweb;

import org.ethereum.core.Block;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stdweb.Repository.LedgerAccountRepository;
import stdweb.Repository.LedgerBlockRepository;
import stdweb.Repository.LedgerEntryRepository;
import stdweb.ethereum.EthereumBean;
import stdweb.ethereum.GenesisBlockWrite;
import stdweb.ethereum.LedgerSyncService;

/**
 * Created by bitledger on 06.12.15.
 */
@Service
public class Service2 {

    @Autowired
    LedgerBlockRepository blockRepo;
    @Autowired
    LedgerAccountRepository accRepo;
    @Autowired
    LedgerEntryRepository ledgerRepo;
    @Autowired
    EthereumBean ethereumBean;

    @Transactional
    public void ensureGenesis(LedgerSyncService bean) {

        if (blockRepo.findOne(0)==null)
        {
            System.out.println("ensure genesis service1");
            Block block0 = ethereumBean.getBlockchain().getBlockByNumber(0);
            GenesisBlockWrite genesis=new GenesisBlockWrite(bean,block0,blockRepo,ledgerRepo);
            genesis.writeGenesis();
        }
    }
}
