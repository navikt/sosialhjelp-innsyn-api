package no.nav.sosialhjelp.innsyn.utils

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

object TimeUtils {

    fun toUtc(tidspunkt: LocalDateTime, zoneId: ZoneId = ZoneId.systemDefault()): LocalDateTime {
        return ZonedDateTime.of(tidspunkt, zoneId)
            .withZoneSameInstant(ZoneOffset.UTC)
            .toLocalDateTime()
    }
}
