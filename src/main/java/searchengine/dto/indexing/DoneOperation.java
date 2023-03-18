package searchengine.dto.indexing;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DoneOperation {
    private String result;

    public DoneOperation(String result) {
        this.result = result;
    }
}
