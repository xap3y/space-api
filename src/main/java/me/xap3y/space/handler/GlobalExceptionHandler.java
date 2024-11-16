package me.xap3y.space.handler;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.dto.DefaultResponse;
import me.xap3y.space.exception.InvalidApiKeyException;
import me.xap3y.space.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.FileNotFoundException;
import java.time.LocalDateTime;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<DefaultResponse> handleAllExceptions(
            BadCredentialsException ex,
            HttpServletRequest request
    ) {
        DefaultResponse defaultResponse = new DefaultResponse(true, ex.getMessage(), LocalDateTime.now());

        return new ResponseEntity<>(defaultResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler({InsufficientAuthenticationException.class, InvalidApiKeyException.class})
    public ResponseEntity<DefaultResponse> handleAllExceptions(
            InsufficientAuthenticationException ex,
            HttpServletRequest request
    ) {
        DefaultResponse defaultResponse = new DefaultResponse(true, ex.getMessage(), LocalDateTime.now());

        return new ResponseEntity<>(defaultResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler({Exception.class, RuntimeException.class})
    public ResponseEntity<DefaultResponse> handleAllExceptions(
            Exception ex,
            HttpServletRequest request
    ) {
        DefaultResponse defaultResponse = new DefaultResponse(true, ex.getMessage(), LocalDateTime.now());

        return new ResponseEntity<>(defaultResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({FileNotFoundException.class, ResourceNotFoundException.class})
    public ResponseEntity<DefaultResponse> handleAllExceptions(
            FileNotFoundException ex,
            HttpServletRequest request
    ) {
        DefaultResponse defaultResponse = new DefaultResponse(true, ex.getMessage(), LocalDateTime.now());

        return new ResponseEntity<>(defaultResponse, HttpStatus.NOT_FOUND);
    }


}
