package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;

import java.util.List;

@Repository
@Transactional
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    @Query(value = "SELECT l from Lemma l where l.lemma =:lemma and l.siteByLemma.id =:siteId")
    Lemma getLemmaByLemmaAndSiteId(@Param("lemma") String lemma, @Param("siteId") int siteId);

    @Modifying
    @Query(value = "DELETE FROM Lemma l WHERE l.id IN :ids")
    void deleteLemmasByIds(@Param("ids") List<Integer> ids);

    @Query(value = "select count(l) from Lemma l where l.siteByLemma.id =:siteId")
    int countLemmasBySiteId(@Param("siteId") int siteId);

}
