package searchengine.services;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public interface Lemmas {
    String[] arrayContainsRussianWords(String text);
    Map<String, Integer> collectLemmas(String text);
    boolean anyWordBaseBelongToParticle(List<String> wordBaseForms);
    boolean hasParticleProperty(String wordBase);
    String getSnippet(String content, String textQuery);
}
