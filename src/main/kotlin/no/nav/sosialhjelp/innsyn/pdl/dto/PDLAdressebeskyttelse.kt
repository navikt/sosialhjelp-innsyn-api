import no.nav.sosialhjelp.innsyn.pdl.dto.PDLGradering
import java.io.Serializable

data class PDLAdressebeskyttelse(
    val gradering: PDLGradering
) : Serializable
