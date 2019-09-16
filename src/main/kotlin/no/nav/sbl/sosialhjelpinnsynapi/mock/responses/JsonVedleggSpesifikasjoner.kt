package no.nav.sbl.sosialhjelpinnsynapi.mock.responses

import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonFiler
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon

val jsonVedleggSpesifikasjon = JsonVedleggSpesifikasjon()
        .withVedlegg(
                listOf(
                        JsonVedlegg()
                                .withType("kontoutskrift")
                                .withTilleggsinfo("additional")
                                .withStatus("LastetOpp")
                                .withFiler(
                                        listOf(
                                                JsonFiler()
                                                        .withFilnavn("soknad vedlegg filnavn 1")
                                                        .withSha512("asknd2341")
                                        )
                                )
                )
        )!!

val jsonVedleggSpesifikasjonEttersendelse = JsonVedleggSpesifikasjon()
        .withVedlegg(
                listOf(
                        JsonVedlegg()
                                .withType("kontoutskrift")
                                .withTilleggsinfo("additional")
                                .withStatus("LastetOpp")
                                .withFiler(
                                        listOf(
                                                JsonFiler()
                                                        .withFilnavn("ettersendelse vedlegg filnavn 1")
                                                        .withSha512("asknd2341")
                                        )
                                )
                )
        )!!

val jsonVedleggSpesifikasjonEttersendelse_2 = JsonVedleggSpesifikasjon()
        .withVedlegg(
                listOf(
                        JsonVedlegg()
                                .withType("tannlegeregning")
                                .withTilleggsinfo("additional")
                                .withStatus("LastetOpp")
                                .withFiler(
                                        listOf(
                                                JsonFiler()
                                                        .withFilnavn("ettersendelse vedlegg filnavn 2")
                                                        .withSha512("uhujasdfbuk")
                                        )
                                )
                )
        )!!