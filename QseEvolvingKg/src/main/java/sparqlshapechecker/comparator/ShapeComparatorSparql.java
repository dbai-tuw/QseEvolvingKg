package sparqlshapechecker.comparator;

import cs.Main;
import cs.qse.common.ExperimentsUtil;
import cs.qse.common.structure.NS;
import cs.qse.querybased.nonsampling.QbParser;
import cs.utils.Constants;
import org.jetbrains.annotations.NotNull;
import shape_comparator.data.ExtractedShapes;
import sparqlshapechecker.utils.ConfigManager;
import sparqlshapechecker.utils.GraphDbUtils;
import sparqlshapechecker.utils.RegexUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ShapeComparatorSparql {
    String graphDbUrl;
    String dataSetName1;
    public String dataSetName2;
    String logFilePath;
    List<NS> firstNodeShapes;
    String shapePath1;
    public String outputPath;

    public void doFullComparisonForMultipleVersions(String threshold, String[] dataSetsToCheck) {
        ComparisonDiff comparisonDiff = prepareQSE(threshold);

        //First Run
        runQse1(comparisonDiff);
        var durationQse1 = comparisonDiff.durationQse1;
        for (var dataSet : dataSetsToCheck) {
            this.dataSetName2 = dataSet;
            doComparisonSparql(firstNodeShapes, shapePath1, comparisonDiff);

            comparisonDiff = new ComparisonDiff();
            comparisonDiff.durationQse1 = durationQse1;
        }
    }

    public ComparisonDiff doFullComparison(String threshold) {
        ComparisonDiff comparisonDiff = prepareQSE(threshold);

        //First Run
        runQse1(comparisonDiff);

        //Check shapes with SPARQL
        doComparisonSparql(firstNodeShapes, shapePath1, comparisonDiff);

        return comparisonDiff;
    }

    public ComparisonDiff doComparison(String threshold, ShapeComparatorQSEQueryBased shapeComparatorQSE) {
        ComparisonDiff comparisonDiff = prepareQSE(threshold);
        comparisonDiff.durationQse1 = shapeComparatorQSE.comparisonDiff.durationQse1;

        //Check shapes with SPARQL
        doComparisonSparql(shapeComparatorQSE.firstNodeShapes, shapeComparatorQSE.shapePath1, comparisonDiff);

        return comparisonDiff;
    }

    public ShapeComparatorSparql(String graphDbUrl, String dataSetName1, String dataSetName2, String logFilePath) {
        this.graphDbUrl = graphDbUrl;
        this.dataSetName1 = dataSetName1;
        this.dataSetName2 = dataSetName2;
        this.logFilePath = logFilePath;
        this.outputPath = System.getProperty("user.dir")+ File.separator + "Output" + File.separator;
    }

    private void runQse1(ComparisonDiff comparisonDiff) {
        Main.datasetName = dataSetName1;
        Main.setOutputFilePathForJar(outputPath+dataSetName1+File.separator);

        Instant startQSE1 = Instant.now();
        QbParser qbParser = new QbParser(100, Constants.RDF_TYPE, graphDbUrl, dataSetName1);
        qbParser.run();
        Instant endQSE1 = Instant.now();
        comparisonDiff.durationQse1 = Duration.between(startQSE1, endQSE1);

        firstNodeShapes = qbParser.shapesExtractor.getNodeShapes();
        shapePath1 = qbParser.shapesExtractor.getOutputFileAddress();
    }

    @NotNull
    private static ComparisonDiff prepareQSE(String threshold) {
        Main.setResourcesPathForJar(ConfigManager.getRelativeResourcesPathFromQse());
        Main.annotateSupportConfidence = "true";
        Main.setPruningThresholds(threshold);
        File currentDir = new File(System.getProperty("user.dir"));
        File emptyConfig = new File(currentDir, "src/test/expected_test_results/emptyconfig.txt");
        Main.configPath = emptyConfig.getAbsolutePath(); //avoid exceptions in QSE
        return new ComparisonDiff();
    }

    private void doComparisonSparql(List<NS> firstNodeShapes, String shapePath1, ComparisonDiff comparisonDiff) {
        Instant startSparql = Instant.now();
        ExtractedShapes extractedShapes1 = new ExtractedShapes();
        extractedShapes1.setNodeShapes(firstNodeShapes,false);
        ExtractedShapes extractedShapes2 = new ExtractedShapes();
        extractedShapes2.setNodeShapes(firstNodeShapes,false);
        HashMap<Double, List<Integer>> pruningThresholds = ExperimentsUtil.getSupportConfRange();
        extractedShapes2.support = pruningThresholds.entrySet().iterator().next().getValue().get(0);
        extractedShapes2.confidence = pruningThresholds.keySet().iterator().next();

        extractedShapes1.fileContentPath = shapePath1;

        GraphDbUtils.checkShapesInNewGraph(graphDbUrl, this.dataSetName2, extractedShapes2.getNodeShapes());

        Path parentDir = Paths.get(extractedShapes1.fileContentPath).getParent().getParent();
        String newFolderName = "SparqlShapeChecker_Results";
        try {
            Files.createDirectories(parentDir.resolve(newFolderName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        var copiedFile = parentDir.resolve(newFolderName + File.separator + dataSetName1 + "_" + dataSetName2+".ttl").toString();
        RegexUtils.copyFile(extractedShapes1.fileContentPath, copiedFile);
        extractedShapes2.fileContentPath = copiedFile;

        var content = RegexUtils.deleteFromFileWithPruning(extractedShapes2, comparisonDiff);
        comparisonDiff.deletedNodeShapes = new ArrayList<>(comparisonDiff.deletedNodeShapes.stream().distinct().toList());
        comparisonDiff.deletedPropertyShapes = new ArrayList<>(comparisonDiff.deletedPropertyShapes.stream().distinct().toList());
        RegexUtils.saveStringAsFile(content, copiedFile);
        Instant endSparql = Instant.now();
        comparisonDiff.durationSecondStep = Duration.between(startSparql, endSparql);

        Instant startComparison = Instant.now();
        extractedShapes1.getFileAsString(false);
        extractedShapes2.getFileAsString(true);
        ComparatorUtils.getEditedNodeShapes(comparisonDiff, extractedShapes1, extractedShapes2, firstNodeShapes);
        ComparatorUtils.getEditedPropertyShapes(comparisonDiff, extractedShapes1, extractedShapes2, firstNodeShapes);
        Instant endComparison = Instant.now();
        comparisonDiff.durationComparison = Duration.between(startComparison, endComparison);

        comparisonDiff.durationTotal = comparisonDiff.durationQse1.plus(comparisonDiff.durationSecondStep).plus(comparisonDiff.durationComparison);
        ComparatorUtils.exportComparisonToFile(logFilePath+dataSetName1+"_"+dataSetName2+ File.separator+"SparqlShapeChecker", comparisonDiff.toStringAll());
    }
}
