package ru.cleardocs.backend.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.cleardocs.backend.dto.ErrorDto;
import ru.cleardocs.backend.exception.BadRequestException;
import ru.cleardocs.backend.exception.NotFoundException;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ErrorDto> handleBadRequestException(BadRequestException exception) {
    log.warn("handleBadRequestException() - {}", exception.getMessage());
    return new ResponseEntity<>(new ErrorDto(exception.getMessage(), HttpStatus.BAD_REQUEST.value(), LocalDateTime.now()), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ErrorDto> handleNotFoundException(NotFoundException exception) {
    log.error("handleNotFoundException() - exception with message = {}", exception.getMessage());
    return new ResponseEntity<>(new ErrorDto(exception.getMessage(), HttpStatus.NOT_FOUND.value(), LocalDateTime.now()), HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorDto> handleException(Exception exception) {
    log.error("handleException() - exception: ", exception);
    return new ResponseEntity<>(new ErrorDto(exception.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), LocalDateTime.now()), HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
