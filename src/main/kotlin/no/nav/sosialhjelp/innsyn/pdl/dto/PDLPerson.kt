import java.io.Serializable

data class PDLPerson(
    val adressebeskyttelse: List<PDLAdressebeskyttelse>,
    val navn: List<PDLNavn>
) : Serializable
