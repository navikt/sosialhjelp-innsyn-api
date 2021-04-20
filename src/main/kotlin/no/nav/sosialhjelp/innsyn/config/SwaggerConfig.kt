package no.nav.sosialhjelp.innsyn.config

import no.nav.sosialhjelp.innsyn.utils.isRunningInProd
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.oas.annotations.EnableOpenApi
import springfox.documentation.spi.DocumentationType.OAS_30
import springfox.documentation.spring.web.plugins.Docket

@Profile("(dev-sbs | mock | mock-alt | local)")
@Configuration
@EnableOpenApi
class SwaggerConfig {

    @Bean
    fun api(): Docket {
        if (isRunningInProd()) {
            throw Error("Swagger-bean blir forsøkt generert i prod. Stopper applikasjonen da dette er en sikkerhetsrisiko.")
        }
        return Docket(OAS_30)
            .select()
            .apis(RequestHandlerSelectors.any())
            .paths(PathSelectors.regex(".*/api/v1/.*"))
            .build()
    }
}
