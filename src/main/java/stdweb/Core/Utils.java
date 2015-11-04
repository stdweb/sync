package stdweb.Core;

/**
 * Created by bitledger on 04.11.15.
 */
public class Utils {
    public static String TimeDiff(String descr,long t1, long t2) {
        return descr +" time : "+(t2-t1)+" milliseconds";
    }
}
