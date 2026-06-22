package com.tickmine.planner;

import com.tickmine.domain.port.PromptTemplateLoader;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PromptLoader implements PromptTemplateLoader {

    private final ResourceLoader resourceLoader;

    @Override
    public String load(String name, Map<String, Object> variables) {
        Resource resource = resourceLoader.getResource("classpath:prompts/" + name);
        PromptTemplate template = new PromptTemplate(resource);
        return template.render(variables);
    }
}
