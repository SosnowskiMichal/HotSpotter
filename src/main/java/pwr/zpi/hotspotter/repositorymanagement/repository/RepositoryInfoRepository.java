package pwr.zpi.hotspotter.repositorymanagement.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pwr.zpi.hotspotter.repositorymanagement.model.RepositoryInfo;

import java.util.List;
import java.util.Optional;

@Repository
public interface RepositoryInfoRepository extends MongoRepository<RepositoryInfo, String> {

    Optional<RepositoryInfo> findByNameAndOwnerAndPlatform(String name, String owner, String platform);

    List<RepositoryInfo> findAllByOrderByLastAccessedAtAsc();

    List<RepositoryInfo> findAllByOrderByAccessCountAsc();

}
