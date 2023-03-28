package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.tartarus.snowball.ext.RussianStemmer;
import searchengine.model.IndexPageLemma;
import searchengine.model.Lemma;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageEntityRepository;
import searchengine.repository.SiteEntityRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class LemmaServiceImpl implements LemmaService {
    private final SiteEntityRepository siteEntityRepository;
    private final PageEntityRepository pageEntityRepository;

    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ"};
    private final LuceneMorphology luceneMorphology;
    private static final String REGEXP_TEXT = "\\s*(\\s|\\?|\\||»|«|\\*|,|!|\\.)\\s*";
    private static final String REGEXP_WORD = "[а-яА-ЯёЁ]+";
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

    public String removeHtmlTags(String html) {
        Document doc = Jsoup.parse(html);
        return Jsoup.clean(doc.body().html(), Safelist.none());
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

    public String getSnippet(String content, String textQuery) {

        String textForSearchQuery = Jsoup.parse(content).getElementsContainingOwnText(textQuery).text();
        Pattern pattern = Pattern.compile(textQuery);
        Matcher matcher = pattern.matcher(textForSearchQuery);
        String snippet;
        if (matcher.find()) {
            int beginIndex = matcher.start() > 80 ?
                    textForSearchQuery.lastIndexOf(' ', matcher.start() - 60) : 0;
            int endIndex = Math.min(beginIndex + matcher.end() + 160, textForSearchQuery.length());
            snippet = textForSearchQuery.substring(beginIndex, endIndex);
            snippet = StringUtils.replace(snippet, textQuery, "<b>" + textQuery + "</b>");

        } else {
            textQuery = textQuery.trim();
            String[] words = textQuery.toLowerCase().split(REGEXP_TEXT);

            StringBuilder builderSnippet = new StringBuilder();
            RussianStemmer russianStemmer = new RussianStemmer();

            for (String word : words) {
                if (wordCheck(word)) {
                    russianStemmer.setCurrent(word);
                    if (russianStemmer.stem()) {
                        word = russianStemmer.getCurrent() + ".*?\\b";
                    }
                    String textForSearchWord = Jsoup.parse(content).getElementsMatchingOwnText(word).text();
                    pattern = Pattern.compile(word, Pattern.CASE_INSENSITIVE);
                    matcher = pattern.matcher(textForSearchWord);
                    if (matcher.find()) {
                        int beginIndex = matcher.start() > 35 ?
                                textForSearchWord.lastIndexOf(' ', matcher.start() - 15) : 0;
                        int endIndex = Math.min(matcher.start() + 80, textForSearchWord.length());
                        String result = matcher.group();
                        String snippetWord = textForSearchWord.substring(beginIndex, endIndex);
                        snippetWord = StringUtils.replace(snippetWord, result, "<b>" + result + "</b>");
                        builderSnippet.append(snippetWord).append("...");
                    }
                }
            }
            snippet = builderSnippet.toString();
        }
        return snippet;
    }

    public boolean wordCheck(String word) {

        if (word.matches(REGEXP_WORD)) {
            List<String> wordBaseForms =
                    luceneMorphology.getMorphInfo(word);
            return !wordBaseForms.get(0).endsWith("ПРЕДЛ") && (!wordBaseForms.get(0).endsWith("СОЮЗ")) &&
                    (!wordBaseForms.get(0).endsWith("ЧАСТ")) && (!wordBaseForms.get(0).endsWith("МЕЖД"));
        }
        return false;
    }
}
