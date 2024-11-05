package shape_comparator.data;

import cs.qse.common.structure.NS;
import jakarta.persistence.*;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import shape_comparator.services.Utils;
import sparqlshapechecker.SparqlShapeChecker;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Entity
public class ExtractedShapes {

    @Transient
    private static final Logger LOGGER = Logger.getLogger(SparqlShapeChecker.class.getName());

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "idgenerator")
    @SequenceGenerator(name = "idgenerator", initialValue = 1000)
    private Long extractedshapesId;


    @ManyToOne
    Version versionEntity;

    LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    QseType qseType;

    public int support;
    public double confidence;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ExtractedShapesClasses")
    List<String> classes;

    public String fileContentPath;

    String fileContentDefaultShapesPath;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    public List<NodeShape> nodeShapes;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    List<NodeShape> nodeShapesDefault;

    @Transient
    Model model;

    String comboBoxString;

    //random file generation
    @Transient
    Random random = new Random();

    //for regex
    @Transient
    String fileAsString;

    @Transient
    public String prefixLines;

    public LocalDateTime getGraphCreationTime() {
        return this.getVersionEntity().getGraph().getCreatedAt();
    }

    public LocalDateTime getVersionCreationTime() {
        return this.getVersionEntity().getCreatedAt();
    }

    //not used, would be used for alternative with rdf4j model and jena model
    public Model getModel() {
        if(model == null) {
            try(FileInputStream inputStream = new FileInputStream(fileContentPath)) {
                this.model = Rio.parse(inputStream, "", RDFFormat.TURTLE);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return this.model;
    }

    public String getFileAsString(boolean reload) {
        if(fileAsString == null || reload) {
            StringBuilder fileContent = new StringBuilder();
            StringBuilder prefixLines = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(fileContentPath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    fileContent.append(line).append("\n");
                    if(line.contains("@prefix"))
                        prefixLines.append(line).append("\n");
                }
            } catch (IOException e) {
                LOGGER.severe("Failed to read file: " + e.getMessage());
            }
            this.fileAsString = fileContent.toString().replace("\r", "").replace("\r\n", "\n");
            this.prefixLines = prefixLines.toString();
        }
        return fileAsString;
    }

    public List<NodeShape> getNodeShapes() {
        return nodeShapes;
    }

    public void setNodeShapes(List<NS> ns, boolean shouldGenerateText) {
        var list = new ArrayList<NodeShape>();
        for(var item : ns) {
            //Bug in QSE...
            var nsAlreadyExists = nsAlreadyExists(list, item);
            if(item.getSupport() > this.support && !nsAlreadyExists)
                list.add(new NodeShape(item, this, shouldGenerateText));
        }
        this.nodeShapes = list;
    }

    public void setNodeShapesWithPruning(List<NS> ns) {
        var list = new ArrayList<NodeShape>();
        for(var item : ns) {
            //Bug in QSE...
            var nsAlreadyExists = nsAlreadyExists(list, item);
            if(item.getSupport() > this.support && !nsAlreadyExists)
                list.add(new NodeShape(item, this));
        }
        this.nodeShapes = list;
    }

    public void setNodeShapesDefault(List<NS> ns) {
        var list = new ArrayList<NodeShape>();
        for(var item : ns) {
            var nsAlreadyExists = nsAlreadyExists(list, item);
            if(!nsAlreadyExists)
                list.add(new NodeShape(item, this, false));
        }
        this.nodeShapesDefault = list;
    }

    private Boolean nsAlreadyExists(ArrayList<NodeShape> list, NS item) {
        return list.stream().anyMatch(li -> li.iri.equals(item.getIri()));
    }

    public String getClassesAsString() {
        if (classes != null && !classes.isEmpty()) {
            var shortenedList = new ArrayList<>(classes);
            for (int i = 0; i < classes.size(); i++) {
                if(shortenedList.get(i).contains("#"))
                    shortenedList.set(i, shortenedList.get(i).split("#")[1]);
                else
                    shortenedList.set(i, shortenedList.get(i));
            }
            return shortenedList.stream().sorted().collect(Collectors.joining(", "));
        }
        return "";
    }

    public Version getVersionObject() {
        return versionEntity;
    }

    public void setVersionEntity(Version versionEntity) {
        this.versionEntity = versionEntity;
    }

    public Version getVersionEntity() {
        return versionEntity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public QseType getQseType() {
        return qseType;
    }

    public void setQseType(QseType qseType) {
        this.qseType = qseType;
    }

    public int getSupport() {
        return support;
    }

    public void setSupport(int support) {
        this.support = support;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public void setClasses(List<String> classes) {
        this.classes = classes;
    }

    public boolean isDefaultShape() {
        return confidence == 0 && support == 0;
    }

    public void generateComboBoxString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        this.comboBoxString = versionEntity.getGraph().getName() + "-" + versionEntity.getVersionNumber() + "-" +
                versionEntity.getName() + "-"
                + formatter.format(createdAt) + "-"
                + qseType + "-" + support + "-" + confidence;
    }

    public String getComboBoxString() {
        return comboBoxString;
    }

    public List<NodeShape> getNodeShapesDefault() {
        return nodeShapesDefault;
    }

    public String getFileContentPath() {
        return fileContentPath;
    }

    public void setFileContentPath(String fileContentPath) {
        if(!fileContentPath.contains(Utils.shapesPath))
            this.fileContentPath = saveFileContentPath(fileContentPath, ".ttl");
        else
            this.fileContentPath = fileContentPath;
    }

    private String saveFileContentPath(String path, String fileEnding) {
        try {
            checkIfShapesDirExists();
            //Save file instead of blob in database
            Path sourcePath = Paths.get(path);
            //Id is not known during creation, therefore random number
            String fileName = random.nextInt()+"_"+this.versionEntity.getGraph().getName()+"_"+this.versionEntity.getName()+fileEnding;
            Path destinationPath = Paths.get(Utils.getGraphDirectory()+File.separator+Utils.shapesPath+File.separator+fileName);
            Files.copy(sourcePath, destinationPath);
            return destinationPath.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public String getFileContentDefaultShapesPath() {
        return fileContentDefaultShapesPath;
    }

    private static void checkIfShapesDirExists() {
        Path path = Paths.get(Utils.getGraphDirectory()+File.separator+Utils.shapesPath);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setFileContentDefaultShapesPath(String fileContentDefaultShapesPath) {
        if(!fileContentDefaultShapesPath.contains(Utils.shapesPath))
            this.fileContentDefaultShapesPath = saveFileContentPath(fileContentDefaultShapesPath, "_default.ttl");
        else
            this.fileContentDefaultShapesPath = fileContentDefaultShapesPath;
    }

    public Long getId() {
        return extractedshapesId;
    }
}
