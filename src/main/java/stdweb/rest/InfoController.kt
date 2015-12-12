package stdweb.rest

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import stdweb.Repository.LedgerBlockRepository
import stdweb.ethereum.EthereumBean
import stdweb.ethereum.LedgerSyncService
import java.util.*
import javax.servlet.http.HttpServletRequest



@RestController
@RequestMapping(value = "/info")
class InfoController
{
    val ethereumBean : EthereumBean

    //@Autowired
    val repo: LedgerBlockRepository

    //@Autowired
    val ledgSync : LedgerSyncService

    @RequestMapping(value = "/status", method = arrayOf( RequestMethod.GET), produces = arrayOf(MediaType.APPLICATION_JSON_VALUE))
    @ResponseBody
    fun status(request: HttpServletRequest): Map<String,String> {

        val map                     = HashMap<String, String>()
        val sqlTopBlock             = repo.topBlock()?.id
        val blockChainSyncStatus    = ethereumBean.blockchainSyncStatus
        val ledgSyncStatus          = ledgSync?.syncStatus

        var topBlock=ethereumBean.blockchain?.bestBlock?.number
        map.put("SqlTopBlock",sqlTopBlock.toString())
        map.put("TopBlock",topBlock.toString())
        map.put("BlockchainSyncStatus",blockChainSyncStatus.toString())
        map.put("LedgSyncStatus",ledgSyncStatus.toString())

        return map
    }

    @Autowired
    constructor(_ethereumBean: EthereumBean, _repo : LedgerBlockRepository ,_ledgSync : LedgerSyncService)
    {
        this.repo=_repo
        this.ethereumBean=_ethereumBean
        this.ledgSync=_ledgSync// =ethereumBean.ledgerSync
    }
}
