package stdweb.Core

import java.math.BigDecimal
import java.math.MathContext
import java.text.DecimalFormat

class Amount {
    var value: BigDecimal = BigDecimal.ZERO

    constructor(d : Double):this(BigDecimal.valueOf(d))
    {

    }
    constructor(_val : Long)
    {
        this.value= BigDecimal.valueOf(_val)
    }
    constructor(_val : BigDecimal)
    {
        this.value=_val
    }
    operator fun plus(other: Amount): Amount {

        return  Amount( value.add(other.value))
    }

    override fun equals(other: Any?): Boolean {
        return (
        when (other)
        {
            is Amount       -> value.equals(other.value)
            is BigDecimal   -> value.equals(other)
            else            -> false
        })
    }

//    fun plusAssign(other: Amount) {
//        value.add(other.value)
//    }

    operator fun minus(other: Amount): Amount {
        return Amount( value.add(other.value.negate()))
    }

//    operator fun minusAssign(other: Amount) {
//        value.add(other.value.negate())
//    }

    operator fun times(other: Amount): Amount {
        return Amount(value.multiply(other.value))
    }
//    operator fun timesAssign(other: Amount) {
//        value.multiply(other.value)
//    }

    operator fun div(other: Amount): Amount {
        return Amount(value.divide(other.value))
    }
    operator fun divAssign(other: Amount) {
        value.divide(other.value)
    }



    operator fun unaryMinus() : Amount
    {
        return Amount(value.negate())
    }
//    fun compareTo(other : BigDecimal) : BigDecimal
//    {
//
//    }
    operator fun compareTo(amo: Amount): Int {
        return value.compareTo(amo.value)
    }

    override fun toString(): String {

            val dec=value
            val dec1 = dec.divide(BigDecimal.valueOf(Math.pow(10.0, 15.0)))

            var ret = DecimalFormat("###,###,##0.##################").format(dec1)

            return ret

    }
    companion object
    {
        var Zero=Amount(0)
    }

}