package searchengine.services.abstracts;

import org.springframework.stereotype.Service;

@Service
public interface SearchService {
    String getSnippet(String content, String textQuery);
}
