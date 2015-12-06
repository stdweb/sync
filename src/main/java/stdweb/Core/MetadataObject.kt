package stdweb.Core

import DEL.Ledger_DEL.DataAnnotation
import DEL.Ledger_DEL.ITableEntry

import sun.plugin2.liveconnect.JavaClass
import sun.security.util.Length
import java.util.*

/**
 * Created by bitledger on 17.11.15.
 */
class MetadataObject
{
    var MetadataClass : Class<ITableEntry>

    var fields : ArrayList<Field> = ArrayList()
    var fieldNames : List<String> = ArrayList()
    public fun Properties() : List<String>
    {
        var list=arrayListOf<String>()

        for( field  in MetadataClass.declaredFields)
        {

           // list.add(field.name)

            if (field.isAnnotationPresent(DataAnnotation::class.java))
            {
                list.add(field.name)
            }
        }

        list.forEach { println (it) }
        return list
    }

    public fun constructSqlInsert() : String{
        return ""
    }

    public fun constructSqlCreateTable() : String{
        var sql="CREATE TABLE IF NOT EXISTS "+MetadataClass.name
        return sql;
        //var fieldlist =fields.reduce { field, field ->  }
    }

    constructor( o : ITableEntry )
    {

        //var c: Class<ITableEntry>
        val ent =entity("LedgerAccount","Account","Ledger_DEL Account")


        MetadataClass = o.javaClass

        fieldNames = listOf("id","address","name","nonce","iscontract","lastblock","balance","stateroot")

        fields=arrayListOf(
                Field("id","int","integer",null, KeyType.SurrogateKey,"Account#","Account"),
                Field("address","byte","",null , KeyType.NaturalKey,"Address","Address")
        )


    }
    enum class KeyType {field, NaturalKey, SurrogateKey, Identity}

    data class Field (var id : String, var javatype : String, var sqltype : String,
                      var length: Int?, var fieldType : KeyType ,var shortDescr: String, var longDescr : String )

    data class entity ( var id : String , var shortDescr : String , var longDescr : String )

}