package stdweb.Core;

import org.ethereum.db.ByteArrayWrapper;
import org.spongycastle.util.encoders.Hex;

/**
 * Created by bitledger on 08.11.15.
 */
public class Sha3Hash {

    ByteArrayWrapper wrapper;
    public byte[] getBytes(){
        return wrapper.getData();
    }


    public Sha3Hash(byte[] _bytes)
    {
        this.wrapper=new ByteArrayWrapper(_bytes);
    }

    public boolean equals(Object other) {

        if (!(other instanceof Sha3Hash))
            return false;

        return ((Sha3Hash)other).wrapper.equals(this.wrapper);
    }

    @Override
    public int hashCode() {
        return this.wrapper.hashCode();
    }
    @Override
    public String toString()
    {
        return Hex.toHexString(getBytes());
    }
}
