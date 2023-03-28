package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexPageLemma;
import searchengine.model.Lemma;
import searchengine.model.PageEntity;

import java.util.List;

@Repository
@Transactional
public interface IndexRepository extends JpaRepository<IndexPageLemma, Integer> {
    void deleteIndexByPageByIndex(PageEntity page);
    int getIndexByLemmaByIndex(Lemma lemma);

    @Query(value = "select i.lemmaByIndex.id from IndexPageLemma i where i.pageByIndex.id =:pageId")
    List<Integer> getIndexesByPageByIndex(@Param("pageId") int pageId);
}
