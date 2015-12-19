package stdweb.Entity

import org.hibernate.annotations.Type
import stdweb.Core.Amount
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id



@Entity
open class AmountEntity
{
    @Id //@GeneratedValue
    var id: Int? = null

    var descr : String = ""
    //@Type(type="AmountUserType")
    var amount = Amount.Zero


}