package stdweb.Core;

import org.ethereum.core.Block;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.spongycastle.util.encoders.Hex;
import stdweb.ethereum.EthereumBean;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

/**
 * Created by bitledger on 27.10.15.
 */
public class LedgerQuery {


    private final Connection conn;
    LedgerStore ledgerStore;

    public static JSONArray getJson(ResultSet resultSet,boolean calcBalance,boolean addFeeEntry) throws Exception {
        JSONArray jsonArray = new JSONArray();

        BigDecimal balance=new BigDecimal(0);
        BigDecimal fee=new BigDecimal(0);
        BigDecimal received=new BigDecimal(0);
        BigDecimal sent=new BigDecimal(0);

        while (resultSet.next()) {

            int total_cols = resultSet.getMetaData().getColumnCount();
            JSONObject obj = new JSONObject();

            EntryResult result=EntryResult.values()[resultSet.getByte("ENTRYRESULT")];

            if (calcBalance && result==EntryResult.Ok) {
                balance = balance.add(resultSet.getBigDecimal("RECEIVED")).subtract(resultSet.getBigDecimal("SENT"));
                obj.put("BALANCE", Convert2json.BI2ValStr(balance.toBigInteger(), true));

                received = received.add(resultSet.getBigDecimal("RECEIVED"));
                sent = sent.add(resultSet.getBigDecimal("SENT"));
            }

            //fee

            for (int i = 1; i <= total_cols; i++) {
                //String columnTypeName = resultSet.getMetaData().getColumnTypeName(i);
                String columnLabel = resultSet.getMetaData().getColumnLabel(i);
                DataItem item= new DataItem("ledger", columnLabel, resultSet.getObject(i));
                obj.put(item.getKey(),item.getValue());

            }
            jsonArray.add(obj);

            if (addFeeEntry && !resultSet.getBigDecimal("FEE").equals(BigDecimal.ZERO) ) {
                DataItem item;
                obj=new JSONObject();
                for (int i = 1; i <= total_cols; i++) {
                    //String columnTypeName = resultSet.getMetaData().getColumnTypeName(i);
                    String columnLabel = resultSet.getMetaData().getColumnLabel(i);
                    switch (columnLabel)
                    {
                        case "TX":
                            item= new DataItem("ledger", columnLabel, new byte[]{0});
                            break;
                        case "RECEIVED":
                            item= new DataItem("ledger", columnLabel, BigDecimal.ZERO);
                            break;
                        case "SENT":
                            item= new DataItem("ledger", columnLabel, resultSet.getObject("FEE"));
                            break;
                        case "GASUSED":
                            item= new DataItem("ledger", columnLabel, 0l);
                            break;
                        case "ENTRYTYPE":
                            item= new DataItem("ledger", columnLabel, EntryType.TxFee.ordinal());
                            break;
                        case "OFFSETACCOUNT":
                            item= new DataItem("ledger", columnLabel, new byte[]{0});
                            break;
                        case "ENTRYRESULT":
                            item= new DataItem("ledger", columnLabel, 0);
                            break;

                        default:
                            item= new DataItem("ledger", columnLabel, resultSet.getObject(i));

                    }
                    obj.put(item.getKey(),item.getValue());
                }//for cols
                jsonArray.add(obj);
            }
        }
        //add total row to json
        if (calcBalance) {
            JSONObject obj = new JSONObject();
            obj.put("BLOCK", "Total:");

            obj.put("RECEIVED", Convert2json.BI2ValStr(received.toBigInteger(), true));
            obj.put("RECEIVED", Convert2json.BI2ValStr(received.toBigInteger(), true));
            obj.put("SENT", Convert2json.BI2ValStr(sent.toBigInteger(), true));
            obj.put("FEE", Convert2json.BI2ValStr(fee.toBigInteger(), true));
            obj.put("BALANCE", Convert2json.BI2ValStr(balance.toBigInteger(), true));
            obj.put("ENTRYRESULT","Total");
            jsonArray.add(obj);
        }

        return jsonArray;
    }

