package shape_comparator.services;

import shape_comparator.data.NodeShape;
import shape_comparator.data.PropertyShape;

import java.util.HashMap;

//represents one line in the tree view. This can be either be a line of 2+ node shapes, or of 2+ property shapes
//node or property shapes have the same shapename
public class ComparisonTreeViewItem {

    public ComparisonTreeViewItem() {
    }

    String shapeName;
    //Long represents the id of the Extracted shapes
    HashMap<Long,NodeShape> nodeShapeList = new HashMap<>();
    HashMap<Long,PropertyShape> propertyShapeList = new HashMap<>();
    Boolean shapesEqual = null;
    ComparisonTreeViewItem parentShape = null;

    public void addNodeShape(NodeShape ns, Long extractedShapesId) {
        if(nodeShapeList.size() == 0)
            shapeName = ns.getIri().getLocalName();
        nodeShapeList.put(extractedShapesId, ns);
    }

    public void addPropertyShape(PropertyShape ps, Long extractedShapesId) {
        if(nodeShapeList.size() == 0)
            shapeName = ps.getIri().getLocalName();
        propertyShapeList.put(extractedShapesId, ps);
    }

    public boolean usesDefaultShapes() {
        if(isNodeShapeLine())
            return nodeShapeList.values().stream().anyMatch(ns -> ns.getExtractedShapes().isDefaultShape());
        else
            return propertyShapeList.values().stream().anyMatch(ps -> ps.getNodeShape().getExtractedShapes().isDefaultShape());
    }

    public int getSupportThreshold() {
        if(isNodeShapeLine())
            return nodeShapeList.values().stream().mapToInt(ns -> ns.getExtractedShapes().getSupport()).max().orElse(0);
        else
            return propertyShapeList.values().stream().mapToInt(ps -> ps.getNodeShape().getExtractedShapes().getSupport()).max().orElse(0);
    }

    public int getConfidenceThreshold() {
        if(isNodeShapeLine())
            return 0;
        else
            return (int)Math.round(propertyShapeList.values().stream().mapToDouble(ps -> ps.getNodeShape().getExtractedShapes().getConfidence()).max().orElse(0));
    }

    public boolean isNodeShapeLine() {
        return nodeShapeList.size() > 0;
    }

    public HashMap<Long, NodeShape> getNodeShapeList() {
        return nodeShapeList;
    }

    public HashMap<Long, PropertyShape> getPropertyShapeList() {
        return propertyShapeList;
    }

    public String getShapeName() {
        return shapeName;
    }

    public Boolean areShapesEqual() {
        return shapesEqual;
    }

    public void setShapesEqual(Boolean shapesEqual) {
        this.shapesEqual = shapesEqual;
    }

    public ComparisonTreeViewItem getParentShape() {
        return parentShape;
    }

    public void setParentShape(ComparisonTreeViewItem parentShape) {
        this.parentShape = parentShape;
    }
}
