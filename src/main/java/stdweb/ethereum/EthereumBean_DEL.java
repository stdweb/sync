package stdweb.ethereum;

import org.ethereum.core.Block;
import org.ethereum.core.BlockchainImpl;
import org.ethereum.db.RepositoryImpl;
import org.ethereum.facade.Ethereum;
import org.ethereum.facade.EthereumFactory;
import org.springframework.beans.factory.annotation.Autowired;
import stdweb.Core.*;
import DEL.Ledger_DEL.*;
import stdweb.Repository.LedgerAccountRepository;
import stdweb.Repository.LedgerBlockRepository;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.SQLException;


public class EthereumBean_DEL {

    public static Ethereum ethereum;
    static EthereumListener listener;


    public static SyncStatus getBlockchainSyncStatus() {
        return blockchainSyncStatus;
    }

    static SyncStatus blockchainSyncStatus;

    public static BlockchainImpl getBlockchainImpl() {
        return blockchain;
    }

    private static BlockchainImpl blockchain;

    public static EthereumListener getListener()
    {
        return  listener;
    }
    public Ethereum getEthereum()
    {
        return  ethereum;
    }


//    public static long getLedgerSyncBlock() {
//        return ledgerSyncBlock;
//    }
//
//
//    private static long ledgerSyncBlock=Long.MAX_VALUE;

    public static RepositoryImpl getRepositoryImpl() {
        return    (RepositoryImpl) ethereum.getRepository();
    }

    public LedgerBlockRepository getBlockRepo() {
        return blockRepo;
    }

    @Autowired
    LedgerBlockRepository blockRepo;
    @Autowired
    LedgerAccountRepository accRepo;


    public void start()  {
        ethereum = EthereumFactory.createEthereum();
        System.out.println( "ethBean hashcode:"+ethereum.hashCode());

        this.listener=new EthereumListener(ethereum);
        this.ethereum.addListener(this.listener);

        blockchain = ((BlockchainImpl) ethereum.getBlockchain());

        blockchainStopSync();
        //blockchainStartSync();

        System.out.println("ethBean.start - block repo is null = "+(blockRepo==null));
        System.out.println("________________________________________________________________________");
        System.out.println("________________________________________________________________________");

    }
    public void start_old()  {
        //printCP();
//        AzureSql();


        ethereum = EthereumFactory.createEthereum();

        this.listener=new EthereumListener(ethereum);
        this.ethereum.addListener(this.listener);
        blockchain = ((BlockchainImpl) ethereum.getBlockchain());
        //blockchainStopSync();
        blockchainStartSync();

        SqlDb sqlDb = SqlDb.getSqlDb();

        System.out.println("________________________________________________________________________");
        System.out.println("________________________________________________________________________");

        //SpringTransaction springTransaction = new SpringTransaction();
        //springTransaction.loadBlockRepo();
        //check();
//        try {

//            loadSomeAccounts();
//            testSomeBlocks();
//            testSomeEntries();
//
//
//        } catch (SQLException e) {
//            e.printStackTrace();
//        } catch (AddressDecodeException e) {
//            e.printStackTrace();
//        } catch (HashDecodeException e) {
//            e.printStackTrace();
//        }

    }




    public void printCP()
    {
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        URL[] urls = ((URLClassLoader)cl).getURLs();
        for(URL url: urls){
            System.out.println(url.getFile());
        }
    }