    public HashMap<LedgerAccount, BigDecimal> getLedgerBalancesOnBlock(Block block,boolean checkAll) throws SQLException {

        if (block==null)
            block=EthereumBean.getBlockchain().getBlockByNumber(getSqlTopBlock());
        ResultSet rs;
        Statement statement = conn.createStatement();
        String sql;

            sql="select  address,sum(case when entryresult =0 then grossamount else grossamount-amount end) amo, count(*) c from ledger  where block<="+block.getNumber() ;
        if (!checkAll)
            sql+=" and address in (select address from ledger where block="+block.getNumber()+" ) ";

       // sql+=" and entryResult="+EntryResult.Ok.ordinal();
        sql+=" group by address";

        rs = statement.executeQuery(sql);

        HashMap<LedgerAccount,BigDecimal> account_balances =new HashMap<>();
        rs.first();
        while (rs.next())
        {
            LedgerAccount account = new LedgerAccount(rs.getBytes("ADDRESS"));
            BigDecimal amo = rs.getBigDecimal("AMO");
            account_balances.put(account,amo);
        }

        return account_balances;
    }

    public BigDecimal getLedgerBlockBalance() throws SQLException {
        return getLedgerBlockBalance(getSqlTopBlock());
    }

    public BigDecimal getLedgerAccountBalance(LedgerAccount acc,long blockNumber) throws SQLException {
        ResultSet rs;
        Statement statement = conn.createStatement();

        //String accStr = Hex.toHexString(account);
        //'f0134ff161a5c8f7c4f8cc33d3e1a7ae088594a9'
        String sql="select  sum(case when entryresult =0 then grossamount else grossamount-amount end) amo, count(*) c from ledger  where block<="+blockNumber;
        sql+=" and address=X'"+Hex.toHexString(acc.getBytes())+"' ";
        rs = statement.executeQuery(sql);
        rs.first();

        return rs.getBigDecimal(1);
    }
    public BigDecimal getLedgerBlockBalance(long blockNumber) throws SQLException {
        ResultSet rs;
        Statement statement = conn.createStatement();

        //String accStr = Hex.toHexString(account);
        //'f0134ff161a5c8f7c4f8cc33d3e1a7ae088594a9'
        String sql="select  sum(case when entryresult =0 then grossamount else grossamount-amount end) amo, count(*) c from ledger  where block<="+blockNumber;
        //sql+=" and entryResult="+EntryResult.Ok.ordinal();
        rs = statement.executeQuery(sql);
        rs.first();

        return rs.getBigDecimal(1);
    }

    public int getSqlTopBlock() throws SQLException {
        ResultSet rs;
        Statement statement = conn.createStatement();

        //String accStr = Hex.toHexString(account);
        //'f0134ff161a5c8f7c4f8cc33d3e1a7ae088594a9'
        String sql = "select max(block) as topblock from ledger";

        rs = statement.executeQuery(sql);

        boolean first = rs.first();

        return rs.getInt("topblock");

    }
    int acc_page_size=25;

    public BigDecimal getLedgerBlockDelta(Block block) throws SQLException {
        ResultSet rs;
        Statement statement = conn.createStatement();

        //String accStr = Hex.toHexString(account);
        //'f0134ff161a5c8f7c4f8cc33d3e1a7ae088594a9'
        String sql="select  sum(case when entryresult =0 then grossamount else grossamount-amount end) amo, count(*) c from ledger  where block="+block.getNumber();
        //sql+=" and entryResult="+EntryResult.Ok.ordinal();
        rs = statement.executeQuery(sql);
        rs.first();

        return rs.getBigDecimal(1);
    }
    public int ledgerCount(long _block) throws SQLException {
        ResultSet rs;
        Statement statement = conn.createStatement();

        String sql="select count(*) as c from ledger where block<="+_block;

        rs = statement.executeQuery(sql);
        rs.first();
        return  rs.getInt("c");
    }


