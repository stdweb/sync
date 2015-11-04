package stdweb.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import stdweb.Core.LedgerQuery;
import stdweb.Core.LedgerStore;
import stdweb.ethereum.EthereumBean;

import java.io.IOException;
import java.sql.SQLException;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

/**
 * Created by bitledger on 28.10.15.
 */
@RestController
public class SearchController {

    @Autowired
    EthereumBean ethereumBean;

    @RequestMapping(value = "/search/{search_string}", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String ledger(@PathVariable String search_string) throws IOException, SQLException, InterruptedException {
        LedgerQuery query = LedgerStore.getLedgerStore(ethereumBean.getListener()).getQuery();
        String s = query.search(search_string).toJSONString();
        s=s.replace(":"," ");
        return  s;
    }
}