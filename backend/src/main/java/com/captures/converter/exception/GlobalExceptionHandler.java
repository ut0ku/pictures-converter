package com.captures.converter.exception;
import com.captures.converter.dto.Dtos.ErrorResponse;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
 @ExceptionHandler(ApiException.class)
 ResponseEntity<ErrorResponse> api(ApiException ex){return ResponseEntity.status(ex.getStatus()).body(new ErrorResponse(ex.getStatus().getReasonPhrase(),ex.getMessage(),System.currentTimeMillis()));}
 @ExceptionHandler(MaxUploadSizeExceededException.class)
 ResponseEntity<ErrorResponse> upload(MaxUploadSizeExceededException ex){return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(new ErrorResponse("PAYLOAD_TOO_LARGE","Один файл превышает лимит 50 MB",System.currentTimeMillis()));}
 @ExceptionHandler(MethodArgumentNotValidException.class)
 ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException ex){String msg=ex.getBindingResult().getFieldErrors().stream().findFirst().map(e->e.getField()+": "+e.getDefaultMessage()).orElse("Некорректные параметры");return ResponseEntity.badRequest().body(new ErrorResponse("VALIDATION_ERROR",msg,System.currentTimeMillis()));}
}