    private void testSomeEntries() throws SQLException, HashDecodeException {

        SqlDb sqlDb = SqlDb.getSqlDb();
        LedgerEntryStore ledgerStore = sqlDb.getLedgerStore();
        AccountStore accountStore = sqlDb.getAccountStore();

        LedgerAccount_del acc1 = accountStore.get(1l);
        LedgerAccount_del acc2 = accountStore.get(2l);

        Tx tx = new Tx();
        tx.setTxhash(Utils.hash_decode("0xa535fdbb19975f664ed8c359d6c80e86634e249bdc2da53370e7a20ccf89ff69"));
        tx.setId(100);

        LedgerEntry ledgerEntry = new LedgerEntry();
        //ledgerEntry.setId(10);
        ledgerEntry.setAccountDel(acc1);
        ledgerEntry.setOffsetAccountDel(acc2);
        ledgerEntry.setTx(tx);
        ledgerEntry.setAmount(BigDecimal.TEN);
        ledgerEntry.setFee(BigDecimal.TEN);
        ledgerEntry.setGrossAmount(BigDecimal.TEN);
        ledgerEntry.setEntryType(EntryType.ContractCreated);
        ledgerStore.write(ledgerEntry);

        String s=ledgerEntry.toString();
        System.out.println(s);

        ledgerStore.commit();
    }

    private void testSomeBlocks() throws SQLException {
        LedgerBlockStore blockStore = LedgerBlockStore.getInstance();
        BlockchainImpl blockchain = EthereumBean_DEL.getBlockchainImpl();

        for (int b=170001;b<=180000;++b)
        {

            Block blockByNumber = blockchain.getBlockByNumber(b);


            BigDecimal balance=BigDecimal.ZERO;
            BigDecimal reward=BigDecimal.ZERO;
            BigDecimal fee=BigDecimal.ZERO;
//
            del_LBlock ledgerBlockDel = blockStore.create(blockByNumber, fee, reward, balance);
            blockStore.write(ledgerBlockDel);
            System.out.println("insert block:"+ ledgerBlockDel.getNumber());
//            System.out.println("block sql insert:"+b);
        }
    }

    private void loadSomeAccounts() throws SQLException, AddressDecodeException, HashDecodeException {
        String a="0xde0b295669a9fd93d5f28d9ec85e40f4cb697bae";
        String b="0x18edbb78c987efd0ef489359ed253b672298596b";
        String c="0x18edbb78c987efd0ef489359ed253b67229859aa";

        SqlDb sqlDb = SqlDb.getSqlDb();
        AccountStore accountStore=AccountStore.getInstance();

        LedgerAccount_del ledgerAccount=accountStore.get(a);
        ledgerAccount.setName("n aaa");
        accountStore.write(ledgerAccount);

        ledgerAccount=accountStore.get(b);
        ledgerAccount.setName("n bbb");
        accountStore.write(ledgerAccount);

        ledgerAccount=accountStore.get(c);
        ledgerAccount.setName("n cccc");
        accountStore.write(ledgerAccount);


        ledgerAccount=accountStore.get("0x18edbb78c987efd0ef489359ed253b67229859aa");
        ledgerAccount.setName("bb cc cc");

        accountStore.write(ledgerAccount);

    }

    private void check() throws HashDecodeException, AddressDecodeException {
        ReplayBlock_DEL replayBlock = new ReplayBlock_DEL( 181692);
        replayBlock.run();
    }

    public String getDifficulty(){
        return "" + ethereum.getBlockchain().getTotalDifficulty().toString();
    }


//    public Block getBlock(String blockId) {
//        Block block;
//        if (blockId.equals("top"))
//            block=ethereum.getBlockchain().getBestBlock();
//        else  if (blockId.length() >= 64) {
//            if(blockId.startsWith("0x"))
//                blockId=blockId.substring(2);
//
//            block=ethereum.getBlockchain().getBlockByHash(Hex.decode(blockId));
//
//        }
//        else {
//            Long blockNo = Long.parseLong(blockId);
//            block = ethereum.getBlockchain().getBlockByNumber(blockNo);
//
//        }
//        return block;
//    }
//    public String getBestBlock(){
//        return "" + ethereum.getBlockchain().getBestBlock().getNumber();
//    }

//    public String getBlockStr(String blockId) {
//
//        String result = "";
//        try {
//            Block block = getBlock(blockId);
//            result = Convert2json.block2json(block, listener);
//        }
//
//        catch (NumberFormatException e)
//        {
//            e.printStackTrace();
//            //result=e.toString();
//        }
//        catch (Exception e)
//        {
//            e.printStackTrace();
//            //result=e.toString();
//        }
//        return  result;
//    }




