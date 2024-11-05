package shape_comparator.data;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
public class Graph extends AbstractEntity{
    private String name;
    private LocalDateTime createdAt;

    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy = "graph")
    private List<Version> versions;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