    public String LedgerSelectByTx(String txid) throws SQLException {
        if (txid.startsWith("0x"))
            txid=txid.substring(2);

        ResultSet rs;
        Statement statement = conn.createStatement();
        //String accStr = Hex.toHexString(account);
        //'f0134ff161a5c8f7c4f8cc33d3e1a7ae088594a9'



        String sql1="select  id   , tx ,address sender,abs(amount) amount ,block ,blocktimestamp ,depth ,gasused ,fee ,entryType , offsetaccount receiver, descr ," +
                " GrossAmount,entryResult from ledger  where tx =X'" +txid+"' and entryType  in ("+EntryType.Send.ordinal()+","
                +EntryType.InternalCall.ordinal()
                +","+EntryType.Call.ordinal()
                +","+EntryType.NA.ordinal()
                +", "+EntryType.ContractCreation.ordinal()+")"+
                " order by id";
        try {

            rs = statement.executeQuery(sql1);
            JSONArray jsonArray = getJson(rs,false,false);


            return jsonArray.toJSONString();
        } catch (Exception e) {
            e.printStackTrace();
            return "{error :"+e.toString()+"}";
        }
    }

    public String LedgerSelectByBlock(long blockNumber) throws SQLException {

        String blockStr=String.valueOf(blockNumber);

        ResultSet rs;
        Statement statement = conn.createStatement();
        //String accStr = Hex.toHexString(account);
        //'f0134ff161a5c8f7c4f8cc33d3e1a7ae088594a9'

        String sql="select  id   , tx ,address Receiver ,amount ,block ,blocktimestamp ,depth ,gasused ,fee ,entryType , offsetaccount sender, descr ," +
                " GrossAmount,entryResult from ledger  where block =" +blockStr+
                " and entryType in ("+EntryType.FeeReward.ordinal()+","+EntryType.CoinbaseReward.ordinal()+", "+EntryType.UncleReward.ordinal()+")"+
                " order by id";

        String sql1="";
        if (blockNumber!=0) //not genesis
            sql1="select  id   , tx ,address sender,abs(amount) amount ,block ,blocktimestamp ,depth ,gasused ,fee ,entryType , offsetaccount receiver, descr ," +
                    " GrossAmount,entryResult from ledger  where block =" +blockStr+
                    " and entryType  in ("+EntryType.Send.ordinal()+","
                    +EntryType.InternalCall.ordinal()
                    +","+EntryType.Call.ordinal()
                    +","+EntryType.NA.ordinal()
                    +","+EntryType.Genesis.ordinal()
                    +", "+EntryType.ContractCreation.ordinal()+")"+
                    " order by id";
        else //genesis
            sql1="select  id   , tx ,address Receiver,abs(amount) amount ,block ,blocktimestamp ,depth ,gasused ,fee ,entryType , offsetaccount sender, descr ," +
                " GrossAmount,entryResult from ledger  where block =" +blockStr+
                " and entryType  in ("+EntryType.Send.ordinal()+","
                +EntryType.InternalCall.ordinal()
                +","+EntryType.Call.ordinal()
                +","+EntryType.NA.ordinal()
                +","+EntryType.Genesis.ordinal()
                +", "+EntryType.ContractCreation.ordinal()+")"+
                " order by id";

        try {

            rs = statement.executeQuery(sql);
            JSONArray jsonArray = getJson(rs,false,false);
            rs = statement.executeQuery(sql1);
            jsonArray.addAll(getJson(rs,false,false));

            return jsonArray.toJSONString();
        } catch (Exception e) {
            e.printStackTrace();
            return "{error :"+e.toString()+"}";
        }
    }

