package no.nav.sosialhjelp.innsyn.app.tokenAnnotation

import no.nav.sosialhjelp.innsyn.app.subjecthandler.SubjectHandler
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class TokenPersonidentResolver(
    private val subjectHandler: SubjectHandler
) : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.getParameterAnnotation(TokenPersonident::class.java) != null && parameter.parameterType == String::class.java

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): String = subjectHandler.getUserIdFromToken()
}
