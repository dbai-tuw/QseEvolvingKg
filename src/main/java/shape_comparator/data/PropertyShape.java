package shape_comparator.data;

import cs.qse.common.structure.PS;
import jakarta.persistence.*;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import shape_comparator.services.Utils;

import java.util.Objects;


import java.util.ArrayList;
import java.util.List;

@Entity
public class PropertyShape {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long propertyShapeId;

    public IRI iri;
    String path;
    String nodeKind;
    String dataTypeOrClass;
    public Integer support;
    public Double confidence;

    @Transient
    public IRI nodeKindAsIri;

    @Transient
    public IRI pathAsIri;

    @Transient
    public IRI dataTypeAsIri;

    @Transient
    public IRI dataTypeOrClassAsIri;

    @Transient
    public IRI classIri;

    @Transient
    public List<ShaclOrListItem> orItems = new ArrayList<>();

    @Transient
    public boolean errorDuringGeneration = false;

    @Lob
    String generatedText;

    @ManyToOne
    @JoinColumn(name="nodeShapeId")
    NodeShape nodeShape;

    private PropertyShape(PS ps) {
        iri = ps.getIri();
        path = ps.getPath();
        nodeKind = ps.getNodeKind();
        dataTypeOrClass = ps.getDataTypeOrClass();
        support = ps.getSupport();
        confidence = ps.getConfidence();

        pathAsIri = Values.iri(ps.getPath());
        nodeKindAsIri = getNodeKindFromQSE(ps.getNodeKind());
        dataTypeOrClassAsIri = Utils.getIRI(ps.getDataTypeOrClass());

        //set shaclOrItems (copied from Shactor)
        //Also resets support and confidence to the maximum confidence if ShaclOrItems are used (copied from Shactor)
        if(ps.getShaclOrListItems() != null && !ps.getShaclOrListItems().isEmpty()){
            cs.qse.common.structure.ShaclOrListItem maxConfidenceItem = null;
            var shaclOrListItems = new ArrayList<ShaclOrListItem>();
            for (var item : ps.getShaclOrListItems()) {
                shaclOrListItems.add(new ShaclOrListItem(item.getNodeKind(),item.getDataTypeOrClass(), item.getSupport(), item.getConfidence(), this));
                if (maxConfidenceItem == null) {
                    maxConfidenceItem = item;
                }
                if (item.getConfidence() > maxConfidenceItem.getConfidence()) {
                    maxConfidenceItem = item;
                }
            }
            support = maxConfidenceItem.getSupport();
            confidence = maxConfidenceItem.getConfidence();
            this.orItems = shaclOrListItems;
        }
    }

    public PropertyShape(PS ps, NodeShape ns) {
        this(ps);
        this.nodeShape = ns;
        if(willPSbeAdded())
            this.generateText();
    }

    public Boolean willPSbeAdded() {
        //Bug in Shactor that all shapes are passed, no mather if support and confidence are correct
        return this.getSupport() > this.getNodeShape().getExtractedShapes().getSupport() && this.getConfidence()*100 > this.getNodeShape().getExtractedShapes().getConfidence();
    }

    public PropertyShape() {}

    public static IRI getNodeKindFromQSE(String nodeKind) {
        if(nodeKind != null && nodeKind.equals("Literal"))
            return Values.iri("http://www.w3.org/ns/shacl#Literal");
        if(nodeKind != null && nodeKind.equals("IRI"))
            return  Values.iri("http://www.w3.org/ns/shacl#IRI");
        return null;
    }

    public static int getValueFromQse(Integer value) {
        return Objects.requireNonNullElse(value, 0);
    }

    public static double getValueFromQse(Double value) {
        if(value == null)
            return 0;
        else
            return value;
    }

    public NodeShape getNodeShape() {
        return nodeShape;
    }

    public void setNodeShape(NodeShape nodeShape) {
        this.nodeShape = nodeShape;
    }

    public IRI getIri() {
        return iri;
    }

    public void setIri(IRI iri) {
        this.iri = iri;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getNodeKind() {
        return nodeKind;
    }

    public void setNodeKind(String nodeKind) {
        this.nodeKind = nodeKind;
    }

    public String getDataTypeOrClass() {
        return dataTypeOrClass;
    }

    public void setDataTypeOrClass(String dataTypeOrClass) {
        this.dataTypeOrClass = dataTypeOrClass;
    }

    public Integer getSupport() {
        return support;
    }

    public void setSupport(Integer support) {
        this.support = support;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getGeneratedText() {
        return generatedText;
    }

    public void generateText() {
        if(this.nodeShape.shouldGenerateText) {
            this.generatedText = Utils.generateTTLFromRegex(iri, this.nodeShape.extractedShapes.getFileAsString(false), this.nodeShape.extractedShapes.prefixLines);
        }
    }
}
