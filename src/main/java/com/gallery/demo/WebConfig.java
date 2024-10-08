package com.gallery.demo;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:/app/images/");

        registry.addResourceHandler("/thumbnails/128x128/**")
                .addResourceLocations("file:/app/thumbnails/128x128/");

        registry.addResourceHandler("/thumbnails/256x256/**")
                .addResourceLocations("file:/app/thumbnails/256x256/");

        registry.addResourceHandler("/thumbnails/512x512/**")
                .addResourceLocations("file:/app/thumbnails/512x512/");
    }

}
