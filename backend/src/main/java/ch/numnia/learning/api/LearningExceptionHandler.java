package ch.numnia.learning.api;

import ch.numnia.iam.api.dto.ErrorResponse;
import ch.numnia.learning.service.TaskPoolNotConfiguredException;
import ch.numnia.learning.service.TrainingSessionNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "ch.numnia.learning.api")
public class LearningExceptionHandler {

    @ExceptionHandler(TaskPoolNotConfiguredException.class)
    public ResponseEntity<ErrorResponse> handlePool(TaskPoolNotConfiguredException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse("TASK_POOL_NOT_CONFIGURED", ex.getMessage()));
    }

    @ExceptionHandler(TrainingSessionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSession(TrainingSessionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("SESSION_NOT_FOUND", ex.getMessage()));
    }
}
