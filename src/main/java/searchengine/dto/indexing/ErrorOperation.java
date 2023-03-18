package searchengine.dto.indexing;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ErrorOperation {
    private final String result = "false";
    private String error;

    public ErrorOperation(String error) {
        this.error = error;
    }
}
