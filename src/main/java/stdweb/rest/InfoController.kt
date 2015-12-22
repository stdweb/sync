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

import kotlinx.html.*
import kotlinx.html.dom.*
import kotlinx.html.stream.appendHTML


@RestController
@RequestMapping(value = "/info")
class InfoController {
    val ethereumBean: EthereumBean

    //@Autowired
    val repo: LedgerBlockRepository

    //@Autowired
    val ledgSync: LedgerSyncService

    @RequestMapping(value = "/status", method = arrayOf(RequestMethod.GET), produces = arrayOf(MediaType.APPLICATION_JSON_VALUE))
    @ResponseBody
    fun status(request: HttpServletRequest): Map<String, String> {

        val map = HashMap<String, String>()
        val sqlTopBlock = repo.topBlock()?.id
        val blockChainSyncStatus = ethereumBean.blockchainSyncStatus
        val ledgSyncStatus = ledgSync?.syncStatus

        var topBlock = ethereumBean.blockchain?.bestBlock?.number
        map.put("SqlTopBlock", sqlTopBlock.toString())
        map.put("TopBlock", topBlock.toString())
        map.put("BlockchainSyncStatus", blockChainSyncStatus.toString())
        map.put("LedgSyncStatus", ledgSyncStatus.toString())

        return map
    }

    @Autowired
    constructor(_ethereumBean: EthereumBean, _repo: LedgerBlockRepository, _ledgSync: LedgerSyncService) {
        this.repo = _repo
        this.ethereumBean = _ethereumBean
        this.ledgSync = _ledgSync// =ethereumBean.ledgerSync
    }

    @RequestMapping(value = "/test")
    fun info() : String {
        //val h=html {a("http://bitledger.net")}


        val text = StringBuilder {
            appendln("<!DOCTYPE html>")
            appendHTML().html {
                head { title {"title html"}}
                body {
                    a("http://kotlinlang.org") { +"link" }
                    div { +"div body" }

                    ul {
                        classes = setOf("dropdown-menu")
                        role = "menu"

                        li { a("#") { +"Action" } }
                        li { a("#") { +"Another action" } }
                        li { a("#") { +"Something else here" } }
                        li { classes = setOf("divider")}
                        li { classes = setOf("dropdown-header"); +"Nav header" }
                        li { a("#") { +"Separated link" } }
                        li { a("#") { +"One more separated link" } }
                    }
                    table {
                        thead { tr { td { +"h1" } ; td { +"h2" } } }
                        tr {
                            td { +"1" }
                            td { +"2" }
                        }
                    }
                }
            }
            appendln()
        }
        return text.toString()
    }
}
