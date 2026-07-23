package com.aegis.common.web;

import com.aegis.auth.web.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the {@link AuthInterceptor} for authenticated areas of the app.
 *
 * <p>The {@code /admin/**} area is behind the interceptor (CWE-306 remediation);
 * {@code AdminController} additionally enforces the {@code ADMIN} role, since
 * authentication alone is not sufficient for those endpoints.
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
                        "/policies/**", "/documents/**", "/reports/**", "/admin/**")
                .excludePathPatterns("/login", "/logout", "/css/**", "/js/**",
                        "/webjars/**", "/error", "/health");
    }
}
