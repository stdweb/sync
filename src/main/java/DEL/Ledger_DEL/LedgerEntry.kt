package DEL.Ledger_DEL

import org.ethereum.core.Block
import org.ethereum.core.TransactionReceipt
import org.ethereum.util.ByteUtil
import org.spongycastle.util.encoders.Hex
import stdweb.Core.Convert2json
import stdweb.Core.EntryResult
import stdweb.Core.EntryType
import java.math.BigDecimal
import java.sql.ResultSet
import java.sql.SQLException


/**
 * Created by bitledger on 09.10.15.
 */

annotation class DataAnnotation

class LedgerEntry  : ITableEntry {

    @DataAnnotation var id: Int = 0
    @DataAnnotation var tx: Tx? = null//
    var AccountDel: LedgerAccount_del? = null
    var Txhash: ByteArray?=null
    var amount: BigDecimal = BigDecimal.ZERO
    var block: Block? = null
    var blockNumber: Long = 0
    var blockTimestamp: Long = 0
    var depth: Byte = 0
    var gasUsed: Long = 0
    var fee: BigDecimal = BigDecimal.ZERO
    var entryType: EntryType = EntryType.NA
    var entryResult: EntryResult = EntryResult.Ok
    var offsetAccountDel: LedgerAccount_del? = null//
    var extraData: String? = null
    var grossAmount: BigDecimal = BigDecimal.ZERO
    var balance: BigDecimal = BigDecimal.ZERO
    var receipt: TransactionReceipt? = null

    private var dirty: Boolean = false

    @Throws(SQLException::class)
    override fun reload(rs: ResultSet) {
        if (rs.isFirst) {
            this.id= rs.getInt("ID")
            //this.txhash = rs.getBytes("TX")
            //this.address = rs.getBytes("ADDRESS")
            this.AccountDel =SqlDb.getSqlDb().accountStore.get(rs.getInt("ACCOUNT").toLong())
            this.amount = rs.getBigDecimal("AMOUNT")
            this.blockNumber = rs.getInt("BLOCK").toLong()
            this.blockTimestamp = rs.getLong("BLOCKTIMESTAMP")
            this.depth = rs.getByte("DEPTH")
            this.gasUsed = rs.getLong("GASUSED")

            this.fee = rs.getBigDecimal("FEE")
            this.entryType =  EntryType.values().get( rs.getByte("ENTRYTYPE").toInt())
            this.entryResult = EntryResult.values().get(rs.getByte("ENTRYRESULT").toInt())
            //this.offsetAddress = rs.getBytes("OFFSETACCOUNT")
            this.extraData = String(rs.getBytes("DESCR"))
            this.grossAmount = rs.getBigDecimal("GROSSAMOUNT")
            this.balance = rs.getBigDecimal("BALANCE")

            this.dirty = false
        }
    }

    var txNumber: Int = 0

    constructor() {    }
    override fun toString(): String {
        return "%-70s%-43s%-16s%-20s%-43s%n".format(Hex.toHexString(tx?.txhash ?: ByteUtil.EMPTY_BYTE_ARRAY), AccountDel, entryType, Convert2json.BD2ValStr(amount, true), offsetAccountDel)
    }

    //    public BigDecimal getTotalFee()
    //    {
    //        return recordSet.stream()
    //                .map(x -> x.fee)
    //                .reduce(BigDecimal.ZERO, BigDecimal::add);
    //    }

    //    public BigDecimal getTotalFee(org.ethereum.core.Account acc,EntryType _entryType)
    //    {
    //        //LedgerStore.EntryType.values().
    //        final List<EntryType> et=new ArrayList<>();
    //
    //        if (_entryType!=null)
    //            et.add(_entryType);
    //        else
    //            et .addAll( Arrays.asList(EntryType.values()));
    //
    //        return recordSet.stream()
    //                .filter(r -> r.Account.equals(acc))
    //                .filter( e -> et.contains( e ))
    //                .map(x -> x.fee)
    //                .reduce(BigDecimal.ZERO, BigDecimal::add);
    //    }

    //    public BigDecimal getTotalGrossAmount(org.ethereum.core.Account acc,EntryType _entryType)
    //    {
    //        //LedgerStore.EntryType.values().
    //        final List<EntryType> et=new ArrayList<>();
    //
    //        if (_entryType!=null)
    //            et.add(_entryType);
    //        else
    //            et .addAll( Arrays.asList(EntryType.values()));
    //
    //        return recordSet.stream()
    //                .filter(r -> r.Account.equals(acc))
    //                .filter( e -> et.contains( e ))
    //                .map(x -> x.grossAmount)
    //                .reduce(BigDecimal.ZERO, BigDecimal::add);
    //    }


    //    public LedgerEntry(LedgerAccount _acc,EntryType _entryType,byte[] _txhash, BigDecimal _amount,
    //                       Block _block,long _blockNo , long _timestamp,short _depth, long _gasUsed , BigDecimal _fee, LedgerAccount _offsetAcc, String _descr)
    //    {
    //        this.Account=_acc;
    //        this.entryType=_entryType;
    //        this.txhash=_txhash;
    //        this.amount=_amount;
    //        this.block=_block;
    //        this.blockNumber=_blockNo;
    //        this.blockTimestamp=_timestamp;
    //        this.depth=_depth;
    //        this.gasUsed=_gasUsed;
    //        this.fee=_fee;
    //        this.offsetAccount=_offsetAcc;
    //        this.extraData=_descr;
    //    }


    //    public  String tx2json()
    //    {
    //        //Block bl = replayBlock.getBlock();
    //
    //        HashMap<String, String> hashMap = new HashMap<>();
    //        String indStr=String.valueOf(this.txNumber);
    //
    //        if (this.depth>0)
    //            indStr+= " (deep " + this.depth + ")";
    //
    //        //hashMap.put("txno",Convert2json.addParentheses(indStr));
    //        hashMap.put("TX",Convert2json.addParentheses("0x" + this.txhash));
    //        hashMap.put("FEE",Convert2json.addParentheses(Convert2json.BD2ValStr(fee, true)));
    //        hashMap.put("BLOCK",String.valueOf(this.blockNumber) );
    //        hashMap.put("GASUSED",Convert2json.addParentheses(String.valueOf(this.gasUsed)));
    //        String dateStr = Convert2json.convertTimestamp2str(this.blockTimestamp);
    //        hashMap.put("BLOCKTIMESTAMP",Convert2json.addParentheses(dateStr));
    //
    //        hashMap.put("ACCOUNT", Convert2json.addParentheses( this.Account.toString()));
    //
    //        hashMap.put("AMOUNT",Convert2json.addParentheses(Convert2json.BD2ValStr(amount,true)));
    //
    //        return Convert2json.map2json(hashMap);
    //    }

}




