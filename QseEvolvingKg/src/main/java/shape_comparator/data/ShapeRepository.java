package shape_comparator.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ShapeRepository
        extends
        JpaRepository<ExtractedShapes, Long>,
        JpaSpecificationExecutor<ExtractedShapes> {
}
