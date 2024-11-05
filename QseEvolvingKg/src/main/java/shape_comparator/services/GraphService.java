package shape_comparator.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import shape_comparator.data.Graph;
import shape_comparator.data.GraphRepository;

import java.util.List;
import java.util.Optional;

@Service
public class GraphService {
    private final GraphRepository repository;


    public GraphService(GraphRepository repository) {
        this.repository = repository;
    }

    public Optional<Graph> get(Long id) {
        return repository.findById(id);
    }

    public void update(Graph entity) {
        repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
        repository.flush();
    }

    public Page<Graph> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public List<Graph> listAll() {
        return repository.findAll();
    }

    public Page<Graph> list(Pageable pageable, Specification<Graph> filter) {
        return repository.findAll(filter, pageable);
    }

    public int count() {
        return (int) repository.count();
    }

    public Graph insert(Graph graph) {
        Graph g = repository.save(graph);
        repository.flush();
        return this.get(g.getId()).get();
    }

}
