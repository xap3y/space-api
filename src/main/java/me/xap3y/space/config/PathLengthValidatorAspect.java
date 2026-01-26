package me.xap3y.space.config;

import jakarta.servlet.http.HttpServletRequest;
import me.xap3y.space.api.exception.BadRequestException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

@Aspect
@Component
public class PathLengthValidatorAspect {

    private final ServerInfo serverInfo;

    public PathLengthValidatorAspect(ServerInfo serverInfo) {
        this.serverInfo = serverInfo;
    }

    @Around("@annotation(me.xap3y.space.api.iface.PathLengthValidator)")
    public Object validatePathVariables(ProceedingJoinPoint pjp) throws Throwable {
        HttpServletRequest request = currentRequest();
        if (request != null) {
            @SuppressWarnings("unchecked")
            Map<String, String> pathVars = (Map<String, String>)
                    request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

            if (pathVars != null && !pathVars.isEmpty()) {
                int max = serverInfo.getMaxUniqueIdLength();
                for (Map.Entry<String, String> entry : pathVars.entrySet()) {
                    String name = entry.getKey();
                    String value = entry.getValue();
                    if (value != null && value.length() > max) {
                        /*throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Path variable '" + name + "' exceeds max length of " + max
                        );*/
                        throw new BadRequestException("Path variable '" + name + "' exceeds max length of " + max);
                    }
                }
            }
        }
        return pjp.proceed();
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }
}
