package com.studyapp.backend.api;

import com.studyapp.backend.service.AuthenticationService;
import com.studyapp.backend.service.ResourceNotFoundException;
import com.studyapp.backend.service.SyncService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleNotFound(ResourceNotFoundException exception) {
        return this.problem(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(AuthenticationService.InvalidCredentialsException.class)
    ProblemDetail handleInvalidCredentials(
            AuthenticationService.InvalidCredentialsException exception) {
        return this.problem(HttpStatus.UNAUTHORIZED, "Email or password is incorrect.");
    }

    @ExceptionHandler(AuthenticationService.EmailNotVerifiedException.class)
    ProblemDetail handleUnverifiedEmail(AuthenticationService.EmailNotVerifiedException exception) {
        return this.problem(HttpStatus.FORBIDDEN, "Verify your email before signing in.");
    }

    @ExceptionHandler(AuthenticationService.InvalidRefreshTokenException.class)
    ProblemDetail handleInvalidRefreshToken(
            AuthenticationService.InvalidRefreshTokenException exception) {
        return this.problem(HttpStatus.UNAUTHORIZED, "Refresh token is invalid or expired.");
    }

    @ExceptionHandler(AuthenticationService.InvalidActionCodeException.class)
    ProblemDetail handleInvalidActionCode(
            AuthenticationService.InvalidActionCodeException exception) {
        return this.problem(HttpStatus.BAD_REQUEST, "Code is invalid or expired.");
    }

    @ExceptionHandler(SyncService.SyncConflictException.class)
    ProblemDetail handleSyncConflict(SyncService.SyncConflictException exception) {
        ProblemDetail problem = this.problem(HttpStatus.CONFLICT, exception.getMessage());
        problem.setProperty("entityType", exception.entityType());
        problem.setProperty("entityId", exception.entityId());
        problem.setProperty("submittedVersion", exception.submittedVersion());
        problem.setProperty("currentVersion", exception.currentVersion());
        problem.setProperty("serverOperation", exception.serverOperation());
        problem.setProperty("serverPayload", exception.serverPayload());
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleInvalidRequest(IllegalArgumentException exception) {
        return this.problem(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception) {

        String detail = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        return this.problem(HttpStatus.BAD_REQUEST, detail);
    }

    private ProblemDetail problem(HttpStatus status, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("about:blank"));
        return problem;
    }
}
