package stdweb.Core;

import org.ethereum.core.Block;
import org.ethereum.core.BlockchainImpl;
import org.ethereum.core.Repository;
import org.ethereum.db.ByteArrayWrapper;
import org.ethereum.db.ContractDetails;
import org.ethereum.db.RepositoryImpl;
import org.spongycastle.util.encoders.Hex;
import stdweb.ethereum.EthereumBean;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Created by bitledger on 09.10.15.
 */
public class LedgerAccount {

    ByteArrayWrapper addrWrapper;
    private ContractDetails contractDetails;

    public byte[] getBytes() { return addrWrapper.getData(); }
    public static LedgerAccount GenesisAccount()
    {
        return new LedgerAccount(Hex.decode("0000000000000000000000000000000000000000"));
    }

    public LedgerAccount(byte[] _address) {
        this.addrWrapper=new ByteArrayWrapper(_address);
    }

    public LedgerAccount(byte[] receiveAddress, byte[] contractAddress) {
        if (receiveAddress==null)
            this.addrWrapper=new ByteArrayWrapper(contractAddress);
        else
            this.addrWrapper=new ByteArrayWrapper(receiveAddress);
    }

    public boolean equals(Object other) {

        if (!(other instanceof LedgerAccount))
            return false;

        return ((LedgerAccount)other).addrWrapper.equals(this.addrWrapper);
    }

    @Override
    public int hashCode() {
        return this.addrWrapper.hashCode();
    }


    public boolean isContract() {
        RepositoryImpl repository = (RepositoryImpl) EthereumBean.ethereum.getRepository();

        contractDetails = repository.getContractDetails(this.addrWrapper.getData());

        if (contractDetails==null)
            return false;
        if (contractDetails.getCode()==null)
            return false;

        return  (contractDetails.getCode().length!=0);
    }
    @Override
    public String toString()
    {
        return  Hex.toHexString(addrWrapper.getData());
    }


    public BigDecimal getBalance(Block block)
    {
        BlockchainImpl blockchain = (BlockchainImpl)EthereumBean.getBlockchain();
        Repository track = blockchain.getRepository();

        Repository snapshot = track.getSnapshotTo(block.getStateRoot());

        BigInteger balance=BigInteger.valueOf(0);

        balance=balance.add(snapshot.getBalance(this.getBytes()));

        return new BigDecimal(balance);
    }
}
