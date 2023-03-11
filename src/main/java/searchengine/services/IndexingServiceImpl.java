package searchengine.services;

import lombok.SneakyThrows;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repository.PageEntityRepository;
import searchengine.repository.SiteEntityRepository;

import javax.print.Doc;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Collectors;

@Service
public class IndexingServiceImpl implements IndexingService {
    private final SiteEntityRepository siteEntityRepository;
    private final SitesList sites;
    private final ForkJoinPool forkJoinPool = new ForkJoinPool(20);
    private final PageEntityRepository pageEntityRepository;

    public IndexingServiceImpl(SiteEntityRepository siteEntityRepository, SitesList sites,
                               PageEntityRepository pageEntityRepository) {
        this.siteEntityRepository = siteEntityRepository;
        this.sites = sites;
        this.pageEntityRepository = pageEntityRepository;
    }

    @Override
    public HttpStatus startIndexing() {
        List<Site> sitesToIndex = sites.getSites();
        if (sitesToIndex.isEmpty()) {
            return HttpStatus.NOT_FOUND;
        }
        forkJoinPool.invoke(new SiteCrawler(sitesToIndex));
        return HttpStatus.OK;
    }

    private class SiteCrawler extends RecursiveAction {
        private final List<Site> sites;

        public SiteCrawler(List<Site> sites) {
            this.sites = sites;
        }

        @Override
        protected void compute() {
            pageEntityRepository.deleteAll();
            siteEntityRepository.deleteAll();
            for (Site site : sites) {
                try {
                    // создание новой записи для сайта со статусом INDEXING
                    SiteEntity siteEntity = new SiteEntity();
                    siteEntity.setUrl(site.getUrl());
                    siteEntity.setStatusTime(LocalDateTime.now());
                    siteEntity.setStatus(Status.INDEXING);
                    siteEntity.setName(site.getName());
                    siteEntityRepository.save(siteEntity);

                    // обход сайта и добавление страниц в базу данных
                    crawlSite(site, siteEntity);

                    // обновление статуса сайта на INDEXED
                    siteEntity.setStatus(Status.INDEXED);
                    siteEntity.setStatusTime(LocalDateTime.now());
                    siteEntityRepository.save(siteEntity);
                } catch (Exception e) {
//                     обновление статуса сайта на FAILED и сохранение информации об ошибке
                    SiteEntity siteEntity = siteEntityRepository.findByUrl(site.getUrl());
                    siteEntity.setStatus(Status.FAILED);
                    siteEntity.setStatusTime(LocalDateTime.now());
                    siteEntity.setLastError(e.getMessage());
                    siteEntityRepository.save(siteEntity);
                }
            }
        }

        @SneakyThrows
        private void crawlSite(Site site, SiteEntity siteEntity) {
            Set<String> visitedUrls = new HashSet<>();
            Queue<String> urlsToCrawl = new LinkedList<>();
            urlsToCrawl.add(site.getUrl());

            while (!urlsToCrawl.isEmpty()) {
                String currentUrl = urlsToCrawl.poll();

                Connection.Response response = connection(currentUrl);
                Document doc = response.parse();
                String contentType = response.contentType();
                String relativePath = new File(new URL(doc.location()).getPath()).getPath();

                if (contentType != null && contentType.contains("text/html")) {
                    List<Element> elements = getElementsFromDocument(doc);
                    for (Element element : elements) {
                        String link = element.absUrl("href");
                        if (!visitedUrls.contains(link)) {
                            if (!link.isEmpty() & (link.startsWith(site.getUrl()))) {
                                urlsToCrawl.add(link);
                            }
                            visitedUrls.add(link);
                        }
                    }
                }
                PageEntity pageDuplicates = pageEntityRepository.findPageEntityByPath(relativePath);
                if (pageDuplicates == null) {
                    PageEntity page = new PageEntity();
                    page.setSiteId(siteEntity);
                    page.setPath(relativePath);
                    page.setCode(response.statusCode());
                    page.setContent(doc.html());
                    pageEntityRepository.save(page);
                }
            }
        }

        private Connection.Response connection(String urlToConnection) {
            try {
                return Jsoup.connect(urlToConnection)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                "Chrome/67.0.3396.99 Safari/537.36")
                        .ignoreHttpErrors(true)
                        .ignoreContentType(true)
                        .referrer("http://www.google.com")
                        .execute();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }



        private List<Element> getElementsFromDocument(Document document) {
            return document.select("a[href$=.html], a[href$=.htm], a[href]:not([href$=.])")
                    .stream()
                    .filter(link -> link.absUrl("href")
                            .startsWith("http://") || link.absUrl("href")
                            .startsWith("https://"))
                    .toList();
        }
    }
}

