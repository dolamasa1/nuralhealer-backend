ackage com.neuralhealer.backend.shared.exception.EngagementNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class EngagementNotFoundException extends RuntimeException {
    public EngagementNotFoundException(String message) {
        super(message);
    }
}
