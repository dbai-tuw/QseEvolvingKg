package shacldiffextractor.test;

import cs.Main;
import cs.qse.common.EntityData;
import cs.qse.filebased.Parser;
import cs.qse.filebased.SupportConfidence;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import shape_comparator.data.ExtractedShapes;
import shacldiffextractor.logic.DiffExtractor;
import shacldiffextractor.logic.DiffManager;
import shacldiffextractor.logic.DiffShapeGenerator;
import sparqlshapechecker.utils.ConfigManager;
import sparqlshapechecker.utils.RegexUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Objects;

import static org.junit.Assert.*;

//failed tests need to be rerun, there are some issues with file locks
interface BugWithSupportTests {}

public class ShaclDiffExtractorUnitTests {
    public String instanceTypeProperty = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>";

    @Before
    public void prepareQSE() throws IOException {
        Main.setResourcesPathForJar(ConfigManager.getRelativeResourcesPathFromQse());
        Main.annotateSupportConfidence = "true";
        Main.setPruningThresholds("{(0,0)}");
        File currentDir = new File(System.getProperty("user.dir"));
        File emptyConfig = new File(currentDir, "src/test/expected_test_results/emptyconfig.txt");
        Main.configPath = emptyConfig.getAbsolutePath(); //avoid exceptions in QSE
        Main.saveCountInPropertyData=true;
        Path basePath = Paths.get( "Output", "UnitTestOutput", "Original");
        Files.createDirectories(basePath);
        Main.setOutputFilePathForJar(basePath.toAbsolutePath()+File.separator);

        Files.walk(Paths.get(Main.outputFilePath).getParent())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                }
        });
    }


    @Test
    public void runQseWithPeople() {
        Main.datasetName = "People2";
        var datasetPath = System.getProperty("user.dir")+"\\notes\\defaultGraphs\\miniexample\\People2AdaptWithMultipleKnows.nt";

        Parser parser = new Parser(datasetPath, 3, 10, instanceTypeProperty);
        runParser(parser);
    }

    @Test
    public void runQseWithPeopleWithSupport() throws IOException {
        Main.datasetName = "People3";
        var contentNew = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/bob> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/jenny> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/name> "Alice" .
                <http://example.org/bob> <http://xmlns.com/foaf/0.1/name> "Bob" .
                <http://example.org/jenny> <http://xmlns.com/foaf/0.1/name> "Jenny" .
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/knows> <http://example.org/orangeCat> .
                <http://example.org/bob> <http://xmlns.com/foaf/0.1/knows> <http://example.org/blackCat> .
                <http://example.org/jenny> <http://xmlns.com/foaf/0.1/knows> <http://example.org/greyCat> .
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/blackCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://example.org/color> "orange" .
                """;
        var tempFileNew = Files.createTempFile("QSERQ2TmpFileNew", ".nt");
        Files.write(tempFileNew, contentNew.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
        Main.setPruningThresholds("{(0,0)}");
        Parser parser = new Parser(tempFileNew.toAbsolutePath().toString(), 3, 10, instanceTypeProperty);
        parser.entityExtraction();
        parser.entityConstraintsExtraction();
        parser.computeSupportConfidence();
        parser.extractSHACLShapes(true, Main.qseFromSpecificClasses);
    }

    @Test
    public void runQseWithPeople2() throws IOException {
        Main.datasetName = "People2";
        var tempFileNew = Files.createTempFile("QSERQ2TmpFileNew", ".nt");
        var contentNew = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://example.org/color> "orange" .
                """;
        Files.write(tempFileNew, contentNew.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);

        Parser parser = new Parser(tempFileNew.toAbsolutePath().toString(), 3, 10, instanceTypeProperty);
        runParser(parser);
    }

    public DiffExtractor testQseOutput(String content, String contentNew, String contentAdded, String contentDeleted) throws IOException {
        Main.datasetName = "People2";
        int support = 0;
        double confidence = 0.0;

        var tempFile = Files.createTempFile("QSERQ2TmpFile", ".nt");
        Files.write(tempFile, content.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);

        var tempFileNew = Files.createTempFile("QSERQ2TmpFileNew", ".nt");
        Files.write(tempFileNew, contentNew.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);

        var tempFileAdded = Files.createTempFile("QSERQ2TmpFileAdded", ".nt");
        Files.write(tempFileAdded, contentAdded.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);

        var tempFileDeleted = Files.createTempFile("QSERQ2TmpFileDeleted", ".nt");
        Files.write(tempFileDeleted, contentDeleted.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);

        Parser parser = new Parser(tempFile.toAbsolutePath().toString(), 3, 10, instanceTypeProperty);
        runParser(parser);
        ExtractedShapes tmp = new ExtractedShapes();
        tmp.setNodeShapes(parser.shapesExtractor.getNodeShapes(), false);
        DiffExtractor diffExtractor = new DiffExtractor(tempFileAdded.toAbsolutePath().toString(),tempFileDeleted.toAbsolutePath().toString(),parser,support, confidence, parser.shapesExtractor.getOutputFileAddress(), tmp.getNodeShapes());
        diffExtractor.extractFromFile();

        var oldDataSetName = Main.datasetName;
        var oldOutputPath = Main.outputFilePath;
        Main.datasetName = Main.datasetName+"_Full";
        Main.outputFilePath = Main.outputFilePath+ "Full" + File.separator;
        Parser parserV3 = new Parser(tempFileNew.toAbsolutePath().toString(), 3, 10, instanceTypeProperty);
        parserV3.setStringEncoder(parser.getStringEncoder());
        runParser(parserV3);
        Main.datasetName = oldDataSetName;
        Main.outputFilePath = oldOutputPath;

        assertEquals(parser.classEntityCount, parserV3.classEntityCount);
        assertTrue(areMapsEqual(parser.entityDataHashMap, parserV3.entityDataHashMap));
        assertTrue(areMapsEqual(parser.classToPropWithObjTypes, parserV3.classToPropWithObjTypes));
        assertTrue(areMapsEqual(parser.statsComputer.getShapeTripletSupport(), parserV3.statsComputer.getShapeTripletSupport()));
        return diffExtractor;
    }

    @Test
    public void testBasicDelete() throws IOException {

        var content = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/bob> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/knows> <http://example.org/bob> .
                <http://example.org/bob> <http://xmlns.com/foaf/0.1/knows> <http://example.org/alice> .
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/knows> <http://example.org/orangeCat> .
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                """;

        var contentNew = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/bob> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/bob> <http://xmlns.com/foaf/0.1/knows> <http://example.org/alice> .
                """;

        var contentAdded = "";

        var contentDeleted = """
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/knows> <http://example.org/bob> .
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/knows> <http://example.org/orangeCat> .
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                """;

        testQseOutput(content, contentNew, contentAdded, contentDeleted);
    }

    @Test
    public void testDeleteReferenceItself() throws IOException {

        var content = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/knows> <http://example.org/alice> .
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/knows> <http://example.org/orangeCat> .
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                """;

        var contentNew = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/knows> <http://example.org/orangeCat> .
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                """;

        var contentAdded = "";

        var contentDeleted = """
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/knows> <http://example.org/alice> .
                """;

        testQseOutput(content, contentNew, contentAdded, contentDeleted);
    }

    @Test
    public void replaceLiteralValue() throws IOException {

        var content = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/name> "Alice" .
                """;

        var contentNew = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/name> "AliceOtherName" .
                """;

        var contentAdded = """
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/name> "AliceOtherName" .
                """;

        var contentDeleted = """
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/name> "Alice" .
                """;

        testQseOutput(content, contentNew, contentAdded, contentDeleted);
    }

    @Test
    public void replaceIRIValue() throws IOException {

        var content = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/bob> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/anna> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/knows>  <http://example.org/bob>.
                """;

        var contentNew = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/bob> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/anna> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/knows>  <http://example.org/anna>.
                """;

        var contentAdded = """
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/knows>  <http://example.org/anna>.
                """;

        var contentDeleted = """
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/knows>  <http://example.org/bob>.
                """;

        testQseOutput(content, contentNew, contentAdded, contentDeleted);
    }

    @Test
    public void testDeleteSameOutcome() throws IOException {

        var content = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/bob> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/knows> <http://example.org/bob> .
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/name> "Alice" .
                <http://example.org/bob> <http://xmlns.com/foaf/0.1/name> "Bob" .
                <http://example.org/bob> <http://xmlns.com/foaf/0.1/knows> <http://example.org/alice> .
                """;

        var contentNew = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/bob> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/knows> <http://example.org/bob> .
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/name> "Alice" .
                """;

        var contentAdded = "";

        var contentDeleted = """
                <http://example.org/bob> <http://xmlns.com/foaf/0.1/name> "Bob" .
                <http://example.org/bob> <http://xmlns.com/foaf/0.1/knows> <http://example.org/alice> .
                """;

        testQseOutput(content, contentNew, contentAdded, contentDeleted);
    }

    @Test
    public void testDeleteOnePropertyLeft() throws IOException {

        var content = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/bob> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/knows> <http://example.org/bob> .
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/name> "Alice" .
                <http://example.org/bob> <http://xmlns.com/foaf/0.1/name> "Bob" .
                <http://example.org/bob> <http://xmlns.com/foaf/0.1/knows> <http://example.org/alice> .
                """;

        var contentNew = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/bob> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/name> "Alice" .
                """;

        var contentAdded = "";

        var contentDeleted = """
                <http://example.org/bob> <http://xmlns.com/foaf/0.1/name> "Bob" .
                <http://example.org/bob> <http://xmlns.com/foaf/0.1/knows> <http://example.org/alice> .
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/knows> <http://example.org/bob> .
                """;

        testQseOutput(content, contentNew, contentAdded, contentDeleted);

    }

    @Test
    public void testDeleteNodeShape() throws IOException {

        var content = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://xmlns.com/foaf/0.1/Person> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Class> .
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/name> "Alice" .
                """;

        var contentNew = "";

        var contentAdded = "";

        var contentDeleted = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://xmlns.com/foaf/0.1/Person> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Class> .
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/name> "Alice" .
                """;

        testQseOutput(content, contentNew, contentAdded, contentDeleted);

    }

    @Test
    @Category(BugWithSupportTests.class)
    public void testDeleteOneOfMultipleTypes() throws IOException {

        var content = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://xmlns.com/foaf/0.1/Person> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Class> .
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Alien> .
                <http://xmlns.com/foaf/0.1/Alien> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#SomeOtherClass> .
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/name> "Alice" .
                """;

        var contentNew = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://xmlns.com/foaf/0.1/Person> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Class> .
                <http://xmlns.com/foaf/0.1/Alien> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#SomeOtherClass> .
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/name> "Alice" .
                """;

        var contentAdded = "";

        var contentDeleted = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Alien> .
                """;

        testQseOutput(content, contentNew, contentAdded, contentDeleted);

    }

    @Test
    @Category(BugWithSupportTests.class)
    public void testDeleteOneOfMultipleTypesWithSameParentClass() throws IOException {

        var content = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://xmlns.com/foaf/0.1/Person> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Class> .
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Alien> .
                <http://xmlns.com/foaf/0.1/Alien> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Class> .
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/name> "Alice" .
                """;

        var contentNew = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://xmlns.com/foaf/0.1/Person> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Class> .
                <http://xmlns.com/foaf/0.1/Alien> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Class> .
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/name> "Alice" .
                """;

        var contentAdded = "";

        var contentDeleted = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Alien> .
                """;

        testQseOutput(content, contentNew, contentAdded, contentDeleted);

    }

    @Test
    public void testDeleteMultipleTypeStatements() throws IOException {

        var content = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Alien> .
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/name> "Alice" .
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/nickName> "Al" .
                """;

        var contentNew = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Alien> .
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/name> "Alice" .
                """;

        var contentAdded = "";

        var contentDeleted = "<http://example.org/alice> <http://xmlns.com/foaf/0.1/nickName> \"Al\" .\n";

        testQseOutput(content, contentNew, contentAdded, contentDeleted);
    }

    @Test
    public void testBasicAdd() throws IOException {

        var content = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/bob> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/jenny> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/name> "Alice" .
                <http://example.org/bob> <http://xmlns.com/foaf/0.1/name> "Bob" .
                <http://example.org/jenny> <http://xmlns.com/foaf/0.1/name> "Jenny" .
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/knows> <http://example.org/bob> .
                <http://example.org/bob> <http://xmlns.com/foaf/0.1/knows> <http://example.org/alice> .
                <http://example.org/jenny> <http://xmlns.com/foaf/0.1/knows> <http://example.org/alice> .
                """;

        var contentNew = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/bob> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/jenny> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/name> "Alice" .
                <http://example.org/bob> <http://xmlns.com/foaf/0.1/name> "Bob" .
                <http://example.org/jenny> <http://xmlns.com/foaf/0.1/name> "Jenny" .
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/knows> <http://example.org/bob> .
                <http://example.org/bob> <http://xmlns.com/foaf/0.1/knows> <http://example.org/alice> .
                <http://example.org/jenny> <http://xmlns.com/foaf/0.1/knows> <http://example.org/alice> .
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/blackCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/greyCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://example.org/color> "orange" .
                <http://example.org/blackCat> <http://example.org/color> "black" .
                <http://example.org/greyCat> <http://example.org/color> "grey" .
                """;

        var contentAdded = """
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/blackCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/greyCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://example.org/color> "orange" .
                <http://example.org/blackCat> <http://example.org/color> "black" .
                <http://example.org/greyCat> <http://example.org/color> "grey" .""";

        var contentDeleted = "";

        testQseOutput(content, contentNew, contentAdded, contentDeleted);
    }

    @Test
    public void addCheckDiffMapAddNodeShape() throws IOException {
        var content = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/bob> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/jenny> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/name> "Alice" .
                <http://example.org/bob> <http://xmlns.com/foaf/0.1/name> "Bob" .
                <http://example.org/jenny> <http://xmlns.com/foaf/0.1/name> "Jenny" .
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/knows> <http://example.org/bob> .
                <http://example.org/bob> <http://xmlns.com/foaf/0.1/knows> <http://example.org/alice> .
                <http://example.org/jenny> <http://xmlns.com/foaf/0.1/knows> <http://example.org/alice> .
                """;

        var contentNew = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/bob> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/jenny> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/name> "Alice" .
                <http://example.org/bob> <http://xmlns.com/foaf/0.1/name> "Bob" .
                <http://example.org/jenny> <http://xmlns.com/foaf/0.1/name> "Jenny" .
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/knows> <http://example.org/bob> .
                <http://example.org/bob> <http://xmlns.com/foaf/0.1/knows> <http://example.org/alice> .
                <http://example.org/jenny> <http://xmlns.com/foaf/0.1/knows> <http://example.org/alice> .
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/blackCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/greyCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://example.org/color> "orange" .
                <http://example.org/blackCat> <http://example.org/color> "black" .
                <http://example.org/greyCat> <http://example.org/color> "grey" .
                """;

        var contentAdded = """
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/blackCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/greyCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://example.org/color> "orange" .
                <http://example.org/blackCat> <http://example.org/color> "black" .
                <http://example.org/greyCat> <http://example.org/color> "grey" .""";

        var contentDeleted = "";

        var diffExtractor = testQseOutput(content, contentNew, contentAdded, contentDeleted);
        DiffShapeGenerator diffShapeGenerator = new DiffShapeGenerator(diffExtractor);
        diffShapeGenerator.generateUpdatesMap();
        var codeForCat = diffExtractor.parser.getStringEncoder().encode("http://xmlns.com/foaf/0.1/Cat");
        var codeForType = diffExtractor.parser.getStringEncoder().encode("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
        var codeForColor = diffExtractor.parser.getStringEncoder().encode("http://example.org/color");

        var diffMap = diffShapeGenerator.updatedShapes;
        assertEquals(1, diffMap.size());
        assertTrue(diffMap.containsKey(codeForCat));
        assertEquals(2, diffMap.get(codeForCat).size());
        assertTrue(diffMap.get(codeForCat).containsKey(codeForType));
        assertEquals(1, diffMap.get(codeForCat).get(codeForType).size());
        assertTrue(diffMap.get(codeForCat).get(codeForType).contains(2));
        assertTrue(diffMap.get(codeForCat).containsKey(codeForColor));
        assertEquals(1, diffMap.get(codeForCat).get(codeForColor).size());
        assertTrue(diffMap.get(codeForCat).get(codeForColor).contains(4));
    }

    @Test
    public void addCheckDiffMapAddPropertyShape() throws IOException {
        var content = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://example.org/color> "orange" .
                """;

        var contentNew = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://example.org/color> "orange" .
                <http://example.org/orangeCat> <http://example.org/age> "10"^^<http://www.w3.org/2001/XMLSchema#integer> .
                """;

        var contentAdded = "<http://example.org/orangeCat> <http://example.org/age> \"10\"^^<http://www.w3.org/2001/XMLSchema#integer> .\n";
        var contentDeleted = "";

        var diffExtractor = testQseOutput(content, contentNew, contentAdded, contentDeleted);
        DiffShapeGenerator diffShapeGenerator = new DiffShapeGenerator(diffExtractor);
        diffShapeGenerator.generateUpdatesMap();
        var diffMap = diffShapeGenerator.updatedShapes;
        var codeForCat = diffExtractor.parser.getStringEncoder().encode("http://xmlns.com/foaf/0.1/Cat");
        var codeForAge = diffExtractor.parser.getStringEncoder().encode("http://example.org/age");
        var codeForInt = diffExtractor.parser.getStringEncoder().encode("<http://www.w3.org/2001/XMLSchema#integer>");
        assertEquals(1, diffMap.size());
        assertTrue(diffMap.containsKey(codeForCat));
        assertEquals(1, diffMap.get(codeForCat).size());
        assertTrue(diffMap.get(codeForCat).containsKey(codeForAge));
        assertEquals(1, diffMap.get(codeForCat).get(codeForAge).size());
        assertTrue(diffMap.get(codeForCat).get(codeForAge).contains(codeForInt));
    }

    @Test
    public void addCheckDiffShapeFile() throws IOException {
        var content = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://example.org/color> "orange" .
                """;

        var contentNew = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://example.org/color> "orange" .
                <http://example.org/orangeCat> <http://example.org/age> "10"^^<http://www.w3.org/2001/XMLSchema#integer> .
                """;

        var contentAdded = "<http://example.org/orangeCat> <http://example.org/age> \"10\"^^<http://www.w3.org/2001/XMLSchema#integer> .\n";
        var contentDeleted = "";

        var diffExtractor = testQseOutput(content, contentNew, contentAdded, contentDeleted);
        DiffShapeGenerator diffShapeGenerator = new DiffShapeGenerator(diffExtractor);
        diffShapeGenerator.generateUpdatesMap();
        ExtractedShapes extractedShapes = diffShapeGenerator.generateUpdatedShapesWithQse();
        String file = extractedShapes.getFileAsString(true);
        var catShapeString = RegexUtils.getShapeAsString("http://shaclshapes.org/CatShape", file);
        assertFalse(catShapeString.isEmpty());
        var ageShapeString = RegexUtils.getShapeAsString("http://shaclshapes.org/ageCatShapeProperty", file);
        assertFalse(ageShapeString.isEmpty());
    }

    @Test
    public void addDiffNSToExistingShaclFile() throws IOException {
        var content = "<http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .\n";

        var contentNew = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://example.org/color> "orange" .
                """;
        var contentAdded = """
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://example.org/color> "orange" .
                """;
        var contentDeleted = "";

        var diffExtractor = testQseOutput(content, contentNew, contentAdded, contentDeleted);
        DiffShapeGenerator diffShapeGenerator = new DiffShapeGenerator(diffExtractor);
        diffShapeGenerator.generateUpdatesMap();
        diffShapeGenerator.generateUpdatedShapesWithQse();
        var newFilePath = diffShapeGenerator.mergeAddedShapesToOrginialFileAsString();
        var fileAsString = RegexUtils.getFileAsString(newFilePath);
        assertTrue(fileAsString.contains("colorCatShape"));

        var nodeShapes = diffShapeGenerator.resultExtractedShapes.nodeShapes;
        assertEquals(nodeShapes.size(),2);
        var catShape = nodeShapes.stream().filter(ns -> ns.getIri().toString().contains("http://shaclshapes.org/CatShape")).toList().get(0);
        var personShape = nodeShapes.stream().filter(ns -> ns.getIri().toString().contains("http://shaclshapes.org/PersonShape")).toList().get(0);
        assertNotNull(catShape);
        assertNotNull(personShape);
        assertEquals(personShape.propertyShapes.size(),1);
        var colorShape = catShape.propertyShapes.stream().filter(ps -> ps.iri.toString().contains("colorCatShapeProperty")).toList().get(0);
        assertNotNull(colorShape);
        var instanceTypeShape = catShape.propertyShapes.stream().filter(ps -> ps.iri.toString().contains("instanceTypeCatShapeProperty")).toList().get(0);
        assertNotNull(instanceTypeShape);
        assertEquals(catShape.propertyShapes.size(),2);
    }

    @Test
    public void addDiffPSToExistingShaclFile() throws IOException {
        var content = "<http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .\n";

        var contentNew = """
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://example.org/color> "orange" .
                """;

        var contentAdded = "<http://example.org/orangeCat> <http://example.org/color> \"orange\" .\n";
        var contentDeleted = "";

        var diffExtractor = testQseOutput(content, contentNew, contentAdded, contentDeleted);
        DiffShapeGenerator diffShapeGenerator = new DiffShapeGenerator(diffExtractor);
        diffShapeGenerator.generateUpdatesMap();
        diffShapeGenerator.generateUpdatedShapesWithQse();
        var newFilePath = diffShapeGenerator.mergeAddedShapesToOrginialFileAsString();
        var fileAsString = RegexUtils.getFileAsString(newFilePath);
        assertTrue(fileAsString.contains("colorCatShape"));
        assertTrue(fileAsString.contains("<http://www.w3.org/ns/shacl#property> <http://shaclshapes.org/colorCatShapeProperty> ;"));

        var nodeShapes = diffShapeGenerator.resultExtractedShapes.nodeShapes;
        assertEquals(nodeShapes.size(),1);
        var catShape = nodeShapes.stream().filter(ns -> ns.getIri().toString().contains("http://shaclshapes.org/CatShape")).toList().get(0);
        assertNotNull(catShape);
        var colorShape = catShape.propertyShapes.stream().filter(ps -> ps.iri.toString().contains("colorCatShapeProperty")).toList().get(0);
        assertNotNull(colorShape);
        assertEquals(catShape.propertyShapes.size(),2);
    }

    @Test
    public void constraintAddedInPropertyShape() throws IOException {
        var content = """
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://example.org/color> "orange" .
                """;

        var contentNew = """
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://example.org/color> "orange" .
                <http://example.org/orangeCat> <http://example.org/color> "10"^^<http://www.w3.org/2001/XMLSchema#integer> .
                """;
        var contentAdded = "<http://example.org/orangeCat> <http://example.org/color> \"10\"^^<http://www.w3.org/2001/XMLSchema#integer> .\n";
        var contentDeleted = "";

        var diffExtractor = testQseOutput(content, contentNew, contentAdded, contentDeleted);
        DiffShapeGenerator diffShapeGenerator = new DiffShapeGenerator(diffExtractor);
        diffShapeGenerator.generateUpdatesMap();
        var diffMap = diffShapeGenerator.updatedShapes;
        diffShapeGenerator.generateUpdatedShapesWithQse();

        assertTrue(diffExtractor.editedShapesMap.containsKey(0));
        assertEquals(1, diffExtractor.editedShapesMap.size());
        assertEquals(1, diffExtractor.editedShapesMap.get(0).size());
        assertTrue(diffExtractor.editedShapesMap.get(0).contains(3));
        assertTrue(diffMap.containsKey(0));
        assertEquals(1, diffMap.size());
        assertEquals(1, diffMap.get(0).size());
        assertNotNull(diffMap.get(0).get(3));

        var newFilePath = diffShapeGenerator.mergeAddedShapesToOrginialFileAsString();
        var fileAsString = RegexUtils.getFileAsString(newFilePath);
        assertTrue(fileAsString.contains("""
                <http://shaclshapes.org/colorCatShapeProperty> rdf:type <http://www.w3.org/ns/shacl#PropertyShape> ;
                  <http://www.w3.org/ns/shacl#minCount> 1 ;
                  <http://www.w3.org/ns/shacl#or> ( [
                    <http://shaclshapes.org/confidence> 1E0 ;
                    <http://shaclshapes.org/support> "1"^^xsd:int ;
                    <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#Literal> ;
                    <http://www.w3.org/ns/shacl#datatype> xsd:string ;
                  ] [
                    <http://shaclshapes.org/confidence> 1E0 ;
                    <http://shaclshapes.org/support> "1"^^xsd:int ;
                    <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#Literal> ;
                    <http://www.w3.org/ns/shacl#datatype> xsd:integer ;
                  ] ) ;
                  <http://www.w3.org/ns/shacl#path> <http://example.org/color> .
                """));


        var nodeShapes = diffShapeGenerator.resultExtractedShapes.nodeShapes;
        assertEquals(nodeShapes.size(),1);
        var catShape = nodeShapes.stream().filter(ns -> ns.getIri().toString().contains("http://shaclshapes.org/CatShape")).toList().get(0);
        assertNotNull(catShape);
        var colorShape = catShape.propertyShapes.stream().filter(ps -> ps.iri.toString().contains("colorCatShapeProperty")).toList().get(0);
        assertNotNull(colorShape);
        assertEquals(catShape.propertyShapes.size(),2);
    }

    @Test
    public void constraintDeletedInPropertyShape() throws IOException {
        var content = """
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://example.org/color> "orange" .
                <http://example.org/orangeCat> <http://example.org/color> "10"^^<http://www.w3.org/2001/XMLSchema#integer> .
                """;

        var contentNew = """
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://example.org/color> "orange" .
                """;
        var contentAdded = "";
        var contentDeleted = "<http://example.org/orangeCat> <http://example.org/color> \"10\"^^<http://www.w3.org/2001/XMLSchema#integer> .\n";

        var diffExtractor = testQseOutput(content, contentNew, contentAdded, contentDeleted);
        DiffShapeGenerator diffShapeGenerator = new DiffShapeGenerator(diffExtractor);
        diffShapeGenerator.generateUpdatesMap();
        var diffMap = diffShapeGenerator.updatedShapes;
        diffShapeGenerator.generateUpdatedShapesWithQse();

        assertTrue(diffExtractor.editedShapesMap.containsKey(0));
        assertEquals(1, diffExtractor.editedShapesMap.size());
        assertEquals(1, diffExtractor.editedShapesMap.get(0).size());
        assertTrue(diffExtractor.editedShapesMap.get(0).contains(3));
        assertTrue(diffMap.containsKey(0));
        assertEquals(1, diffMap.size());
        assertEquals(1, diffMap.get(0).size());
        assertNotNull(diffMap.get(0).get(3));

        var newFilePath = diffShapeGenerator.mergeAddedShapesToOrginialFileAsString();
        var fileAsString = RegexUtils.getFileAsString(newFilePath);
        assertTrue(fileAsString.contains("""
                <http://shaclshapes.org/colorCatShapeProperty> rdf:type <http://www.w3.org/ns/shacl#PropertyShape> ;
                  <http://shaclshapes.org/confidence> 1E0 ;
                  <http://shaclshapes.org/support> "1"^^xsd:int ;
                  <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#Literal> ;
                  <http://www.w3.org/ns/shacl#datatype> xsd:string ;
                  <http://www.w3.org/ns/shacl#minCount> 1 ;
                  <http://www.w3.org/ns/shacl#path> <http://example.org/color> ."""));

        var nodeShapes = diffShapeGenerator.resultExtractedShapes.nodeShapes;
        assertEquals(nodeShapes.size(),1);
        var catShape = nodeShapes.stream().filter(ns -> ns.getIri().toString().contains("http://shaclshapes.org/CatShape")).toList().get(0);
        assertNotNull(catShape);
        var colorShape = catShape.propertyShapes.stream().filter(ps -> ps.iri.toString().contains("colorCatShapeProperty")).toList().get(0);
        assertNotNull(colorShape);
        assertEquals(catShape.propertyShapes.size(),2);
    }

    @Test
    public void deleteNSFromShaclFile() throws IOException {
        var content = """
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://example.org/color> "orange" .
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                """;

        var contentNew = "<http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .\n";
        var contentAdded = "";
        var contentDeleted = """
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://example.org/color> "orange" .
                """;

        var diffExtractor = testQseOutput(content, contentNew, contentAdded, contentDeleted);
        DiffShapeGenerator diffShapeGenerator = new DiffShapeGenerator(diffExtractor);
        diffShapeGenerator.computeDeletedShapes();
        var deletedShapeMap = diffShapeGenerator.deletedShapes;
        diffShapeGenerator.generateUpdatesMap();
        var diffMap = diffShapeGenerator.updatedShapes;
        diffShapeGenerator.generateUpdatedShapesWithQse();

        assertTrue(deletedShapeMap.containsKey(0));
        assertEquals(1, deletedShapeMap.size());
        assertEquals(2, deletedShapeMap.get(0).size());
        assertTrue(deletedShapeMap.get(0).contains(2));
        assertTrue(deletedShapeMap.get(0).contains(4));
        assertEquals(0, diffMap.size());

        var newFilePath = diffShapeGenerator.mergeAddedShapesToOrginialFileAsString();
        var fileAsString = RegexUtils.getFileAsString(newFilePath);
        var fileWithDeletedShapes = diffShapeGenerator.deleteShapesFromFile(fileAsString);
        RegexUtils.saveStringAsFile(fileWithDeletedShapes,  Main.outputFilePath+"finished.ttl");
        assertFalse(fileWithDeletedShapes.contains("<http://shaclshapes.org/CatShape>"));
        assertFalse(fileWithDeletedShapes.contains("<http://shaclshapes.org/colorCatShapeProperty>"));
        assertFalse(fileWithDeletedShapes.contains("<http://shaclshapes.org/instanceTypeCatShapeProperty>"));

        var nodeShapes = diffShapeGenerator.resultExtractedShapes.nodeShapes;
        assertEquals(nodeShapes.size(),1);
        var personShape = nodeShapes.stream().filter(ns -> ns.getIri().toString().contains("http://shaclshapes.org/PersonShape")).toList().get(0);
        assertNotNull(personShape);
        assertEquals(personShape.propertyShapes.size(),1);
        var instanceTypeShape = personShape.propertyShapes.stream().filter(ps -> ps.iri.toString().contains("instanceTypePersonShapeProperty")).toList().get(0);
        assertNotNull(instanceTypeShape);
        assertEquals(personShape.propertyShapes.size(),1);
    }

    @Test
    public void deletePSFromShaclFile() throws IOException {
        var content = """
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://example.org/color> "orange" .
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                """;

        var contentNew = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                """;
        var contentAdded = "";
        var contentDeleted = "<http://example.org/orangeCat> <http://example.org/color> \"orange\" .\n";

        var diffExtractor = testQseOutput(content, contentNew, contentAdded, contentDeleted);
        DiffShapeGenerator diffShapeGenerator = new DiffShapeGenerator(diffExtractor);
        diffShapeGenerator.computeDeletedShapes();
        var deletedShapeMap = diffShapeGenerator.deletedShapes;
        diffShapeGenerator.generateUpdatesMap();
        var diffMap = diffShapeGenerator.updatedShapes;
        diffShapeGenerator.generateUpdatedShapesWithQse();

        assertTrue(deletedShapeMap.containsKey(0));
        assertEquals(1, deletedShapeMap.size());
        assertEquals(1, deletedShapeMap.get(0).size());
        assertTrue(deletedShapeMap.get(0).contains(4));
        assertEquals(0, diffMap.size());

        var newFilePath = diffShapeGenerator.mergeAddedShapesToOrginialFileAsString();
        var fileAsString = RegexUtils.getFileAsString(newFilePath);
        var fileWithDeletedShapes = diffShapeGenerator.deleteShapesFromFile(fileAsString);
        RegexUtils.saveStringAsFile(fileWithDeletedShapes,  Main.outputFilePath+"finished.ttl");
        assertTrue(fileWithDeletedShapes.contains("<http://shaclshapes.org/CatShape>"));
        assertFalse(fileWithDeletedShapes.contains("<http://shaclshapes.org/colorCatShapeProperty>"));
        assertTrue(fileWithDeletedShapes.contains("<http://shaclshapes.org/instanceTypeCatShapeProperty>"));

        var nodeShapes = diffShapeGenerator.resultExtractedShapes.nodeShapes;
        assertEquals(nodeShapes.size(),2);
        var catShape = nodeShapes.stream().filter(ns -> ns.getIri().toString().contains("http://shaclshapes.org/CatShape")).toList().get(0);
        var personShape = nodeShapes.stream().filter(ns -> ns.getIri().toString().contains("http://shaclshapes.org/PersonShape")).toList().get(0);
        assertNotNull(catShape);
        assertNotNull(personShape);
        assertEquals(personShape.propertyShapes.size(),1);
        var instanceCatShape = catShape.propertyShapes.stream().filter(ps -> ps.iri.toString().contains("instanceTypeCatShapeProperty")).toList().get(0);
        var instancePerson = personShape.propertyShapes.stream().filter(ps -> ps.iri.toString().contains("instanceTypePersonShapeProperty")).toList().get(0);
        assertNotNull(instanceCatShape);
        assertNotNull(instancePerson);
        assertEquals(catShape.propertyShapes.size(),1);
    }

    @Test
    public void testBasicDeleteAndAdd() throws IOException {

        var content = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/bob> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/jenny> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/name> "Alice" .
                <http://example.org/bob> <http://xmlns.com/foaf/0.1/name> "Bob" .
                <http://example.org/jenny> <http://xmlns.com/foaf/0.1/name> "Jenny" .
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/knows> <http://example.org/bob> .
                <http://example.org/bob> <http://xmlns.com/foaf/0.1/knows> <http://example.org/alice> .
                <http://example.org/jenny> <http://xmlns.com/foaf/0.1/knows> <http://example.org/alice> .
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/blackCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/greyCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://example.org/color> "orange" .
                <http://example.org/blackCat> <http://example.org/color> "black" .
                <http://example.org/greyCat> <http://example.org/color> "grey" .
                """;

        var contentNew = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/bob> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/jenny> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/name> "Alice" .
                <http://example.org/bob> <http://xmlns.com/foaf/0.1/name> "Bob" .
                <http://example.org/jenny> <http://xmlns.com/foaf/0.1/name> "Jenny" .
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/knows> <http://example.org/orangeCat> .
                <http://example.org/bob> <http://xmlns.com/foaf/0.1/knows> <http://example.org/blackCat> .
                <http://example.org/jenny> <http://xmlns.com/foaf/0.1/knows> <http://example.org/greyCat> .
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/blackCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/greyCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://example.org/color> "orange" .""";

        var contentAdded = """
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/knows> <http://example.org/orangeCat> .
                <http://example.org/bob> <http://xmlns.com/foaf/0.1/knows> <http://example.org/blackCat> .
                <http://example.org/jenny> <http://xmlns.com/foaf/0.1/knows> <http://example.org/greyCat> .""";

        var contentDeleted = """
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/knows> <http://example.org/bob> .
                <http://example.org/bob> <http://xmlns.com/foaf/0.1/knows> <http://example.org/alice> .
                <http://example.org/jenny> <http://xmlns.com/foaf/0.1/knows> <http://example.org/alice> .
                <http://example.org/blackCat> <http://example.org/color> "black" .
                <http://example.org/greyCat> <http://example.org/color> "grey" .""";

        testQseOutput(content, contentNew, contentAdded, contentDeleted);
    }

    @Test
    public void testDiffManager()
    {
        var content = "<http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .\n";

        var contentAdded = "<http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .\n";
        var contentDeleted = "<http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .\n";
        String filePath = generateFile(content);
        String addedPath = generateFile(contentAdded);
        String deletedPath = generateFile(contentDeleted);

        DiffManager diffManager = new DiffManager();
        var extractedShapes = diffManager.run("People", filePath, addedPath, deletedPath, "");
        assertTrue(extractedShapes.getFileAsString(true).contains("<http://shaclshapes.org/PersonShape>"));
        assertTrue(extractedShapes.getFileAsString(true).contains("<http://shaclshapes.org/instanceTypePersonShapeProperty>"));
        var nodeShapes = extractedShapes.getNodeShapes();
        assertEquals(1, nodeShapes.size());
        assertTrue(nodeShapes.get(0).getIri().toString().contains("http://shaclshapes.org/PersonShape"));
        assertEquals(nodeShapes.get(0).propertyShapes.size(),1);
        assertTrue(nodeShapes.get(0).propertyShapes.get(0).iri.toString().contains("http://shaclshapes.org/instanceTypePersonShapeProperty"));
    }

    @Test
    public void testEditedNodeShapes()
    {
        var content = "<http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .\n";

        var contentAdded = "<http://example.org/orangeCat> <http://example.org/color> \"orange\" .\n";
        var contentDeleted = "";
        String filePath = generateFile(content);
        String addedPath = generateFile(contentAdded);
        String deletedPath = generateFile(contentDeleted);

        DiffManager diffManager = new DiffManager();
        diffManager.run("People", filePath, addedPath, deletedPath,"");

        assertEquals(diffManager.diffShapeGenerator.editedNodeShapes.size(),1);
        assertEquals(diffManager.diffShapeGenerator.editedNodeShapes.get(0).shapeName,"http://shaclshapes.org/CatShape");
        assertEquals(diffManager.diffShapeGenerator.editedNodeShapes.get(0).shapeAsTextNew.replace("\r",""), """
                @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .

                <http://shaclshapes.org/CatShape> rdf:type <http://www.w3.org/ns/shacl#NodeShape> ;
                  <http://www.w3.org/ns/shacl#property> <http://shaclshapes.org/colorCatShapeProperty> ;
                  <http://www.w3.org/ns/shacl#property> <http://shaclshapes.org/instanceTypeCatShapeProperty> ;
                  <http://www.w3.org/ns/shacl#targetClass> <http://xmlns.com/foaf/0.1/Cat> .""");

        assertEquals(diffManager.diffShapeGenerator.editedNodeShapes.get(0).shapeAsTextOld.replace("\r",""), """
                @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .

                <http://shaclshapes.org/CatShape> rdf:type <http://www.w3.org/ns/shacl#NodeShape> ;
                  <http://www.w3.org/ns/shacl#property> <http://shaclshapes.org/instanceTypeCatShapeProperty> ;
                  <http://www.w3.org/ns/shacl#targetClass> <http://xmlns.com/foaf/0.1/Cat> .""");
    }

    @Test
    public void testEditedPropertyShapes() {
        var content = """
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://example.org/color> "orange" .
                <http://example.org/orangeCat> <http://example.org/color> "10"^^<http://www.w3.org/2001/XMLSchema#integer> .
                """;

        var contentAdded = "";
        var contentDeleted = "<http://example.org/orangeCat> <http://example.org/color> \"10\"^^<http://www.w3.org/2001/XMLSchema#integer> .\n";

        String filePath = generateFile(content);
        String addedPath = generateFile(contentAdded);
        String deletedPath = generateFile(contentDeleted);
        DiffManager diffManager = new DiffManager();
        diffManager.run("People", filePath, addedPath, deletedPath, "");

        assertEquals(diffManager.diffShapeGenerator.editPropertyShapes.size(),1);
        assertEquals(diffManager.diffShapeGenerator.editedNodeShapes.size(),0);

        assertEquals(diffManager.diffShapeGenerator.editPropertyShapes.get(0).shapeName,"http://shaclshapes.org/colorCatShapeProperty");
        assertEquals(diffManager.diffShapeGenerator.editPropertyShapes.get(0).shapeAsTextNew.replace("\r",""), """
                @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
                @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

                <http://shaclshapes.org/colorCatShapeProperty> rdf:type <http://www.w3.org/ns/shacl#PropertyShape> ;
                  <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#Literal> ;
                  <http://www.w3.org/ns/shacl#datatype> xsd:string ;
                  <http://www.w3.org/ns/shacl#minCount> 1 ;
                  <http://www.w3.org/ns/shacl#path> <http://example.org/color> .""");

        assertEquals(diffManager.diffShapeGenerator.editPropertyShapes.get(0).shapeAsTextOld.replace("\r",""), """
                @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
                @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

                <http://shaclshapes.org/colorCatShapeProperty> rdf:type <http://www.w3.org/ns/shacl#PropertyShape> ;
                  <http://www.w3.org/ns/shacl#minCount> 1 ;
                  <http://www.w3.org/ns/shacl#or> ( [
                    <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#Literal> ;
                    <http://www.w3.org/ns/shacl#datatype> xsd:integer ;
                  ] [
                    <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#Literal> ;
                    <http://www.w3.org/ns/shacl#datatype> xsd:string ;
                  ] ) ;\s
                  <http://www.w3.org/ns/shacl#path> <http://example.org/color> .""");
    }

    @Test
    public void propertyShapeMinCountRemoval() {
        var content = """
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/blackCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://example.org/color> "orange" .
                <http://example.org/blackCat> <http://example.org/color> "black" .
                """;

        var contentAdded = "";
        var contentDeleted = "<http://example.org/blackCat> <http://example.org/color> \"black\" .\n";

        String filePath = generateFile(content);
        String addedPath = generateFile(contentAdded);
        String deletedPath = generateFile(contentDeleted);
        DiffManager diffManager = new DiffManager();
        diffManager.run("People", filePath, addedPath, deletedPath, "");

        assertEquals(diffManager.diffShapeGenerator.editPropertyShapes.size(),1);

        assertEquals(diffManager.diffShapeGenerator.editPropertyShapes.get(0).shapeName,"http://shaclshapes.org/colorCatShapeProperty");
        assertEquals(diffManager.diffShapeGenerator.editPropertyShapes.get(0).shapeAsTextNew.replace("\r",""), """
                @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
                @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

                <http://shaclshapes.org/colorCatShapeProperty> rdf:type <http://www.w3.org/ns/shacl#PropertyShape> ;
                  <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#Literal> ;
                  <http://www.w3.org/ns/shacl#datatype> xsd:string ;
                  <http://www.w3.org/ns/shacl#path> <http://example.org/color> .""");

        assertEquals(diffManager.diffShapeGenerator.editPropertyShapes.get(0).shapeAsTextOld.replace("\r",""), """
                @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
                @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

                <http://shaclshapes.org/colorCatShapeProperty> rdf:type <http://www.w3.org/ns/shacl#PropertyShape> ;
                  <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#Literal> ;
                  <http://www.w3.org/ns/shacl#datatype> xsd:string ;
                  <http://www.w3.org/ns/shacl#minCount> 1 ;
                  <http://www.w3.org/ns/shacl#path> <http://example.org/color> .""");
    }

    @Test
    public void testCommaDotReplacement() {
        String shape = """
                <http://shaclshapes.org/colorCatShapeProperty> rdf:type <http://www.w3.org/ns/shacl#PropertyShape> ;
                  <http://shaclshapes.org/confidence> 3,3333E-1 ;
                  <http://shaclshapes.org/support> "1"^^xsd:int ;
                  <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#Literal> ;
                  <http://www.w3.org/ns/shacl#datatype> xsd:string ;
                  <http://www.w3.org/ns/shacl#path> <http://example.org/color> .""";
        var newShape = RegexUtils.replaceCommaWithDotInConfidenceValues(shape);
        assertEquals(newShape, """
                <http://shaclshapes.org/colorCatShapeProperty> rdf:type <http://www.w3.org/ns/shacl#PropertyShape> ;
                  <http://shaclshapes.org/confidence> 3.3333E-1 ;
                  <http://shaclshapes.org/support> "1"^^xsd:int ;
                  <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#Literal> ;
                  <http://www.w3.org/ns/shacl#datatype> xsd:string ;
                  <http://www.w3.org/ns/shacl#path> <http://example.org/color> .""");
    }

    @Test
    public void testEditedNodeShapesNoDifference()
    {
        var content = "<http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .\n";

        var contentAdded = "<http://example.org/orangeCat> <http://example.org/color> \"orange\" .\n";
        var contentDeleted = "";
        String filePath = generateFile(content);
        String addedPath = generateFile(contentAdded);
        String deletedPath = generateFile(contentDeleted);

        DiffManager diffManager = new DiffManager();
       diffManager.run("People", filePath, addedPath, deletedPath, "");

        assertEquals(diffManager.diffShapeGenerator.editedNodeShapes.size(),1);
        assertEquals(diffManager.diffShapeGenerator.editedNodeShapes.get(0).shapeName,"http://shaclshapes.org/CatShape");
        assertEquals(diffManager.diffShapeGenerator.editedNodeShapes.get(0).shapeAsTextNew.replace("\r",""), """
                @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .

                <http://shaclshapes.org/CatShape> rdf:type <http://www.w3.org/ns/shacl#NodeShape> ;
                  <http://www.w3.org/ns/shacl#property> <http://shaclshapes.org/colorCatShapeProperty> ;
                  <http://www.w3.org/ns/shacl#property> <http://shaclshapes.org/instanceTypeCatShapeProperty> ;
                  <http://www.w3.org/ns/shacl#targetClass> <http://xmlns.com/foaf/0.1/Cat> .""");

        assertEquals(diffManager.diffShapeGenerator.editedNodeShapes.get(0).shapeAsTextOld.replace("\r",""), """
                @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .

                <http://shaclshapes.org/CatShape> rdf:type <http://www.w3.org/ns/shacl#NodeShape> ;
                  <http://www.w3.org/ns/shacl#property> <http://shaclshapes.org/instanceTypeCatShapeProperty> ;
                  <http://www.w3.org/ns/shacl#targetClass> <http://xmlns.com/foaf/0.1/Cat> .""");
    }

    @Test
    public void testDeletePSCheckDeletedNodeShapes()
    {
        var content = """
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://example.org/color> "orange" .
                """;

        var contentDeleted = "<http://example.org/orangeCat> <http://example.org/color> \"orange\" .\n";
        var contentAdded = "";
        String filePath = generateFile(content);
        String addedPath = generateFile(contentAdded);
        String deletedPath = generateFile(contentDeleted);

        DiffManager diffManager = new DiffManager();
        diffManager.run("People", filePath, addedPath, deletedPath, "");

        assertEquals(diffManager.diffShapeGenerator.editedNodeShapes.size(),1);
        assertEquals(diffManager.diffShapeGenerator.editedNodeShapes.get(0).shapeName,"http://shaclshapes.org/CatShape");
    }

    @Test
    public void supportForShapes()
    {
        var content = """
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/blackCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://example.org/color> "orange" .
                <http://example.org/blackCat> <http://example.org/color> "black" .
                """;

        var contentAdded = """
                <http://example.org/greyCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/greyCat> <http://example.org/color> "grey" .
                """;

        var contentDeleted = "";
        String filePath = generateFile(content);
        String addedPath = generateFile(contentAdded);
        String deletedPath = generateFile(contentDeleted);

        DiffManager diffManager = new DiffManager();
        var extractedShapes = diffManager.run("People", filePath, addedPath, deletedPath, "");

        assertEquals(extractedShapes.nodeShapes.size(), 1);
        assertEquals(3, (int) extractedShapes.nodeShapes.get(0).support);
        assertEquals(3, (int)extractedShapes.nodeShapes.get(0).propertyShapes.stream().filter(ps -> ps.getIri().toString().contains("color")).findFirst().get().support);
    }

    @Test
    @Category(BugWithSupportTests.class)
    public void deleteOneOfMultipleTypesWithFurtherClassDefinition() throws IOException {
        var content = """
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/AnotherCat> .
                <http://xmlns.com/foaf/0.1/Cat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Class> .
                <http://xmlns.com/foaf/0.1/AnotherCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#AnotherClass> .
                """;

        var contentDeleted = """
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                """;

        var contentAdded = "";

        var contentNew = """
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/AnotherCat> .
                <http://xmlns.com/foaf/0.1/Cat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Class> .
                <http://xmlns.com/foaf/0.1/AnotherCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#AnotherClass> .
                """;
        testQseOutput(content, contentNew, contentAdded, contentDeleted);
    }

    @Test
    @Category(BugWithSupportTests.class)
    public void deleteOneOfMultipleTypesWithFurtherClassDefinitionDifferentClasses() throws IOException {
        var content = """
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/AnotherCat> .
                <http://xmlns.com/foaf/0.1/Cat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Class> .
                <http://xmlns.com/foaf/0.1/AnotherCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Class> .
                """;

        var contentDeleted = """
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                """;

        var contentAdded = "";

        var contentNew = """
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/AnotherCat> .
                <http://xmlns.com/foaf/0.1/Cat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Class> .
                <http://xmlns.com/foaf/0.1/AnotherCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Class> .
                """;
        testQseOutput(content, contentNew, contentAdded, contentDeleted);
    }

    @Test
    @Category(BugWithSupportTests.class)
    public void deleteOneOfMultipleStatementsWithoutFurtherClassDefinition() throws IOException {
        var content = """
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/AnotherCat> .
                """;

        var contentDeleted = """
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                """;

        var contentAdded = "";

        var contentNew = """
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/AnotherCat> .
                """;
        testQseOutput(content, contentNew, contentAdded, contentDeleted);
    }

    @Test
    @Category(BugWithSupportTests.class)
    public void deleteOneOfMultipleStatementsWithoutFurtherClassDefinitionMultipleTypes() throws IOException {
        var content = """
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/AnotherCat> .
                <http://example.org/blackCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/blackCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/AnotherCat> .
                """;

        var contentDeleted = """
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/blackCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                """;

        var contentAdded = "";

        var contentNew = """
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/AnotherCat> .
                <http://example.org/blackCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/AnotherCat> .
                """;
        testQseOutput(content, contentNew, contentAdded, contentDeleted);
    }

    @Test
    @Category(BugWithSupportTests.class)
    public void deleteOneOfMultipleStatementsWithoutFurtherClassDefinitionMultipleNoTypes() throws IOException {
        var content = """
                <http://example.org/orangeCat> <http://example.org/likes> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://example.org/likes> <http://xmlns.com/foaf/0.1/AnotherCat> .
                <http://example.org/blackCat> <http://example.org/likes> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/blackCat> <http://example.org/likes> <http://xmlns.com/foaf/0.1/AnotherCat> .
                """;

        var contentDeleted = """
                <http://example.org/orangeCat> <http://example.org/likes> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/blackCat> <http://example.org/likes>> <http://xmlns.com/foaf/0.1/Cat> .
                """;

        var contentAdded = "";

        var contentNew = """
                <http://example.org/orangeCat> <http://example.org/likes> <http://xmlns.com/foaf/0.1/AnotherCat> .
                <http://example.org/blackCat> <http://example.org/likes> <http://xmlns.com/foaf/0.1/AnotherCat> .
                """;
        testQseOutput(content, contentNew, contentAdded, contentDeleted);
    }

    @Test
    @Category(BugWithSupportTests.class)
    public void deleteOneOfMultipleStatementsWithoutFurtherClassDefinitionMultipleTimes() throws IOException {
        var content = """
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/AnotherCat> .
                <http://example.org/blackCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/blackCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/AnotherCat> .
                """;

        var contentDeleted = """
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/AnotherCat> .
                """;

        var contentAdded = "";

        var contentNew = """
                <http://example.org/blackCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/blackCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/AnotherCat> .""";
        testQseOutput(content, contentNew, contentAdded, contentDeleted);
    }

    @Test
    @Category(BugWithSupportTests.class)
    public void deleteOneOfMultipleStatementsWithoutFurtherClassDefinitionMultipleTimesWithoutType() throws IOException {
        var content = """
                <http://example.org/orangeCat> <http://example.org/likes> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://example.org/likes> <http://xmlns.com/foaf/0.1/AnotherCat> .
                <http://example.org/blackCat> <http://example.org/likes> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/blackCat> <http://example.org/likes> <http://xmlns.com/foaf/0.1/AnotherCat> .
                """;

        var contentDeleted = """
                <http://example.org/orangeCat> <http://example.org/likes> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://example.org/likes> <http://xmlns.com/foaf/0.1/AnotherCat> .
                """;

        var contentAdded = "";

        var contentNew = """
                <http://example.org/blackCat> <http://example.org/likes> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/blackCat> <http://example.org/likes> <http://xmlns.com/foaf/0.1/AnotherCat> .""";
        testQseOutput(content, contentNew, contentAdded, contentDeleted);
    }

    @Test
    @Category(BugWithSupportTests.class)
    public void deleteOneOfMultipleStatementsWithoutFurtherClassDefinitionCats() throws IOException {
        var content = """
                <http://example.org/orangeCat> <http://example.org/likes> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://example.org/likes> <http://xmlns.com/foaf/0.1/AnotherCat> .
                """;

        var contentDeleted = """
                <http://example.org/orangeCat> <http://example.org/likes> <http://xmlns.com/foaf/0.1/Cat> .
                """;

        var contentAdded = "";

        var contentNew = """
                <http://example.org/orangeCat> <http://example.org/likes> <http://xmlns.com/foaf/0.1/AnotherCat> .
                """;
        testQseOutput(content, contentNew, contentAdded, contentDeleted);
    }

    @Test
    @Category(BugWithSupportTests.class)
    public void addStatementForMultipleTypes() throws IOException {
        var content = """
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/AnotherCat> .
                """;

        var contentDeleted = """
                """;

        var contentAdded = """
                <http://example.org/someEntity> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Entity> .
                <http://example.org/orangeCat> <http://example.org/likes> <http://xmlns.com/foaf/0.1/someEntity> .
                """;

        var contentNew = """
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/AnotherCat> .
                <http://example.org/someEntity> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Entity> .
                <http://example.org/orangeCat> <http://example.org/likes> <http://xmlns.com/foaf/0.1/someEntity> .
                """;
        testQseOutput(content, contentNew, contentAdded, contentDeleted);
    }

    @Test
    @Category(BugWithSupportTests.class)
    public void replaceSomethingCheckSupport() throws IOException {
        var content = """
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/someEntity> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Entity> .
                <http://example.org/orangeCat> <http://example.org/likes> <http://example.org/someEntity> .
                """;

        var contentDeleted = """
                <http://example.org/someEntity> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Entity> .
                <http://example.org/orangeCat> <http://example.org/likes> <http://example.org/someEntity> .
                """;

        var contentAdded = """
                <http://example.org/someEntity2> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Entity> .
                <http://example.org/orangeCat> <http://example.org/likes> <http://example.org/someEntity2> .
                """;

        var contentNew = """
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/someEntity2> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Entity> .
                <http://example.org/orangeCat> <http://example.org/likes> <http://example.org/someEntity2> .
                """;
        testQseOutput(content, contentNew, contentAdded, contentDeleted);
    }

    @Test
    @Category(BugWithSupportTests.class)
    public void addSomethingCheckSupport() throws IOException {
        var content = """
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/someEntity> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Entity> .
                <http://example.org/orangeCat> <http://example.org/likes> <http://example.org/someEntity> .
                """;

        var contentDeleted = """
                """;

        var contentAdded = """
                <http://example.org/someEntity2> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Entity> .
                <http://example.org/orangeCat> <http://example.org/likes> <http://example.org/someEntity2> .
                """;

        var contentNew = """
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/someEntity> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Entity> .
                <http://example.org/orangeCat> <http://example.org/likes> <http://example.org/someEntity> .
                <http://example.org/someEntity2> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Entity> .
                <http://example.org/orangeCat> <http://example.org/likes> <http://example.org/someEntity2> .
                """;
        testQseOutput(content, contentNew, contentAdded, contentDeleted);
    }

    @Test
    public void checkIfNodeShapeIsAmongDeletedShapes() throws IOException {
        var content = """
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://example.org/likes> <http://example.org/someEntity> .
                <http://example.org/blackCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/OtherCat> .
                <http://example.org/blackCat> <http://example.org/likes> <http://example.org/someEntity2> .
                """;

        var contentDeleted = """
                <http://example.org/blackCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/OtherCat> .
                <http://example.org/blackCat> <http://example.org/likes> <http://example.org/someEntity2> .
                """;

        var contentAdded = """
                """;

        var contentNew = """
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://example.org/likes> <http://example.org/someEntity> .
                """;
        var diffExtractor = testQseOutput(content, contentNew, contentAdded, contentDeleted);
        DiffShapeGenerator diffShapeGenerator = new DiffShapeGenerator(diffExtractor);
        diffShapeGenerator.computeDeletedShapes();
        assertFalse(diffShapeGenerator.checkIfTargetClassIsAmongDeletedShapes("http://xmlns.com/foaf/0.1/Cat"));
        assertTrue(diffShapeGenerator.checkIfTargetClassIsAmongDeletedShapes("http://xmlns.com/foaf/0.1/OtherCat"));

    }

    @Test
    public void pruningNodeShapeAdded()
    {
        var content = """
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://example.org/color> "orange" .
                """;

        var contentAdded = """
                <http://example.org/greyCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/greyCat> <http://example.org/color> "grey" .
                <http://example.org/blackCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/blackCat> <http://example.org/color> "black" .
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                """;

        var contentDeleted = "";
        String filePath = generateFile(content);
        String addedPath = generateFile(contentAdded);
        String deletedPath = generateFile(contentDeleted);

        DiffManager diffManager = new DiffManager();
        diffManager.support = 2;
        var extractedShapes = diffManager.run("People", filePath, addedPath, deletedPath, "");

        assertEquals(extractedShapes.nodeShapes.size(), 1);
        assertEquals(3, (int) extractedShapes.nodeShapes.get(0).support);
        assertEquals(3, (int)extractedShapes.nodeShapes.get(0).propertyShapes.stream().filter(ps -> ps.getIri().toString().contains("color")).findFirst().get().support);
        assertEquals(1,diffManager.diffShapeGenerator.addedNodeShapeNames.size());
        assertEquals(2, diffManager.diffShapeGenerator.addedPropertyShapeNames.size());
    }

    @Test
    public void pruningPropertyShapeAdded()
    {
        var contentAdded = """
                <http://example.org/blackCat> <http://example.org/color> "black" .
                <http://example.org/greyCat> <http://example.org/color> "grey" .
                """;

        var content = """
                <http://example.org/greyCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/blackCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://example.org/color> "orange" .
                <http://example.org/alice> <http://example.org/name> "Alice" .
                """;

        var contentDeleted = "";
        String filePath = generateFile(content);
        String addedPath = generateFile(contentAdded);
        String deletedPath = generateFile(contentDeleted);

        DiffManager diffManager = new DiffManager();
        diffManager.support = 2;
        var extractedShapes = diffManager.run("People", filePath, addedPath, deletedPath, "");

        assertEquals(extractedShapes.nodeShapes.size(), 1);
        assertEquals(3, (int) extractedShapes.nodeShapes.get(0).support);
        assertEquals(3, (int)extractedShapes.nodeShapes.get(0).propertyShapes.stream().filter(ps -> ps.getIri().toString().contains("color")).findFirst().get().support);
        assertEquals(0,diffManager.diffShapeGenerator.addedNodeShapeNames.size());
        assertEquals(1, diffManager.diffShapeGenerator.addedPropertyShapeNames.size());
        var file = RegexUtils.getFileAsString(extractedShapes.fileContentPath);
        assertFalse(file.contains("Person"));
    }

    @Test
    public void pruningPropertyShapeAddedWithConfidence()
    {
        var contentAdded = """
                <http://example.org/blackCat> <http://example.org/color> "black" .
                <http://example.org/greyCat> <http://example.org/color> "grey" .
                """;

        var content = """
                <http://example.org/greyCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/blackCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://example.org/color> "orange" .
                <http://example.org/alice> <http://example.org/name> "Alice" .
                """;

        var contentDeleted = "";
        String filePath = generateFile(content);
        String addedPath = generateFile(contentAdded);
        String deletedPath = generateFile(contentDeleted);

        DiffManager diffManager = new DiffManager();
        diffManager.confidence = 0.5;
        var extractedShapes = diffManager.run("People", filePath, addedPath, deletedPath, "");

        assertEquals(extractedShapes.nodeShapes.size(), 2);
        assertEquals(3, (int) extractedShapes.nodeShapes.get(0).support);
        assertEquals(3, (int)extractedShapes.nodeShapes.get(0).propertyShapes.stream().filter(ps -> ps.getIri().toString().contains("color")).findFirst().get().support);
        assertEquals(1.0, extractedShapes.nodeShapes.get(0).propertyShapes.stream().filter(ps -> ps.getIri().toString().contains("color")).findFirst().get().confidence, 0.0);
        assertEquals(0,diffManager.diffShapeGenerator.addedNodeShapeNames.size());
        assertEquals(1, diffManager.diffShapeGenerator.addedPropertyShapeNames.size());
    }

    @Test
    public void pruningNodeShapeDeleted()
    {
        var contentDeleted = """
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/blackCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                """;

        var content = """
                <http://example.org/greyCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/blackCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                """;

        var contentAdded = "";
        String filePath = generateFile(content);
        String addedPath = generateFile(contentAdded);
        String deletedPath = generateFile(contentDeleted);

        DiffManager diffManager = new DiffManager();
        diffManager.support = 2;
        var extractedShapes = diffManager.run("People", filePath, addedPath, deletedPath, "");
        assertEquals(1,diffManager.diffShapeGenerator.deletedNodeShapeNames.size());
        assertEquals(1, diffManager.diffShapeGenerator.deletedPropertyShapeNames.size());
        assertEquals(extractedShapes.nodeShapes.size(), 0);
        var file = RegexUtils.getFileAsString(extractedShapes.fileContentPath);
        assertFalse(file.contains("Cat"));
  }

    @Test
    public void pruningPropertyShapeDeleted()
    {
        var contentDeleted = """
                <http://example.org/blackCat> <http://example.org/color> "black" .
                <http://example.org/greyCat> <http://example.org/color> "grey" .
                """;

        var content = """
                <http://example.org/greyCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/blackCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://example.org/color> "orange" .
                <http://example.org/alice> <http://example.org/name> "Alice" .
                <http://example.org/blackCat> <http://example.org/color> "black" .
                <http://example.org/greyCat> <http://example.org/color> "grey" .
                """;

        var contentAdded = "";
        String filePath = generateFile(content);
        String addedPath = generateFile(contentAdded);
        String deletedPath = generateFile(contentDeleted);

        DiffManager diffManager = new DiffManager();
        diffManager.support = 2;
        var extractedShapes = diffManager.run("People", filePath, addedPath, deletedPath, "");
        assertEquals(0,diffManager.diffShapeGenerator.deletedNodeShapeNames.size());
        assertEquals(1, diffManager.diffShapeGenerator.deletedPropertyShapeNames.size());
        assertTrue(diffManager.diffShapeGenerator.deletedPropertyShapeNames.get(0).contains("color"));
        var file = RegexUtils.getFileAsString(extractedShapes.fileContentPath);
        assertFalse(file.contains("color"));
        assertFalse(file.contains("Person"));
    }

    @Test
    public void nodePropertyWhenShapeIsEdited() {
        var contentDeleted = """
                <http://example.org/alice> <http://example.org/knows> <http://example.org/orangeCat> .
                """;

        var content = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/someDog> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Dog> .
                <http://example.org/alice> <http://example.org/knows> <http://example.org/orangeCat> .
                <http://example.org/alice> <http://example.org/knows> <http://example.org/someDog> .
                """;

        var contentAdded = "";
        String filePath = generateFile(content);
        String addedPath = generateFile(contentAdded);
        String deletedPath = generateFile(contentDeleted);


        DiffManager diffManager = new DiffManager();
        var extractedShapes = diffManager.run("People", filePath, addedPath, deletedPath, "");
        var shape = "http://shaclshapes.org/knowsPersonShapeProperty";

        var text = RegexUtils.getShapeAsString(shape, extractedShapes.getFileAsString(false));
        var textExpected = """
                
                <http://shaclshapes.org/knowsPersonShapeProperty> rdf:type <http://www.w3.org/ns/shacl#PropertyShape> ;
                  <http://shaclshapes.org/confidence> 1E0 ;
                  <http://shaclshapes.org/support> "1"^^xsd:int ;
                  <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#IRI> ;
                  <http://www.w3.org/ns/shacl#class> <http://xmlns.com/foaf/0.1/Dog> ;
                  <http://www.w3.org/ns/shacl#minCount> 1 ;
                  <http://www.w3.org/ns/shacl#node> <http://shaclshapes.org/DogShape> ;
                  <http://www.w3.org/ns/shacl#path> <http://example.org/knows> .
                """;

        assertEquals(textExpected, text);
    }

    @Test
    public void deleteMinCount() {
        var contentDeleted = """
                <http://example.org/alice> <http://example.org/name> "Alice" .
                """;

        var content = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/bob> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/alice> <http://example.org/name> "Alice" .
                <http://example.org/bob> <http://example.org/name> "Bob" .
                """;

        var contentAdded = "";
        String filePath = generateFile(content);
        String addedPath = generateFile(contentAdded);
        String deletedPath = generateFile(contentDeleted);


        DiffManager diffManager = new DiffManager();
        var extractedShapes = diffManager.run("People", filePath, addedPath, deletedPath, "");
        var shape = "http://shaclshapes.org/namePersonShapeProperty";

        var text = RegexUtils.getShapeAsString(shape, extractedShapes.getFileAsString(false));
        var textExpected = """
                
                <http://shaclshapes.org/namePersonShapeProperty> rdf:type <http://www.w3.org/ns/shacl#PropertyShape> ;
                  <http://shaclshapes.org/confidence> 5E-1 ;
                  <http://shaclshapes.org/support> "1"^^xsd:int ;
                  <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#Literal> ;
                  <http://www.w3.org/ns/shacl#datatype> xsd:string ;
                  <http://www.w3.org/ns/shacl#path> <http://example.org/name> .
                """;

        assertEquals(textExpected, text);
    }

    @Test
    public void addMultiplePropertyShapeCheckNSText() {
        var contentDeleted = "";

        var content = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                """;

        var contentAdded = """
                <http://example.org/alice> <http://example.org/name> "Alice" .
                <http://example.org/alice> <http://example.org/shortName> "Al" .
                """;
        String filePath = generateFile(content);
        String addedPath = generateFile(contentAdded);
        String deletedPath = generateFile(contentDeleted);


        DiffManager diffManager = new DiffManager();
        var extractedShapes = diffManager.run("People", filePath, addedPath, deletedPath, "");
        var shape = "http://shaclshapes.org/PersonShape";

        var text = RegexUtils.getShapeAsString(shape, extractedShapes.getFileAsString(false));
        var textExpected = """
                
                <http://shaclshapes.org/PersonShape> rdf:type <http://www.w3.org/ns/shacl#NodeShape> ;
                  <http://shaclshapes.org/support> "1"^^xsd:int ;
                  <http://www.w3.org/ns/shacl#property> <http://shaclshapes.org/instanceTypePersonShapeProperty> ;
                  <http://www.w3.org/ns/shacl#property> <http://shaclshapes.org/namePersonShapeProperty> ;
                  <http://www.w3.org/ns/shacl#property> <http://shaclshapes.org/shortNamePersonShapeProperty> ;
                  <http://www.w3.org/ns/shacl#targetClass> <http://xmlns.com/foaf/0.1/Person> .
                """;

        assertEquals(textExpected, text);
    }

    @Test
    public void nodePropertyWhenShapeIsEditedWithOrItems() {
        var contentDeleted = "";

        var content = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/someHorse> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Horse> .
                <http://example.org/someHorse2> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Horse> .
                <http://example.org/someDog> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Dog> .
                <http://example.org/alice> <http://example.org/knows> <http://example.org/orangeCat> .
                <http://example.org/alice> <http://example.org/knows> <http://example.org/someDog> .
                <http://example.org/alice> <http://example.org/knows> <http://example.org/someHorse> .
                """;

        var contentNew = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/bob> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/someHorse> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Horse> .
                <http://example.org/someHorse2> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Horse> .
                <http://example.org/someDog> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Dog> .
                <http://example.org/alice> <http://example.org/knows> <http://example.org/orangeCat> .
                <http://example.org/alice> <http://example.org/knows> <http://example.org/someDog> .
                <http://example.org/alice> <http://example.org/knows> <http://example.org/someHorse> .
                <http://example.org/alice> <http://example.org/knows> <http://example.org/someHorse2> .
                <http://example.org/bob> <http://example.org/knows> <http://example.org/someHorse> .
                <http://example.org/bob> <http://example.org/knows> <http://example.org/someHorse2> .
                """;

        var contentAdded = """
                <http://example.org/bob> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/alice> <http://example.org/knows> <http://example.org/someHorse2> .
                <http://example.org/bob> <http://example.org/knows> <http://example.org/someHorse> .
                <http://example.org/bob> <http://example.org/knows> <http://example.org/someHorse2> .
                """;
        String filePath = generateFile(content);
        String addedPath = generateFile(contentAdded);
        String deletedPath = generateFile(contentDeleted);
        String newFilePath = generateFile(contentNew);

        Parser parserV3 = new Parser(newFilePath, 3, 10, instanceTypeProperty);
        runParser(parserV3);

        DiffManager diffManager = new DiffManager();
        var extractedShapes = diffManager.run("People", filePath, addedPath, deletedPath, "");
        var shape = "http://shaclshapes.org/knowsPersonShapeProperty";

        var text = RegexUtils.getShapeAsStringFormattedFromFile(shape, extractedShapes.getFileAsString(false), extractedShapes.prefixLines);
        var textExpected = """
                @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
                @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

                <http://shaclshapes.org/knowsPersonShapeProperty> rdf:type <http://www.w3.org/ns/shacl#PropertyShape> ;
                  <http://www.w3.org/ns/shacl#minCount> 1 ;
                  <http://www.w3.org/ns/shacl#node> <http://shaclshapes.org/HorseShape> ;
                  <http://www.w3.org/ns/shacl#or> ( [
                    <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#IRI> ;
                    <http://www.w3.org/ns/shacl#class> <http://xmlns.com/foaf/0.1/Cat> ;
                  ] [
                    <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#IRI> ;
                    <http://www.w3.org/ns/shacl#class> <http://xmlns.com/foaf/0.1/Dog> ;
                  ] [
                    <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#IRI> ;
                    <http://www.w3.org/ns/shacl#class> <http://xmlns.com/foaf/0.1/Horse> ;
                  ] ) ;   
                  <http://www.w3.org/ns/shacl#path> <http://example.org/knows> .""";

        assertEquals(textExpected.replaceAll(" ", "").replaceAll("\r", ""), text.replaceAll(" ", "").replaceAll("\r", ""));
    }

    @Test
    public void pruningPropertyShapeDeletedWithConfidence()
    {
        var contentDeleted = """
                <http://example.org/blackCat> <http://example.org/color> "black" .
                <http://example.org/greyCat> <http://example.org/color> "grey" .
                """;

        var content = """
                <http://example.org/greyCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/blackCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://example.org/color> "orange" .
                <http://example.org/alice> <http://example.org/name> "Alice" .
                <http://example.org/blackCat> <http://example.org/color> "black" .
                <http://example.org/greyCat> <http://example.org/color> "grey" .
                """;

        var contentAdded = "";
        String filePath = generateFile(content);
        String addedPath = generateFile(contentAdded);
        String deletedPath = generateFile(contentDeleted);

        DiffManager diffManager = new DiffManager();
        diffManager.confidence = 0.5;
        var extractedShapes = diffManager.run("People", filePath, addedPath, deletedPath, "");

        assertEquals(extractedShapes.nodeShapes.size(), 2);
        assertEquals(3, (int) extractedShapes.nodeShapes.get(0).support);
        assertEquals(1, extractedShapes.nodeShapes.get(0).propertyShapes.size());
        assertEquals(0,diffManager.diffShapeGenerator.deletedNodeShapeNames.size());
        assertEquals(1, diffManager.diffShapeGenerator.deletedPropertyShapeNames.size());
        assertTrue(diffManager.diffShapeGenerator.deletedPropertyShapeNames.get(0).contains("color"));
    }

    @Test
    public void testFilm12() {
        Main.datasetName = "Film";
        int support = 0;
        double confidence = 0.0;

        var tempFile = Paths.get(System.getProperty("user.dir")+"\\resources\\defaultGraphs\\film-diff\\film.nt");


        var tempFileNew = Paths.get(System.getProperty("user.dir")+"\\resources\\defaultGraphs\\film-diff\\film2.nt");
        var tempFileAdded = Paths.get(System.getProperty("user.dir")+"\\resources\\defaultGraphs\\film-diff\\film1film2added.nt");

        var tempFileDeleted = Paths.get(System.getProperty("user.dir")+"\\resources\\defaultGraphs\\film-diff\\film1film2deleted.nt");

        Parser parser = new Parser(tempFile.toAbsolutePath().toString(), 3, 10, instanceTypeProperty);
        runParser(parser);
        ExtractedShapes tmp = new ExtractedShapes();
        tmp.setNodeShapes(parser.shapesExtractor.getNodeShapes(), false);
        DiffExtractor diffExtractor = new DiffExtractor(tempFileAdded.toAbsolutePath().toString(),tempFileDeleted.toAbsolutePath().toString(),parser,support, confidence, parser.shapesExtractor.getOutputFileAddress(), tmp.getNodeShapes());
        diffExtractor.extractFromFile();

        var oldDataSetName = Main.datasetName;
        var oldOutputPath = Main.outputFilePath;
        Main.datasetName = Main.datasetName+"_Full";
        Main.outputFilePath = Main.outputFilePath+ "Full" + File.separator;
        Parser parserV3 = new Parser(tempFileNew.toAbsolutePath().toString(), 3, 10, instanceTypeProperty);
        parserV3.setStringEncoder(parser.getStringEncoder());
        runParser(parserV3);
        Main.datasetName = oldDataSetName;
        Main.outputFilePath = oldOutputPath;

        assertEquals(parser.classEntityCount, parserV3.classEntityCount);
        assertTrue(areMapsEqual(parser.entityDataHashMap, parserV3.entityDataHashMap));
        assertTrue(areMapsEqual(parser.classToPropWithObjTypes, parserV3.classToPropWithObjTypes));
        assertTrue(areMapsEqual(parser.statsComputer.getShapeTripletSupport(), parserV3.statsComputer.getShapeTripletSupport()));
    }

    static String generateFile(String content) {
        Path tempFile;
        try {
            var randomString =  RandomStringUtils.random(5, true, true);
            tempFile = Files.createTempFile("QSEDiff"+randomString,".nt");
            Files.write(tempFile, content.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
            return tempFile.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void runParser(Parser parser) {
        parser.entityExtraction();
        parser.entityConstraintsExtraction();
        parser.computeSupportConfidence();
        parser.extractSHACLShapes(false, Main.qseFromSpecificClasses);
    }

    public static <K, V> boolean areMapsEqual(Map<K, V> map1, Map<K, V> map2) {
        if (map1.size() != map2.size()) {
            return false;
        }

        for (Map.Entry<K, V> entry : map1.entrySet()) {
            K key = entry.getKey();
            V value = entry.getValue();
            if (!map2.containsKey(key)) {
                return false;
            }
            if(value instanceof EntityData) {
                var value1 = (EntityData)value;
                var value2 = (EntityData) map2.get(key);
                if(!value1.classTypes.equals(value2.classTypes)) {
                    System.out.println("EntityData classTypes for " + key + " not equal");
                    return false;
                }
                if(!areMapsEqual(value1.propertyConstraintsMap, value2.propertyConstraintsMap)) {
                    return false;
                }
            }
            else if(value instanceof EntityData.PropertyData) {
                var value1 = (EntityData.PropertyData)value;
                var value2 = (EntityData.PropertyData) map2.get(key);
                if(!value1.objTypes.equals(value2.objTypes)) {
                    System.out.println("PropertyData objTypes " + value1.objTypes + " not equal");
                    return false;
                }
                if(value1.count!=value2.count) {
                    System.out.println("PropertyData count" + value1.count + " not equal");
                    return false;
                }
                if(!areMapsEqual(value1.objTypesCount, value2.objTypesCount)) {
                    return false;
                }
            }
            else if(value instanceof SupportConfidence) {
                var value1 = (SupportConfidence)value;
                var value2 = (SupportConfidence)map2.get(key);
                if(!Objects.equals(value1.getSupport(), value2.getSupport()))
                    return false;
                if(!Objects.equals(value1.getConfidence(), value2.getConfidence()))
                    return false;
            }
            else if (!value.equals(map2.get(key))) {
                return false;
            }
        }

        return true;
    }

}
