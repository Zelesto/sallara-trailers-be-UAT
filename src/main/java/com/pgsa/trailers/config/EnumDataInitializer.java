// src/main/java/com/pgsa/trailers/config/EnumDataInitializer.java
package com.pgsa.trailers.config;

import com.pgsa.trailers.entity.enums.CustomEnum;
import com.pgsa.trailers.repository.CustomEnumRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class EnumDataInitializer implements ApplicationRunner {

    private final CustomEnumRepository customEnumRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Skip seeding - system enums are provided by the service
        // Only seed if you want default custom enums
        log.info("Enum data initializer completed");
    }
}
