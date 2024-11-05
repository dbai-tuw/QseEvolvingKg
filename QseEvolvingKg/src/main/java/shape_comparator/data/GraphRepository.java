package shape_comparator.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GraphRepository
        extends
        JpaRepository<Graph, Long>,
        JpaSpecificationExecutor<Graph> {
}
