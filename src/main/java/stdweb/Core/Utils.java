package stdweb.Core;

import org.slf4j.Logger;

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

    public static void log(String meth,long start, HttpServletRequest request)
    {
        String remoteAddr = request.getRemoteAddr();
        long finish=System.currentTimeMillis();
        long duration=finish-start;
        //request.getMethod()

        String.format("%-43s%-40s%-40s%-40s%n","DateTime","RequestURI","ip","Duration");
        String.format("%-43s%-40s%-40s%-40s%n",new Date(System.currentTimeMillis()),request.getRequestURI(),request.getRemoteAddr(),String.valueOf(duration));

    }
}
