package searchengine.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.SiteEntity;

import java.util.List;


@Repository
@Transactional
public interface SiteEntityRepository extends JpaRepository<SiteEntity, Integer> {
    SiteEntity getSiteEntityByUrl(String url);
    SiteEntity findSiteEntityByName(String siteName);

    @Query(value = "select s from SiteEntity s")
    List<SiteEntity> getSiteEntities();

}