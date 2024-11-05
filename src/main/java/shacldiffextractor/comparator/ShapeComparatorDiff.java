package shacldiffextractor.comparator;

import cs.Main;
import cs.qse.common.ExperimentsUtil;
import cs.qse.common.structure.NS;
import shacldiffextractor.ConfigVersionElement;
import shacldiffextractor.logic.DiffManager;
import shape_comparator.data.ExtractedShapes;
import shape_comparator.data.NodeShape;
import sparqlshapechecker.comparator.ComparatorUtils;
import sparqlshapechecker.comparator.ComparisonDiff;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

//handles the execution of the SHACL-DiffExtractor
public class ShapeComparatorDiff {
    public String filePath1;
    public String filePath2;
    public String dataSetName1;
    public String dataSetName2;
    String logFilePath;
    List<NS> firstNodeShapes;
    String shapePath1;
    public String outputPath;
    public String filePathAdded;
    public String filePathDeleted;
    ExtractedShapes extractedShapes2;

    public ShapeComparatorDiff(String filePath1, String filePath2, String filePathAdded, String filePathDeleted, String dataSetName1, String dataSetName2, String logFilePath) {
        this.filePathAdded = filePathAdded;
        this.filePathDeleted = filePathDeleted;
        this.filePath1 = filePath1;
        this.filePath2 = filePath2;
        this.dataSetName1 = dataSetName1;
        this.dataSetName2 = dataSetName2;
        this.logFilePath = logFilePath;
        this.outputPath = System.getProperty("user.dir")+ File.separator + "Output" + File.separator;
    }

    public void doFullComparisonForMultipleVersions(String threshold, List<ConfigVersionElement> versions) {
        ShapeComparatorQseFileBased.prepareQse(threshold);
        ComparisonDiff comparisonDiff = new ComparisonDiff();

        var diffManager = runQse1(comparisonDiff, threshold);
        ExtractedShapes firstExtractedShapes = new ExtractedShapes();
        firstExtractedShapes.setNodeShapes(firstNodeShapes,false);
        var nodeShapes = firstExtractedShapes.nodeShapes;
        Duration durationSecondStep;
        for (var versionElement : versions) {
            filePathAdded=versionElement.filePathAdded;
            filePathDeleted=versionElement.filePathDeleted;
            dataSetName2 = versionElement.versionName;

            doComparisonDiff(nodeShapes, shapePath1, comparisonDiff, diffManager, true);
            durationSecondStep = comparisonDiff.durationSecondStep;

            comparisonDiff = new ComparisonDiff();
            comparisonDiff.durationQse1 = durationSecondStep;
            shapePath1 = extractedShapes2.getFileContentPath();
            dataSetName1 = versionElement.versionName;
            nodeShapes = extractedShapes2.nodeShapes;
            Main.datasetName = versionElement.versionName;
        }
    }

    public ComparisonDiff doFullComparison(String threshold) {
        ComparisonDiff comparisonDiff = new ComparisonDiff();
        DiffManager diffManager = this.runQse1(comparisonDiff, threshold);
        this.doComparisonDiffWithOutExistingShapes(firstNodeShapes, shapePath1, comparisonDiff, diffManager);
        return comparisonDiff;
    }

    public ComparisonDiff doComparison(String threshold, ShapeComparatorQseFileBased shapeComparatorQseFileBased) {
        ShapeComparatorQseFileBased.prepareQse(threshold);
        ComparisonDiff comparisonDiff = new ComparisonDiff();
        comparisonDiff.durationQse1 = shapeComparatorQseFileBased.comparisonDiff.durationQse1;
        DiffManager diffManager = new DiffManager();
        diffManager.parser = shapeComparatorQseFileBased.parser;
        doComparisonDiffWithOutExistingShapes(shapeComparatorQseFileBased.firstNodeShapes, shapeComparatorQseFileBased.shapePath1, comparisonDiff, diffManager);

        return comparisonDiff;
    }

