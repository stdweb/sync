package stdweb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import stdweb.Repository.LedgerBlockRepository;
import stdweb.ethereum.EthereumBean;
import stdweb.ethereum.LedgerSyncService;

import java.util.concurrent.Executors;

//@Configuration
//@EnableTransactionManagement
public class Config {



    @Bean
    EthereumBean ethereumBean() throws Exception {
        EthereumBean ethereumBean = new EthereumBean();

        ethereumBean.initService();

//        Executors.newSingleThreadExecutor().
//                submit(ethereumBean::start);

        //System.out.println("eth bean created, repo is null = "+(ethereumBean.getBlockRepo()==null));
        return ethereumBean;
    }

    @Autowired
    EthereumBean ethereumBean ;

    @Autowired
    LedgerBlockRepository blockRepository;

 //   @Bean
    @DependsOn({"ethereumBean","ledgerBlockRepository","ledgerAccountRepository","ledgerEntryRepository"})
    LedgerSyncService ledgerSync() throws Exception
    {

        Thread.sleep(2000);
        LedgerSyncService ledgerSync = new LedgerSyncService();
        ethereumBean.getListener().setLedgerSync(ledgerSync);
        //ledgerSync.setEthereumBean(ethereumBean);
//        Executors.newSingleThreadExecutor().
//                submit(ledgerSync::start);

        return ledgerSync;
    }
//
//    @Bean
//    SpringTransaction springTransactionBean() throws Exception
//    {
//        //System.out.println("Spring transaction bean");
//
//        SpringTransaction springTransaction = new SpringTransaction();
//
//        //Thread.sleep(3000);
//
//        Executors.newSingleThreadExecutor().
//                submit(springTransaction::start);
//
//        //System.out.println("ctor bean SpringTransaction,repo is null = "+(springTransaction.getBlockRepo()==null));
//        return springTransaction;
//    }

}
