package no.nav.sosialhjelp.innsyn.klage

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonKlage
import no.nav.sbl.soknadsosialhjelp.digisos.soker.klage.JsonKlageHendelse
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

interface KlageClient {
    fun send(klage: Klage)
    fun lastOppDokument()
    fun hentKlage(fiksDigisosId: UUID, klageId: UUID): JsonKlage
    fun hentKlager(fiksDigisosId: String): List<JsonKlage>
}

@Profile("prodgcp | preprod")
@Component
class FiksKlageClient: KlageClient {

    override fun send(klage: Klage) {
        TODO("Not yet implemented")
    }

    override fun lastOppDokument() {
        TODO("Not yet implemented")
    }

    override fun hentKlage(fiksDigisosId: UUID, klageId: UUID): JsonKlage {
        TODO("Not yet implemented")
    }

    override fun hentKlager(fiksDigisosId: String): List<JsonKlage> {
        TODO("Not yet implemented")
    }

}


@Profile("!(prodgcp|preprod)")
@Component
class MockedKlageClient : KlageClient {
    override fun send(klage: Klage) {
        TODO("Not yet implemented")
    }

    override fun lastOppDokument() {
        TODO("Not yet implemented")
    }

    override fun hentKlage(fiksDigisosId: UUID, klageId: UUID): JsonKlage {
        TODO("Not yet implemented")
    }

    override fun hentKlager(fiksDigisosId: String): List<JsonKlage> {
        TODO("Not yet implemented")
    }

    companion object {
        val klageId1 = UUID.randomUUID()
        val klageId2 = UUID.randomUUID()
        val fiksDigisosId = UUID.randomUUID()

        private val storage: MutableMap<UUID, JsonKlage> = mutableMapOf(
            klageId1 to klage1,
            klageId2 to klage2,
        )
    }
}

private val klage1 = JsonKlage()
    .apply {
        this.klageId = MockedKlageClient.klageId1.toString()
        this.tittel = "Ny klage 1"
        this.hendelser = emptyList()
    }

private val klage2 = JsonKlage()
    .apply {
        this.klageId = MockedKlageClient.klageId2.toString()
        this.tittel = "Ny klage 2"
        this.hendelser = emptyList()
    }