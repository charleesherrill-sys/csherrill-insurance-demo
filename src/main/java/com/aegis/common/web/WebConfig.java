package com.aegis.common.web;

import com.aegis.auth.web.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the {@link AuthInterceptor} for authenticated areas of the app.
 *
 * <p>SECURITY (CWE-306): the {@code /admin/**} area is covered by the auth
 * interceptor so every admin endpoint requires an authenticated session.
 * Role-based authorization (ADMIN only) is enforced in {@code AdminController}.
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
