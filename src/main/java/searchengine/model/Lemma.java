package searchengine.model;

import lombok.Data;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Data
@Table(name = "lemma")
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "lemma")
    private String lemma;

    private int frequency;

    @ManyToOne()
    @JoinColumn(name = "site_id", referencedColumnName = "id", nullable = false)
    private SiteEntity siteByLemma;

    @OneToMany(mappedBy = "lemmaByIndex", cascade = CascadeType.REMOVE)
    private List<IndexPageLemma> indexesLemma = new ArrayList<>();

    @Override
    public String toString() {
        return "Lemma{" +
                "lemma='" + lemma + '\'' +
                '}';
    }


}