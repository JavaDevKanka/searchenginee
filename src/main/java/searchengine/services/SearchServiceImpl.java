package searchengine.services;

import org.apache.lucene.morphology.LuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.tartarus.snowball.ext.RussianStemmer;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SearchServiceImpl {

    private final LuceneMorphology luceneMorphology;
    private static final String REGEXP_TEXT = "\\s*(\\s|\\?|\\||»|«|\\*|,|!|\\.)\\s*";
    private static final String REGEXP_WORD = "[а-яА-ЯёЁ]+";

    public SearchServiceImpl(LuceneMorphology luceneMorphology) {
        this.luceneMorphology = luceneMorphology;
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

    public String removeHtmlTags(String html) {
        Document doc = Jsoup.parse(html);
        return Jsoup.clean(doc.body().html(), Safelist.none());
    }
}
