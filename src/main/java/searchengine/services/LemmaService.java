package searchengine.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.List;
import java.util.Map;

@Service
@Transactional
public interface LemmaService {
    String[] arrayContainsRussianWords(String text);
    Map<String, Integer> lemmasAndCount(String text);
    boolean anyWordBaseBelongToParticle(List<String> wordBaseForms);
    boolean hasParticleProperty(String wordBase);
    String getSnippet(String content, String textQuery);
    String removeHtmlTags(String html);
    void saveLemma(PageEntity page, SiteEntity site);
}
