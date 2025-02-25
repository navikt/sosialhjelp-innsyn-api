package no.nav.sosialhjelp.innsyn.pdl

import com.nimbusds.jwt.JWTParser
import org.springframework.cache.interceptor.KeyGenerator
import org.springframework.stereotype.Component
import java.lang.reflect.Method

/** This method returns the person ident  */
@Component
class PdlCacheKeyGenerator : KeyGenerator {
    override fun generate(
        target: Any,
        method: Method,
        vararg params: Any,
    ): Any {
        assert(params.size == 1 && params[0] is String)
        val token = params[0] as String
        return JWTParser.parse(token).jwtClaimsSet.getStringClaim("pid")
    }
}
