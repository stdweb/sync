package stdweb.Repository

import org.springframework.data.repository.PagingAndSortingRepository
import stdweb.Entity.LedgerAccount

public interface LedgerAccountRepository : PagingAndSortingRepository<LedgerAccount, Int>
{
    public fun findByAddress(address : ByteArray?) : LedgerAccount?
}

