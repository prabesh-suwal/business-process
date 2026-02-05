package com.cas.common.security;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Spring MVC argument resolver for @CurrentUser annotated parameters.
 * 
 * Register this resolver in your WebMvcConfigurer:
 * 
 * <pre>
 * {
 *     &#64;code
 *     &#64;Configuration
 *     public class WebConfig implements WebMvcConfigurer {
 *         @Override
 *         public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
 *             resolvers.add(new CurrentUserArgumentResolver());
 *         }
 *     }
 * }
 * </pre>
 */
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class) &&
                UserContext.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

        CurrentUser annotation = parameter.getParameterAnnotation(CurrentUser.class);
        boolean required = annotation != null && annotation.required();

        UserContext context = UserContextHolder.getContext();

        if (required && (context == null || !context.isAuthenticated())) {
            throw new IllegalStateException("@CurrentUser parameter requires authenticated user context. " +
                    "Ensure UserContextFilter is configured and request passed through gateway.");
        }

        return context;
    }
}
