package no.nav.sosialhjelp.innsyn.app.tokenAnnotation

import no.nav.sosialhjelp.innsyn.app.subjecthandler.SubjectHandler
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class TokenPersonidentResolverConfig(
    private val subjectHandler: SubjectHandler
) : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(TokenPersonidentResolver(subjectHandler))
    }
}
