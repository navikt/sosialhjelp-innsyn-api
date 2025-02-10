import java.io.Serializable

data class PDLNavn (
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String
) : Serializable
