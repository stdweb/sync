package stdweb.Core;

import org.ethereum.core.Block;
import org.ethereum.core.BlockchainImpl;
import org.ethereum.core.Repository;
import stdweb.ethereum.EthereumBean;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;

/**
 * Created by bitledger on 01.11.15.
 */
public class BlockchainQuery {

    public static BigDecimal getTrieBalance(Block block) throws SQLException {

        BlockchainImpl blockchain = (BlockchainImpl) EthereumBean.getBlockchain();
        Repository track = blockchain.getRepository();

        Repository snapshot = track.getSnapshotTo(block.getStateRoot());

        BigInteger balance=BigInteger.valueOf(0);
        for (byte[] acc : snapshot.getAccountsKeys()) {
            balance=balance.add(snapshot.getBalance(acc));
        }
        return new BigDecimal(balance);
    }

    public static BigDecimal getCoinbaseTrieDelta(Block block) throws SQLException {

        BlockchainImpl blockchain = (BlockchainImpl) EthereumBean.getBlockchain();
        Repository track = blockchain.getRepository();

        Repository snapshot = track.getSnapshotTo(block.getStateRoot());

        BigInteger balance=BigInteger.valueOf(0);
        byte[] acc=block.getCoinbase();

        balance=balance.add(snapshot.getBalance(acc));


        if (block.getNumber()>0) {
            Block blockPrev = blockchain.getBlockByHash(block.getParentHash());
            Repository snapshotPrev = track.getSnapshotTo(blockPrev.getStateRoot());


            BigInteger balancePrev = snapshotPrev.getBalance(acc);
            balance = balance.subtract(balancePrev);

        }

        return new BigDecimal(balance);
    }
    public static BigDecimal getTrieDelta(Block block) throws SQLException {

        BlockchainImpl blockchain = (BlockchainImpl) EthereumBean.getBlockchain();
        Repository track = blockchain.getRepository();

        Repository snapshot = track.getSnapshotTo(block.getStateRoot());

        BigInteger balance=BigInteger.valueOf(0);
        for (byte[] acc : snapshot.getAccountsKeys()) {
            balance=balance.add(snapshot.getBalance(acc));
        }

        if (block.getNumber()>0) {
            Block blockPrev = blockchain.getBlockByHash(block.getParentHash());
            Repository snapshotPrev = track.getSnapshotTo(blockPrev.getStateRoot());

            for (byte[] acc : snapshotPrev.getAccountsKeys()) {
                BigInteger balancePrev = snapshotPrev.getBalance(acc);
                balance = balance.subtract(balancePrev);
            }
        }

        return new BigDecimal(balance);
    }
}
