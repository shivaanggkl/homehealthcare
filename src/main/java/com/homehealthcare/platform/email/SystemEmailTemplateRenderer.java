package com.homehealthcare.platform.email;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SystemEmailTemplateRenderer {

    private final ResourceLoader resourceLoader;

    public String render(String templatePath, Map<String, String> variables) {
        String content = readTemplate(templatePath);
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            content = content.replace("{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : entry.getValue());
        }
        return content;
    }

    private String readTemplate(String templatePath) {
        try (InputStream inputStream = resourceLoader.getResource("classpath:" + templatePath).getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load email template " + templatePath, exception);
        }
    }
}
