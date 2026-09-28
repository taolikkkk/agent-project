package cn.hollis.llm.mentor.know.engine.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.http.CacheControl;
import java.time.Duration;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Vite 产物带内容哈希，允许浏览器长期缓存。
        registry.addResourceHandler("/assets/**").addResourceLocations("classpath:/static/assets/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable());
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longIdsAsStrings() {
        // 防止浏览器丢失 Long 主键的精度。
        return builder -> builder.serializerByType(Long.class, ToStringSerializer.instance);
    }

    @Controller
    static class FrontendRoutes {
        @GetMapping({
                "/", "/login", "/staff-login", "/chat", "/documents", "/documents/**", "/qa-cache",
                "/login.html", "/staff-login.html", "/chat.html", "/document.html", "/upload.html", "/qa-cache.html"
        })
        public String index() { return "forward:/index.html"; }
    }
}
