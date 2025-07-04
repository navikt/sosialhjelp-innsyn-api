package no.nav.sosialhjelp.innsyn.klage

import org.springframework.stereotype.Component
import java.io.InputStream
import java.util.UUID

interface FiksMellomlagerClient {
    fun lastOppVedlegg(
        klageId: UUID,
        filerForOpplasting: List<FilOpplasting>,
    )

    fun hentVedlegg(vedleggId: UUID): ByteArray

    fun hentMetadata(klageId: UUID): List<FilMetadata>
}

@Component
class LocalFiksMellomlagerClient : FiksMellomlagerClient {
    override fun lastOppVedlegg(
        klageId: UUID,
        filerForOpplasting: List<FilOpplasting>,
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
        val mellomlager = mutableMapOf<UUID, List<FilOpplasting>>()
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
