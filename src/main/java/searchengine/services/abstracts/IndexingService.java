package searchengine.services.abstracts;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.dto.indexing.IndexingInfo;
import searchengine.model.SiteEntity;

import javax.persistence.Index;

@Service
public interface IndexingService {
    Object startIndexing();
    Object stopIndexing();
    Object indexPage(String address, Site site);
    boolean getIsIndexingStarted();
    boolean getIsIndexingStopped();
}
