package my.gov.perkeso.assist.infrastructure.jersey;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ext.Provider;
import java.util.Set;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ApplicationPath("/api")
public class JerseyConfig extends ResourceConfig {

    private final ApplicationContext applicationContext;

    public JerseyConfig(final ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void setup() {
        register(MultiPartFeature.class);
        applicationContext.getBeansWithAnnotation(Path.class).values().forEach(this::register);
        applicationContext.getBeansWithAnnotation(Provider.class).values().forEach(this::register);
        registerOpenApi();
    }

    private void registerOpenApi() {
        final OpenAPI openAPI = new OpenAPI()
                .info(new Info().title("NS3 ASSIST API").version("0.1.0")
                        .description("PERKESO ASSIST employer registration API (Fineract-style Jersey resources)"))
                .addServersItem(new Server().url("/assist-provider").description("Local server"))
                .components(new Components().addSecuritySchemes("basicAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")))
                .addSecurityItem(new SecurityRequirement().addList("basicAuth"));

        final SwaggerConfiguration swaggerConfiguration = new SwaggerConfiguration().openAPI(openAPI)
                .resourcePackages(Set.of("my.gov.perkeso.assist.registration.api",
                        "my.gov.perkeso.assist.identity.api"));

        final OpenApiResource openApiResource = new OpenApiResource();
        openApiResource.setOpenApiConfiguration(swaggerConfiguration);
        register(openApiResource);
    }
}
