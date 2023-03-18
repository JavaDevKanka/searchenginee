package searchengine;

import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import searchengine.repository.PageEntityRepository;
import searchengine.services.Lemmas;
import searchengine.services.LemmasImpl;

import java.io.IOException;
public class Tester {



    public static void main(String[] args) throws IOException {
        PageEntityRepository pageEntityRepository;
        String text = "Повторное появление леопарда в Осетии позволяет предположить, что леопард постоянно обитает в некоторых районах Северного Кавказа.";
        Lemmas lemmas = new LemmasImpl(new RussianLuceneMorphology());
//        System.out.println(lemmas.getSnippet());
    }



}