    private DiffManager runQse1(ComparisonDiff comparisonDiff, String threshold) {
        ShapeComparatorQseFileBased comparatorQseFileBased = new ShapeComparatorQseFileBased(filePath1, filePath2, dataSetName1, dataSetName2, logFilePath);
        var comparisonDiffFirst = comparatorQseFileBased.runQseFirstTime(threshold, Path.of("Output", "DiffExtractor"));

        comparisonDiff.durationQse1 = comparisonDiffFirst.durationQse1;
        firstNodeShapes = comparatorQseFileBased.firstNodeShapes;
        shapePath1 = comparatorQseFileBased.shapePath1;
        DiffManager diffManager = new DiffManager();
        diffManager.parser = comparatorQseFileBased.parser;
        return diffManager;
    }

    private void doComparisonDiffWithOutExistingShapes(List<NS> firstNodeShapes, String shapePath1, ComparisonDiff comparisonDiff, DiffManager diffManager) {
        ExtractedShapes extractedShapes = new ExtractedShapes();
        extractedShapes.setNodeShapesWithPruning(firstNodeShapes);
        doComparisonDiff(extractedShapes.getNodeShapes(), shapePath1, comparisonDiff, diffManager, false);
    }

    private void doComparisonDiff(List<NodeShape> firstNodeShapes, String shapePath1, ComparisonDiff comparisonDiff, DiffManager diffManager, boolean useExistingShapes) {
        Instant startShacl = Instant.now();
        HashMap<Double, List<Integer>> pruningThresholds = ExperimentsUtil.getSupportConfRange();
        var support = pruningThresholds.entrySet().iterator().next().getValue().get(0);
        var confidence = pruningThresholds.keySet().iterator().next();
        diffManager.support = support;
        diffManager.confidence = confidence;
        if(useExistingShapes)
            extractedShapes2 = diffManager.executeQseDiffForConsecutiveVersions(filePathAdded, filePathDeleted, dataSetName1, dataSetName2, shapePath1, firstNodeShapes);
        else
            extractedShapes2 = diffManager.executeQseDiff(filePathAdded, filePathDeleted, dataSetName1, dataSetName2);

        extractedShapes2.support = support;
        extractedShapes2.confidence = confidence;
        Instant endShacl = Instant.now();
        comparisonDiff.durationSecondStep = Duration.between(startShacl, endShacl);

        Instant startComparison = Instant.now();

        ExtractedShapes extractedShapes1 = new ExtractedShapes();
        extractedShapes1.fileContentPath = shapePath1;
        extractedShapes1.nodeShapes = firstNodeShapes;

        comparisonDiff.deletedNodeShapes = new ArrayList<>(diffManager.diffShapeGenerator.deletedNodeShapeNames.stream().distinct().toList());
        comparisonDiff.deletedPropertyShapes = new ArrayList<>(diffManager.diffShapeGenerator.deletedPropertyShapeNames.stream().distinct().toList());
        comparisonDiff.addedNodeShapes = new ArrayList<>(diffManager.diffShapeGenerator.addedNodeShapeNames.stream().distinct().toList());
        comparisonDiff.addedPropertyShapes = new ArrayList<>(diffManager.diffShapeGenerator.addedPropertyShapeNames.stream().distinct().toList());
        comparisonDiff.editedNodeShapes = new ArrayList<>(diffManager.diffShapeGenerator.editedNodeShapes.stream().distinct().toList());
        comparisonDiff.editedPropertyShapes = new ArrayList<>(diffManager.diffShapeGenerator.editPropertyShapes.stream().distinct().toList());

        extractedShapes1.getFileAsString(false);
        extractedShapes2.getFileAsString(true);

        adaptAddedShapeForQSEBugWithMultipleClassesForShape(comparisonDiff, extractedShapes1);

        Instant endComparison = Instant.now();
        comparisonDiff.durationComparison = Duration.between(startComparison, endComparison);

        comparisonDiff.durationTotal = comparisonDiff.durationQse1.plus(comparisonDiff.durationSecondStep).plus(comparisonDiff.durationComparison);
        ComparatorUtils.exportComparisonToFile(logFilePath+dataSetName1+"_"+dataSetName2+ File.separator+"Diff", comparisonDiff.toStringAll());
    }

    private void adaptAddedShapeForQSEBugWithMultipleClassesForShape(ComparisonDiff comparisonDiff, ExtractedShapes extractedShapes1) {
        comparisonDiff.addedNodeShapes.removeIf(ns -> extractedShapes1.getFileAsString(false).contains(ns));
        comparisonDiff.addedPropertyShapes.removeIf(ps -> extractedShapes1.getFileAsString(false).contains(ps));
    }
}
