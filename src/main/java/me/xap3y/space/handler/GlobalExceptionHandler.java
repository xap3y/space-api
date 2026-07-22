package me.xap3y.space.handler;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.api.enums.MetricRecordType;
import me.xap3y.space.api.exception.*;
import me.xap3y.space.model.response.DefaultResponse;
import me.xap3y.space.service.PrometheusMetricService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;

import java.io.FileNotFoundException;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    private final PrometheusMetricService prometheusMetricService;

    public GlobalExceptionHandler(PrometheusMetricService prometheusMetricService) {
        this.prometheusMetricService = prometheusMetricService;
    }

    @ExceptionHandler({BadCredentialsException.class, UnauthorizedException.class, InsufficientAuthenticationException.class, InvalidApiKeyException.class})
    public ResponseEntity<DefaultResponse> handleBadCredentialsExceptions(
            Exception ex
    ) {
        DefaultResponse defaultResponse = new DefaultResponse(true, ex.getMessage());

        return new ResponseEntity<>(defaultResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(ImageToolsException.class)
    public ResponseEntity<DefaultResponse> handleImageTools(ImageToolsException ex) {
        DefaultResponse defaultResponse = new DefaultResponse(true, ex.getMessage());
        return new ResponseEntity<>(defaultResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<DefaultResponse> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request
    ) {
        prometheusMetricService.recordEvent(MetricRecordType.EXCEPTION_CAUGHT);
        log.info("User tried unsupported operation: {} on path: {}", ex.getMessage(), request.getRequestURI());
        DefaultResponse defaultResponse = new DefaultResponse(true, ex.getMessage());

        return new ResponseEntity<>(defaultResponse, HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler({Exception.class, RuntimeException.class})
    public ResponseEntity<DefaultResponse> handleAllExceptions(
            Exception ex
    ) {
        prometheusMetricService.recordEvent(MetricRecordType.ERROR_OCCURRED);
        prometheusMetricService.recordEvent(MetricRecordType.EXCEPTION_CAUGHT);
        log.error("Exception: ", ex);
        DefaultResponse defaultResponse = new DefaultResponse(true, ex.getMessage());

        HttpHeaders errorHeaders = new HttpHeaders();
        errorHeaders.setContentType(MediaType.APPLICATION_JSON);

        return new ResponseEntity<>(defaultResponse, errorHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({FileNotFoundException.class, ResourceNotFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<?> handleResourceNotFoundException(
            Exception ex
    ) {
        DefaultResponse defaultResponse = new DefaultResponse(true, ex.getMessage());

        return new ResponseEntity<>(defaultResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({MaxUploadSizeExceededException.class, InvalidFormatException.class, BadRequestException.class, IllegalArgumentException.class, InvalidInviteCodeException.class, InvalidUniqueIdException.class, MissingRequestCookieException.class, MissingCredentialsException.class, HttpMediaTypeNotSupportedException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<?> handleInvalidApiKeyException(
            RuntimeException ex
    ) {
        prometheusMetricService.recordEvent(MetricRecordType.EXCEPTION_CAUGHT);
        DefaultResponse defaultResponse = new DefaultResponse(true, ex.getMessage());

        return new ResponseEntity<>(defaultResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InternalServerException.class)
    public ResponseEntity<?> handleInternalServerException(
            InternalServerException ex
    ) {
        DefaultResponse defaultResponse = new DefaultResponse(true, ex.getMessage());
        return new ResponseEntity<>(defaultResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({ResourceExpiredException.class, ResourceVisibilityException.class, ResourceAccessForbiddenException.class})
    public ResponseEntity<?> handleNoAuthorizedException(
            RuntimeException ex
    ) {
        DefaultResponse defaultResponse = new DefaultResponse(true, ex.getMessage());

        return new ResponseEntity<>(defaultResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(EmailVerifyCodeExpired.class)
    public ResponseEntity<?> handleEmailVerifyCodeExpired(
            EmailVerifyCodeExpired ex
    ) {
        DefaultResponse defaultResponse = new DefaultResponse(true, ex.getMessage());
        return new ResponseEntity<>(defaultResponse, HttpStatus.GONE);
    }
}