    public JSONObject acc_entry_count(String accStr,String offsetStr) throws SQLException {

        if (accStr.startsWith("0x"))
            accStr=accStr.substring(2);

        int offset;
        try {
            offset= Integer.parseInt(offsetStr);
        }
        catch (NumberFormatException e)
        {
            offset=0;
        }

        ResultSet rs;
        Statement statement = conn.createStatement();

        String sql="";
        //sql+="select sum(case when fee<>0 then 2 else 1 end) c from  ledger  where address =X'" +accStr+"' ";

        sql+=" select  count(*) c from ledger  where address =X'" +accStr+"' ";

        rs = statement.executeQuery(sql);
        rs.first();
        long entries_count = rs.getLong("c");

        try {
            JSONObject obj = new JSONObject();
            obj.put("entries_count",entries_count);


            long page_count = entries_count / acc_page_size+1;


            obj.put("page_count", page_count);

            return obj;

        } catch (Exception e) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("err",e.toString());
            return jsonObject;
        }
    }

    public JSONArray LedgerSelect(String accStr,String offsetStr) throws SQLException {

        if (accStr.startsWith("0x"))
            accStr=accStr.substring(2);

        int offset;
        try {
            offset= Integer.parseInt(offsetStr);
        }
        catch (NumberFormatException e)
        {
            offset=1;
        }

        ResultSet rs;
        Statement statement = conn.createStatement();

        String sql="";
        sql+="select  id   , tx ,address, case when amount>0 then amount else 0 end as Received ,case when amount<0 then -amount else 0 end as sent, block ,blocktimestamp ,depth ,gasused ,fee ,entryType , offsetaccount, descr ," +
                " GrossAmount,entryResult from ledger  where address =X'" +accStr+"' "
                ;//+                "order by id"; //"+EntryType.TxFee.ordinal()+" as
        //sql +=" union all ";
        //sql+=" select  id   , X'00' as tx ,address ,0 as received,fee as sent ,block ,blocktimestamp ,depth ,0 gasused, fee, "+EntryType.TxFee.ordinal()+" as  entryType , X'00' as offsetaccount, descr ," +
        //        " GrossAmount ,0 entryResult from ledger  where fee<>0 and address =X'" +accStr+"' ";
        sql+=                " order by block desc limit 25 " ;
        sql+=  "offset "+(offset-1)*acc_page_size;
        rs = statement.executeQuery(sql);

        try {

            JSONArray jsonArray = getJson(rs,true,true);
            return jsonArray;
        } catch (Exception e) {
            //e.printStackTrace();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("err",e.toString());

            JSONArray arr = new JSONArray();
            arr.add(jsonObject);
            return arr;
        }
    }


    private LedgerQuery(LedgerStore ledg_store) {
        this.ledgerStore=ledg_store;
        this.conn=this.ledgerStore.getConn();
    }

    public static LedgerQuery getQuery(LedgerStore _ledg_store)
    {

        return new LedgerQuery(_ledg_store);
    }


    public JSONObject search(String search_string) throws SQLException {
        JSONObject jsonObject = new JSONObject();

        try {
            long blockNo=Long.parseLong(search_string);
            Block block = EthereumBean.getBlockchain().getBlockByNumber(blockNo);
            if (block!=null) {
                jsonObject.put("resulttype","block");
                jsonObject.put("blocknumber", String.valueOf(block.getNumber()));
                jsonObject.put("blockhash", Hex.toHexString(block.getHash()));
                return jsonObject;
            }

        }
        catch (NumberFormatException e)
        {

        }

        if (search_string.startsWith("0x"))
            search_string=search_string.substring(2);


        String sql ="select top 1 id from ledger ";
        Statement statement;
        ResultSet rs;
        switch (search_string.length())
        {
            case 40:
                sql+=" where address =X'"+search_string+"'";
                statement = conn.createStatement();
                rs = statement.executeQuery(sql);
                if (rs.first())
                {
                    jsonObject.put("resulttype","address");
                    jsonObject.put("address",search_string);
                    return jsonObject;
                }

                break;
            case 64:

                Block block = EthereumBean.getBlockchain().getBlockByHash(Hex.decode(search_string));
                if (block!=null) {
                    jsonObject.put("resulttype","block");
                    jsonObject.put("blocknumber", String.valueOf(block.getNumber()));
                    jsonObject.put("blockhash", Hex.toHexString(block.getHash()));
                    return jsonObject;
                }

                sql+=" where tx =X'"+search_string+"'";
                statement = conn.createStatement();
                rs = statement.executeQuery(sql);
                if (rs.first())
                {
                    jsonObject.put("resulttype","tx");
                    jsonObject.put("tx",search_string);
                    return jsonObject;
                }

        }


        jsonObject.put("resulttype","");
        return jsonObject;
    }
}
