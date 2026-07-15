package com.aegis.common.web;

import com.aegis.auth.web.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the {@link AuthInterceptor} for authenticated areas of the app.
 *
 * <p>NOTE (see REVIEW.md): the {@code /admin/**} area is intentionally NOT behind
 * the interceptor here, and {@code AdminController} has an unauthenticated
 * maintenance endpoint (CWE-306, Missing Authentication for Critical Function).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Autowired
    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/dashboard", "/claims/**", "/billing/**",
                        "/policies/**", "/documents/**", "/reports/**")
                .excludePathPatterns("/login", "/logout", "/css/**", "/js/**",
                        "/webjars/**", "/error", "/health");
    }
}
