package stdweb.rest;

import DEL.Ledger_DEL.TestBalances;
import org.ethereum.core.BlockchainImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import stdweb.Core.*;
import DEL.Ledger_DEL.SqlDb;
import DEL.Ledger_DEL.EthereumBean_DEL;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

/**
 * Created by bitledger on 29.10.15.
 */

//@RestController
@RequestMapping( value = "/api" )
public class ApiController {


    private static final Logger logger = LoggerFactory.getLogger("rest");
    @Autowired
    EthereumBean_DEL ethereumBean;


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

//    @RequestMapping(value = "/balance/{addr}/{blockNumber}", method = GET, produces = APPLICATION_JSON_VALUE)
//    @ResponseBody
//    public String checkBalance(@PathVariable String addr, @PathVariable String blockNumber,HttpServletRequest request) throws IOException, InterruptedException, SQLException, HashDecodeException, AddressDecodeException {
//
//        long t1=System.currentTimeMillis();
//
//        SqlDb sqlDb = SqlDb.getSqlDb();
//        Block block = ethereumBean.getBlock(blockNumber);
//
//        LedgerAccount acc = AccountStore.getInstance().create(addr);
//
//        BigDecimal trieBalance=acc.getBalance(block);
//        BigDecimal ledgBalance= sqlDb.getQuery().getLedgerAccountBalance(acc,block.getNumber());
//
//        String result="balance for "+acc.toString()+" on block "+block.getNumber();
//
//        if (!trieBalance.equals(ledgBalance)){
//            result+=String.format("%-43s%-40s%-40s%-40s%n",
//                    acc.toString(),
//                    Convert2json.BD2ValStr(trieBalance, false),
//                    Convert2json.BD2ValStr(ledgBalance, false),
//                    Convert2json.BD2ValStr(trieBalance.subtract(ledgBalance), false)
//            );
//
//            //result+="Bl:" + block.getNumber() + ",Balance Incorrect trie - ledger: " + Convert2json.BI2ValStr(trieBalance.toBigInteger(), false) + " - " + Convert2json.BI2ValStr(ledgBalance.toBigInteger(), false);
//            result+="\n";
//        }
//        Utils.log("CheckBalance",t1,request);
//        return result;
//    }

    @RequestMapping(value = "/ledger/{cmd}/{param}", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String ledger_cmd(@PathVariable String cmd, @PathVariable String param,HttpServletRequest request) throws IOException,  InterruptedException {
        long t1=System.currentTimeMillis();
        SqlDb sqlDb = SqlDb.getSqlDb();

        System.out.println("caller ip:"+request.getRemoteAddr());

        int i = Integer.parseInt(param);

        String ret="";
        try {
            switch (cmd) {
                case "delete":
                    sqlDb.deleteBlocksFrom(i);
                    ret="block deleted:"+i;
                    break;
                case "insert":
                    sqlDb.setNextStatus(sqlDb.getSyncStatus());
                    sqlDb.setSyncStatus(SyncStatus.SingleInsert);
                    sqlDb.replayAndInsertBlock(i);

                    ret= "manual block inserted:"+i;
                    break;
                case "checkall":

                    ret= TestBalances.checkBalance(i);
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
        } catch (AddressDecodeException e) {
            e.printStackTrace();
        } catch (HashDecodeException e) {
            e.printStackTrace();
        }
        Utils.log("ledger_cmd",t1,request , new ResponseEntity(null, HttpStatus.NOT_IMPLEMENTED));
        return  ret;


    }

    @RequestMapping(value = "/ledger/{cmd}", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String ledger(@PathVariable String cmd) throws IOException, SQLException, InterruptedException {

        BlockchainImpl blockchain = (BlockchainImpl)ethereumBean.getEthereum().getBlockchain();
        SqlDb sqlDb = SqlDb.getSqlDb();
        switch (cmd.toLowerCase())
        {
            case "stop":
                sqlDb.stopSync();
                break;
            //ethereumBean.ledgerStopSync();
            case "start":
                sqlDb.ledgerBulkLoad();
                //ethereumBean.ledgerStartSync(Long.MAX_VALUE);
                break;
            case "sync":
                sqlDb.setNextStatus(SyncStatus.onBlockSync);
                break;
            case "stopsync":
                sqlDb.setSyncStatus(SyncStatus.stopped);
                sqlDb.setNextStatus(SyncStatus.stopped);
                break;
            default:
                try {
                    int block = Integer.parseInt(cmd);
                    sqlDb.ledgerBulkLoad(block);
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
        SqlDb sqlDb = SqlDb.getSqlDb();
        String result = blockchainStatus( );
        switch (cmd.toLowerCase())
        {
            case "stop":
                ethereumBean.blockchainStopSync();

                break;
            case "start":
                sqlDb.setSyncStatus(SyncStatus.onBlockSync);
                sqlDb.setNextStatus(SyncStatus.onBlockSync);
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
        //result+="stopOn:"+blockchain.getStopOn()+"\n";

        result+="Blockchain syncStatus:"+ EthereumBean_DEL.getBlockchainSyncStatus()+"\n";

//        if (blockchain.getStopOn()<=blockchain.getBestBlock().getNumber())
//            result+=String.format("Top block %s, Blockchain is stopped\n",String.valueOf(blockchain.getBestBlock().getNumber()));
//        else
//            result+=String.format("Top block %s, Blockchain is loading\n",String.valueOf(blockchain.getBestBlock().getNumber()));

        try {
            result+=String.format("Ledger_DEL sql Top block: %s\n", SqlDb.getSqlDb().getQuery().getSqlTopBlock());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        SqlDb sqlDb = SqlDb.getSqlDb();
        result+="Ledger_DEL syncStatus:"+ sqlDb.getSyncStatus()+"\n";
        result+="Ledger_DEL nextStatus:"+ sqlDb.getNextStatus()+"\n";
        //result+="Ledger_DEL sql top block:"+ledgerStore.getSqlTopBlock()+"\n";
        //result+=checkBalance(blockchain.getBestBlock())+"\n";
        //result+=checkDelta(blockchain.getBestBlock())+"\n";

        //result+="ledgerSyncBlock: "+EthereumBean.getLedgerSyncBlock()+"\n";

        return result;
    }


}
