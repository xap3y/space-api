package me.xap3y.space.handler;

import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.api.exception.*;
import me.xap3y.space.model.response.DefaultResponse;
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
    public ResponseEntity<DefaultResponse> handleBadCredentialsExceptions(
            BadCredentialsException ex
    ) {
        DefaultResponse defaultResponse = new DefaultResponse(true, ex.getMessage(), LocalDateTime.now());

        return new ResponseEntity<>(defaultResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler({InsufficientAuthenticationException.class, InvalidApiKeyException.class})
    public ResponseEntity<DefaultResponse> handleUnauthorizedExceptions(
            Exception ex
    ) {
        DefaultResponse defaultResponse = new DefaultResponse(true, ex.getMessage(), LocalDateTime.now());

        return new ResponseEntity<>(defaultResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler({Exception.class, RuntimeException.class})
    public ResponseEntity<DefaultResponse> handleAllExceptions(
            Exception ex
    ) {
        log.error("Exception: ", ex);
        DefaultResponse defaultResponse = new DefaultResponse(true, ex.getMessage(), LocalDateTime.now());

        return new ResponseEntity<>(defaultResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({FileNotFoundException.class, ResourceNotFoundException.class})
    public ResponseEntity<?> handleResourceNotFoundException(
            Exception ex
    ) {
        DefaultResponse defaultResponse = new DefaultResponse(true, ex.getMessage() + "", LocalDateTime.now());

        return new ResponseEntity<>(defaultResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({InvalidInviteCodeException.class, InvalidUniqueIdException.class})
    public ResponseEntity<?> handleInvalidApiKeyException(
            RuntimeException ex
    ) {
        DefaultResponse defaultResponse = new DefaultResponse(true, ex.getMessage(), LocalDateTime.now());

        return new ResponseEntity<>(defaultResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InternalServerException.class)
    public ResponseEntity<?> handleInternalServerException(
            InternalServerException ex
    ) {
        DefaultResponse defaultResponse = new DefaultResponse(true, ex.getMessage(), LocalDateTime.now());
        return new ResponseEntity<>(defaultResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }


}
