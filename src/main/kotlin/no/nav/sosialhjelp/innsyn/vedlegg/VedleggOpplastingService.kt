package no.nav.sosialhjelp.innsyn.vedlegg

import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import java.io.InputStream
import java.time.LocalDate

const val MAKS_TOTAL_FILSTORRELSE: Int = 1024 * 1024 * 10 // 10 MB

data class OppgaveValidering(
    val type: String,
    val tilleggsinfo: String?,
    val innsendelsesfrist: LocalDate?,
    val hendelsetype: JsonVedlegg.HendelseType?,
    val hendelsereferanse: String?,
    val filer: List<FilValidering>,
)

data class FilValidering(
    val filename: String?,
    val status: ValidationResult,
)

data class ValidationResult(
    val result: ValidationValues,
    val fileType: TikaFileType = TikaFileType.UNKNOWN,
)

enum class ValidationValues {
    OK,
    COULD_NOT_LOAD_DOCUMENT,
    PDF_IS_ENCRYPTED,
    ILLEGAL_FILE_TYPE,
    ILLEGAL_FILENAME,
    FILE_TOO_LARGE,
}

data class FilForOpplasting(
    val filnavn: Filename?,
    val mimetype: String?,
    val storrelse: Long,
    val data: InputStream,
)

fun OpplastetFil.createFilename(): Filename {
    val filenameSplit = splitFileName(filnavn.sanitize())
    return Filename(filenameSplit.name.take(50) + "-" + uuid.toString().substringBefore("-") + validering.status.fileType.toExt())
}
