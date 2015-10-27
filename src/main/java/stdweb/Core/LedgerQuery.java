package stdweb.Core;

import org.ethereum.core.Block;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by bitledger on 27.10.15.
 */
public class LedgerQuery {


    private final Connection conn;
    LedgerStore ledgerStore;

    public static JSONArray getJson(ResultSet resultSet,boolean calcBalance) throws Exception {
        JSONArray jsonArray = new JSONArray();

        BigDecimal balance=new BigDecimal(0);
        BigDecimal fee=new BigDecimal(0);
        BigDecimal received=new BigDecimal(0);
        BigDecimal sent=new BigDecimal(0);

        while (resultSet.next()) {

            int total_cols = resultSet.getMetaData().getColumnCount();
            JSONObject obj = new JSONObject();

            if (calcBalance) {
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
            jsonArray.add(obj);
        }

        return jsonArray;
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

    public BigDecimal getLedgerBlockDelta(Block block) throws SQLException {
        ResultSet rs;
        Statement statement = conn.createStatement();

        //String accStr = Hex.toHexString(account);
        //'f0134ff161a5c8f7c4f8cc33d3e1a7ae088594a9'
        String sql="select  sum(grossamount) amo, count(*) c from ledger  where block="+block.getNumber();
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
                " GrossAmount from ledger  where tx =X'" +txid+"' and entryType  in ("+EntryType.Send.ordinal()+","
                +EntryType.InternalCall.ordinal()
                +","+EntryType.Call.ordinal()
                +","+EntryType.NA.ordinal()
                +", "+EntryType.ContractCreation.ordinal()+")"+
                " order by id";
        try {
            System.out.println(sql1);
            rs = statement.executeQuery(sql1);
            JSONArray jsonArray = getJson(rs,false);

            //System.out.println(jsonArray.toJSONString());
            return jsonArray.toJSONString();
        } catch (Exception e) {
            e.printStackTrace();
            return "{error :"+e.toString()+"}";
        }
    }

    public String LedgerSelectByBlock(String blockStr) throws SQLException {

        if (blockStr.startsWith("0x"))
            blockStr=blockStr.substring(2);

        ResultSet rs;
        Statement statement = conn.createStatement();
        //String accStr = Hex.toHexString(account);
        //'f0134ff161a5c8f7c4f8cc33d3e1a7ae088594a9'

        String sql="select  id   , tx ,address Receiver ,amount ,block ,blocktimestamp ,depth ,gasused ,fee ,entryType , offsetaccount sender, descr ," +
                " GrossAmount from ledger  where block =" +blockStr+
                " and entryType in ("+EntryType.FeeReward.ordinal()+","+EntryType.CoinbaseReward.ordinal()+", "+EntryType.UncleReward.ordinal()+")"+
                " order by id";


        String sql1="select  id   , tx ,address sender,abs(amount) amount ,block ,blocktimestamp ,depth ,gasused ,fee ,entryType , offsetaccount receiver, descr ," +
                " GrossAmount from ledger  where block =" +blockStr+
                " and entryType  in ("+EntryType.Send.ordinal()+","
                +EntryType.InternalCall.ordinal()
                +","+EntryType.Call.ordinal()
                +","+EntryType.NA.ordinal()
                +", "+EntryType.ContractCreation.ordinal()+")"+
                " order by id";
        try {
            System.out.println(sql1);
            rs = statement.executeQuery(sql);
            JSONArray jsonArray = getJson(rs,false);
            rs = statement.executeQuery(sql1);
            jsonArray.addAll(getJson(rs,false));


            System.out.println(jsonArray.toJSONString());
            return jsonArray.toJSONString();
        } catch (Exception e) {
            e.printStackTrace();
            return "{error :"+e.toString()+"}";
        }
    }

    public String LedgerSelect(String accStr) throws SQLException {

        if (accStr.startsWith("0x"))
            accStr=accStr.substring(2);

        ResultSet rs;
        Statement statement = conn.createStatement();

        String sql="select  id   , tx ,address, case when amount>0 then amount else 0 end as Received ,case when amount<0 then -amount else 0 end as sent, block ,blocktimestamp ,depth ,gasused ,fee ,entryType , offsetaccount, descr ," +
                " GrossAmount from ledger  where address =X'" +accStr+"' "
                ;//+                "order by id"; //"+EntryType.TxFee.ordinal()+" as
        sql +=" union all ";
        sql+=" select  id   , X'00' as tx ,address ,0 as received,fee as sent ,block ,blocktimestamp ,depth ,0 gasused, fee, "+EntryType.TxFee.ordinal()+" as  entryType , X'00' as offsetaccount, descr ," +
                " GrossAmount from ledger  where fee<>0 and address =X'" +accStr+"' "
                +                "order by id,entryType limit 25 ";
        rs = statement.executeQuery(sql);

        try {

            JSONArray jsonArray = getJson(rs,true);
            System.out.println(jsonArray.toJSONString());
            return jsonArray.toJSONString();
        } catch (Exception e) {
            e.printStackTrace();
            return "{error :"+e.toString()+"}";
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


}
