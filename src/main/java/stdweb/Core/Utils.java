package stdweb.Core;

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
}
