package stdweb.Core;

import org.ethereum.core.Block;
import org.ethereum.core.Transaction;
import org.ethereum.core.TransactionReceipt;
import org.spongycastle.util.encoders.Hex;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


/**
 * Created by bitledger on 09.10.15.
 */
public class LedgerEntry {

    public static List<LedgerEntry> recordSet;
    int txNumber;
    LedgerAccount Account;
    EntryType entryType;
    byte[] txhash;
    BigDecimal amount;
    Block block;
    long blockNo;
    long blockTimestamp;
    byte depth;
    long gasUsed;

    BigDecimal fee;
    BigDecimal grossAmount;
    EntryResult entryResult;

    LedgerAccount offsetAccount;
    String extraData;
    public Transaction tx;
    public TransactionReceipt receipt;

    public BigDecimal getTotalFee()
    {
        return recordSet.stream()
                .map(x -> x.fee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalFee(org.ethereum.core.Account acc,EntryType _entryType)
    {
        //LedgerStore.EntryType.values().
        final List<EntryType> et=new ArrayList<>();

        if (_entryType!=null)
            et.add(_entryType);
        else
            et .addAll( Arrays.asList(EntryType.values()));

        return recordSet.stream()
                .filter(r -> r.Account.equals(acc))
                .filter( e -> et.contains( e ))
                .map(x -> x.fee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalGrossAmount(org.ethereum.core.Account acc,EntryType _entryType)
    {
        //LedgerStore.EntryType.values().
        final List<EntryType> et=new ArrayList<>();

        if (_entryType!=null)
            et.add(_entryType);
        else
            et .addAll( Arrays.asList(EntryType.values()));

        return recordSet.stream()
                .filter(r -> r.Account.equals(acc))
                .filter( e -> et.contains( e ))
                .map(x -> x.grossAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public LedgerEntry(){}


    @Override
    public String toString()
    {
        return String.format("%-70s%-43s%-16s%-20s%-10s%-25s%-43s%n",
                Hex.toHexString(txhash),
                Account,entryType,
                Convert2json.BD2ValStr(amount, true),
                Convert2json.BD2ValStr(fee,true),
                Convert2json.BD2ValStr(grossAmount,true),
                offsetAccount);
    }


    //    public LedgerEntry(LedgerAccount _acc,EntryType _entryType,byte[] _txhash, BigDecimal _amount,
//                       Block _block,long _blockNo , long _timestamp,short _depth, long _gasUsed , BigDecimal _fee, LedgerAccount _offsetAcc, String _descr)
//    {
//        this.Account=_acc;
//        this.entryType=_entryType;
//        this.txhash=_txhash;
//        this.amount=_amount;
//        this.block=_block;
//        this.blockNo=_blockNo;
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
//        hashMap.put("BLOCK",String.valueOf(this.blockNo) );
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
