package no.nav.sosialhjelp.innsyn.klage

import org.springframework.stereotype.Component
import java.io.InputStream
import java.util.UUID
import no.nav.sosialhjelp.innsyn.vedlegg.FilForOpplasting
import org.springframework.context.annotation.Profile

interface MellomlagerClient {
    fun lastOppVedlegg(
        klageId: UUID,
        filerForOpplasting: List<FilForOpplasting>,
    )

    fun hentVedlegg(vedleggId: UUID): ByteArray

    fun hentMetadata(klageId: UUID): List<FilMetadata>
}

@Profile("!local")
@Component
class FiksMellomlagerClient: MellomlagerClient {
    override fun lastOppVedlegg(
        klageId: UUID,
        filerForOpplasting: List<FilForOpplasting>
    ) {
        TODO("Not yet implemented")
    }

    override fun hentVedlegg(vedleggId: UUID): ByteArray {
        TODO("Not yet implemented")
    }

    override fun hentMetadata(klageId: UUID): List<FilMetadata> {
        TODO("Not yet implemented")
    }

}

@Profile("local")
@Component
class LocalMellomlagerClient : MellomlagerClient {
    override fun lastOppVedlegg(
        klageId: UUID,
        filerForOpplasting: List<FilForOpplasting>,
    ) {
        mellomlager.put(klageId, filerForOpplasting)
    }

    override fun hentMetadata(klageId: UUID): List<FilMetadata> =
        mellomlager[klageId]
            ?.map { it.metadata }
            ?: emptyList()

    override fun hentVedlegg(vedleggId: UUID): ByteArray {
        // Simulerer henting av vedlegg
        return ByteArray(0) // Returnerer en tom byte-array for eksempel
    }

    companion object {
        val mellomlager = mutableMapOf<UUID, List<FilForOpplasting>>()
    }
}

data class FilOpplasting(
    val metadata: FilMetadata,
    val data: InputStream,
)

data class FilMetadata(
    val filnavn: String,
    val mimetype: String,
    val storrelse: Long,
)

data class FilForOpplasting(
    val filnavn: String,
    val metadata: Any,
    val data: InputStream,
)
