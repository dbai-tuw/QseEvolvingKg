package sparqlshapechecker.comparator;

import cs.Main;
import cs.qse.common.structure.NS;
import cs.qse.querybased.nonsampling.QbParser;
import cs.utils.Constants;
import org.jetbrains.annotations.NotNull;
import shape_comparator.data.ExtractedShapes;
import sparqlshapechecker.utils.ConfigManager;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class ShapeComparatorQSEQueryBased {
    String graphDbUrl;
    public String dataSetName1;
    public String dataSetName2;
    public String logFilePath;
    public List<NS> firstNodeShapes;
    public List<NS> secondNodeShapes;
    public String shapePath1;
    public String shapePath2;
    public String outputPath;
    public ComparisonDiff comparisonDiff;

    public ShapeComparatorQSEQueryBased(String graphDbUrl, String dataSetName1, String dataSetName2, String logFilePath) {
        this.graphDbUrl = graphDbUrl;
        this.dataSetName1 = dataSetName1;
        this.dataSetName2 = dataSetName2;
        this.logFilePath = logFilePath;
        this.outputPath = System.getProperty("user.dir")+ File.separator + "Output" + File.separator;
    }

    public ComparisonDiff doComparisonSparql(String threshold) {
        prepareQse(threshold);

        //First Run
        comparisonDiff = runQseFirstTime(threshold);

        //Second Run
        return doSecondComparisonAndCompare(comparisonDiff);
    }

    public ComparisonDiff doComparisonForFollowingVersion(String threshold, ComparisonDiff comparisonDiff) {
        prepareQse(threshold);

        //Second Run
        return doSecondComparisonAndCompare(comparisonDiff);
    }

    @NotNull
    private ComparisonDiff doSecondComparisonAndCompare(ComparisonDiff comparisonDiff) {
        Main.datasetName = dataSetName2;
        Main.setOutputFilePathForJar(outputPath+dataSetName2+File.separator);

        Instant startQSE2 = Instant.now();
        QbParser qbParser = new QbParser(100, Constants.RDF_TYPE, graphDbUrl, dataSetName2);
        qbParser.run();
        Instant endQSE2 = Instant.now();
        Duration durationQSE2 = Duration.between(startQSE2, endQSE2);
        comparisonDiff.durationSecondStep = durationQSE2;

        secondNodeShapes = qbParser.shapesExtractor.getNodeShapes();
        shapePath2 = qbParser.shapesExtractor.getOutputFileAddress();

        Instant startComparison = Instant.now();
        ComparatorUtils.getDeletedNodeShapes(comparisonDiff, firstNodeShapes, secondNodeShapes);
        ComparatorUtils.getDeletedPropertyShapes(comparisonDiff, firstNodeShapes, secondNodeShapes);
        ExtractedShapes extractedShapes1 = new ExtractedShapes();
        ExtractedShapes extractedShapes2 = new ExtractedShapes();
        extractedShapes1.fileContentPath = shapePath1;
        extractedShapes2.fileContentPath = shapePath2;
        extractedShapes1.getFileAsString(false);
        extractedShapes2.getFileAsString(true);
        ComparatorUtils.getEditedNodeShapes(comparisonDiff, extractedShapes1, extractedShapes2, firstNodeShapes);
        ComparatorUtils.getEditedPropertyShapes(comparisonDiff, extractedShapes1, extractedShapes2, firstNodeShapes);
        ComparatorUtils.getAddedNodeShapes(comparisonDiff, firstNodeShapes, secondNodeShapes);
        ComparatorUtils.getAddedPropertyShapes(comparisonDiff, firstNodeShapes, secondNodeShapes);

        Instant endComparison = Instant.now();
        Duration durationComparison = Duration.between(startComparison, endComparison);
        comparisonDiff.durationComparison = durationComparison;

        comparisonDiff.durationTotal = comparisonDiff.durationQse1.plus(durationQSE2).plus(durationComparison);
        ComparatorUtils.exportComparisonToFile(logFilePath+dataSetName1+"_"+dataSetName2+ File.separator+"QSE", comparisonDiff.toStringAll());
        this.comparisonDiff = comparisonDiff;
        return comparisonDiff;
    }

    public ComparisonDiff runQseFirstTime(String threshold) {
        prepareQse(threshold);

        Main.datasetName = dataSetName1;
        Main.setOutputFilePathForJar(outputPath+dataSetName1+File.separator);
        ComparisonDiff comparisonDiff = new ComparisonDiff();

        Instant startQSE1 = Instant.now();
        QbParser qbParser = new QbParser(100, Constants.RDF_TYPE, graphDbUrl, dataSetName1);
        qbParser.run();
        Instant endQSE1 = Instant.now();
        Duration durationQSE1 = Duration.between(startQSE1, endQSE1);
        comparisonDiff.durationQse1 = durationQSE1;

        firstNodeShapes = qbParser.shapesExtractor.getNodeShapes();
        shapePath1 = qbParser.shapesExtractor.getOutputFileAddress();
        return comparisonDiff;
    }

    private static void prepareQse(String threshold) {
        Main.setResourcesPathForJar(ConfigManager.getRelativeResourcesPathFromQse());
        Main.annotateSupportConfidence = "true";
        Main.setPruningThresholds(threshold);
        File currentDir = new File(System.getProperty("user.dir"));
        File emptyConfig = new File(currentDir, "src/test/expected_test_results/emptyconfig.txt");
        Main.configPath = emptyConfig.getAbsolutePath(); //avoid exceptions in QSE
    }

}
