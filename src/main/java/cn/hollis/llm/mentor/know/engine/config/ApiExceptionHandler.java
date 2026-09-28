package cn.hollis.llm.mentor.know.engine.config;

import cn.dev33.satoken.exception.NotLoginException;
import cn.hollis.llm.mentor.know.engine.common.R;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<R<Void>> statusError(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(R.fail(error.getStatusCode().value(), error.getReason()));
    }

    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<R<Void>> notLoggedIn() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(R.fail(401, "登录已过期，请重新登录"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<R<Void>> invalidInput(IllegalArgumentException error) {
        return ResponseEntity.badRequest().body(R.fail(400, error.getMessage()));
    }
}
