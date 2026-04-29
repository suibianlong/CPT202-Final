package com.cpt202.HerLink.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class UploadStaticResourceConfig implements WebMvcConfigurer {

    private final String uploadDir;
    private final String frontendDir;

    public UploadStaticResourceConfig(@Value("${HerLink.upload-dir:uploads}") String uploadDir,
                                      @Value("${HerLink.frontend-dir:../frontend}") String frontendDir) {
        this.uploadDir = uploadDir;
        this.frontendDir = frontendDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        String uploadLocation = toDirectoryLocation(uploadPath);
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadLocation);

        Path frontendPath = Paths.get(frontendDir).toAbsolutePath().normalize();
        registry.addResourceHandler("/**")
                .addResourceLocations(toDirectoryLocation(frontendPath), "classpath:/static/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
    }

    private String toDirectoryLocation(Path path) {
        String location = path.toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        return location;
    }
}
