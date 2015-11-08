package stdweb.rest;

import org.ethereum.core.Block;
import org.ethereum.core.BlockchainImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import stdweb.Core.*;
import stdweb.Ledger.LedgerAccount;
import stdweb.Ledger.LedgerStore;
import stdweb.ethereum.EthereumBean;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

/**
 * Created by bitledger on 29.10.15.
 */

@RestController
@RequestMapping( value = "/api" )
public class ApiController {

    private static final Logger logger = LoggerFactory.getLogger("rest");
    @Autowired
    EthereumBean ethereumBean;

    @RequestMapping(value = "/help", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String help(){

        String s="api/ledger/delete/{i} - delete blocks from i block (sql db)\n" +
                "api/ledger/insert/{i} - insert i block into sql db\n" +
                "api/ledger/check/{i} - compare balances for only BLOCK accounts on i block - blockchain vs sql db\n" +
                "api/ledger/checkall/{i} - compare balances for ALL accounts on i  block - blockchain vs sql db\n" +
                "\n" +
                "api/ledger/start - start bulk load sql from last sql block\n" +
                "api/ledger/stop - stop bulk loading sql db\n" +
                "api/ledger/sync - set  sync status to 'OnBlockSync'  after sql bulkload finished\n" +
                "api/ledger/stopsync - set sync status to Stop\n" +
                "api/ledger/{i} - delete and start sql db bulkload from i block" +
                "\n" +
                "api/blockchain/start - start blockchain sync\n" +
                "api/blockchain/stop - stop blockchain sync\n" +
                "api/blockchain/check - compare balances for blockchain top block vs sql db ";

        return  s;
    }

    @RequestMapping(value = "/balance/{addr}/{blockNumber}", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String checkBalance(@PathVariable String addr, @PathVariable String blockNumber,HttpServletRequest request) throws IOException, InterruptedException, SQLException {

        long t1=System.currentTimeMillis();

        LedgerStore ledgerStore = LedgerStore.getLedgerStore(ethereumBean.getListener());
        Block block = ethereumBean.getBlock(blockNumber);

        LedgerAccount acc = new LedgerAccount(addr);

        BigDecimal trieBalance=acc.getBalance(block);
        BigDecimal ledgBalance=ledgerStore.getQuery().getLedgerAccountBalance(acc,block.getNumber());

        String result="balance for "+acc.toString()+" on block "+block.getNumber();

        if (!trieBalance.equals(ledgBalance)){
            result+=String.format("%-43s%-40s%-40s%-40s%n",
                    acc.toString(),
                    Convert2json.BD2ValStr(trieBalance, false),
                    Convert2json.BD2ValStr(ledgBalance, false),
                    Convert2json.BD2ValStr(trieBalance.subtract(ledgBalance), false)
            );

            //result+="Bl:" + block.getNumber() + ",Balance Incorrect trie - ledger: " + Convert2json.BI2ValStr(trieBalance.toBigInteger(), false) + " - " + Convert2json.BI2ValStr(ledgBalance.toBigInteger(), false);
            result+="\n";
        }

        Utils.log("CheckBalance",t1,request);
        return result;

    }
    @RequestMapping(value = "/ledger/{cmd}/{param}", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String ledger_cmd(@PathVariable String cmd, @PathVariable String param,HttpServletRequest request) throws IOException,  InterruptedException {
        long t1=System.currentTimeMillis();
        LedgerStore ledgerStore = LedgerStore.getLedgerStore(ethereumBean.getListener());

        System.out.println("caller ip:"+request.getRemoteAddr());

        int i = Integer.parseInt(param);

        String ret="";
        try {
            switch (cmd) {
                case "delete":
                    ledgerStore.deleteBlocksFrom(i);
                    ret="block deleted:"+i;
                    break;
                case "insert":
                    ledgerStore.setNextStatus(ledgerStore.getSyncStatus());
                    ledgerStore.setSyncStatus(SyncStatus.SingleInsert);
                    ledgerStore.replayAndInsertBlock(i);

                    ret= "manual block inserted:"+i;
                    break;
                case "checkall":

                    ret=TestBalances.checkBalance(i);
                    ret+="\n";
                    ret+=TestBalances.checkAccountsBalance(i,true);
                    break;
                case "check":
                    //ret=TestBalances.checkBalance(i);
                    ret+="\n";
                    ret+=TestBalances.checkAccountsBalance(i,false);
                    break;

                case "findempty":
                    //ret=TestBalances.checkBalance(i);
                    ret+="\n";
                    ret+=TestBalances.findEmpty(i, false);
                    break;
            }
        }
        catch (SQLException e)
        {
            System.out.println(e.getMessage()+"\n");
            e.printStackTrace();
        }
        Utils.log("ledger_cmd",t1,request);
        return  ret;


    }

    @RequestMapping(value = "/ledger/{cmd}", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String ledger(@PathVariable String cmd) throws IOException, SQLException, InterruptedException {

        BlockchainImpl blockchain = (BlockchainImpl)ethereumBean.getEthereum().getBlockchain();
        LedgerStore ledgerStore = LedgerStore.getLedgerStore(ethereumBean.getListener());
        switch (cmd.toLowerCase())
        {
            case "stop":
                ledgerStore.stopSync();
                break;
            //ethereumBean.ledgerStopSync();
            case "start":
                ledgerStore.ledgerBulkLoad();
                //ethereumBean.ledgerStartSync(Long.MAX_VALUE);
                break;
            case "sync":
                ledgerStore.setNextStatus(SyncStatus.onBlockSync);
                break;
            case "stopsync":
                ledgerStore.setSyncStatus(SyncStatus.stopped);
                ledgerStore.setNextStatus(SyncStatus.stopped);
                break;
            default:
                try {
                    int block = Integer.parseInt(cmd);
                    ledgerStore.ledgerBulkLoad(block);
                    //ethereumBean.ledgerStartSync(block);
                }
                catch (NumberFormatException e)
                {
                    return "Wrong cmd in method /ledger/{cmd}";
                }
        }
        String result = blockchainStatus( );

        return result;
    }

    @RequestMapping(value = "/blockchain/{cmd}", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String blockchain(@PathVariable String cmd) throws IOException, SQLException, InterruptedException {

        BlockchainImpl blockchain = (BlockchainImpl)ethereumBean.getEthereum().getBlockchain();
        LedgerStore ledgerStore = LedgerStore.getLedgerStore(ethereumBean.getListener());
        String result = blockchainStatus( );
        switch (cmd.toLowerCase())
        {
            case "stop":
                ethereumBean.blockchainStopSync();

                break;
            case "start":
                ledgerStore.setSyncStatus(SyncStatus.onBlockSync);
                ledgerStore.setNextStatus(SyncStatus.onBlockSync);
                ethereumBean.blockchainStartSync();
                break;
            case "check":
                result+=TestBalances.checkBalance();
                break;
            //status
        }
        //long stopBlockNo=Long.valueOf(blockId);
        return result;
    }

    private String blockchainStatus(  ) throws SQLException, InterruptedException {

        BlockchainImpl blockchain = (BlockchainImpl)ethereumBean.getEthereum().getBlockchain();
        String result="TopBlock:"+blockchain.getBestBlock().getNumber()+"\n";
        result+="stopOn:"+blockchain.getStopOn()+"\n";

        result+="Blockchain syncStatus:"+EthereumBean.getBlockchainSyncStatus()+"\n";

        if (blockchain.getStopOn()<=blockchain.getBestBlock().getNumber())
            result+=String.format("Top block %s, Blockchain is stopped\n",String.valueOf(blockchain.getBestBlock().getNumber()));
        else
            result+=String.format("Top block %s, Blockchain is loading\n",String.valueOf(blockchain.getBestBlock().getNumber()));

        try {
            result+=String.format("Ledger sql Top block: %s\n", LedgerStore.getLedgerStore(ethereumBean.getListener()).getQuery().getSqlTopBlock());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        LedgerStore ledgerStore = LedgerStore.getLedgerStore(ethereumBean.getListener());
        result+="Ledger syncStatus:"+ledgerStore.getSyncStatus()+"\n";
        result+="Ledger nextStatus:"+ledgerStore.getNextStatus()+"\n";
        //result+="Ledger sql top block:"+ledgerStore.getSqlTopBlock()+"\n";
        //result+=checkBalance(blockchain.getBestBlock())+"\n";
        //result+=checkDelta(blockchain.getBestBlock())+"\n";

        //result+="ledgerSyncBlock: "+EthereumBean.getLedgerSyncBlock()+"\n";

        return result;
    }


}
