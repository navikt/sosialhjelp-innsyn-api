package no.nav.sbl.sosialhjelpinnsynapi.subjectHandler

interface SubjectHandlerInterface {
    fun getConsumerId(): String
    fun getUserIdFromToken(): String
    fun getToken(): String
}