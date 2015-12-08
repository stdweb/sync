package stdweb.rest_del;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import stdweb.Core.AddressDecodeException;
import stdweb.Core.Convert2json;
import stdweb.Core.Utils;
import DEL.Ledger_DEL.AccountStore;
import DEL.Ledger_DEL.LedgerAccount_del;
import DEL.Ledger_DEL.LedgerQuery;
import DEL.Ledger_DEL.SqlDb;

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

//@RestController
public class AccountController {

    AccountStore store;
    @RequestMapping(value = "/account/{accountId}/{offset}", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody

    public String getAccountLedger(@PathVariable String accountId,@PathVariable String offset,HttpServletRequest request) throws IOException {

        try {
            long t1=System.currentTimeMillis();
            SqlDb sqlDb = SqlDb.getSqlDb();
            LedgerQuery ledgerQuery = LedgerQuery.getQuery(sqlDb);

            JSONArray jsonArray = ledgerQuery.LedgerSelect(accountId, offset);

            Utils.log("LedgerSql", t1, request, new ResponseEntity(null, HttpStatus.NOT_IMPLEMENTED),true);
            //JSONObject entriesJson=new JSONObject();

            long t2=System.currentTimeMillis();
            JSONObject entriesJson=ledgerQuery.acc_entry_count(accountId,offset);

            Utils.log("entries count",t2,request,new ResponseEntity(null, HttpStatus.NOT_IMPLEMENTED),true);

            byte[] acc=address_decode(accountId);
            LedgerAccount_del account= store.get(acc);

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
            Utils.log("AccountLedger",t1,request,new ResponseEntity(null, HttpStatus.NOT_IMPLEMENTED));
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
        store =AccountStore.getInstance();
    }
}
