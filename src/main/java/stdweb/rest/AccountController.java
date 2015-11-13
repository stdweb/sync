package stdweb.rest;

import org.ethereum.core.Account;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import stdweb.Core.AddressDecodeException;
import stdweb.Core.Convert2json;
import stdweb.Core.Utils;
import stdweb.Ledger.AccountStore;
import stdweb.Ledger.LedgerAccount;
import stdweb.Ledger.LedgerQuery;
import stdweb.Ledger.LedgerStore;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static stdweb.Core.Utils.address_decode;

/**
 * Created by bitledger on 13.11.15.
 */

@RestController
public class AccountController {

    AccountStore astore;
    @RequestMapping(value = "/account/{accountId}/{offset}", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody

    public String getAccountLedger(@PathVariable String accountId,@PathVariable String offset,HttpServletRequest request) throws IOException {

        try {
            long t1=System.currentTimeMillis();
            LedgerStore ledgerStore = LedgerStore.getLedgerStore();
            LedgerQuery ledgerQuery = LedgerQuery.getQuery(ledgerStore);

            JSONArray jsonArray = ledgerQuery.LedgerSelect(accountId, offset);

            Utils.log("LedgerSql", t1, request, true);
            //JSONObject entriesJson=new JSONObject();

            long t2=System.currentTimeMillis();
            JSONObject entriesJson=ledgerQuery.acc_entry_count(accountId,offset);

            Utils.log("entries count",t2,request,true);

            byte[] acc=address_decode(accountId);
            LedgerAccount account=astore.get(acc);

            t2=System.currentTimeMillis();
            //BigDecimal ledgerBlockBalance = ledgerQuery.getLedgerAccountBalance(account,ledgerQuery.getSqlTopBlock());
            BigDecimal ledgerBlockBalance = account!=null ? account.getBalance() : BigDecimal.ZERO;

            //Utils.log("acc balance",t2,request,true);


            //BigDecimal ledgerBlockBalance=BigDecimal.ZERO;
            entriesJson.put("balance", Convert2json.BD2ValStr(ledgerBlockBalance, true));
            if (account!=null)
                entriesJson.put("addresstype",account.isContract() ? "Contract" : "Account");


            entriesJson.put("entries",jsonArray);
            String s=entriesJson.toJSONString();



            s=s.replace(":"," ");
            Utils.log("AccountLedger",t1,request);
            return s;

        } catch (SQLException e) {
            e.printStackTrace();
            return  e.toString();
        } catch (AddressDecodeException e) {
            e.printStackTrace();
            return  e.toString();
        }
    }

    public AccountController() throws SQLException {
        astore=AccountStore.getInstance();
    }
}
