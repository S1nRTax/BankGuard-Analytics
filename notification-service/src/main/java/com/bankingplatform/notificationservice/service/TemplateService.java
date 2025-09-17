package com.bankingplatform.notificationservice.service;

import com.bankingplatform.notificationservice.entity.NotificationTemplateEntity;
import com.bankingplatform.notificationservice.model.NotificationChannel;
import com.bankingplatform.notificationservice.model.NotificationType;
import com.bankingplatform.notificationservice.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateService {

    private final NotificationTemplateRepository templateRepository;
    private static final Pattern TEMPLATE_VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    public Optional<String> renderTemplate(
            NotificationType type,
            NotificationChannel channel,
            String language,
            Map<String, Object> templateData) {

        Optional<NotificationTemplateEntity> templateOpt =
                templateRepository.findByTypeAndChannelAndLanguage(type, channel, language);

        if (templateOpt.isEmpty()) {
            // Fallback to English template
            templateOpt = templateRepository.findByTypeAndChannelAndLanguage(type, channel, "en");
        }

        if (templateOpt.isEmpty()) {
            log.warn("No template found for type: {}, channel: {}, language: {}", type, channel, language);
            return Optional.empty();
        }

        NotificationTemplateEntity template = templateOpt.get();
        String rendered = renderTemplateString(template.getBodyTemplate(), templateData);

        log.debug("Rendered template for type: {}, channel: {}", type, channel);
        return Optional.of(rendered);
    }

    public Optional<String> renderSubject(
            NotificationType type,
            NotificationChannel channel,
            String language,
            Map<String, Object> templateData) {

        Optional<NotificationTemplateEntity> templateOpt =
                templateRepository.findByTypeAndChannelAndLanguage(type, channel, language);

        if (templateOpt.isEmpty()) {
            templateOpt = templateRepository.findByTypeAndChannelAndLanguage(type, channel, "en");
        }

        return templateOpt.map(template -> renderTemplateString(template.getSubject(), templateData));
    }

    public Optional<String> renderHtmlTemplate(
            NotificationType type,
            NotificationChannel channel,
            String language,
            Map<String, Object> templateData) {

        Optional<NotificationTemplateEntity> templateOpt =
                templateRepository.findByTypeAndChannelAndLanguage(type, channel, language);

        if (templateOpt.isEmpty()) {
            templateOpt = templateRepository.findByTypeAndChannelAndLanguage(type, channel, "en");
        }

        return templateOpt
                .filter(template -> template.getHtmlTemplate() != null)
                .map(template -> renderTemplateString(template.getHtmlTemplate(), templateData));
    }

    private String renderTemplateString(String template, Map<String, Object> templateData) {
        if (template == null || templateData == null) {
            return template;
        }

        String result = template;
        Matcher matcher = TEMPLATE_VARIABLE_PATTERN.matcher(template);

        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object value = templateData.get(variableName);
            String replacement = value != null ? value.toString() : "";
            result = result.replace("${" + variableName + "}", replacement);
        }

        return result;
    }
}