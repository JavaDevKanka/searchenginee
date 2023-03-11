package searchengine;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.List;

public class Tester {
    public static void main(String[] args) throws IOException {
        LuceneMorphology luceneMorphology = new RussianLuceneMorphology();
        List<String> wordBaseForms = luceneMorphology.getNormalForms("замокший");
        wordBaseForms.forEach(System.out::println);

    }
}
