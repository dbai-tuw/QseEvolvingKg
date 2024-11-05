package shacldiffextractor.logic;

import cs.Main;
import cs.qse.filebased.Parser;
import shacldiffextractor.ShaclDiffExtractor;
import shape_comparator.data.ExtractedShapes;
import shape_comparator.data.NodeShape;
import sparqlshapechecker.comparator.ComparatorUtils;
import sparqlshapechecker.utils.ConfigManager;
import sparqlshapechecker.utils.RegexUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Logger;

//wraps the whole process from the SHACL-DiffExtractor
public class DiffManager {
    private static final Logger LOGGER = Logger.getLogger(ShaclDiffExtractor.class.getName());
    public static String instanceTypeProperty = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>";
    public Parser parser;
    public DiffExtractor diffExtractor;
    public DiffShapeGenerator diffShapeGenerator;
    public int support = 0;
    public double confidence = 0.0;


    public ExtractedShapes run(String dataSetName, String filePath, String filePathAdded, String filePathDeleted, String newVersionName) {
        this.executeQse(filePath, dataSetName);
        return this.executeQseDiff(filePathAdded, filePathDeleted, dataSetName, newVersionName);
    }

    public void executeQse(String filePath, String dataSetName) {
        Main.setResourcesPathForJar(ConfigManager.getRelativeResourcesPathFromQse());
        Main.annotateSupportConfidence = "true";
        Main.setPruningThresholds("{("+confidence+","+support+")}");
        File currentDir = new File(System.getProperty("user.dir"));
        File emptyConfig = new File(currentDir, "src/test/expected_test_results/emptyconfig.txt");
        Main.configPath = emptyConfig.getAbsolutePath(); //avoid exceptions in QSE
        Main.saveCountInPropertyData=true;
        Path basePath = Paths.get( "Output", "DiffExtractor", dataSetName, "Original");
        try {
            Files.createDirectories(basePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Main.setOutputFilePathForJar(basePath.toAbsolutePath()+File.separator);

        clearOutputDirectory(basePath.getParent());
        Main.datasetName = dataSetName;
        parser = new Parser(filePath, 3, 10, instanceTypeProperty);
        runParser(parser);
    }

    public static void clearOutputDirectory(Path outputPath) {
        try {
            Files.walk(outputPath)
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                        }
                    });
        } catch (IOException e) {
            LOGGER.warning("Could not clear output directory "+ outputPath.toString());
        }
    }

    public void runParser(Parser parser) {
        parser.entityExtraction();
        parser.entityConstraintsExtraction();
        parser.computeSupportConfidence();
        parser.extractSHACLShapes(support != 0 || confidence != 0, Main.qseFromSpecificClasses);
    }

    public ExtractedShapes executeQseDiff(String filePathAdded, String filePathDeleted, String oldVersionName, String newVersionName) {
        ExtractedShapes shapes = new ExtractedShapes();
        shapes.support = this.support;
        shapes.confidence = this.confidence*100;
        shapes.setNodeShapesWithPruning(parser.shapesExtractor.getNodeShapes());
        diffExtractor = new DiffExtractor(filePathAdded, filePathDeleted,parser,support, confidence, parser.shapesExtractor.getOutputFileAddress(), shapes.getNodeShapes());
        return executeQseDiffWithGivenDiffExtractor(oldVersionName, newVersionName);
    }

    public ExtractedShapes executeQseDiffForConsecutiveVersions(String filePathAdded, String filePathDeleted, String oldVersionName, String newVersionName, String outputPath, List<NodeShape> existingNodeShapes) {
        diffExtractor = new DiffExtractor(filePathAdded, filePathDeleted, parser, support, confidence, outputPath, existingNodeShapes);
        return executeQseDiffWithGivenDiffExtractor(oldVersionName, newVersionName);
    }

    private ExtractedShapes executeQseDiffWithGivenDiffExtractor(String oldVersionName, String newVersionName) {
        diffExtractor.extractFromFile();

        diffShapeGenerator = new DiffShapeGenerator(diffExtractor);
        diffShapeGenerator.newVersionName = newVersionName;
        diffShapeGenerator.oldVersionName = oldVersionName;
        diffShapeGenerator.computeDeletedShapes();
        diffShapeGenerator.generateUpdatesMap();
        diffShapeGenerator.generateUpdatedShapesWithQse();

        var mergedFilePath = diffShapeGenerator.mergeAddedShapesToOrginialFileAsString();
        var fileAsString = RegexUtils.getFileAsString(mergedFilePath);
        var fileWithDeletedShapes = diffShapeGenerator.deleteShapesFromFile(fileAsString);
        var filePath = Paths.get(Main.outputFilePath).getParent() + File.separator +oldVersionName +"InclDiffTo"+ newVersionName +".ttl";
        RegexUtils.saveStringAsFile(fileWithDeletedShapes, filePath);
        var extractedShapes = new ExtractedShapes();
        extractedShapes.fileContentPath = filePath;
        extractedShapes.nodeShapes = diffShapeGenerator.resultExtractedShapes.getNodeShapes();
        diffShapeGenerator.editedNodeShapes = ComparatorUtils.generateEditedShapesObjects(diffShapeGenerator.nodeShapeNamesToCheckForEditedShapes, diffShapeGenerator.resultExtractedShapes, extractedShapes);
        diffShapeGenerator.editPropertyShapes = ComparatorUtils.generateEditedShapesObjects(diffShapeGenerator.propertyShapeNamesToCheckForEditedShapes, diffShapeGenerator.resultExtractedShapes, extractedShapes);
        return extractedShapes;
    }
}
