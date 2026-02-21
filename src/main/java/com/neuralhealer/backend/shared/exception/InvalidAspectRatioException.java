ackage com.neuralhealer.backend.shared.exception.InvalidAspectRatioException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidAspectRatioException extends RuntimeException {
    public InvalidAspectRatioException(String message) {
        super(message);
    }
}
