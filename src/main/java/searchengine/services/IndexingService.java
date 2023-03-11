package searchengine.services;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public interface IndexingService {
    HttpStatus startIndexing();
}