    public String getBalance(Block block) throws SQLException {

        BigDecimal bigDecimal = SqlDb.getSqlDb().getQuery().getLedgerBlockBalance(block.getNumber());


        BigInteger bi=BigInteger.valueOf(0);

        if (bigDecimal!= null)
            bi = bigDecimal.toBigInteger();

        //Long count=rs.getLong(2);

        BigInteger trieBalance = BlockchainQuery.getTrieBalance(block).toBigInteger();


        return "{'balance' "+ Convert2json.BI2ValStr(bi, false) +", 'triebalance'" +Convert2json.BI2ValStr(trieBalance,false)+ " }";
    }

//    public String getBalance(String blockId) throws SQLException, HashDecodeException  {
//        SqlDb sqlDb = SqlDb.getSqlDb();
//        String b=getBalance(getBlock(blockId));
//
//        return b;
//    }



    public static void blockchainStartSync()
    {
        String ret="";
        blockchain.setStopOn(Long.MAX_VALUE);
        if (blockchainSyncStatus==SyncStatus.onBlockSync)
            ret="blockchain sync is already started";
        else {
            ret="blockchain sync is started";
        }
        blockchainSyncStatus=SyncStatus.onBlockSync;
        SqlDb sqlDb = SqlDb.getSqlDb();
        //ledgerStore.setSyncStatus(ledgerStore.getNextStatus());
        System.out.println(ret);
    }

    public static void blockchainStopSync()  {
        String ret="";
//        if (blockchainSyncStatus==SyncStatus.stopped)
//            ret="blockchain sync is already stopped";
//        else
        ret="blockchain sync is stopped";

        blockchain.setStopOn(0);
        blockchainSyncStatus=SyncStatus.stopped;

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
                  ;
        System.out.println(ret);
    }

    public static Block getBlockByNumber(long blockNo) {
        return getBlockchainImpl().getBlockByNumber(blockNo);
    }


//    public void ledgerStopSync() {
//
//    }
//
//    public void ledgerStartSync(long _ledgerSyncBlock)  {
//        blockchainStopSync();
//
//        LedgerStore ledgerStore = LedgerStore.getLedgerStore(listener);
//        int ledgerTopBlock=0;
//        try {
//            ledgerTopBlock=ledgerStore.getSqlTopBlock();
//
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//    }
//    public List<Transaction> getTxList(String blockId) {
//
//
//        Block block = getBlock(blockId);
//        ReplayBlock replayBlock=new ReplayBlock(ethereum,block);
//
//        ReplayBlock replayBlock1 = new ReplayBlock(ethereum, block);
//        replayBlock.run();
//        List<Transaction> transactionsList = replayBlock.txlist;
//
//        return transactionsList;
//    }


//
//    private void loadLedger() {
//        try {
//            LedgerStore ledgerStore = LedgerStore.getLedgerStore(listener);
//
//            ledgerStore.write(89789);
//            ledgerStore.write(89906);
//            ledgerStore.write(94665);
//            ledgerStore.write(94783);
//            ledgerStore.write(114958);
//            ledgerStore.write(115844);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    //contract
    //[{"constant":false,"inputs":
//        [{"name":"amount","type":"uint256"}],"name":"withdraw","outputs":[]
    //      ,"type":"function"},{"constant":false,"inputs":[],"name":"remove","outputs":[],"type":"function"},
    //{"constant":false,"inputs":[],"name":"deposit","outputs":[],"type":"function"},{"inputs":[],"type":"constructor"}]
    //[{"constant":false,"inputs":[{"name":"amount","type":"uint256"}],"name":"withdraw","outputs":[],"type":"function"},{"constant":false,"inputs":[],"name":"remove","outputs":[],"type":"function"},{"constant":false,"inputs":[],"name":"deposit","outputs":[],"type":"function"},{"inputs":[],"type":"constructor"}]

//    var bankContract = web3.eth.contract([{"constant":false,"inputs":[{"name":"amount","type":"uint256"}],"name":"withdraw","outputs":[],"type":"function"},{"constant":false,"inputs":[],"name":"remove","outputs":[],"type":"function"},{"constant":false,"inputs":[],"name":"deposit","outputs":[],"type":"function"},{"inputs":[],"type":"constructor"}]);
//        var bank = bankContract.new(
//        {
//        from: web3.eth.accounts[0],
//        data: '60606040525b33600060006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908302179055505b6102098061003f6000396000f360606040526000357c0100000000000000000000000000000000000000000000000000000000900480632e1a7d4d1461004f578063a7f4377914610062578063d0e30db01461006f5761004d565b005b6100606004803590602001506100bb565b005b61006d600450610175565b005b61007a60045061007c565b005b34600160005060003373ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000206000828282505401925050819055505b565b80600160005060003373ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000206000505410806100f85750600081145b1561010257610172565b80600160005060003373ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000206000828282505403925050819055503373ffffffffffffffffffffffffffffffffffffffff16600082604051809050600060405180830381858888f19350505050505b50565b600060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff16141561020657600060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16ff5b5b56',
//        gas: 1000000
//        }, function(e, contract){
//        if (typeof contract.address != 'undefined') {
//        console.log(e, contract);
//        console.log('Contract mined! address: ' + contract.address + ' transactionHash: ' + contract.transactionHash);
//        }
//        })

