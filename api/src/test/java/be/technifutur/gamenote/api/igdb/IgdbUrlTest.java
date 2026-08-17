package be.technifutur.gamenote.api.igdb;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("local")
class IgdbUrlTest {

    @Autowired
    private IgdbProperties properties;

    @Test
    void testApiUrl() {
        assertEquals("https://api.igdb.com/v4", properties.getApiUrl());
    }
}
