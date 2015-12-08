package stdweb.rest_del;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import stdweb.Core.AddressDecodeException;
import stdweb.Core.HashDecodeException;
import stdweb.Core.Utils;
import DEL.Ledger_DEL.LedgerQuery;
import DEL.Ledger_DEL.SqlDb;
import DEL.Ledger_DEL.EthereumBean_DEL;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

/**
 * Created by bitledger on 28.10.15.
 */
//@RestController
public class SearchController {

    @Autowired
    EthereumBean_DEL ethereumBean;

    @RequestMapping(value = "/search/{search_string}", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String ledger(@PathVariable String search_string,HttpServletRequest request) throws IOException, SQLException, InterruptedException, HashDecodeException, AddressDecodeException {
        long t1=System.currentTimeMillis();
        LedgerQuery query = SqlDb.getSqlDb().getQuery();
        String s = query.search(search_string).toJSONString();
        s=s.replace(":"," ");
        Utils.log("Search",t1,request,new ResponseEntity(null, HttpStatus.NOT_IMPLEMENTED));
        return  s;
    }
}
