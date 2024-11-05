package sparqlshapechecker.comparator;

import cs.qse.common.structure.NS;
import shape_comparator.data.ExtractedShapes;
import sparqlshapechecker.SparqlShapeChecker;
import sparqlshapechecker.utils.RegexUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ComparatorUtils {
    private static final Logger LOGGER = Logger.getLogger(SparqlShapeChecker.class.getName());

    public static void getEditedPropertyShapes(ComparisonDiff comparisonDiff, ExtractedShapes extractedShapes1, ExtractedShapes extractedShapes2, List<NS> firstNodeShapes) {
        var propertyShapesToCheck = firstNodeShapes.stream().flatMap(ns -> ns.getPropertyShapes().stream().map(ps -> ps.getIri().toString()))
                .filter(ps -> !comparisonDiff.deletedPropertyShapes.contains(ps)).collect(Collectors.toSet());
        var editedShapes = generateEditedShapesObjects(propertyShapesToCheck, extractedShapes1, extractedShapes2);
        comparisonDiff.editedPropertyShapes = editedShapes;
    }

    public static void getEditedNodeShapes(ComparisonDiff comparisonDiff, ExtractedShapes extractedShapes1, ExtractedShapes extractedShapes2, List<NS> firstNodeShapes) {
        var nodeShapesToCheck = firstNodeShapes.stream().filter(ns -> !comparisonDiff.deletedNodeShapes.contains(ns.getIri().toString())).map(ns -> ns.getIri().toString()).collect(Collectors.toSet());
        var editedShapes = generateEditedShapesObjects(nodeShapesToCheck, extractedShapes1, extractedShapes2);
        comparisonDiff.editedNodeShapes = editedShapes;
    }

    public static ArrayList<EditedShapesComparisonObject> generateEditedShapesObjects(Set<String> shapesToCheck, ExtractedShapes extractedShapes1, ExtractedShapes extractedShapes2) {
        var editedShapesComparisonObjects = new ArrayList<EditedShapesComparisonObject>();
        for(var shape : shapesToCheck) {
            EditedShapesComparisonObject editedShapesComparisonObject = new EditedShapesComparisonObject();
            editedShapesComparisonObject.shapeName = shape;
            var shapeString1 = RegexUtils.getShapeAsStringFormattedFromFile(shape, extractedShapes1.getFileAsString(false), extractedShapes1.prefixLines);
            var shapeString2 = RegexUtils.getShapeAsStringFormattedFromFile(shape, extractedShapes2.getFileAsString(false), extractedShapes2.prefixLines);
            if(!shapeString1.equals(shapeString2)) {
                editedShapesComparisonObject.shapeAsTextNew = shapeString2;
                editedShapesComparisonObject.shapeAsTextOld = shapeString1;
                editedShapesComparisonObjects.add(editedShapesComparisonObject);
            }
        }
        return editedShapesComparisonObjects;
    }

    public static void exportComparisonToFile(String filePath, String content) {
        try {
            String fileName = filePath+"Comparison.txt";
            File outputFile = new File(fileName);
            File parentDir = outputFile.getParentFile();
            if (!parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    return;
                }
            }
            FileWriter writer = new FileWriter(fileName, false);
            writer.write(content);
            writer.close();
            LOGGER.info("Saved file " + fileName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void getDeletedNodeShapes(ComparisonDiff comparisonDiff, List<NS> firstNodeShapes, List<NS> secondNodeShapes) {
        var firstShapesCopied = new ArrayList<>(firstNodeShapes.stream().map(ns -> ns.getIri().toString()).distinct().toList());
        var secondShapesCopied = secondNodeShapes.stream().map(ns -> ns.getIri().toString()).distinct().toList();
        firstShapesCopied.removeAll(secondShapesCopied);
        comparisonDiff.deletedNodeShapes = firstShapesCopied;
    }

    public static void getDeletedPropertyShapes(ComparisonDiff comparisonDiff, List<NS> firstNodeShapes, List<NS> secondNodeShapes) {
        var propertyShapes1 = new ArrayList<>(firstNodeShapes.stream().flatMap(ns -> ns.getPropertyShapes().stream().map(ps -> ps.getIri().toString())).distinct().toList());
        var propertyShapes2 = secondNodeShapes.stream().flatMap(ns -> ns.getPropertyShapes().stream().map(ps -> ps.getIri().toString())).distinct().toList();
        propertyShapes1.removeAll(propertyShapes2);
        comparisonDiff.deletedPropertyShapes = propertyShapes1;
    }

    public static void getAddedNodeShapes(ComparisonDiff comparisonDiff, List<NS> firstNodeShapes, List<NS> secondNodeShapes) {
        var secondShapesCopied = new ArrayList<>(secondNodeShapes.stream().map(ns -> ns.getIri().toString()).distinct().toList());
        var firstShapesCopied = firstNodeShapes.stream().map(ns -> ns.getIri().toString()).distinct().toList();
        secondShapesCopied.removeAll(firstShapesCopied);
        comparisonDiff.addedNodeShapes = secondShapesCopied;
    }

    public static void getAddedPropertyShapes(ComparisonDiff comparisonDiff, List<NS> firstNodeShapes, List<NS> secondNodeShapes) {
        var propertyShapes2 = new ArrayList<>(secondNodeShapes.stream().flatMap(ns -> ns.getPropertyShapes().stream().map(ps -> ps.getIri().toString())).distinct().toList());
        var propertyShapes1 = firstNodeShapes.stream().flatMap(ns -> ns.getPropertyShapes().stream().map(ps -> ps.getIri().toString())).distinct().toList();
        propertyShapes2.removeAll(propertyShapes1);
        comparisonDiff.addedPropertyShapes = propertyShapes2;
    }
}
