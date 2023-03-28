package searchengine.services;

import lombok.Getter;
import lombok.Setter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import searchengine.config.BatchSize;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.DoneOperation;
import searchengine.dto.indexing.ErrorOperation;
import searchengine.model.Lemma;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageEntityRepository;
import searchengine.repository.SiteEntityRepository;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
    private final BatchSize batchSize;
    private final ForkJoinPool forkJoinPool = new ForkJoinPool(5);
    private final Logger logger = Logger.getLogger(IndexingServiceImpl.SiteCrawler.class.getName());
    private final PageEntityRepository pageEntityRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final LemmaService lemmaService;
    private volatile boolean isIndexingStarted;
    private volatile boolean isIndexingStopped;


    public IndexingServiceImpl(SiteEntityRepository siteEntityRepository, SitesList sites,
                               PageEntityRepository pageEntityRepository, LemmaRepository lemmaRepository,
                               IndexRepository indexRepository, LemmaService lemmaService,
                               BatchSize batchSize) {
        this.siteEntityRepository = siteEntityRepository;
        this.sites = sites;
        this.pageEntityRepository = pageEntityRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.lemmaService = lemmaService;
        this.batchSize = batchSize;
    }

    @Override
    public Object startIndexing() {
        setIndexingStarted(true);
        setIndexingStopped(false);
        indexRepository.deleteAll();
        lemmaRepository.deleteAll();
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
        SiteEntity site = siteEntityRepository.getSiteEntityByUrl(siteConfig.getUrl());
        try {
            Connection.Response response = connection(url);


            if (!siteConfig.getUrl().equals(site.getUrl())) {
                return new ErrorOperation("Такого сайта в конфигурации нет");
            }

            synchronized (site) {
                site.setStatus(Status.INDEXING);
                siteEntityRepository.save(site);
            }

            Document document = response.parse();
            String pagePath = getRelativePathFromDocument(document);
            PageEntity page = pageEntityRepository.getPageEntityByPath(pagePath);
            if (page != null) {
                deletePage(page, site);
            }

            PageEntity pageEntity = new PageEntity(
                    site,
                    pagePath,
                    response.statusCode(),
                    document.html());
            pageEntityRepository.save(pageEntity);
            lemmaService.saveLemma(pageEntity, site);

            synchronized (site) {
                site.setStatus(Status.INDEXED);
                site.setStatusTime(LocalDateTime.now());
                siteEntityRepository.save(site);
            }

            return new DoneOperation("true");
        } catch (IOException e) {
            site.setStatus(Status.FAILED);
            site.setStatusTime(LocalDateTime.now());
            site.setLastError(e.getMessage());
            siteEntityRepository.save(site);
            setIndexingStarted(false);
            logger.log(Level.WARNING, "Ошибка при индексации страницы " + url, e);
            throw new RuntimeException(e);
        }
    }

    private void deletePage(PageEntity page, SiteEntity site) {
        Map<String, Integer> lemmasFromPage = lemmaService.lemmasAndCount(page.getContent());
        lemmasFromPage.forEach((lemmaName, count) -> {
            Lemma getLemmaFromDB = lemmaRepository.getLemmaByLemmaAndSiteId(lemmaName, site.getId());
            if (getLemmaFromDB != null) {
                getLemmaFromDB.setFrequency(getLemmaFromDB.getFrequency() - 1);
                lemmaRepository.save(getLemmaFromDB);
            }
        });
        pageEntityRepository.deletePageEntityById(page.getId());
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
                    SiteEntity siteEntity = siteEntityRepository.save(new SiteEntity(
                            Status.INDEXING,
                            LocalDateTime.now(),
                            site.getUrl(),
                            site.getName()));

                    crawlSite(site, siteEntity);
                    if (isIndexingStopped) {
                        SiteEntity siteForUpdate = siteEntityRepository.getSiteEntityByUrl(site.getUrl());
                        siteForUpdate.setStatus(Status.FAILED);
                        siteForUpdate.setStatusTime(LocalDateTime.now());
                        siteForUpdate.setLastError("Индексация остановлена пользователем");
                        siteEntityRepository.save(siteForUpdate);
                    } else {
                        SiteEntity siteForUpdate = siteEntityRepository.getSiteEntityByUrl(site.getUrl());
                        siteForUpdate.setStatus(Status.INDEXED);
                        siteForUpdate.setStatusTime(LocalDateTime.now());
                        siteEntityRepository.save(siteForUpdate);
                    }
                } catch (IOException e) {
                    SiteEntity siteEntity = siteEntityRepository.getSiteEntityByUrl(site.getUrl());
                    siteEntity.setStatus(Status.FAILED);
                    siteEntity.setStatusTime(LocalDateTime.now());
                    siteEntity.setLastError(e.getMessage());
                    siteEntityRepository.save(siteEntity);
                    setIndexingStarted(false);
                    throw new RuntimeException("ошибка в методе compute()" + e.getMessage());
                } finally {
                    if (TransactionSynchronizationManager.isActualTransactionActive() && !TransactionSynchronizationManager.isSynchronizationActive()) {
                        TransactionSynchronizationManager.clear();
                    }
                }
            }
        }

        @Transactional
        void crawlSite(Site site, SiteEntity siteEntity) throws MalformedURLException {
            Set<String> visitedUrls = new HashSet<>();
            Queue<String> urlsToCrawl = new LinkedList<>();
            urlsToCrawl.add(site.getUrl());
            List<PageEntity> pageEntities = new ArrayList<>();
            while (!urlsToCrawl.isEmpty()) {
                if (isIndexingStarted) {
                    String currentUrl = urlsToCrawl.poll();
                    Connection.Response response = connection(currentUrl);
                    Document doc;
                    try {
                        doc = response.parse();
                    } catch (IOException e) {
                        throw new RuntimeException(e.getMessage() + "Ошибка при парсинге");
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
                    if (pageEntityRepository.getFirstByPath(relativePath) == null) {
                        PageEntity page = new PageEntity(siteEntity, relativePath, response.statusCode(), doc.html());
                        pageEntities.add(page);
                        if (pageEntities.size() == batchSize.getBatchSize()) {
                            pageEntityRepository.saveAll(pageEntities);
                            for (PageEntity savedPage : pageEntities) {
                                lemmaService.saveLemma(savedPage, siteEntity);
                            }
                            pageEntities.clear();
                        }
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

