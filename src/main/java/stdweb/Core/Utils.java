package stdweb.Core;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.DecoderException;
import org.spongycastle.util.encoders.Hex;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

/**
 * Created by bitledger on 04.11.15.
 */
public class Utils {

    public static final byte[] ZERO_BYTE_ARRAY_20 = new byte[]{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};

    public static String TimeDiff(String descr,long t1 ) {
        long t2=System.currentTimeMillis();
        System.out.println(descr +" time : "+(t2-t1)+" milliseconds");
        return (descr +" time : "+(t2-t1)+" milliseconds");
    }

    public static String Remove0x(String str) {
        if(str.startsWith("0x"))
            str=str.substring(2);
        return str;
    }

    private static final Logger logger = LoggerFactory.getLogger("rest");

    public static void log(String meth,long start, HttpServletRequest request,ResponseEntity responseEntity,boolean isInner)
    {
        String remoteAddr = request.getRemoteAddr();
        long finish=System.currentTimeMillis();
        long duration=finish-start;
        //request.getMethod()
        if (isInner ) meth=" > > "+meth;
        String.format("%-43s%-40s%-40s%-40s%n","DateTime","RequestURI","ip","Duration");
        String msg=String.format("%-32s%-75s%-20s%-16s%-16s%-16s",new Date(System.currentTimeMillis()),request.getRequestURI(),meth,request.getRemoteAddr(),String.valueOf(duration),
                responseEntity.getStatusCode());
        logger.info(msg);

    }
    public static void log(String meth, long start, HttpServletRequest request, ResponseEntity responseEntity)
    {
        log(meth,start,request,responseEntity,false);

    }

    public static byte[] address_decode(String s) throws AddressDecodeException {
        s= Utils.Remove0x(s);
        if (s.length()!=40) return null;//throw new AddressDecodeException("Error decoding address. Incorrect string size: "+ s +" : "+s.length());
        try{
            return Hex.decode(s);

        }
        catch (DecoderException e)
        {
            return null;
            //throw new AddressDecodeException("incorrect address:"+s);
        }
    }

    public static byte[] hash_decode(String s) throws HashDecodeException {

        s= Utils.Remove0x(s);
        if (s.length()!=64) return null;// throw new HashDecodeException("Error decoding hash. Incorrect string size: "+ s +" : "+s.length());
        try{
            return Hex.decode(s);

        }
        catch (DecoderException e)
        {
            return null;
            //throw new HashDecodeException("incorrect hash:"+s);
        }
    }
}
