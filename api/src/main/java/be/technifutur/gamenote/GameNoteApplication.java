package be.technifutur.gamenote;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = "be.technifutur")
@ComponentScan(basePackages = "be.technifutur")
@EntityScan(basePackages = "be.technifutur.dal")
@EnableJpaRepositories(basePackages = "be.technifutur.dal")
public class GameNoteApplication {

    public static void main(String[] args) {
        SpringApplication.run(GameNoteApplication.class, args);
    }

}
