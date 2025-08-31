package Assignment.Recipe_Generator.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Forward all non-API routes to index.html for SPA routing
        // This handles routes like /recipes, /saved, etc.
        registry.addViewController("/")
            .setViewName("forward:/index.html");
        registry.addViewController("/recipes")
            .setViewName("forward:/index.html");
        registry.addViewController("/saved")
            .setViewName("forward:/index.html");
        registry.addViewController("/setup")
            .setViewName("forward:/index.html");
        registry.addViewController("/ingredients")
            .setViewName("forward:/index.html");
        registry.addViewController("/image")
            .setViewName("forward:/index.html");
        registry.addViewController("/logs")
            .setViewName("forward:/index.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/static/");
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/");
        registry.addResourceHandler("/*.js", "/*.css", "/*.png", "/*.jpg", "/*.svg", "/*.ico")
                .addResourceLocations("classpath:/static/");
    }
}



