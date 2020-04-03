/*package no.nav.sbl.sosialhjelpinnsynapi.subjectHandler

class StaticSubjectHandlerImpl : SubjectHandlerInterface {
    private val DEFAULT_USER = "26104500284" //Testbruker i idporten.
    private val DEFAULT_TOKEN = "token"
    private var user = DEFAULT_USER
    private var token = DEFAULT_TOKEN

    override fun getUserIdFromToken(): String {
        return this.user
    }

    override fun getToken(): String {
        return this.token
    }

    override fun getConsumerId() : String {
        return "StaticConsumerId"
    }

    fun setUser(user: String) {
        this.user = user
    }

    fun setFakeToken(fakeToken: String) {
        this.token = fakeToken
    }

    fun reset() {
        this.user = DEFAULT_USER
        this.token = DEFAULT_TOKEN
    }
}*/