package stdweb.Core;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;

/**
 * Created by bitledger on 05.11.15.
 */
class Amount1 extends BigDecimal{

    public static final Amount1 ZERO =new Amount1(BigInteger.ZERO);
    public static final Amount1 ONE =new Amount1(BigInteger.ONE);


    public static Amount1 valueOf(long val) {
        return new Amount1(BigDecimal.valueOf(val).toBigInteger());
    }
    public Amount1 add(Amount1 amount)
    {
        return new Amount1(super.add(amount).toBigInteger());
    }

    public Amount1 multiply(Amount1 amount) {
        return new Amount1(super.multiply(amount).toBigInteger());
    }

    public Amount1 (int val)
    {
        super(val);
    }
    public Amount1(BigInteger bi)
    {
        super(bi);
    }
    @Override
    public Amount1 divide(BigDecimal divisor) {
          return new Amount1(super.divide(divisor).toBigInteger());
    }


    public static Amount1 fromBytes(byte[] val)
    {
        return new Amount1(new BigInteger(1,val));
    }

    @Override
    public  String toString () {
        return toString(false);
    }

    public String toString(boolean hideZero) {
        try {

            BigDecimal dec1 = this.divide(BigDecimal.valueOf(Math.pow(10, 15)));

            String ret = new DecimalFormat("###,###,##0.##################").format(dec1);
            if (hideZero && dec1.signum() == 0)
                ret = "";
            return ret;
        }
        catch (NumberFormatException e)
        {
            return e.toString();
        }
    }
}
