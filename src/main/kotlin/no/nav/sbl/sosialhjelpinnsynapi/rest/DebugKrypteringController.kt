package no.nav.sbl.sosialhjelpinnsynapi.rest

import no.ks.kryptering.CMSKrypteringImpl
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.*
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.security.Security
import java.security.cert.X509Certificate
import java.util.concurrent.Executors
import javax.servlet.http.HttpServletRequest


@RestController
@RequestMapping("/debug")
class DebugKrypteringController(private val vedleggOpplastingService: VedleggOpplastingService,
                        private val krypteringService: KrypteringServiceImpl,
                        private val clientProperties: ClientProperties) {


    private val executor = Executors.newFixedThreadPool(4)
    private var certificate: X509Certificate? = null
    private var securityProvider = Security.getProvider("BC")

    @PostMapping("/kryptering", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun sendVedlegg(@PathVariable fiksDigisosId: String,
                    @RequestParam("files") files: MutableList<MultipartFile>,
                    @RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String,
                    request: HttpServletRequest
    ) {

        files.forEach { file ->
            val pipedInputStream = PipedInputStream()
            val pipedOutputStream = PipedOutputStream(pipedInputStream)

            val kryptering = CMSKrypteringImpl()
            if (certificate == null) {
                certificate = krypteringService.getDokumentlagerPublicKeyX509Certificate(token)
            }
            if (securityProvider == null) {
                securityProvider = Security.getProvider("BC")
            }
            kryptering.krypterData(pipedOutputStream, file.inputStream, certificate, securityProvider)
        }
    }
}
