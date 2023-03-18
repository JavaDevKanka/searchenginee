package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.PageEntity;

@Repository
@Transactional
public interface PageEntityRepository extends JpaRepository<PageEntity, Integer> {
    int countPageEntitiesByPath(String path);
    PageEntity getPageEntityByPath(String path);

}