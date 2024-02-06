import org.springframework.stereotype.Component
import org.springframework.web.server.CoWebFilter
import org.springframework.web.server.CoWebFilterChain
import org.springframework.web.server.ServerWebExchange
import java.net.URI
import java.net.URISyntaxException

@Component
class TrailingSlashRedirectFilter : CoWebFilter() {

    override suspend fun filter(exchange: ServerWebExchange, chain: CoWebFilterChain) {

        val originalUri = exchange.request.uri;

        if (originalUri.path.endsWith("/")) {
            val originalPath = originalUri.getPath();
            val newPath = originalPath.substring(0, originalPath.length - 1); // ignore trailing slash
            try {
                val newUri = URI(
                    originalUri.scheme,
                    originalUri.getUserInfo(),
                    originalUri.host,
                    originalUri.port,
                    newPath,
                    originalUri.getQuery(),
                    originalUri.getFragment(),
                );

                val response = exchange.response;
                response.headers.location = newUri;
                return
            } catch (e: URISyntaxException) {
                throw IllegalStateException(e.message, e);
            }
        }
        return chain.filter(exchange);
    }
}
