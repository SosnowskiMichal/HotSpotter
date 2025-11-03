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
                        new Server().url("https://hotspotter.net.pl").description("Production"),
                        new Server().url("https://www.hotspotter.net.pl").description("WWW alias"),
                        new Server().url("http://localhost:8080").description("Localhost")
                ));
    }
}