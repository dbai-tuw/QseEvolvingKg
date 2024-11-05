package shacldiffextractor.test;

import cs.Main;
import cs.qse.filebased.Parser;
import cs.utils.FilesUtil;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.junit.Before;
import org.junit.Test;
import shacldiffextractor.ConfigVersionElement;
import shacldiffextractor.ShaclDiffExtractor;
import shacldiffextractor.logic.DiffExtractor;
import shacldiffextractor.logic.DiffManager;
import shape_comparator.data.ExtractedShapes;
import sparqlshapechecker.comparator.ComparatorUtils;
import sparqlshapechecker.comparator.MetaComparator;
import shacldiffextractor.comparator.ShapeComparatorDiff;
import shacldiffextractor.comparator.ShapeComparatorQseFileBased;
import sparqlshapechecker.utils.ConfigManager;
import sparqlshapechecker.utils.RegexUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

//Tests are only used for debugging
public class ShaclDiffExtractorComparisonTests {
    public static final String logPath = System.getProperty("user.dir")+"\\Output\\compareLogsDiff\\";
    public static final String defaultGraphsPath = System.getProperty("user.dir") + "\\resources\\defaultGraphs\\";
    public String instanceTypeProperty = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>";

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

    @Before
    public void cleanUp() throws IOException {
        ShaclDiffExtractor.versionElements.clear();

        Files.walk(Paths.get(System.getProperty("user.dir")+"\\Output\\"))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                    }
                });
    }

    @Test
    public void peopleTest() {
        MetaComparator metaComparator = new MetaComparator();
        String dataSetName1 = "PeopleV1";
        String dataSetName2 = "PeopleV2";
        String pruningThresholds =  "{(0,0)}";
        var content = "<http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .\n";
        var contentAdded = "<http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .\n";
        var contentDeleted = "<http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .\n";
        var contentNew = "<http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .\n";

        String filePath = ShaclDiffExtractorUnitTests.generateFile(content);
        String addedPath = ShaclDiffExtractorUnitTests.generateFile(contentAdded);
        String deletedPath = ShaclDiffExtractorUnitTests.generateFile(contentDeleted);
        String newPath = ShaclDiffExtractorUnitTests.generateFile(contentNew);

        ShapeComparatorQseFileBased comparatorQseFileBased = new ShapeComparatorQseFileBased(filePath, newPath, dataSetName1, dataSetName2, logPath);
        metaComparator.diffQse = comparatorQseFileBased.doComparison(pruningThresholds);
        ShapeComparatorDiff comparatorDiff = new ShapeComparatorDiff(filePath, newPath, addedPath, deletedPath, dataSetName1, dataSetName2, logPath);
        metaComparator.diffAlgorithm = comparatorDiff.doFullComparison(pruningThresholds);
        System.out.println(metaComparator.compareAll());
        ComparatorUtils.exportComparisonToFile(logPath+dataSetName1+"_"+dataSetName2+ File.separator + "Meta", metaComparator.compareAll());

        var metaFilePath = logPath+"PeopleV1_PeopleV2\\MetaComparison.txt";
        var metaFileString = RegexUtils.getFileAsString(metaFilePath);
        assertFalse(metaFileString.contains("Count"));
    }

    @Test
    public void peopleTestAddPropertyShapes() {
        MetaComparator metaComparator = new MetaComparator();
        String dataSetName1 = "PeopleV1";
        String dataSetName2 = "PeopleV2";
        String pruningThresholds =  "{(0,0)}";
        var content = "<http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .\n";
        var contentAdded = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/orangeCat> <http://example.org/knows> <http://xmlns.com/foaf/0.1/somePerson> .";
                """;
        var contentDeleted = "";
        var contentNew = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://example.org/orangeCat> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Cat> .
                <http://example.org/orangeCat> <http://example.org/knows> <http://xmlns.com/foaf/0.1/somePerson> .";
                """;

        String filePath = ShaclDiffExtractorUnitTests.generateFile(content);
        String addedPath = ShaclDiffExtractorUnitTests.generateFile(contentAdded);
        String deletedPath = ShaclDiffExtractorUnitTests.generateFile(contentDeleted);
        String newPath = ShaclDiffExtractorUnitTests.generateFile(contentNew);

        ShapeComparatorQseFileBased comparatorQseFileBased = new ShapeComparatorQseFileBased(filePath, newPath, dataSetName1, dataSetName2, logPath);
        metaComparator.diffQse = comparatorQseFileBased.doComparison(pruningThresholds);
        ShapeComparatorDiff comparatorDiff = new ShapeComparatorDiff(filePath, newPath, addedPath, deletedPath, dataSetName1, dataSetName2, logPath);
        metaComparator.diffAlgorithm = comparatorDiff.doFullComparison(pruningThresholds);
        System.out.println(metaComparator.compareAll());
        ComparatorUtils.exportComparisonToFile(logPath+dataSetName1+"_"+dataSetName2+ File.separator + "Meta", metaComparator.compareAll());
        var metaFilePath = logPath+"PeopleV1_PeopleV2\\MetaComparison.txt";
        var metaFileString = RegexUtils.getFileAsString(metaFilePath);
        assertFalse(metaFileString.contains("Count"));
    }

    @Test
    public void peopleTestQse() {
        var filePath =  defaultGraphsPath+ "miniexample\\People1.nt";
        ShapeComparatorQseFileBased comparator = new ShapeComparatorQseFileBased(filePath, "", "PeopleV1", "","");
        comparator.runQseFirstTime("(0,0)",Paths.get(System.getProperty("user.dir")+"\\Output\\"));
        System.out.println();
    }

    @Test
    public void peopleTestMultipleVersionsWithMetaComparison() {
        ShaclDiffExtractor.filePathInitialVersion = defaultGraphsPath+ "miniexample\\People1.nt";
        ShaclDiffExtractor.pruningThresholds = "{(0,0)}";
        ShaclDiffExtractor.doMetaComparison = true;
        ShaclDiffExtractor.initialVersionName = "People1";
        ConfigVersionElement e1 = new ConfigVersionElement();
        e1.versionName = "People2";
        e1.filePathFullVersion = defaultGraphsPath+ "miniexample\\People2.nt";
        e1.filePathAdded = defaultGraphsPath+ "miniexample\\Diff\\People1People2Added.nt";
        e1.filePathDeleted = defaultGraphsPath+ "miniexample\\Diff\\People1People2Deleted.nt";
        ShaclDiffExtractor.versionElements.add(e1);
        ConfigVersionElement e2 = new ConfigVersionElement();
        e2.versionName = "People3";
        e2.filePathFullVersion = defaultGraphsPath+ "miniexample\\People3.nt";
        e2.filePathAdded = defaultGraphsPath+ "miniexample\\Diff\\People2People3Added.nt";
        e2.filePathDeleted = defaultGraphsPath+ "miniexample\\Diff\\People2People3Deleted.nt";
        ShaclDiffExtractor.versionElements.add(e2);
        ShaclDiffExtractor.run();

        var metaFilePath = logPath + "People1_People2\\MetaComparison.txt";
        var metaFileString = RegexUtils.getFileAsString(metaFilePath);
        assertFalse(metaFileString.contains("Count"));

        metaFilePath = logPath + "People2_People3\\MetaComparison.txt";
        metaFileString = RegexUtils.getFileAsString(metaFilePath);
        assertFalse(metaFileString.contains("Count"));
    }

    @Test
    public void peopleTestV2V3ForWriting() {
        ShaclDiffExtractor.filePathInitialVersion = defaultGraphsPath+ "miniexample\\People2.nt";
        ShaclDiffExtractor.pruningThresholds = "{(0,0)}";
        ShaclDiffExtractor.doMetaComparison = false;
        ShaclDiffExtractor.initialVersionName = "People2";
        ConfigVersionElement e2 = new ConfigVersionElement();
        e2.versionName = "People3";
        e2.filePathFullVersion = defaultGraphsPath+ "miniexample\\People3.nt";
        e2.filePathAdded = defaultGraphsPath+ "miniexample\\Diff\\People2People3Added.nt";
        e2.filePathDeleted = defaultGraphsPath+ "miniexample\\Diff\\People2People3Deleted.nt";
        ShaclDiffExtractor.versionElements.add(e2);
        ShaclDiffExtractor.run();
        var metaFilePath = logPath + "People2_People3\\DiffComparison.txt";
        var metaFileString = RegexUtils.getFileAsString(metaFilePath);
        System.out.println(metaFileString);
    }

    @Test
    public void peopleTestMultipleVersionsWithOutMetaComparison() {
        ShaclDiffExtractor.filePathInitialVersion = defaultGraphsPath+ "miniexample\\People1.nt";
        ShaclDiffExtractor.pruningThresholds = "{(0,0)}";
        ShaclDiffExtractor.doMetaComparison = false;
        ShaclDiffExtractor.initialVersionName = "People1";
        ConfigVersionElement e1 = new ConfigVersionElement();
        e1.versionName = "People2";
        e1.filePathAdded = defaultGraphsPath+ "miniexample\\Diff\\People1People2Added.nt";
        e1.filePathDeleted = defaultGraphsPath+ "miniexample\\Diff\\People1People2Deleted.nt";
        ShaclDiffExtractor.versionElements.add(e1);
        ConfigVersionElement e2 = new ConfigVersionElement();
        e2.versionName = "People3";
        e2.filePathAdded = defaultGraphsPath+ "miniexample\\Diff\\People2People3Added.nt";
        e2.filePathDeleted = defaultGraphsPath+ "miniexample\\Diff\\People2People3Deleted.nt";
        ShaclDiffExtractor.versionElements.add(e2);
        ShaclDiffExtractor.run();

        var filePath = logPath + "People1_People2\\DiffComparison.txt";
        var fileString = RegexUtils.getFileAsString(filePath);
        System.out.println(fileString);
        assertTrue(fileString.contains("""
                === Added Node Shapes ===
                http://shaclshapes.org/CatShape
                === Added Property Shapes ===
                http://shaclshapes.org/instanceTypeCatShapeProperty
                http://shaclshapes.org/colorCatShapeProperty"""));

        filePath = logPath + "People2_People3\\DiffComparison.txt";
        fileString = RegexUtils.getFileAsString(filePath);
        System.out.println(fileString);
        assertTrue(fileString.contains("""
                === Edited Property Shape Names ===
                http://shaclshapes.org/knowsPersonShapeProperty
                http://shaclshapes.org/colorCatShapeProperty"""));
    }

    @Test
    public void peopleTestMultipleVersionsPruning() {
        ShaclDiffExtractor.filePathInitialVersion = defaultGraphsPath+ "miniexample\\People1.nt";
        ShaclDiffExtractor.pruningThresholds = "{(0,2)}";
        ShaclDiffExtractor.doMetaComparison = false;
        ShaclDiffExtractor.initialVersionName = "People1";
        ConfigVersionElement e1 = new ConfigVersionElement();
        e1.versionName = "People2";
        e1.filePathAdded = defaultGraphsPath+ "miniexample\\Diff\\People1People2Added.nt";
        e1.filePathDeleted = defaultGraphsPath+ "miniexample\\Diff\\People1People2Deleted.nt";
        ShaclDiffExtractor.versionElements.add(e1);
        ConfigVersionElement e2 = new ConfigVersionElement();
        e2.versionName = "People3";
        e2.filePathAdded = defaultGraphsPath+ "miniexample\\Diff\\People2People3Added.nt";
        e2.filePathDeleted = defaultGraphsPath+ "miniexample\\Diff\\People2People3Deleted.nt";
        ShaclDiffExtractor.versionElements.add(e2);
        ShaclDiffExtractor.run();

        var diffFilePath1 = logPath + "People1_People2\\DiffComparison.txt";
        var diffFileString1 = RegexUtils.getFileAsString(diffFilePath1);
        assertTrue(diffFileString1.contains("""
                === Added Node Shapes ===
                http://shaclshapes.org/CatShape
                === Added Property Shapes ===
                http://shaclshapes.org/instanceTypeCatShapeProperty
                http://shaclshapes.org/colorCatShapeProperty"""));

        var diffFilePath2 = logPath + "People2_People3\\DiffComparison.txt";
        var diffFileString2 = RegexUtils.getFileAsString(diffFilePath2);
        assertTrue(diffFileString2.contains("""
          === Deleted Property Shapes ===
          http://shaclshapes.org/colorCatShapeProperty
          === Edited Node Shape Names ===
          http://shaclshapes.org/CatShape
          === Edited Property Shape Names ===
          http://shaclshapes.org/knowsPersonShapeProperty
          """));
    }

    @Test
    public void peopleTestMultipleVersionsPruningMetaComparison() {
        ShaclDiffExtractor.filePathInitialVersion = defaultGraphsPath+ "miniexample\\People1.nt";
        ShaclDiffExtractor.pruningThresholds = "{(0,2)}";
        ShaclDiffExtractor.doMetaComparison = true;
        ShaclDiffExtractor.initialVersionName = "People1";
        ConfigVersionElement e1 = new ConfigVersionElement();
        e1.versionName = "People2";
        e1.filePathAdded = defaultGraphsPath+ "miniexample\\Diff\\People1People2Added.nt";
        e1.filePathDeleted = defaultGraphsPath+ "miniexample\\Diff\\People1People2Deleted.nt";
        e1.filePathFullVersion = defaultGraphsPath+ "miniexample\\People2.nt";
        ShaclDiffExtractor.versionElements.add(e1);
        ConfigVersionElement e2 = new ConfigVersionElement();
        e2.versionName = "People3";
        e2.filePathAdded = defaultGraphsPath+ "miniexample\\Diff\\People2People3Added.nt";
        e2.filePathDeleted = defaultGraphsPath+ "miniexample\\Diff\\People2People3Deleted.nt";
        e2.filePathFullVersion = defaultGraphsPath+ "miniexample\\People3.nt";

        ShaclDiffExtractor.versionElements.add(e2);
        ShaclDiffExtractor.run();

        var metaFilePath1 = logPath + "People1_People2\\MetaComparison.txt";
        var metaFileString1 = RegexUtils.getFileAsString(metaFilePath1);
        assertFalse(metaFileString1.contains("Count"));

        var metaFilePath2 = logPath + "People2_People3\\MetaComparison.txt";
        var metaFileString2 = RegexUtils.getFileAsString(metaFilePath2);
        assertFalse(metaFileString2.contains("Count"));
    }

    @Test
    public void filmV1V2NoGenderWithMetaComparison() {
        ShaclDiffExtractor.filePathInitialVersion = defaultGraphsPath+ "film-diff\\film.nt";
        ShaclDiffExtractor.pruningThresholds = "{(0,0)}";
        ShaclDiffExtractor.doMetaComparison = true;
        ShaclDiffExtractor.initialVersionName = "Film";
        ConfigVersionElement e1 = new ConfigVersionElement();
        e1.versionName = "Film2";
        e1.filePathAdded = defaultGraphsPath+ "film-diff\\film1film2added.nt";
        e1.filePathDeleted = defaultGraphsPath+ "film-diff\\film1film2deleted.nt";
        e1.filePathFullVersion = defaultGraphsPath+ "film-diff\\film2.nt";
        ShaclDiffExtractor.versionElements.add(e1);
        ShaclDiffExtractor.run();

        var metaFilePath = logPath + "Film_Film2\\MetaComparison.txt";
        var metaFileString = RegexUtils.getFileAsString(metaFilePath);
        assertFalse(metaFileString.contains("Count"));
    }

    @Test
    public void filmV2V3NoFilmStudioWithMetaComparison() {
        ShaclDiffExtractor.filePathInitialVersion = defaultGraphsPath+ "film-diff\\film2.nt";
        ShaclDiffExtractor.pruningThresholds = "{(0,0)}";
        ShaclDiffExtractor.doMetaComparison = true;
        ShaclDiffExtractor.initialVersionName = "film2";
        ConfigVersionElement e1 = new ConfigVersionElement();
        e1.versionName = "film3";
        e1.filePathAdded = defaultGraphsPath+ "film-diff\\film2film3added.nt";
        e1.filePathDeleted = defaultGraphsPath+ "film-diff\\film2film3deleted.nt";
        e1.filePathFullVersion = defaultGraphsPath+ "film-diff\\film3.nt";
        ShaclDiffExtractor.versionElements.add(e1);
        ShaclDiffExtractor.run();

        var metaFilePath = logPath + "film2_film3\\MetaComparison.txt";
        var metaFileString = RegexUtils.getFileAsString(metaFilePath);
        assertFalse(metaFileString.contains("Count"));
    }

    @Test
    public void BearC1BearC2MetaComparison() throws IOException {
        Main.setResourcesPathForJar(ConfigManager.getRelativeResourcesPathFromQse());
        Main.annotateSupportConfidence = "true";
        File currentDir = new File(System.getProperty("user.dir"));
        File emptyConfig = new File(currentDir, "src/test/expected_test_results/emptyconfig.txt");
        Main.configPath = emptyConfig.getAbsolutePath(); //avoid exceptions in QSE
        Main.saveCountInPropertyData=true;
        Main.datasetName="Bear-C1";
        Path basePath = Paths.get( "Output");
        Files.createDirectories(basePath);
        Main.setOutputFilePathForJar(basePath.toAbsolutePath()+File.separator);
        ShaclDiffExtractor.filePathInitialVersion = "C:\\Users\\evapu\\Downloads\\alldata.IC_bearc.nt\\1.nt\\data-1624.nt";
        ShaclDiffExtractor.pruningThresholds = "{(0,0)}";
        ShaclDiffExtractor.doMetaComparison = true;
        ShaclDiffExtractor.initialVersionName="Bear-C1";
        ConfigVersionElement e1 = new ConfigVersionElement();
        e1.versionName = "Bear-C2";
        e1.filePathAdded = "C:\\Users\\evapu\\Downloads\\alldata.CB.ntBearC\\data-added_1-2.nt\\data-added_1-2.nt";
        e1.filePathDeleted = "C:\\Users\\evapu\\Downloads\\alldata.CB.ntBearC\\data-deleted_1-2.nt\\data-deleted_1-2.nt";
        e1.filePathFullVersion = "C:\\Users\\evapu\\Downloads\\alldata.IC_bearc.nt\\2.nt\\data-1625.nt";
        ShaclDiffExtractor.versionElements.add(e1);
        ShaclDiffExtractor.run();

        var metaFilePath = logPath + "Bear-C1_Bear-C2\\MetaComparison.txt";
        var metaFileString = RegexUtils.getFileAsString(metaFilePath);
        Pattern pattern = Pattern.compile(Pattern.quote("Count")); //count instances of count
        Matcher matcher = pattern.matcher(metaFileString);

        int count = 0;
        while (matcher.find()) {
            count++;
        }

        assertEquals(count,1);
        assertTrue(metaFileString.contains("http://shaclshapes.org/instanceTypeOrganizationShapeProperty"));
    }

    @Test
    public void sampleQSEOutput() throws IOException {
        var file = """
                <http://example.org/alice> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
                <http://xmlns.com/foaf/0.1/Person> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Class> .
                <http://xmlns.com/foaf/0.1/Alien> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#SomeOtherClass> .
                <http://example.org/alice> <http://xmlns.com/foaf/0.1/name> "Alice" .
                """;

        var tempFile = Files.createTempFile("QSERQ2TmpFile", ".nt");
        Files.write(tempFile, file.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);

        ShapeComparatorQseFileBased comparator = new ShapeComparatorQseFileBased(tempFile.toAbsolutePath().toString(), "", "SmallEx", "","");
        comparator.runQseFirstTime("(0,0)",Paths.get("C:\\Users\\evapu\\Documents\\GitHub\\QseEvolvingKg\\QseEvolvingKg\\Output\\"));
        System.out.println();
    }

    @Test
    public void sampleQSEOutputBearC() {
        ShapeComparatorQseFileBased comparator = new ShapeComparatorQseFileBased("C:\\Users\\evapu\\Downloads\\alldata.IC_bearc.nt\\1.nt\\data-1624.nt", "", "SmallEx", "","");
        comparator.runQseFirstTime("(0,0)",Paths.get("C:\\Users\\evapu\\Documents\\GitHub\\QseEvolvingKg\\QseEvolvingKg\\Output\\"));
        System.out.println();
    }

    @Test
    public void testBearCIntermediateResults() throws IOException {
        prepareQSE();
        Main.datasetName = "Bear-C2";
        int support = 0;
        double confidence = 0.0;

        Parser parser = new Parser("C:\\Users\\evapu\\Downloads\\alldata.IC_bearc.nt\\1.nt\\data-1624.nt", 3, 10, instanceTypeProperty);
        ShaclDiffExtractorUnitTests.runParser(parser);
        ExtractedShapes tmp = new ExtractedShapes();
        tmp.setNodeShapes(parser.shapesExtractor.getNodeShapes(), false);
        DiffExtractor diffExtractor = new DiffExtractor("C:\\Users\\evapu\\Downloads\\alldata.CB.ntBearC\\data-added_1-2.nt\\data-added_1-2.nt","C:\\Users\\evapu\\Downloads\\alldata.CB.ntBearC\\data-deleted_1-2.nt\\data-deleted_1-2.nt",parser,support, confidence, parser.shapesExtractor.getOutputFileAddress(), tmp.getNodeShapes());
        diffExtractor.extractFromFile();

        var oldDataSetName = Main.datasetName;
        var oldOutputPath = Main.outputFilePath;
        Main.datasetName = Main.datasetName+"_Full";
        Main.outputFilePath = Main.outputFilePath+ "Full" + File.separator;
        Parser parserV3 = new Parser("C:\\Users\\evapu\\Downloads\\alldata.IC_bearc.nt\\2.nt\\data-1625.nt", 3, 10, instanceTypeProperty);
        parserV3.setStringEncoder(parser.getStringEncoder());
        ShaclDiffExtractorUnitTests.runParser(parserV3);
        Main.datasetName = oldDataSetName;
        Main.outputFilePath = oldOutputPath;

        assertEquals(parser.classEntityCount, parserV3.classEntityCount);
        assertTrue(ShaclDiffExtractorUnitTests.areMapsEqual(parser.entityDataHashMap, parserV3.entityDataHashMap));
        assertTrue(ShaclDiffExtractorUnitTests.areMapsEqual(parser.classToPropWithObjTypes, parserV3.classToPropWithObjTypes));
        assertTrue(ShaclDiffExtractorUnitTests.areMapsEqual(parser.statsComputer.getShapeTripletSupport(), parserV3.statsComputer.getShapeTripletSupport()));
    }

    @Test
    public void testBearCIntermediateResultsOnlyAdd() throws IOException {
        prepareQSE();
        Main.datasetName = "Bear-C2";
        int support = 0;
        double confidence = 0.0;

        Parser parser = new Parser("C:\\Users\\evapu\\Downloads\\alldata.IC_bearc.nt\\1.nt\\data-1624.nt", 3, 10, instanceTypeProperty);
        ShaclDiffExtractorUnitTests.runParser(parser);
        ExtractedShapes tmp = new ExtractedShapes();
        tmp.setNodeShapes(parser.shapesExtractor.getNodeShapes(), false);
        DiffExtractor diffExtractor = new DiffExtractor("C:\\Users\\evapu\\Downloads\\alldata.CB.ntBearC\\data-added_1-2.nt\\data-added_1-2.nt","C:\\Users\\evapu\\Downloads\\empty.nt",parser,support, confidence, parser.shapesExtractor.getOutputFileAddress(), tmp.getNodeShapes());
        diffExtractor.extractFromFile();

        var oldDataSetName = Main.datasetName;
        var oldOutputPath = Main.outputFilePath;
        Main.datasetName = Main.datasetName+"_Full";
        Main.outputFilePath = Main.outputFilePath+ "Full" + File.separator;
        Parser parserV3 = new Parser("C:\\Users\\evapu\\Downloads\\data-1624_added.nt", 3, 10, instanceTypeProperty);
        parserV3.setStringEncoder(parser.getStringEncoder());
        ShaclDiffExtractorUnitTests.runParser(parserV3);
        Main.datasetName = oldDataSetName;
        Main.outputFilePath = oldOutputPath;

        assertEquals(parser.classEntityCount, parserV3.classEntityCount);
        assertTrue(ShaclDiffExtractorUnitTests.areMapsEqual(parser.entityDataHashMap, parserV3.entityDataHashMap));
        assertTrue(ShaclDiffExtractorUnitTests.areMapsEqual(parser.classToPropWithObjTypes, parserV3.classToPropWithObjTypes));
        assertTrue(ShaclDiffExtractorUnitTests.areMapsEqual(parser.statsComputer.getShapeTripletSupport(), parserV3.statsComputer.getShapeTripletSupport()));
    }

    @Test
    public void BearB1BearB2MetaComparison() throws IOException {
        Main.setResourcesPathForJar(ConfigManager.getRelativeResourcesPathFromQse());
        Main.annotateSupportConfidence = "true";
        File currentDir = new File(System.getProperty("user.dir"));
        File emptyConfig = new File(currentDir, "src/test/expected_test_results/emptyconfig.txt");
        Main.configPath = emptyConfig.getAbsolutePath(); //avoid exceptions in QSE
        Main.saveCountInPropertyData=true;
        Path basePath = Paths.get( "Output");
        Files.createDirectories(basePath);
        Main.setOutputFilePathForJar(basePath.toAbsolutePath()+File.separator);
        ShaclDiffExtractor.filePathInitialVersion = "C:\\Users\\evapu\\Downloads\\alldata.IC.nt\\000001.nt\\000001.nt";
        ShaclDiffExtractor.pruningThresholds = "{(0,0)}";
        ShaclDiffExtractor.doMetaComparison = true;
        ShaclDiffExtractor.initialVersionName="Bear-B1";
        ConfigVersionElement e1 = new ConfigVersionElement();
        e1.versionName = "Bear-B2";
        e1.filePathAdded = "C:\\Users\\evapu\\Downloads\\alldata.CB.nt\\data-added_1-2.nt\\data-added_1-2.nt";
        e1.filePathDeleted = "C:\\Users\\evapu\\Downloads\\alldata.CB.nt\\data-deleted_1-2.nt\\data-deleted_1-2.nt";
        e1.filePathFullVersion = "C:\\Users\\evapu\\Downloads\\alldata.IC.nt\\000002.nt\\000002.nt";
        ShaclDiffExtractor.versionElements.add(e1);
        ShaclDiffExtractor.run();

        var metaFilePath = logPath + "Bear-B1_Bear-B2\\MetaComparison.txt";
        var metaFileString = RegexUtils.getFileAsString(metaFilePath);
        System.out.println(metaFileString);
    }

    @Test
    public void BearB3BearB4NoMetaComparison() throws IOException {
        Main.setResourcesPathForJar(ConfigManager.getRelativeResourcesPathFromQse());
        Main.annotateSupportConfidence = "true";
        File currentDir = new File(System.getProperty("user.dir"));
        File emptyConfig = new File(currentDir, "src/test/expected_test_results/emptyconfig.txt");
        Main.configPath = emptyConfig.getAbsolutePath(); //avoid exceptions in QSE
        Main.saveCountInPropertyData=true;
        Path basePath = Paths.get( "Output");
        Files.createDirectories(basePath);
        Main.setOutputFilePathForJar(basePath.toAbsolutePath()+File.separator);
        ShaclDiffExtractor.filePathInitialVersion = "C:\\Users\\evapu\\Downloads\\alldata.IC.nt\\000003.nt\\000003.nt";
        ShaclDiffExtractor.pruningThresholds = "{(0,0)}";
        ShaclDiffExtractor.doMetaComparison = false;
        ShaclDiffExtractor.initialVersionName="Bear-B3";
        ConfigVersionElement e1 = new ConfigVersionElement();
        e1.versionName = "Bear-B4";
        e1.filePathAdded = "C:\\Users\\evapu\\Downloads\\alldata.CB.nt\\data-added_3-4.nt\\data-added_3-4.nt";
        e1.filePathDeleted = "C:\\Users\\evapu\\Downloads\\alldata.CB.nt\\data-deleted_3-4.nt\\data-deleted_3-4.nt";
        e1.filePathFullVersion = "C:\\Users\\evapu\\Downloads\\alldata.IC.nt\\000004.nt\\000004.nt";
        ShaclDiffExtractor.versionElements.add(e1);
        ShaclDiffExtractor.run();

        var metaFilePath = logPath + "Bear-B3_Bear-B4\\DiffComparison.txt";
        var metaFileString = RegexUtils.getFileAsString(metaFilePath);
        System.out.println(metaFileString);
    }

    @Test
    public void querySailRepo() {
        File dbDir = new File("C:\\Users\\evapu\\Documents\\GitHub\\QseEvolvingKg\\QseEvolvingKg\\Output\\DiffExtractor\\People\\Original\\db_default");

        var defaultShapesDb = new SailRepository(new NativeStore(new File(dbDir.getAbsolutePath())));

        try {
            RepositoryConnection conn = defaultShapesDb.getConnection();

            try {
                conn.getStatements(null, null, null).forEach(statement -> {
                    System.out.println(statement);
                });
                Main.resourcesPath="C:\\Users\\evapu\\Documents\\GitHub\\qse\\src\\main\\resources";
                var tquery = conn.prepareTupleQuery(FilesUtil.readShaclQuery("sh_class_indirect_ps").replace("PROPERTY_SHAPE", "http://shaclshapes.org/knowsPersonShapeProperty").replace("NODE_SHAPE", "http://shaclshapes.org/PersonShape"));

                TupleQueryResult result = tquery.evaluate();

                    while(result.hasNext()) {
                        BindingSet solution = result.next();
                        System.out.println(solution);
                    }
            }
            catch(Exception ex) {
                ex.printStackTrace();
            }

            if (conn != null) {
                conn.close();
            }
        } finally {
            defaultShapesDb.shutDown();
        }
    }

    @Test
    public void testPruningWorksWithSelectedClasses() {
        var filePath =  defaultGraphsPath+ "film\\film.nt";
        var outputPath = Paths.get("C:\\Users\\evapu\\Documents\\GitHub\\QseEvolvingKg\\QseEvolvingKg\\Output\\");
        ShapeComparatorQseFileBased.prepareQse("(0,3)");
        ShapeComparatorQseFileBased.prepareQsePath("Film", outputPath);
        var parser = new Parser(filePath, 300, 1000, DiffManager.instanceTypeProperty);

        parser.entityExtraction();
        parser.entityConstraintsExtraction();
        parser.computeSupportConfidence();
        parser.extractSHACLShapes(false, new ArrayList<>());
        parser.extractSHACLShapesWithPruning(false, 0.0, 3, new ArrayList<>());


        //select only one class
        ShapeComparatorQseFileBased.prepareQse("(0,3)");
        ShapeComparatorQseFileBased.prepareQsePath("Film-OnlyScriptWriter", outputPath);
        parser = new Parser(filePath, 300, 1000, DiffManager.instanceTypeProperty);

        parser.entityExtraction();
        parser.entityConstraintsExtraction();
        parser.computeSupportConfidence();
        parser.extractSHACLShapes(true, new ArrayList<>(Arrays.asList("http://semantics.id/ns/example/film#ScriptWriter")));
        parser.extractSHACLShapesWithPruning(true, 0.0,3, new ArrayList<>(Arrays.asList("http://semantics.id/ns/example/film#ScriptWriter")));
    }

    @Test
    public void testQSEWithEncodingIssues() throws IOException {
        var contentNew = """
<http://dbpedia.org/resource/2015_US_Open_(tennis)> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .
<http://dbpedia.org/resource/2015_US_Open_(tennis)> <http://dbpedia.org/ontology/budget> "4.22534E7"^^<http://dbpedia.org/datatype/usDollar> .
                """;
        var tempFileNew = Files.createTempFile("QSERQ2TmpFileNew", ".nt");
        Files.write(tempFileNew, contentNew.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
        var outputPath = Paths.get("C:\\Users\\evapu\\Documents\\GitHub\\QseEvolvingKg\\QseEvolvingKg\\Output\\");
        ShapeComparatorQseFileBased.prepareQse("(0,3)");
        ShapeComparatorQseFileBased.prepareQsePath("testGraph", outputPath);
        var parser = new Parser(tempFileNew.toAbsolutePath().toString(), 300, 1000, DiffManager.instanceTypeProperty);

        DiffManager diffManager = new DiffManager();
        diffManager.runParser(parser);
    }
}
