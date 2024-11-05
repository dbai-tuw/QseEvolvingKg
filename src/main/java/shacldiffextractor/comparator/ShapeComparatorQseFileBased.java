package shacldiffextractor.comparator;

import cs.Main;
import cs.qse.common.ExperimentsUtil;
import cs.qse.common.structure.NS;
import cs.qse.filebased.Parser;
import org.jetbrains.annotations.NotNull;
import shacldiffextractor.ShaclDiffExtractor;
import shape_comparator.data.ExtractedShapes;
import shacldiffextractor.logic.DiffManager;
import sparqlshapechecker.comparator.ComparatorUtils;
import sparqlshapechecker.comparator.ComparisonDiff;
import sparqlshapechecker.utils.ConfigManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static shacldiffextractor.logic.DiffManager.clearOutputDirectory;

public class ShapeComparatorQseFileBased {
    private static final Logger LOGGER = Logger.getLogger(ShaclDiffExtractor.class.getName());

    String filePath1;
    public String filePath2;
    public String dataSetName1;
    public String dataSetName2;
    public String logFilePath;
    public List<NS> firstNodeShapes;
    public List<NS> secondNodeShapes;
    public String shapePath1;
    public String shapePath2;
    public String outputPath;
    public ComparisonDiff comparisonDiff;
    public Parser parser;

    public ShapeComparatorQseFileBased(String filePath1, String filePath2, String dataSetName1, String dataSetName2, String logFilePath) {
        this.filePath1 = filePath1;
        this.filePath2 = filePath2;
        this.dataSetName1 = dataSetName1;
        this.dataSetName2 = dataSetName2;
        this.logFilePath = logFilePath;
        this.outputPath = System.getProperty("user.dir")+ File.separator + "Output" + File.separator + "DiffExtractor" + File.separator;
    }

    public ComparisonDiff doComparison(String threshold) {
        prepareQse(threshold);

        //First Run
        comparisonDiff = runQseFirstTime(threshold, Path.of(this.outputPath));

        //Second Run
        return doSecondComparisonAndCompare(comparisonDiff, threshold);
    }

    public ComparisonDiff doComparisonForFollowingVersion(String threshold, ComparisonDiff comparisonDiff) {
        prepareQse(threshold);

        //Second Run
        return doSecondComparisonAndCompare(comparisonDiff, threshold);
    }

    @NotNull
    private ComparisonDiff doSecondComparisonAndCompare(ComparisonDiff comparisonDiff, String threshold) {
        prepareQsePath(dataSetName2, Path.of(this.outputPath));

        Instant startQSE2 = Instant.now();
        Parser parser = new Parser(filePath2, 300, 1000, DiffManager.instanceTypeProperty);
        DiffManager diffManager = new DiffManager();
        ExperimentsUtil.getSupportConfRange();
        diffManager.support = pruningThresholdGetSupport(threshold);
        diffManager.confidence = pruningThresholdGetConfidence(threshold);
        diffManager.runParser(parser);
        Instant endQSE2 = Instant.now();
        Duration durationQSE2 = Duration.between(startQSE2, endQSE2);
        comparisonDiff.durationSecondStep = durationQSE2;

        secondNodeShapes = parser.shapesExtractor.getNodeShapes();
        shapePath2 = parser.shapesExtractor.getOutputFileAddress();

        Instant startComparison = Instant.now();
        ComparatorUtils.getDeletedNodeShapes(comparisonDiff, firstNodeShapes, secondNodeShapes);
        ComparatorUtils.getDeletedPropertyShapes(comparisonDiff, firstNodeShapes, secondNodeShapes);
        ComparatorUtils.getAddedNodeShapes(comparisonDiff, firstNodeShapes, secondNodeShapes);
        ComparatorUtils.getAddedPropertyShapes(comparisonDiff, firstNodeShapes, secondNodeShapes);
        ExtractedShapes extractedShapes1 = new ExtractedShapes();
        ExtractedShapes extractedShapes2 = new ExtractedShapes();
        extractedShapes1.fileContentPath = shapePath1;
        extractedShapes2.fileContentPath = shapePath2;
        extractedShapes1.getFileAsString(false);
        extractedShapes2.getFileAsString(true);
        ComparatorUtils.getEditedNodeShapes(comparisonDiff, extractedShapes1, extractedShapes2, firstNodeShapes);
        ComparatorUtils.getEditedPropertyShapes(comparisonDiff, extractedShapes1, extractedShapes2, firstNodeShapes);
        Instant endComparison = Instant.now();
        Duration durationComparison = Duration.between(startComparison, endComparison);
        comparisonDiff.durationComparison = durationComparison;

        comparisonDiff.durationTotal = comparisonDiff.durationQse1.plus(durationQSE2).plus(durationComparison);
        ComparatorUtils.exportComparisonToFile(logFilePath+dataSetName1+"_"+dataSetName2+ File.separator+"QSE", comparisonDiff.toStringAll());
        this.comparisonDiff = comparisonDiff;
        return comparisonDiff;
    }

    public static int pruningThresholdGetSupport(String pruningThreshold) {
        Matcher m = Pattern.compile("\\((.*?)\\)").matcher(pruningThreshold);

        if(m.find()) {
            String[] pair = m.group(1).split(",");
            return Integer.parseInt(pair[1]);
        }
        else {
            LOGGER.warning("Could not get support value from " + pruningThreshold);
            return 0;
        }
    }

    public static double pruningThresholdGetConfidence(String pruningThreshold) {
        Matcher m = Pattern.compile("\\((.*?)\\)").matcher(pruningThreshold);

        if(m.find()) {
            String[] pair = m.group(1).split(",");
            return Double.parseDouble(pair[0]);
        }
        else {
            LOGGER.warning("Could not get confidence value from " + pruningThreshold);
            return 0;
        }
    }

    public ComparisonDiff runQseFirstTime(String threshold, Path outputPath) {
        prepareQse(threshold);

        prepareQsePath(dataSetName1, outputPath);

        ComparisonDiff comparisonDiff = new ComparisonDiff();

        Instant startQSE1 = Instant.now();
        parser = new Parser(filePath1, 300, 1000, DiffManager.instanceTypeProperty);
        DiffManager diffManager = new DiffManager();
        diffManager.support = pruningThresholdGetSupport(threshold);
        diffManager.confidence = pruningThresholdGetConfidence(threshold);
        diffManager.runParser(parser);
        Instant endQSE1 = Instant.now();
        comparisonDiff.durationQse1 = Duration.between(startQSE1, endQSE1);

        firstNodeShapes = parser.shapesExtractor.getNodeShapes();
        shapePath1 = parser.shapesExtractor.getOutputFileAddress();
        return comparisonDiff;
    }

    public static void prepareQsePath(String datasetName, Path basePath) {
        basePath = Path.of(basePath + File.separator + datasetName);

        try {
            Files.createDirectories(basePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Main.setOutputFilePathForJar(basePath.toAbsolutePath()+File.separator);

        clearOutputDirectory(basePath);
        Main.datasetName = datasetName;
    }

    public static void prepareQse(String threshold) {
        Main.setResourcesPathForJar(ConfigManager.getRelativeResourcesPathFromQse());
        Main.annotateSupportConfidence = "true";
        Main.setPruningThresholds(threshold);
        File currentDir = new File(System.getProperty("user.dir"));
        File emptyConfig = new File(currentDir, "src/test/expected_test_results/emptyconfig.txt");
        Main.configPath = emptyConfig.getAbsolutePath(); //avoid exceptions in QSE
        Main.saveCountInPropertyData=true;
    }
}
