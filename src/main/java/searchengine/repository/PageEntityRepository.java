package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.PageEntity;

@Repository
@Transactional
public interface PageEntityRepository extends JpaRepository<PageEntity, Integer> {

    PageEntity getFirstByPath(String path);
    PageEntity getPageEntityByPath(String path);
    void deletePageEntityById(int pageId);
    @Query(value = "select count(p) from PageEntity p where p.siteId.id =:siteId")
    int countPagesbySiteId(@Param("siteId") int siteId);
}