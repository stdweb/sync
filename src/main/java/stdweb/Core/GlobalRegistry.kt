package stdweb.Core

import org.ethereum.core.CallTransaction
import org.spongycastle.util.encoders.Hex
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import stdweb.ethereum.EthereumBean


@Service
class GlobalRegistry
{
    @Autowired open var ethereumBean     : EthereumBean? = null

    internal fun Hex2ASCII(s : String): String {

        val sb = StringBuilder(s.length / 2)
        var i = 0
        while (i < s.length) {
            val hex = "" + s[i] + s[i + 1]
            val ival = Integer.parseInt(hex, 16)
            if (ival==0 )
                break
            sb.append(ival.toChar())
            i += 2
        }
        val string = sb.toString()
        return string
    }

    private fun func_name(address: String) : String {


        val registrarAddress = "33990122638b9132ca29c723bdf037f1a891a70c";
        val function = CallTransaction.Function.fromSignature("name","address");

        //val ethpoolStr="4bb96091ee9d802ed039c4d1a5f6216f90f81b01"

        val pr = ethereumBean!!.ethereum.callConstantFunction(registrarAddress,function,address);
        val hexString = Hex.toHexString(pr.getHReturn());
        //val feeValue = BigInteger(hexString, 16);

        return Hex2ASCII(hexString)
    }

    fun getName(address : ByteArray) : String {
        return getName(Hex.toHexString(address))
    }
    fun getName(hexaddress : String) : String {
        return func_name(hexaddress)
    }
}