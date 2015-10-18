package stdweb.ethereum;

import org.ethereum.core.Block;
import org.ethereum.core.BlockchainImpl;
import org.ethereum.facade.Ethereum;
import org.ethereum.facade.EthereumFactory;
import org.spongycastle.util.encoders.Hex;
import stdweb.Core.Convert2json;
import stdweb.Core.LedgerStore;
import stdweb.Core.ReplayBlock;

import java.net.URL;
import java.net.URLClassLoader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;




public class EthereumBean {

    public static Ethereum ethereum;
    static EthereumListener listener;
    public EthereumListener getListener()
    {
        return  listener;
    }
    public Ethereum getEthereum()
    {
        return  ethereum;
    }

    public static long getLedgerSyncBlock() {
        return ledgerSyncBlock;
    }

    public static void setLedgerSyncBlock(long ledgerSyncBlock) {
        EthereumBean.ledgerSyncBlock = ledgerSyncBlock;
    }

    private static long ledgerSyncBlock=Long.MAX_VALUE;

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


    public void printCP()
    {
        ClassLoader cl = ClassLoader.getSystemClassLoader();

        URL[] urls = ((URLClassLoader)cl).getURLs();

        for(URL url: urls){
            System.out.println(url.getFile());
        }
    }

    public void start()  {
        //printCP();
        ethereum = EthereumFactory.createEthereum();
        //((BlockchainImpl)ethereum.getBlockchain()).setStopOn(0);



        //System.out.println("web Ethereum hashcode:"+ethereum.hashCode());
        System.out.println("web Repo hashcode:"+ethereum.getRepository().hashCode());

        this.listener=new EthereumListener(ethereum);
        this.ethereum.addListener(this.listener);

        System.out.println("________________________________________________________________________");
        System.out.println("________________________________________________________________________");


        //loadLedger();
    }

    public synchronized  void  syncLedger() throws SQLException, InterruptedException {

        ((BlockchainImpl)ethereum.getBlockchain()).setStopOn(0);
        Thread.sleep(1000);
        //EthereumBean.setLedgerSyncBlock(0);

        LedgerStore ledgerStore = LedgerStore.getLedgerStore(listener);
        ledgerSyncBlock = ledgerStore.ledgerTopBlock();

        Thread sync = new Thread(() -> {
            while (ledgerSyncBlock <= ethereum.getBlockchain().getBestBlock().getNumber()) {
                try {
                    ledgerStore.insertBlock(ledgerSyncBlock);
                    ++ledgerSyncBlock;
                } catch (SQLException e) {
                    e.printStackTrace();
                }

            }
        });
        sync.start();

        if (ledgerSyncBlock==ethereum.getBlockchain().getBestBlock().getNumber())
            ledgerSyncBlock=Long.MAX_VALUE;
    }




