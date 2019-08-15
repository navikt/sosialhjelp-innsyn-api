package no.nav.sbl.sosialhjelpinnsynapi.common

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.ktor.application.ApplicationCall
import io.ktor.features.origin
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.request.ApplicationRequest
import io.ktor.request.header
import io.ktor.request.receive
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.xml.datatype.XMLGregorianCalendar

internal suspend inline fun <reified T : Any> ApplicationCall.receiveTry() = try {
    receive<T>()
} catch (e: Throwable) {
    when (e) {
        is InvalidFormatException, is MissingKotlinParameterException, is JsonParseException, is MismatchedInputException -> {
            throw InvalidInputException(e.message ?: "Could not parse input")
        }
        else -> throw e
    }
}

internal sealed class ParamType(val description: String) {
    object Header : ParamType("header")
    object Parameter : ParamType("parameter")
    object QueryParameter : ParamType("query parameter")

    override fun toString(): String = this.description
}

internal inline fun <reified T> ApplicationCall.getFromRequest(
    type: ParamType,
    name: String,
    defaultValue: T? = null
): T =
    when (type) {
        ParamType.Header -> request.header(name)
        ParamType.Parameter -> parameters[name]
        ParamType.QueryParameter -> request.queryParameters[name]
    }.let { value ->
        when (T::class) {
            String::class -> value.takeIf { !value.isNullOrBlank() } as T?
                ?: defaultValue
                ?: throw InvalidInputException("Missing or empty $type '$name', no default string value specified")
            Boolean::class -> (value.takeIf { !value.isNullOrBlank() }?.toBoolean()
                ?: defaultValue
                ?: throw InvalidInputException("Missing or empty $type '$name', no default boolean value specified")
                ) as T
            else -> throw IllegalArgumentException("Unsupported type ${T::class.java.simpleName}")
        }
    }

internal fun ApplicationRequest.url(): String {
    val port = when (origin.port) {
        in listOf(80, 443) -> ""
        else -> ":${origin.port}"
    }
    return "${origin.scheme}://${origin.host}$port${origin.uri}"
}

internal fun String.formatDate(
    formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("ddMMyy"),
    formatTo: DateTimeFormatter? = null
): String =
    try {
        LocalDate.parse(this, formatter).apply {
            formatTo?.let { this.format(it) }
        }.toString()
    } catch (e: Throwable) {
        val message = "Invalid date format '$this', should be DDMMYY"
        throw InvalidInputException("$message - $e")
    }

internal inline fun <reified T : Enum<T>> String.getEnumValueOfOrNull(): T? = try {
    ignoreCaseValueOf<T>(this)
} catch (e: InvalidInputException) {
//    logger.warn { "Could not find enum value for '$this' in enum '${T::class.simpleName}" }
    null
}

internal inline fun <reified T : Enum<T>> String.getEnumValueOfOrDefault(defaultValue: T): T = try {
    ignoreCaseValueOf(this)
} catch (e: InvalidInputException) {
 //   logger.warn { "Could not find enum value for '$this' in enum '${T::class.simpleName}, returning default value $defaultValue" }
    defaultValue
}

internal fun XMLGregorianCalendar?.dateTimeToMillis(): Long? =
    this?.toGregorianCalendar()?.toZonedDateTime()?.toLocalDateTime()?.toInstant(ZoneOffset.UTC)?.toEpochMilli()

internal fun XMLGregorianCalendar?.dateToMilis(): Long? =
    this?.toGregorianCalendar()?.toZonedDateTime()?.toLocalDate()?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()

val HttpHeaders.NavApiKey: String
    get() = "x-nav-apiKey"
val HttpHeaders.CorrelationId: String
    get() = "CorrelationID"
val HttpHeaders.CustomerId: String
    get() = "CustomerID"
val HttpHeaders.PartyId: String
    get() = "PartyID"
val HttpHeaders.LegalMandate: String
    get() = "Legal-Mandate"
val HttpHeaders.NavPersonidenter: String
    get() = "Nav-Personidenter"
val HttpHeaders.NavPersonident: String
    get() = "Nav-Personident"
val HttpHeaders.NavCallid: String
    get() = "Nav-Call-Id"
val HttpHeaders.NavConsumerId: String
    get() = "Nav-Consumer-Id"
val HttpHeaders.NavConsumerToken: String
    get() = "Nav-Consumer-Token"
val HttpHeaders.NavSaksbehandlerId: String
    get() = "Nav-Saksbehandler-Id"
val HttpHeaders.SearchParameter: String
    get() = "SearchParameter"
val ContentType.Application.Jose
    get() = ContentType("application", "jose")
val HttpHeaders.SokId: String
    get() = "SokID"
