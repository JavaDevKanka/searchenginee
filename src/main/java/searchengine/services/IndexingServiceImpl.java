package searchengine.services;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.DoneOperation;
import searchengine.dto.indexing.ErrorOperation;
import searchengine.dto.indexing.IndexingInfo;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repository.PageEntityRepository;
import searchengine.repository.SiteEntityRepository;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@Getter
@Setter
public class IndexingServiceImpl implements IndexingService {
    private final SiteEntityRepository siteEntityRepository;
    private final SitesList sites;
    private final ForkJoinPool forkJoinPool = new ForkJoinPool(5);
    private final Logger logger = Logger.getLogger(IndexingServiceImpl.SiteCrawler.class.getName());
    private final PageEntityRepository pageEntityRepository;
    private volatile boolean isIndexingStarted;
    private volatile boolean isIndexingStopped;

    public IndexingServiceImpl(SiteEntityRepository siteEntityRepository, SitesList sites,
                               PageEntityRepository pageEntityRepository) {
        this.siteEntityRepository = siteEntityRepository;
        this.sites = sites;
        this.pageEntityRepository = pageEntityRepository;
    }

    @Override
    public Object startIndexing() {
        setIndexingStarted(true);
        setIndexingStopped(false);
        pageEntityRepository.deleteAll();
        siteEntityRepository.deleteAll();
        List<Site> sitesToIndex = sites.getSites();
        forkJoinPool.invoke(new SiteCrawler(sitesToIndex));
        return new DoneOperation("true");
    }

    @Override
    public Object stopIndexing() {
        setIndexingStopped(true);
        setIndexingStarted(false);
        return new DoneOperation("true");
    }

    @Override
    public Object indexPage(String url, Site siteConfig) {
        Connection.Response response = connection(url);
        SiteEntity site = siteEntityRepository.findSiteEntityByName(siteConfig.getName());
        if (site != null) {
            site.setStatus(Status.INDEXING);
            try {
                PageEntity page = pageEntityRepository.getPageEntityByPath(getRelativePathFromDocument(response.parse()));
                if (page != null) {
                    page.setPath(getRelativePathFromDocument(response.parse()));
                    page.setCode(response.statusCode());
                    page.setSiteId(site);
                    page.setContent(response.parse().html());
                    pageEntityRepository.save(page);
                }
                return new DoneOperation("true");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return new ErrorOperation("Ошибка");
    }

    @Override
    public boolean getIsIndexingStarted() {
        return this.isIndexingStarted;
    }

    @Override
    public boolean getIsIndexingStopped() {
        return this.isIndexingStopped;
    }


    class SiteCrawler extends RecursiveAction {
        private final List<Site> sites;

        public SiteCrawler(List<Site> sites) {
            this.sites = sites;
        }

        @Override
        @Transactional
        protected void compute() {
            if (sites.size() > 1) {
                int mid = sites.size() / 2;
                SiteCrawler left = new SiteCrawler(sites.subList(0, mid));
                SiteCrawler right = new SiteCrawler(sites.subList(mid, sites.size()));
                invokeAll(left, right);
            } else {
                Site site = sites.get(0);
                try {
                    SiteEntity siteEntity = new SiteEntity();
                    siteEntity.setUrl(site.getUrl());
                    siteEntity.setStatusTime(LocalDateTime.now());
                    siteEntity.setStatus(Status.INDEXING);
                    siteEntity.setName(site.getName());
                    siteEntityRepository.save(siteEntity);

                    crawlSite(site, siteEntity);
                    if (isIndexingStopped) {
                        siteEntity.setStatus(Status.FAILED);
                        siteEntity.setStatusTime(LocalDateTime.now());
                        siteEntity.setLastError("Индексация остановлена пользователем");
                        siteEntityRepository.save(siteEntity);
                    } else {
                        siteEntity.setStatus(Status.INDEXED);
                        siteEntity.setStatusTime(LocalDateTime.now());
                        siteEntityRepository.save(siteEntity);
                    }

                } catch (Exception e) {
                    SiteEntity siteEntity = siteEntityRepository.findByUrl(site.getUrl());
                    siteEntity.setStatus(Status.FAILED);
                    siteEntity.setStatusTime(LocalDateTime.now());
                    siteEntity.setLastError(e.getMessage());
                    siteEntityRepository.save(siteEntity);
                }
            }

        }

        @SneakyThrows
        @Transactional
        void crawlSite(Site site, SiteEntity siteEntity) {
            Set<String> visitedUrls = new HashSet<>();
            Queue<String> urlsToCrawl = new LinkedList<>();
            urlsToCrawl.add(site.getUrl());
            while (!urlsToCrawl.isEmpty()) {
                if (isIndexingStarted) {
                    String currentUrl = urlsToCrawl.poll();
                    Connection.Response response = connection(currentUrl);
                    Document doc;
                    try {
                        doc = response.parse();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    String relativePath = getRelativePathFromDocument(doc);
                    List<Element> elements = getElementsFromDocument(doc);
                    for (Element element : elements) {
                        String link = element.absUrl("href");
                        if (!visitedUrls.contains(link)) {
                            if (!link.isEmpty()) {
                                urlsToCrawl.add(link);
                            }
                            visitedUrls.add(link);
                        }
                    }
                    if (pageEntityRepository.countPageEntitiesByPath(relativePath) == 0) {
                        PageEntity page = new PageEntity();
                        page.setSiteId(siteEntity);
                        page.setPath(relativePath);
                        page.setCode(response.statusCode());
                        page.setContent(doc.html());
                        pageEntityRepository.save(page);
                    }
                } else {
                    break;
                }

            }
        }
    }

    private String getRelativePathFromDocument(Document document) throws MalformedURLException {
        return new File(new URL(document.location()).getPath()).getPath();
    }

    private Connection.Response connection(String urlToConnection) {
        try {
            return Jsoup.connect(urlToConnection)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) " +
                            "Chrome/67.0.3396.99 Safari/537.36")
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .referrer("https://www.google.com")
                    .execute();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Ошибка при подключении к  : " + e.getMessage());
            throw new RuntimeException(urlToConnection);
        }
    }

    private List<Element> getElementsFromDocument(Document document) {
        String baseUrl = document.baseUri();
        return document.select(String.format("a[href^=%s], a[href^=/]", baseUrl))
                .stream()
                .filter(link -> {
                    String absUrl = link.absUrl("href");
                    return absUrl.startsWith(baseUrl);
                })
                .toList();
    }
}

