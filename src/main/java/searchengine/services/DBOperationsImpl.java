package searchengine.services;

import org.springframework.stereotype.Service;
import searchengine.model.SiteEntity;
import searchengine.repository.PageEntityRepository;
import searchengine.repository.SiteEntityRepository;

@Service
public class DBOperationsImpl {
    public final PageEntityRepository pageEntityRepository;
    public final SiteEntityRepository siteEntityRepository;


    public DBOperationsImpl(PageEntityRepository pageEntityRepository, SiteEntityRepository siteEntityRepository) {
        this.pageEntityRepository = pageEntityRepository;
        this.siteEntityRepository = siteEntityRepository;
    }


}
