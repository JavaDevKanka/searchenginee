package searchengine;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import searchengine.config.Site;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageEntityRepository;
import searchengine.repository.SiteEntityRepository;
import searchengine.services.abstracts.IndexingService;
import searchengine.services.abstracts.LemmaService;
import searchengine.services.abstracts.SearchService;

import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest
@RunWith(SpringRunner.class)
public class TestMethods {
    @Autowired
    private SiteEntityRepository siteEntityRepository;
    @Autowired
    private PageEntityRepository pageEntityRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexRepository indexRepository;
    @Autowired
    private LemmaService lemmaService;
    @Autowired
    private IndexingService indexingService;
    @Autowired
    private SearchService searchService;


    @Test
    public void cleanDB() {
        indexRepository.deleteAll();
        lemmaRepository.deleteAll();
        pageEntityRepository.deleteAll();
        siteEntityRepository.deleteAll();
    }

    @Test
    public void createSite() {
        SiteEntity site = new SiteEntity();
        site.setUrl("https://www.svetlovka.ru/");
        site.setStatus(Status.INDEXING);
        site.setName("svetlovka");
        site.setStatusTime(LocalDateTime.now());

        siteEntityRepository.save(site);
    }

    @Test
    public void testLemma() {

        Site sitee = new Site();
        sitee.setName("bulgakov");
        sitee.setUrl("https://dombulgakova.ru/");

        System.out.println(indexingService.indexPage("https://dombulgakova.ru/o-teatre/", sitee));

    }

    @Test
    public void deleteLemmasFromDB() {
        List<Integer> longs = indexRepository.getIndexesByPageByIndex(2);
        lemmaRepository.deleteLemmasByIds(longs);

    }

    @Test
    public void getSnippet() {
//        searchService.getSnippet()
    }

}