    private void loadLedger() {
        try {
            LedgerStore ledgerStore = LedgerStore.getLedgerStore(listener);

            ledgerStore.insertBlock(89789);
            ledgerStore.insertBlock(89906);
            ledgerStore.insertBlock(94665);
            ledgerStore.insertBlock(94783);
            ledgerStore.insertBlock(114958);
            ledgerStore.insertBlock(115844);





//

//            ledgerStore.insertBlock(46147);
            //ledgerStore.insertBlock(46859);
//            while (true)
//            for (int i=1;i<=ethereum.getBlockchain().getBestBlock().getNumber();i++)
//            {
//                Block block=ethereum.getBlockchain().getBlockByNumber(i);
//                RepositoryImpl repo = (RepositoryImpl) ethereum.getRepository();
//
//                //Repository snapshotTo=repo.getSnapshotTo(block.getStateRoot());
//                BigInteger bi;
//                Set<byte[]> accountsKeys = repo.getAccountsKeys();
//                for (byte[] acc : repo.getAccountsKeys())
//                {
//                    bi=BigInteger.ONE.negate();
//
//                    bi=repo.getBalance(acc);
////
//                }
//               // Thread.sleep(500);
//
//                if (i%100==0)
//                    System.out.println("process block:"+i);
//                //ReplayBlock replayBlock=new ReplayBlock(listener,i);
//                //ledgerStore.insertBlock(i);
//            }

            //System.out.println("finish inserting block entries: "+ledgerStore.getCount());
            //insertBlockLedgerEntries(i);

            //insertBlockLedgerEntries(61142);
            //insertBlockLedgerEntries(0);
//            insertBlockLedgerEntries(46341);
//            insertBlockLedgerEntries(46359);
//            insertBlockLedgerEntries(46371);
//
//            insertBlockLedgerEntries(46401);
//            insertBlockLedgerEntries(46406);
//            insertBlockLedgerEntries(46411);
//            insertBlockLedgerEntries(53526);
//            insertBlockLedgerEntries(76988);

//            insertBlockLedgerEntries(279827);
//            insertBlockLedgerEntries(279929);
//            insertBlockLedgerEntries(280158);

//            insertBlockLedgerEntries(280268);//?
//            insertBlockLedgerEntries(280885);
//            insertBlockLedgerEntries(280931);
//            insertBlockLedgerEntries(280950);
//            insertBlockLedgerEntries(281013);
//            insertBlockLedgerEntries(281765);//uncle and no tx
//            insertBlockLedgerEntries(287368);//uncle and 1 tx


            //System.out.println("inserted myContract entries: ");


        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public String getDifficulty(){
        return "" + ethereum.getBlockchain().getTotalDifficulty().toString();
    }

    public List<Block> getBlockList(Block toplistblock)
    {
        long height =toplistblock.getNumber();// ethereum.getBlockchain().getBestBlock().getNumber();
        ArrayList<Block> blocks = new ArrayList<>();


        for (long i=height;i>=height-40;--i) {
            Block block=ethereum.getBlockchain().getBlockByNumber(i);
            blocks.add(block);
        }

        //System.out.println("block count:"+blocks.size());
        return  blocks;
    }



    public String getBestBlock(){
        return "" + ethereum.getBlockchain().getBestBlock().getNumber();
    }

    public String getBlockStr(String blockId) {

        String result = "";
        try {
            Block block = getBlock(blockId);
            result = Convert2json.block2json(block, listener);
        }

        catch (NumberFormatException e)
        {
            result=e.toString();
        }
        catch (Exception e)
        {
            result=e.toString();
        }
        return  result;
    }

    public Block getBlock(String blockId) {
        Block block;
        if (blockId.equals("top"))
            block=ethereum.getBlockchain().getBestBlock();
        else  if (blockId.length() >= 64) {
            if(blockId.startsWith("0x"))
                blockId=blockId.substring(2);

            block=ethereum.getBlockchain().getBlockByHash(Hex.decode(blockId));

        }
        else {
            Long blockNo = Long.parseLong(blockId);
            block = ethereum.getBlockchain().getBlockByNumber(blockNo);

        }
        return block;
    }

    public String getAccountLedger(String accountId) {
        try {
            LedgerStore ledgerStore = LedgerStore.getLedgerStore(listener);
            String s = ledgerStore.LedgerSelect(accountId);

            s=s.replace(":"," ");
            return s;


        } catch (SQLException e) {
            e.printStackTrace();
        }

        return  "error";
    }

    public String getBalance(String blockId) throws SQLException {
        LedgerStore ledgerStore = LedgerStore.getLedgerStore(listener);

        String b=ledgerStore.getBalance(getBlock(blockId));

        return b;

    }


    private void loadGenesis() {
        try {
            LedgerStore ledgerStore = LedgerStore.getLedgerStore(listener);

            ReplayBlock replayBlock = new ReplayBlock(listener, 0);
            ledgerStore.del_insertLedgerEntries(replayBlock);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void insertBlockLedgerEntries(long blockNo) throws Exception {

        ReplayBlock replayBlock=new ReplayBlock(listener,blockNo);
        LedgerStore ledgerStore = LedgerStore.getLedgerStore(listener);
        ledgerStore.del_insertLedgerEntries(replayBlock);
        //ledgerStore.LedgerSelect(Hex.decode("6546b3b4d3be6bb26e32977061a2b1336619c5f4"));

    }
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


}
