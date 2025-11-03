package pwr.zpi.hotspotter.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .servers(List.of(
                        new Server().url("https://www.hotspotter.net.pl/api").description("WWW alias"),
                        new Server().url("https://api.hotspotter.net.pl/api").description("API alias"),
                        new Server().url("http://localhost:8080/api").description("Localhost")
                ));
    }
}