package pwr.zpi.hotspotter.unit.authentication.component;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.NativeWebRequest;
import pwr.zpi.hotspotter.authentication.annotation.CurrentUser;
import pwr.zpi.hotspotter.authentication.component.CurrentUserArgumentResolver;
import pwr.zpi.hotspotter.user.model.User;
import pwr.zpi.hotspotter.user.service.UserService;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurrentUserArgumentResolverTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private CurrentUserArgumentResolver resolver;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    static class DummyController {
        public void method(@CurrentUser User ignoredUser) {}
        public void methodWithoutAnnotation(User ignoredUser) {}
    }

    private MethodParameter paramWithAnnotation() throws Exception {
        Method m = DummyController.class.getMethod("method", User.class);
        return new MethodParameter(m, 0);
    }

    private MethodParameter paramWithoutAnnotation() throws Exception {
        Method m = DummyController.class.getMethod("methodWithoutAnnotation", User.class);
        return new MethodParameter(m, 0);
    }

    @Test
    void supportsParameterReturnsTrueOnlyForAnnotatedUser() throws Exception {
        assertThat(resolver.supportsParameter(paramWithAnnotation())).isTrue();
        assertThat(resolver.supportsParameter(paramWithoutAnnotation())).isFalse();
    }

    @Test
    void throwsIfAuthenticationMissing() {
        SecurityContextHolder.clearContext();
        NativeWebRequest req = mock(NativeWebRequest.class);

        assertThatThrownBy(() ->
                resolver.resolveArgument(paramWithAnnotation(), null, req, null)
        ).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void throwsIfAuthenticationIsAnonymous() {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("key", "anon",
                        List.of(new SimpleGrantedAuthority("ROLE_ANON")))
        );

        NativeWebRequest req = mock(NativeWebRequest.class);

        assertThatThrownBy(() ->
                resolver.resolveArgument(paramWithAnnotation(), null, req, null)
        ).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void resolvesCurrentUserFromAuthentication() throws Exception {
        TestingAuthenticationToken auth =
                new TestingAuthenticationToken("user@example.com", null);
        auth.setAuthenticated(true);

        SecurityContextHolder.getContext().setAuthentication(auth);

        User user = new User();
        when(userService.loadUserEntityByEmail("user@example.com")).thenReturn(user);

        NativeWebRequest req = mock(NativeWebRequest.class);

        Object result = resolver.resolveArgument(paramWithAnnotation(), null, req, null);

        assertThat(result).isSameAs(user);
        verify(userService).loadUserEntityByEmail("user@example.com");
    }
}
