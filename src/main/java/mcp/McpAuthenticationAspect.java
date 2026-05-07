package mcp;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class McpAuthenticationAspect {

    private final McpAuthConfig authConfig;

    public McpAuthenticationAspect(McpAuthConfig authConfig) {
        this.authConfig = authConfig;
    }

    @Around("@annotation(org.springaicommunity.mcp.annotation.McpTool)")
    public Object authenticateToolAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!authConfig.isEnabled()) {
            return joinPoint.proceed();
        }

        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String providedKey = request.getHeader("X-API-Key");

            if (!authConfig.getApiKey().equals(providedKey)) {
                throw new SecurityException("Authentication failed. Provide X-API-Key header");
            }
        }

        return joinPoint.proceed();
    }
}