package shape_comparator.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import shape_comparator.data.Graph;
import shape_comparator.data.Version;
import shape_comparator.data.VersionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class VersionService {
    private final VersionRepository repository;

    public VersionService(VersionRepository repository) {
        this.repository = repository;
    }

    public Optional<Version> get(Long id) {
        return repository.findById(id);
    }

    public void update(Version entity) {
        repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<Version> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<Version> list(Pageable pageable, Specification<Version> filter) {
        return repository.findAll(filter, pageable);
    }

    public Page<Version> listByGraphId(Pageable pageable, Long graphId) {
        var filteredItems = listByGraphId(graphId);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredItems.size());

        List<Version> paginatedList = filteredItems.subList(start, end);

        return new PageImpl<>(paginatedList, pageable, filteredItems.size());
    }

    public List<Version> listByGraphId(Long graphId) {
       return repository.findAll().stream().filter(v -> v.getGraph().getId().equals(graphId)).collect(Collectors.toList());
    }

    public int count() {
        return (int) repository.count();
    }

    public Version generateNewVersion(Graph graph) {
        Version newVersion = new Version();
        newVersion.setGraph(graph);
        newVersion.setCreatedAt(LocalDateTime.now());
        newVersion.setVersionNumber(getLastVersionNumber(graph.getId()));
        return repository.save(newVersion);
    }

    private int getLastVersionNumber(Long graphId) {
        var allVersions = repository.findAll();
        var latestVersionNumber = allVersions.stream().filter(v -> v.getGraph().getId().equals(graphId)).mapToInt(Version::getVersionNumber)
                .max().orElse(0);
        return latestVersionNumber+1;
    }

    public List<Version> listAll() {
        return repository.findAll();
    }
}
