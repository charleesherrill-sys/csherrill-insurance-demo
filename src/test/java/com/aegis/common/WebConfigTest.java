package com.aegis.common;

import com.aegis.auth.web.AuthInterceptor;
import com.aegis.common.web.WebConfig;
import org.junit.Test;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.handler.MappedInterceptor;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class WebConfigTest {

    @Test
    public void adminRoutesAreBehindAuthInterceptor() {
        InspectableInterceptorRegistry registry = new InspectableInterceptorRegistry();
        new WebConfig(mock(AuthInterceptor.class)).addInterceptors(registry);

        List<Object> interceptors = registry.interceptors();
        assertEquals(1, interceptors.size());
        MappedInterceptor interceptor = (MappedInterceptor) interceptors.get(0);
        AntPathMatcher matcher = new AntPathMatcher();
        assertTrue(interceptor.matches("/admin/users", matcher));
        assertTrue(interceptor.matches("/admin/reconciliation/run", matcher));
        assertFalse(interceptor.matches("/login", matcher));
    }

    private static class InspectableInterceptorRegistry extends InterceptorRegistry {
        private List<Object> interceptors() {
            return getInterceptors();
        }
    }
}
