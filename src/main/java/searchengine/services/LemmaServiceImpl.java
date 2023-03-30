package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexPageLemma;
import searchengine.model.Lemma;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageEntityRepository;
import searchengine.repository.SiteEntityRepository;
import searchengine.services.abstracts.LemmaService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
public class LemmaServiceImpl implements LemmaService {
    private final SiteEntityRepository siteEntityRepository;
    private final PageEntityRepository pageEntityRepository;

    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ"};
    private final LuceneMorphology luceneMorphology;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    public LemmaServiceImpl(LuceneMorphology luceneMorphology, LemmaRepository lemmaRepository, IndexRepository indexRepository,
                            PageEntityRepository pageEntityRepository,
                            SiteEntityRepository siteEntityRepository) {
        this.luceneMorphology = luceneMorphology;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.pageEntityRepository = pageEntityRepository;
        this.siteEntityRepository = siteEntityRepository;
    }

    @Override
    @Transactional
    @Modifying
    public void saveLemma(PageEntity page, SiteEntity site) {
        Map<String, Integer> lemmasFromPage = lemmasAndCount(page.getContent());
        List<Lemma> lemmas = new ArrayList<>();
        List<IndexPageLemma> indexes = new ArrayList<>();
        lemmasFromPage.forEach((lemmaName, count) -> {
            IndexPageLemma indexPageLemma = new IndexPageLemma();
            Lemma getLemmaFromDB = lemmaRepository.getLemmaByLemmaAndSiteId(lemmaName, site.getId());
            if (getLemmaFromDB != null) {
                getLemmaFromDB.setFrequency(getLemmaFromDB.getFrequency() + 1);
                lemmas.add(getLemmaFromDB);
                indexPageLemma.setLemmaByIndex(getLemmaFromDB);
                indexes.add(indexPageLemma);
            } else {
                Lemma lemma = new Lemma();
                lemma.setFrequency(1);
                lemma.setLemma(lemmaName);
                lemma.setSiteByLemma(page.getSiteId());
                lemmas.add(lemma);
                indexPageLemma.setLemmaByIndex(lemma);
                indexes.add(indexPageLemma);
            }
            indexPageLemma.setPageByIndex(page);
            indexPageLemma.setRankLemma(count);
            indexes.add(indexPageLemma);
        });
        lemmaRepository.saveAll(lemmas);
        indexRepository.saveAll(indexes);
    }


    public String[] arrayContainsRussianWords(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ")
                .trim()
                .split("\\s+");
    }

    public Map<String, Integer> lemmasAndCount(String text) {
        String[] words = arrayContainsRussianWords(text);
        HashMap<String, Integer> lemmas = new HashMap<>();

        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }

            List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
            if (anyWordBaseBelongToParticle(wordBaseForms)) {
                continue;
            }

            List<String> normalForms = luceneMorphology.getNormalForms(word);
            if (normalForms.isEmpty()) {
                continue;
            }

            String normalWord = normalForms.get(0);

            if (lemmas.containsKey(normalWord)) {
                lemmas.put(normalWord, lemmas.get(normalWord) + 1);
            } else {
                lemmas.put(normalWord, 1);
            }
        }

        return lemmas;
    }


    public boolean anyWordBaseBelongToParticle(List<String> wordBaseForms) {
        return wordBaseForms.stream().anyMatch(this::hasParticleProperty);
    }

    public boolean hasParticleProperty(String wordBase) {
        for (String property : particlesNames) {
            if (wordBase.toUpperCase().contains(property)) {
                return true;
            }
        }
        return false;
    }
}
