package stdweb.rest;


import org.ethereum.core.Block;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import stdweb.Core.*;
import stdweb.Ledger.AccountStore;
import stdweb.Ledger.LedgerAccount;
import stdweb.Ledger.LedgerQuery;
import stdweb.Ledger.LedgerStore;
import stdweb.ethereum.EthereumBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static stdweb.Core.Utils.address_decode;

@RestController
public class TxController {

    private static final Logger logger = LoggerFactory.getLogger("rest");
    @Autowired
    EthereumBean ethereumBean;


    @RequestMapping(value = "/txs/{blockId}", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getTxList(@PathVariable String blockId,HttpServletRequest request) throws IOException {
        long t1=System.currentTimeMillis();
        try {
            Block block=ethereumBean.getBlock(blockId);
            LedgerQuery ledgerQuery = LedgerStore.getLedgerStore().getQuery();

            String s = ledgerQuery.LedgerSelectByBlock(block.getNumber());

            s=s.replace(":"," ");
            Utils.log("TxList",t1,request);
            return s;


        } catch (SQLException e) {
            e.printStackTrace();
        }

        return  null;
    }

    @RequestMapping(value = "/tx/{txId}", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String gettx(@PathVariable String txId,HttpServletRequest request) throws IOException {
        long t1=System.currentTimeMillis();
        try {
            LedgerQuery ledgerQuery = LedgerStore.getLedgerStore().getQuery();
            String s = ledgerQuery.LedgerSelectByTx(txId);

            s=s.replace(":"," ");
            Utils.log("tx",t1,request);
            return s;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return  "error";

    }


}
