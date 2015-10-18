package stdweb.Core;

import org.spongycastle.util.encoders.Hex;

import java.math.BigDecimal;
import java.util.HashMap;

/**
 * Created by bitledger on 14.10.15.
 */
public class DataItem {

    String label;
    String valueStr;
    String key;
    final HashMap<String, String> mapLabels = new HashMap<>();
    final HashMap<String, String> mapAddressName = new HashMap<>();

    public String getKey() {
        return key;
    }
    public String getValue() {
        return valueStr;
    }
    public String getLabel() {
        return label;
    }

    private void setLabel(String dataPrefix, String dataItemId) {
        String key=dataPrefix.toLowerCase()+"."+dataItemId.toLowerCase();
        label=mapLabels.get(key);
    }


    public void callEthFunc(String dataPrefix,String DataItemId, Object object)
    {
//        if (!dataPrefix.toLowerCase().equals("ledger"))
//            return;
        this.key=DataItemId;
        setLabel(dataPrefix,DataItemId);
        try {

            if (DataItemId.equals("SENDER")) {
            }
            switch (DataItemId) {
                case "TX":
                case "OFFSETACCOUNT":
                case "ADDRESS":
                case "SENDER":
                case "RECEIVER":
                    if (lookupAddressName((byte[]) object) == null)
                        valueStr = "0x" + Hex.toHexString((byte[]) object);
                    else
                        valueStr = lookupAddressName((byte[]) object);

                    break;
                case "FEE":
                case "AMOUNT":
                case "GROSSAMOUNT":
                case "RECEIVED":
                case "SENT":
                    valueStr = Convert2json.BI2ValStr(((BigDecimal) object).toBigInteger(), true);
                    break;

                case "BLOCK":
                case "GASUSED":
                    valueStr = ((Long) object).toString();
                    if (valueStr.equals("0")) valueStr = "";
                    break;
                case "DEPTH":
                    valueStr = ((Byte) object).toString();
                    break;
                case "ENTRYTYPE":
                    if (object instanceof Integer)
                        valueStr = EntryType.values()[((Integer) object)].toString();
                    else
                        valueStr = EntryType.values()[((Byte) object)].toString();
                    break;
                case "DESCR":
                    valueStr = object.toString();
                    break;
                default:
                    valueStr = object.toString();
            }
        }
        catch (Exception e)
        {
            System.out.println("err:"+DataItemId);
        }

    }

    private String lookupAddressName(byte[] object) {
        if (mapAddressName.containsKey("0x"+Hex.toHexString(object)))
            return mapAddressName.get("0x"+Hex.toHexString(object));
        else
            return null;
    }


    public  DataItem(String dataPrefix,String DataItemId, Object object) {
        this.loadLabels();
        this.loadAddressNames();
        this.callEthFunc(dataPrefix,DataItemId,object);

    }

    private void loadAddressNames() {
        mapAddressName.put("0x0000000000000000000000000000000000000000","Genesis");
    }

    private void loadLabels() {
        mapLabels.put("ledger.id","Id");
        mapLabels.put("ledger.tx","tx hash");
        mapLabels.put("ledger.fee","tx hash");
        mapLabels.put("ledger.amount","Amount");
        mapLabels.put("ledger.grossamount","Gross amount");
        mapLabels.put("ledger.block","Block");
        mapLabels.put("ledger.blocktimestamp","Timestamp");
        mapLabels.put("ledger.offsetaccount","Offset account");
        mapLabels.put("ledger.address","Account");
        mapLabels.put("ledger.entrytype","Entry type");
        mapLabels.put("ledger.descr","Descr");
        mapLabels.put("ledger.gasused","Gas used");
        mapLabels.put("ledger.depth","Depth");
    }
}
