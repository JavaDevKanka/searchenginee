package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.ErrorOperation;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SitesList sites;

    public ApiController(StatisticsService statisticsService, IndexingService indexingService, SitesList sites) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.sites = sites;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<Object> startIndexing() {
        if (!indexingService.getIsIndexingStarted()) {
            return ResponseEntity.ok(indexingService.startIndexing());
        }
        return ResponseEntity.badRequest().body(new ErrorOperation("Индексация уже запущена"));
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Object> stopIndexing() {
        if (indexingService.getIsIndexingStarted()) {
            return ResponseEntity.ok(indexingService.stopIndexing());
        }
        return ResponseEntity.badRequest().body(new ErrorOperation("Индексация не запущена"));
    }

    @PostMapping("/indexPage")
    public ResponseEntity<Object> indexPage(@RequestParam(name = "url") String url) {
        if (!indexingService.getIsIndexingStarted()) {
            for (Site site : sites.getSites()) {
                if (url.contains(site.getUrl())) {
                    return ResponseEntity.ok(indexingService.indexPage(url, site));
                }
            }
            return ResponseEntity.badRequest().body(new ErrorOperation("Указанная страница не найдена"));
        }
        return ResponseEntity.badRequest().body(new ErrorOperation("Индексация уже запущена"));
    }
}