    //Contract mined! address: 0x0ab9c581e846d0ed7e86c4dcdebe5a68c3520de5 transactionHash: 0x6f00a9eb55bb537806c015bd22cededcf4950bbaa987e507ac228dbfdde819e7
    //Contract mined! address: 0xe5d247f735fc133dd5520cc72184b20fd0b9958e transactionHash: 0xf2656a67833d2d44ae92922f2010d0343235e6a11669064285310728cce8a084
    //Contract mined! address: 0x6546b3b4d3be6bb26e32977061a2b1336619c5f4 transactionHash: 0x1e9565b99fe8c3fe2ee6a4e08e672bee04f5095b61907f86baedef1a377f6e63

    //pingcontract
    //Contract mined! address: 0xfc2c6085cde719bd28c07db43784440f7b8f1099 transactionHash: 0x5131cf527fb2f12798f1e75f0c886fa66f5a66d6e44fbc5eb0cbb6847513b125


    //contract invocation block# 281013 tx 0x275de8f52e08e8f66c7d21900d9ee8bedb1114d2eb82706a7a7e65f6d7e4b745
    //contract address 0x6546b3b4d3be6bb26e32977061a2b1336619c5f4 -50 fi
    //Myaddre 0xf0134ff161a5c8f7c4f8cc33d3e1a7ae088594a9 +50 fi

    ///my geth account f0134ff161a5c8f7c4f8cc33d3e1a7ae088594a9 pass S.. 11
    //0.2 ether


    //private void loadGenesis() {
//        try {
//            LedgerStore ledgerStore = LedgerStore.getLedgerStore(listener);
//            ReplayBlock replayBlock = new ReplayBlock(listener, 0);
//            ledgerStore.del_insertLedgerEntries(replayBlock);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }


    //

//    public void insertBlockLedgerEntries(long blockNo) throws Exception {
//
//        ReplayBlock replayBlock=new ReplayBlock(listener,blockNo);
//        LedgerStore ledgerStore = LedgerStore.getLedgerStore(listener);
//        ledgerStore.del_insertLedgerEntries(replayBlock);
//        //ledgerStore.LedgerSelect(Hex.decode("6546b3b4d3be6bb26e32977061a2b1336619c5f4"));
//
//    }
}
