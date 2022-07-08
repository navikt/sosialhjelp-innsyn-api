package no.nav.sosialhjelp.innsyn.app.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.multipart.commons.CommonsMultipartResolver

@Configuration
class MultipartConfig {

    @Bean(name = ["multipartResolver"])
    fun multipartResolver(): CommonsMultipartResolver {
        val multipartResolver = CommonsMultipartResolver()
        multipartResolver.setMaxUploadSize(MAX_UPLOAD_SIZE)
        return multipartResolver
    }

    companion object {
        private const val MAX_UPLOAD_SIZE = 350 * 1024 * 1024L // 350 MB. Summen av filer som kan sendes i et POST-kall
    }
}
