package stdweb.Core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

/**
 * Created by bitledger on 04.11.15.
 */
public class Utils {
    public static String TimeDiff(String descr,long t1, long t2) {
        System.out.println(descr +" time : "+(t2-t1)+" milliseconds");
        return (descr +" time : "+(t2-t1)+" milliseconds");
    }

    public static String remove0x(String str) {
        if(str.startsWith("0x"))
            str=str.substring(2);
        return str;
    }

    private static final Logger logger = LoggerFactory.getLogger("rest");

    public static void log(String meth,long start, HttpServletRequest request,boolean isInner)
    {
        String remoteAddr = request.getRemoteAddr();
        long finish=System.currentTimeMillis();
        long duration=finish-start;
        //request.getMethod()
        if (isInner ) meth=" > > "+meth;
        String.format("%-43s%-40s%-40s%-40s%n","DateTime","RequestURI","ip","Duration");
        String msg=String.format("%-32s%-80s%-24s%-24s%-16s%n",new Date(System.currentTimeMillis()),request.getRequestURI(),meth,request.getRemoteAddr(),String.valueOf(duration));
        logger.info(msg);

    }
    public static void log(String meth,long start, HttpServletRequest request)
    {
        log(meth,start,request,false);

    }
}
