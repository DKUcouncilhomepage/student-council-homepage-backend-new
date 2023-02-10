package com.dku.council.global.error;

import com.dku.council.global.error.exception.LocalizedMessageException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class ControllerAdvisor {

    private final MessageSource messageSource;

    @ExceptionHandler
    protected ResponseEntity<ErrorResponseDto> localizedException(LocalizedMessageException e, Locale locale) {
        return ResponseEntity.status(e.getStatus()).body(new ErrorResponseDto(messageSource, locale, e));
    }

    @ExceptionHandler
    protected ResponseEntity<ErrorResponseDto> exception(Exception e) {
        log.error("Unexpected exception:", e);
        return ResponseEntity.internalServerError().body(new ErrorResponseDto(e));
    }
}
