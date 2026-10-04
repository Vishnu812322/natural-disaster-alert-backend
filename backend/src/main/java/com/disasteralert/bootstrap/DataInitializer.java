package com.disasteralert.bootstrap;

import com.disasteralert.sources.FeedSource;
import com.disasteralert.sources.FeedSourceRepository;
import com.disasteralert.users.User;
import com.disasteralert.users.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner seed(
            UserRepository repo,
            FeedSourceRepository sourceRepository,
            PasswordEncoder encoder) {

        return args -> {
            if (repo.findByEmail("admin@example.com").isEmpty()) {
                User u = new User();
                u.setEmail("admin@example.com");
                u.setName("Development Administrator");
                u.setPasswordHash(encoder.encode("Admin@12345"));
                u.setRole("ADMIN");
                u.setPhone("+910000000000");
                repo.save(u);
            }

            if (sourceRepository.findByCode("SACHET_NDMA").isEmpty()) {
                FeedSource source = new FeedSource();
                source.setCode("SACHET_NDMA");
                source.setName("NDMA SACHET — India CAP RSS");
                source.setType("CAP_RSS");
                source.setUrl("https://sachet.ndma.gov.in/cap_public_website/rss/rss_india.xml");
                source.setEnabled(true);
                sourceRepository.save(source);
            }
        };
    }
}
