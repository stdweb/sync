package stdweb.Desktop

import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
//import org.ethereum.util.ByteUtil
import stdweb.Core.Sha3Hash

//import io.datafx.controller.ViewController;
/**
 * Created by bitledger on 16.11.15.
 */
class DataClass {

    var id      : String    = ""
    var descr   : Any       = ""
    var hash    : Sha3Hash?  = null//Sha3Hash(ByteUtil.ZERO_BYTE_ARRAY)

    public fun toList() : ObservableList<String>
    {
        var ret=FXCollections.observableArrayList<String>();
        return ret;
    }

}


