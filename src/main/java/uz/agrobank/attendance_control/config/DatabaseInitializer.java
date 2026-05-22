package uz.agrobank.attendance_control.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Ilova ishga tushganda db/init.sql ni bajaradi.
 * CREATE IF NOT EXISTS / CREATE OR REPLACE ishlatilgani uchun idempotent.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("db/init.sql");
            String sql = FileCopyUtils.copyToString(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
            jdbcTemplate.execute(sql);
            log.info("DB schema (tables + trigger) muvaffaqiyatli yaratildi/yangilandi.");
        } catch (Exception e) {
            log.error("DB init xatosi", e);
            throw new RuntimeException("DB init muvaffaqiyatsiz tugadi", e);
        }
    }
}
