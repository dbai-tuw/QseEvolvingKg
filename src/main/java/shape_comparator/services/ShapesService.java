package shape_comparator.services;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import shape_comparator.data.ExtractedShapes;
import shape_comparator.data.NodeShape;
import shape_comparator.data.ShapeRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ShapesService {
    private final ShapeRepository repository;
    @PersistenceContext
    private EntityManager entityManager;

    public ShapesService(ShapeRepository repository) {
        this.repository = repository;
    }

    public Optional<ExtractedShapes> get(Long id) {
        return repository.findById(id);
    }

    public void update(ExtractedShapes entity) {
        repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<ExtractedShapes> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public List<ExtractedShapes> listAll() {
        var all =  repository.findAll();
        Comparator<ExtractedShapes> shapeComparator = Comparator.comparing(ExtractedShapes::getGraphCreationTime)
                .thenComparing(ExtractedShapes::getVersionCreationTime)
                .thenComparing(ExtractedShapes::getCreatedAt);
        return all.stream().sorted(shapeComparator).collect(Collectors.toList());
    }

    public Page<ExtractedShapes> list(Pageable pageable, Specification<ExtractedShapes> filter) {
        return repository.findAll(filter, pageable);
    }

    public int count() {
        return (int) repository.count();
    }

    public void insert(ExtractedShapes extractedShapes) {
        ExtractedShapes s = repository.save(extractedShapes);
        repository.flush();
    }

    public List<ExtractedShapes> listByVersionId(Long versionId) {
        return repository.findAll().stream().filter(s -> s.getVersionObject().getId().equals(versionId)).collect(Collectors.toList());
    }

    @Transactional
    public ExtractedShapes getWithNodeShapesDefault(Long id, List<String> shapeNames) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ExtractedShapes> query = criteriaBuilder.createQuery(ExtractedShapes.class);
        Root<ExtractedShapes> root = query.from(ExtractedShapes.class);
        root.fetch("nodeShapesDefault");
        Join<ExtractedShapes, NodeShape> nodeShapesJoin = root.join("nodeShapesDefault");
        Predicate shapePredicate = nodeShapesJoin.get("iriLocalName").in(shapeNames);
        Predicate idPredicate = criteriaBuilder.equal(root.get("id"), id);
        Predicate finalPredicate = criteriaBuilder.and(idPredicate, shapePredicate);

        query.where(finalPredicate);
        query.select(root);

        return entityManager.createQuery(query).getSingleResult();
    }

    @Transactional
    public ExtractedShapes getWithNodeShapes(Long id) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ExtractedShapes> query = criteriaBuilder.createQuery(ExtractedShapes.class);
        Root<ExtractedShapes> root = query.from(ExtractedShapes.class);
        root.fetch("nodeShapes"); // Eagerly fetch associated nodeShapes
        query.where(criteriaBuilder.equal(root.get("id"), id));
        query.select(root);

        return entityManager.createQuery(query).getSingleResult();
    }
}
