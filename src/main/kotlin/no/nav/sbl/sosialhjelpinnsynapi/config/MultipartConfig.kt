package no.nav.sbl.sosialhjelpinnsynapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.multipart.commons.CommonsMultipartResolver


@Configuration
class MulitpartConfig {

    @Bean(name = ["multipartResolver"])
    fun multipartResolver(): CommonsMultipartResolver {
        val multipartResolver = CommonsMultipartResolver()
        multipartResolver.setMaxUploadSize(50000000) //50 MB. Summen av filer som kan sendes i et POST-kall
        return multipartResolver
    }

}