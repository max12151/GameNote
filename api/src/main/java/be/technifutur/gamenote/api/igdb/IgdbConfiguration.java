package be.technifutur.gamenote.api.igdb;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(IgdbProperties.class)
public class IgdbConfiguration {

    /**
     * Délais volontairement courts.
     * <p>
     * Sans timeout, un réseau qui filtre IGDB ne répond ni ne refuse : la connexion reste
     * pendante jusqu'au délai TCP du système, plusieurs dizaines de secondes, pendant
     * lesquelles le thread HTTP de Tomcat est immobilisé. Quelques visiteurs suffisent
     * alors à figer l'application entière pour une dépendance simplement optionnelle.
     * <p>
     * Échouer vite vaut mieux : les appelants savent déjà se passer d'IGDB — le vivier de
     * la page d'accueil est mis en cache, et la fiche communautaire d'un jeu non noté se
     * dégrade en « introuvable ».
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));

        return new RestTemplate(factory);
    }
}