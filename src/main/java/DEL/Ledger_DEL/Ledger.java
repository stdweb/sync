package DEL.Ledger_DEL;

import org.ethereum.core.Block;
import org.ethereum.db.RepositoryImpl;
import org.ethereum.facade.Ethereum;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by bitledger on 03.10.15.
 */
public class Ledger {

    private final Ethereum ethereum;
    RepositoryImpl repo;

    //public getBalance(byte[])
    public Ledger(Ethereum _ethereum)
    {
        this.ethereum=_ethereum;
        this.repo=(RepositoryImpl)ethereum.getRepository();


    }


    public BigDecimal getBalanceOnBlock(LedgerAccount_del acc, Block block){ return BigDecimal.ZERO;}
    public BigDecimal getBalanceOnDate(LedgerAccount_del acc, Date date){ return BigDecimal.ZERO;}

    public BigDecimal getTotal(){ return BigDecimal.ZERO;}

    public BigDecimal getAccountLedger(LedgerAccount_del acc){ return BigDecimal.ZERO;}

    public BigDecimal getEntriesByFilter(){ return BigDecimal.ZERO;}


    public void Materialize(LedgerAccount_del vertex, String filter)
    {
        //get triples
        //convert to table
        //clojure
        //materialize to local datomic database
    }

    public void SqlMaterialize(LedgerAccount_del vertex, String filter)
    {
        //get triples
        //convert to table
        //clojure
        //materialize to local sql (h2) database
    }


}
