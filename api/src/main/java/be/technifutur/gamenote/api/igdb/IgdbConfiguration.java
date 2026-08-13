package be.technifutur.gamenote.api.igdb;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(IgdbProperties.class)
public class IgdbConfiguration {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}