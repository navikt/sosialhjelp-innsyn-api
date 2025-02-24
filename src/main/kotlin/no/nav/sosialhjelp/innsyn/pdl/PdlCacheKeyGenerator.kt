package no.nav.sosialhjelp.innsyn.pdl

import com.nimbusds.jwt.JWTParser
import org.springframework.cache.interceptor.KeyGenerator
import org.springframework.stereotype.Component
import java.lang.reflect.Method

/** Reeturns person ident for the given token */
@Component
class PdlCacheKeyGenerator : KeyGenerator {
    override fun generate(
        target: Any,
        method: Method,
        vararg params: Any,
    ): Any = JWTParser.parse(params[0] as String).jwtClaimsSet.getStringClaim("pid")
}
