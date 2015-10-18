package stdweb.Core;

import org.ethereum.db.ByteArrayWrapper;
import org.ethereum.db.ContractDetails;
import org.ethereum.db.RepositoryImpl;
import org.spongycastle.util.encoders.Hex;
import stdweb.ethereum.EthereumBean;

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


}
