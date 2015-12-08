package stdweb.rest_del;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import stdweb.Core.Utils;
import DEL.Ledger_DEL.LedgerQuery;
import DEL.Ledger_DEL.SqlDb;
import DEL.Ledger_DEL.EthereumBean_DEL;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

//@RestController
public class TxController {

    private static final Logger logger = LoggerFactory.getLogger("rest");
    @Autowired
    EthereumBean_DEL ethereumBean;


//    @RequestMapping(value = "/txs/{blockId}", method = GET, produces = APPLICATION_JSON_VALUE)
//    @ResponseBody
//    public String getTxList(@PathVariable String blockId,HttpServletRequest request) throws IOException {
//        long t1=System.currentTimeMillis();
//        try {
//            Block block=ethereumBean.getBlock(blockId);
//            LedgerQuery ledgerQuery = SqlDb.getSqlDb().getQuery();
//
//            String s = ledgerQuery.LedgerSelectByBlock(block.getNumber());
//
//            s=s.replace(":"," ");
//            Utils.log("TxList",t1,request);
//            return s;
//
//
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//
//        return  null;
//    }

    @RequestMapping(value = "/tx/{txId}", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String gettx(@PathVariable String txId,HttpServletRequest request) throws IOException {
        long t1=System.currentTimeMillis();
        try {
            LedgerQuery ledgerQuery = SqlDb.getSqlDb().getQuery();
            String s = ledgerQuery.LedgerSelectByTx(txId);

            s=s.replace(":"," ");
            Utils.log("tx",t1,request,new ResponseEntity(null, HttpStatus.NOT_IMPLEMENTED));
            return s;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return  "error";
    }
}
