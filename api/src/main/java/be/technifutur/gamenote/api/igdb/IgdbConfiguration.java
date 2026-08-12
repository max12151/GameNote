package be.technifutur.gamenote.api.igdb;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class IgdbConfiguration {
    @Bean
    RestClient igdbRestClient(IgdbProperties properties) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder().baseUrl(properties.baseUrl()).requestFactory(factory).build();
    }

    @Bean
    RestClient twitchRestClient() {
        return RestClient.builder().baseUrl("https://id.twitch.tv").build();
    }
}
