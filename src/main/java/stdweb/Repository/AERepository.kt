package stdweb.Repository

import org.springframework.data.repository.CrudRepository
import stdweb.Entity.AmountEntity

interface AERepository : CrudRepository<AmountEntity, Int>
