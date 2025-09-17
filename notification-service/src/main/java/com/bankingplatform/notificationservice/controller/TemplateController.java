package com.bankingplatform.notificationservice.controller;

import com.bankingplatform.notificationservice.entity.NotificationTemplateEntity;
import com.bankingplatform.notificationservice.repository.NotificationTemplateRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final NotificationTemplateRepository templateRepository;

    @GetMapping
    public ResponseEntity<List<NotificationTemplateEntity>> getAllTemplates() {
        List<NotificationTemplateEntity> templates = templateRepository.findAll();
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/{templateId}")
    public ResponseEntity<NotificationTemplateEntity> getTemplate(@PathVariable String templateId) {
        return templateRepository.findByTemplateId(templateId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<NotificationTemplateEntity> createTemplate(
            @Valid @RequestBody NotificationTemplateEntity template) {

        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());

        NotificationTemplateEntity saved = templateRepository.save(template);
        log.info("Created template: {}", saved.getTemplateId());

        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{templateId}")
    public ResponseEntity<NotificationTemplateEntity> updateTemplate(
            @PathVariable String templateId,
            @Valid @RequestBody NotificationTemplateEntity template) {

        return templateRepository.findByTemplateId(templateId)
                .map(existing -> {
                    existing.setSubject(template.getSubject());
                    existing.setBodyTemplate(template.getBodyTemplate());
                    existing.setHtmlTemplate(template.getHtmlTemplate());
                    existing.setIsActive(template.getIsActive());
                    existing.setDescription(template.getDescription());
                    existing.setUpdatedAt(LocalDateTime.now());

                    NotificationTemplateEntity saved = templateRepository.save(existing);
                    log.info("Updated template: {}", templateId);

                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{templateId}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable String templateId) {
        return templateRepository.findByTemplateId(templateId)
                .map(template -> {
                    templateRepository.delete(template);
                    log.info("Deleted template: {}", templateId);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/languages")
    public ResponseEntity<List<String>> getSupportedLanguages() {
        List<String> languages = templateRepository.findAllActiveLanguages();
        return ResponseEntity.ok(languages);
    }
}