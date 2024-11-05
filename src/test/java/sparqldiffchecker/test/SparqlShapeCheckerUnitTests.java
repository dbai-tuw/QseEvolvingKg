package sparqldiffchecker.test;

import cs.Main;
import cs.qse.querybased.nonsampling.QbParser;
import cs.utils.Constants;
import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.Before;
import org.junit.Test;
import shape_comparator.data.ExtractedShapes;
import sparqlshapechecker.comparator.ComparisonDiff;
import shape_comparator.data.ShaclOrListItem;
import sparqlshapechecker.utils.GraphDbUtils;
import sparqlshapechecker.utils.RegexUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

//Tests are only used for local execution

public class SparqlShapeCheckerUnitTests {
    public static final String resourcesPath = System.getProperty("user.dir")+"\\resources";
    public static String firstVersionName = "film";

    public static String outputPath = System.getProperty("user.dir")+"\\Output\\"+firstVersionName+"\\";
    //QSE QueryBases does not calculate confidence, therefore it is always 0 and filtering works with > 0 -> filter to -1
    public static final String pruningThresholds = "{(-1,0)}"; //only set one threshold - {(<confidence 10% is 0.1>,<support>)}
    public static final String graphDbUrl = "http://localhost:7201/";

    @Before
    public void prepareQSE() {
        try {
            FileUtils.deleteDirectory((Paths.get("Output").toFile()));
        }
        catch(Exception ex) {}
        try {
            Files.createDirectory(Path.of(System.getProperty("user.dir") + File.separator + "Output"));
        }
        catch(Exception ex) {}
        try {
            Files.createDirectory(Paths.get("Output","SparqlShapeChecker_Results"));
        }
        catch(Exception ex) {}
    }

    public void runFilm() {
        Main.setResourcesPathForJar(resourcesPath);
        Main.setOutputFilePathForJar(outputPath);
        Main.setPruningThresholds(pruningThresholds);
        Main.annotateSupportConfidence = "true";
        Main.datasetName = firstVersionName;

        QbParser qbParser = new QbParser(100, Constants.RDF_TYPE, graphDbUrl, firstVersionName);
        qbParser.run();
    }

    @Test
    public void runQSEQBased() {
        Main.setResourcesPathForJar(resourcesPath);
        Main.setOutputFilePathForJar(outputPath);
        Main.setPruningThresholds(pruningThresholds);
        Main.annotateSupportConfidence = "true";
        Main.datasetName = firstVersionName;
//        Main.datasetName = "film-v4labelgenreoneoritem";

        QbParser qbParser = new QbParser(100, Constants.RDF_TYPE, graphDbUrl, firstVersionName);
        qbParser.run();
    }

    @Test
    public void generateShaclTtlFromDb() {
        Main.setResourcesPathForJar(resourcesPath);
        Main.setOutputFilePathForJar(outputPath);
        Main.setPruningThresholds(pruningThresholds);
        Main.annotateSupportConfidence = "true";
        Main.datasetName = firstVersionName + "_cloned";
        var goalRepo = System.getProperty("user.dir")+"\\Output\\film\\secondVersion";
        GraphDbUtils.constructDefaultShapes(goalRepo);
    }

    @Test
    public void deleteFromOrItems() {
        runFilm();
        //actually SHACL should not contain or list anymore, when only one item is left
        var sourceFile = System.getProperty("user.dir")+"\\Output\\film\\film_QSE_FULL_SHACL.ttl";
        ExtractedShapes extractedShapes = new ExtractedShapes();
        extractedShapes.fileContentPath = sourceFile;
        String shape = RegexUtils.getShapeAsString("http://shaclshapes.org/labelGenreShapeProperty", extractedShapes.getFileAsString(true));
        ShaclOrListItem orListItem = new ShaclOrListItem(SimpleValueFactory.getInstance().createIRI("http://www.w3.org/ns/shacl#Literal"),null,SimpleValueFactory.getInstance().createIRI("xsd:string"));
        String deletedShape = RegexUtils.deleteShaclOrItemWithIriFromString(orListItem, shape, false);
        var expected = "\n<http://shaclshapes.org/labelGenreShapeProperty> rdf:type <http://www.w3.org/ns/shacl#PropertyShape> ;\n" +
                "  <http://www.w3.org/ns/shacl#or> ( [\n" +
                "    <http://shaclshapes.org/confidence> 1,6667E-1 ;\n" +
                "    <http://shaclshapes.org/support> \"1\"^^xsd:int ;\n" +
                "    <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#Literal> ;\n" +
                "    <http://www.w3.org/ns/shacl#datatype> rdf:langString ;\n" +
                "  ] ) ;\n" +
                "  <http://www.w3.org/ns/shacl#path> rdfs:label .\n";
        System.out.println(expected);
        System.out.println(deletedShape);
        assertEquals("Finished shapes do not match",expected, deletedShape);
    }

    @Test
    public void testDeleteWhenOnlyOneOrItemIsLeftWithGivenShape() {
        var shape = "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
                "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n" +
                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
                "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n" +
                "\n<http://shaclshapes.org/labelGenreShapeProperty> rdf:type <http://www.w3.org/ns/shacl#PropertyShape> ;\n" +
                "  <http://www.w3.org/ns/shacl#or> ([\n" +
                "    <http://shaclshapes.org/confidence> 1.6667E-1 ;\n" + //problem with , in double
                "    <http://shaclshapes.org/support> \"1\"^^xsd:int ;\n" +
                "    <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#Literal> ;\n" +
                "    <http://www.w3.org/ns/shacl#datatype> rdf:langString ;\n" +
                "  ] ) ;\n" +
                "  <http://www.w3.org/ns/shacl#path> rdfs:label .\n";
        var deletedShape = GraphDbUtils.deleteOrListAndConnectToParentNode(shape, "http://shaclshapes.org/labelGenreShapeProperty", 1, 1.6667E-1);

        var expected = "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\r\n" +
                "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n" +
                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
                "" +
                "\n<http://shaclshapes.org/labelGenreShapeProperty> rdf:type <http://www.w3.org/ns/shacl#PropertyShape> ;\n" +
                "  <http://shaclshapes.org/confidence> 1,6667E-1 ;\n" + //problem with , in double
                "  <http://shaclshapes.org/support> \"1\"^^xsd:int ;\n" +
                "  <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#Literal> ;\n" +
                "  <http://www.w3.org/ns/shacl#datatype> rdf:langString ;\n" +
                "  <http://www.w3.org/ns/shacl#path> rdfs:label .";
        System.out.println(expected);
        System.out.println(deletedShape);
        assertEquals("Finished shapes do not match",expected.replace("\r\n", "\n"), deletedShape.replace("\r\n", "\n"));
    }

    @Test
    public void testDeleteWhenOnlyOneOrItemIsLeft() {
        runFilm();
        var sourceFile = System.getProperty("user.dir")+"\\Output\\film\\film_QSE_FULL_SHACL.ttl";
        ExtractedShapes extractedShapes = new ExtractedShapes();
        extractedShapes.fileContentPath = sourceFile;
        extractedShapes.getFileAsString(true); //read prefix lines todo maybe optimization
        String shape = RegexUtils.getShapeAsString("http://shaclshapes.org/labelGenreShapeProperty", RegexUtils.getFileAsString(sourceFile));
        ShaclOrListItem orListItem = new ShaclOrListItem(SimpleValueFactory.getInstance().createIRI("http://www.w3.org/ns/shacl#Literal"),null,SimpleValueFactory.getInstance().createIRI("xsd:string"));
        String deletedShape = RegexUtils.deleteShaclOrItemWithIriFromString(orListItem, shape, false);
        shape = extractedShapes.prefixLines + deletedShape;
        var adaptedShape = GraphDbUtils.deleteOrListAndConnectToParentNode(shape, "http://shaclshapes.org/labelGenreShapeProperty", 1, 1);
        //todo remove prefix lines of shape
        var shapeWithoutPrefix = RegexUtils.removeLinesWithPrefix(adaptedShape);
        var expected = "\n<http://shaclshapes.org/labelGenreShapeProperty> rdf:type <http://www.w3.org/ns/shacl#PropertyShape> ;\n" +
                "  <http://shaclshapes.org/confidence> 1E0 ;\n" + //problem with , in double
                "  <http://shaclshapes.org/support> \"1\"^^xsd:int ;\n" +
                "  <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#Literal> ;\n" +
                "  <http://www.w3.org/ns/shacl#datatype> rdf:langString ;\n" +
                "  <http://www.w3.org/ns/shacl#path> rdfs:label ." +
                "\n";
        System.out.println(expected);
        System.out.println(shapeWithoutPrefix);
        assertEquals("Finished shapes do not match",expected.replace("\r\n", "\n"), shapeWithoutPrefix.replace("\r\n", "\n"));
    }

    @Test
    public void testDeleteWhenOnlyOneOrItemIsLeftFullTest() {
        prepareTest();

        QbParser qbParser = new QbParser(100, Constants.RDF_TYPE, graphDbUrl, "film3");
        qbParser.run();

        var nodeShapes = qbParser.shapesExtractor.getNodeShapes();
        ExtractedShapes extractedShapes = new ExtractedShapes();
        extractedShapes.setNodeShapes(nodeShapes, false);

        GraphDbUtils.checkShapesInNewGraph(graphDbUrl, "film-v4labelgenreoneoritem", extractedShapes.getNodeShapes());

        var sourceFile = System.getProperty("user.dir")+"\\Output\\film\\film_QSE_FULL_SHACL.ttl";
        var copiedFile = System.getProperty("user.dir")+"\\Output\\SparqlShapeChecker_Results\\film_QSE_FULL_SHACL_v4labelgenreoneoritem.ttl";
        RegexUtils.copyFile(sourceFile, copiedFile);
        extractedShapes.fileContentPath = copiedFile;
        ComparisonDiff comparisonDiff = new ComparisonDiff();
        var content = RegexUtils.deleteFromFileWithPruning(extractedShapes, comparisonDiff);
        RegexUtils.saveStringAsFile(content, copiedFile);

        //Weird result for rangeDatatypePropertyShapeProperty: points to IRI undefined and is therefore filtered in next version
        assertTrue("Files are not equal", compareFiles(copiedFile,
                System.getProperty("user.dir")+"\\src\\test\\expected_test_results\\film_QSE_FULL_SHACL_v4labelgenreoneoritem.ttl"));
    }

    @Test
    public void deleteFromMultipleOrItems() {
        //SHACL should not contain or list anymore, when only one item is left
        String shape = "<http://shaclshapes.org/labelGenreShapeProperty> rdf:type <http://www.w3.org/ns/shacl#PropertyShape> ;\n" +
                "<http://www.w3.org/ns/shacl#or> ( [\n" +
                "   <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#Literal> ;\n" +
                "   <http://www.w3.org/ns/shacl#datatype> rdf:langString ;\n" +
                "] [\n" +
                "   <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#Literal> ;\n" +
                "   <http://www.w3.org/ns/shacl#datatype> rdf:integer ;\n" +
                "] [\n" +
                "   <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#Literal> ;\n" +
                "   <http://www.w3.org/ns/shacl#datatype> xsd:string ;\n" +
                "] ) ;\n" +
                "<http://www.w3.org/ns/shacl#path> rdfs:label .";
        ShaclOrListItem orListItem = new ShaclOrListItem(SimpleValueFactory.getInstance().createIRI("http://www.w3.org/ns/shacl#Literal"),null,SimpleValueFactory.getInstance().createIRI("xsd:string"));
        String deletedShape = RegexUtils.deleteShaclOrItemWithIriFromString(orListItem, shape, false);
        var expected = "<http://shaclshapes.org/labelGenreShapeProperty> rdf:type <http://www.w3.org/ns/shacl#PropertyShape> ;\n" +
                "<http://www.w3.org/ns/shacl#or> ( [\n" +
                "   <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#Literal> ;\n" +
                "   <http://www.w3.org/ns/shacl#datatype> rdf:langString ;\n" +
                "] [\n" +
                "   <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#Literal> ;\n" +
                "   <http://www.w3.org/ns/shacl#datatype> rdf:integer ;\n" +
                "] ) ;\n" +
                "<http://www.w3.org/ns/shacl#path> rdfs:label .";
        System.out.println(expected);
        System.out.println(deletedShape);
        assertEquals("Finished shapes do not match",expected, deletedShape);
    }

    //with new QSE-shactor version

    private void prepareTest() {
        Main.setResourcesPathForJar(resourcesPath);
        Main.setOutputFilePathForJar(outputPath);
        Main.setPruningThresholds(pruningThresholds);
        Main.annotateSupportConfidence = "true";
        Main.datasetName = firstVersionName;
        Main.configPath = System.getProperty("user.dir")+"\\src\\test\\expected_test_results\\emptyconfig.txt"; //avoid exceptions in QSE
    }

    @Test
    public void testDeletePropertyShapeWithIRI() {
        prepareTest();
        runFilm();
        QbParser qbParser = new QbParser(100, Constants.RDF_TYPE, graphDbUrl, firstVersionName);
        qbParser.run();

        var nodeShapes = qbParser.shapesExtractor.getNodeShapes();
        ExtractedShapes extractedShapes = new ExtractedShapes();
        extractedShapes.setNodeShapes(nodeShapes, false);

        GraphDbUtils.checkShapesInNewGraph(graphDbUrl, "film-NoSubPropertyOfSymmetricProperty", extractedShapes.getNodeShapes());

        var sourceFile = System.getProperty("user.dir")+"\\Output\\film\\film_QSE_FULL_SHACL.ttl";
        var copiedFile = System.getProperty("user.dir")+"\\Output\\film\\film_QSE_FULL_SHACL_subPropertySymmetricPropertyShape.ttl";
        RegexUtils.copyFile(sourceFile, copiedFile);
        extractedShapes.fileContentPath = copiedFile;
        ComparisonDiff comparisonDiff = new ComparisonDiff();
        var content = RegexUtils.deleteFromFileWithPruning(extractedShapes,comparisonDiff);
        RegexUtils.saveStringAsFile(content, copiedFile);

        assertTrue("Files are not equal", compareFiles(copiedFile,
                System.getProperty("user.dir")+"\\src\\test\\expected_test_results\\film_QSE_FULL_SHACL_subPropertySymmetricPropertyShape.ttl"));
    }

    @Test
    public void testDeletePropertyShapeWithLiteral() {
        prepareTest();
        QbParser qbParser = new QbParser(100, Constants.RDF_TYPE, graphDbUrl, firstVersionName);
        qbParser.run();

        var nodeShapes = qbParser.shapesExtractor.getNodeShapes();
        ExtractedShapes extractedShapes = new ExtractedShapes();
        extractedShapes.setNodeShapes(nodeShapes, false);

        GraphDbUtils.checkShapesInNewGraph(graphDbUrl, "Film-NoGender", extractedShapes.getNodeShapes());

        var sourceFile = System.getProperty("user.dir")+"\\Output\\film\\film_QSE_FULL_SHACL.ttl";
        var copiedFile = System.getProperty("user.dir")+"\\Output\\SparqlShapeChecker_Results\\film_QSE_FULL_SHACL_noGender.ttl";
        RegexUtils.copyFile(sourceFile, copiedFile);
        extractedShapes.fileContentPath = copiedFile;
        ComparisonDiff comparisonDiff = new ComparisonDiff();
        var content = RegexUtils.deleteFromFileWithPruning(extractedShapes, comparisonDiff);
        RegexUtils.saveStringAsFile(content, copiedFile);

        assertTrue("Files are not equal", compareFiles(copiedFile,
                System.getProperty("user.dir")+"\\src\\test\\expected_test_results\\film_QSE_FULL_SHACL_noGender.ttl"));
    }

    @Test
    public void testDeleteNoFilmStudio() {
        prepareTest();
        QbParser qbParser = new QbParser(100, Constants.RDF_TYPE, graphDbUrl, firstVersionName);
        qbParser.run();

        var nodeShapes = qbParser.shapesExtractor.getNodeShapes();
        ExtractedShapes extractedShapes = new ExtractedShapes();
        extractedShapes.setNodeShapes(nodeShapes, false);

        GraphDbUtils.checkShapesInNewGraph(graphDbUrl, "film3", extractedShapes.getNodeShapes());

        var sourceFile = System.getProperty("user.dir")+"\\Output\\film\\film_QSE_FULL_SHACL.ttl";
        var copiedFile = System.getProperty("user.dir")+"\\Output\\SparqlShapeChecker_Results\\film_QSE_FULL_SHACL_NoFilmStudio.ttl";
        RegexUtils.copyFile(sourceFile, copiedFile);
        extractedShapes.fileContentPath = copiedFile;
        ComparisonDiff comparisonDiff = new ComparisonDiff();
        var content = RegexUtils.deleteFromFileWithPruning(extractedShapes, comparisonDiff);
        RegexUtils.saveStringAsFile(content, copiedFile);

        assertTrue("Files are not equal", compareFiles(copiedFile,
                System.getProperty("user.dir")+"\\src\\test\\expected_test_results\\film_QSE_FULL_SHACL_NoFilmStudio.ttl"));

    }

    @Test
    public void testSupportThresholdWithSameDataSet() {
        prepareTest();
        Main.setPruningThresholds("{(-1,5)}");
        QbParser qbParser = new QbParser(100, Constants.RDF_TYPE, graphDbUrl, firstVersionName);
        qbParser.run();

        var nodeShapes = qbParser.shapesExtractor.getNodeShapes();
        ExtractedShapes extractedShapes = new ExtractedShapes();
        extractedShapes.support = 5;
        extractedShapes.setNodeShapes(nodeShapes, false);

        GraphDbUtils.checkShapesInNewGraph(graphDbUrl, firstVersionName, extractedShapes.getNodeShapes());

        var sourceFile = System.getProperty("user.dir")+"\\Output\\film\\film_QSE_-1.0_5_SHACL.ttl";
        var copiedFile = System.getProperty("user.dir")+"\\Output\\film\\film_QSE_-1.0_5_SHACL_Support.ttl";
        RegexUtils.copyFile(sourceFile, copiedFile);
        extractedShapes.fileContentPath = copiedFile;
        ComparisonDiff comparisonDiff = new ComparisonDiff();
        var content = RegexUtils.deleteFromFileWithPruning(extractedShapes, comparisonDiff);
        RegexUtils.saveStringAsFile(content, copiedFile);

        assertTrue("Files are not equal", compareFiles(copiedFile,
                System.getProperty("user.dir")+"\\src\\test\\expected_test_results\\film_QSE_-1.0_5_SHACL.ttl"));
    }

    @Test
    public void testSupportThresholdWithNoGender() {
        prepareTest();
        Main.setPruningThresholds("{(-1,5)}");
        QbParser qbParser = new QbParser(100, Constants.RDF_TYPE, graphDbUrl, firstVersionName);
        qbParser.run();

        var nodeShapes = qbParser.shapesExtractor.getNodeShapes();
        ExtractedShapes extractedShapes = new ExtractedShapes();
        extractedShapes.support = 5;
        extractedShapes.setNodeShapes(nodeShapes, false);

        GraphDbUtils.checkShapesInNewGraph(graphDbUrl, "Film-NoGender", extractedShapes.getNodeShapes());

        var sourceFile = System.getProperty("user.dir")+"\\Output\\film\\film_QSE_-1.0_5_SHACL.ttl";
        var copiedFile = System.getProperty("user.dir")+"\\Output\\film\\film_QSE_-1.0_5_SHACL_SupportNoGender.ttl";
        RegexUtils.copyFile(sourceFile, copiedFile);
        extractedShapes.fileContentPath = copiedFile;
        ComparisonDiff comparisonDiff = new ComparisonDiff();
        var content = RegexUtils.deleteFromFileWithPruning(extractedShapes, comparisonDiff);
        RegexUtils.saveStringAsFile(content, copiedFile);

        assertTrue("Files are not equal", compareFiles(copiedFile,
                System.getProperty("user.dir")+"\\src\\test\\expected_test_results\\film_QSE_-1.0_5_SHACL_SupportNoGender.ttl"));
    }

    @Test
    public void testSupportThresholdWithPeople() {
        firstVersionName = "PeopleV2";
        outputPath = System.getProperty("user.dir")+"\\Output\\"+firstVersionName+"\\";
        prepareTest();
        Main.setPruningThresholds("{(-1,2)}");

        QbParser qbParser = new QbParser(100, Constants.RDF_TYPE, graphDbUrl, firstVersionName);
        qbParser.run();

        var nodeShapes = qbParser.shapesExtractor.getNodeShapes();
        ExtractedShapes extractedShapes = new ExtractedShapes();
        extractedShapes.support = 2;
        extractedShapes.setNodeShapes(nodeShapes, false);

        GraphDbUtils.checkShapesInNewGraph(graphDbUrl, "PeopleV3", extractedShapes.getNodeShapes());

        var sourceFile = System.getProperty("user.dir")+"\\Output\\PeopleV2\\PeopleV2_QSE_-1.0_2_SHACL.ttl";
        var copiedFile = System.getProperty("user.dir")+"\\Output\\PeopleV2\\PeopleV2_QSE_-1.0_2_SHACL_V3.ttl";
        RegexUtils.copyFile(sourceFile, copiedFile);
        extractedShapes.fileContentPath = copiedFile;
        ComparisonDiff comparisonDiff = new ComparisonDiff();
        var content = RegexUtils.deleteFromFileWithPruning(extractedShapes, comparisonDiff);
        RegexUtils.saveStringAsFile(content, copiedFile);

        assertTrue("Files are not equal", compareFiles(copiedFile,
                System.getProperty("user.dir")+"\\src\\test\\expected_test_results\\PeopleV2_QSE_-1.0_2_SHACL.ttl"));
    }

    @Test
    public void testConfidenceThresholdWithPeople() {
        firstVersionName = "PeopleV2";
        outputPath = System.getProperty("user.dir")+"\\Output\\"+firstVersionName+"\\";
        prepareTest();
        Main.setPruningThresholds("{(0.7,0)}");

        QbParser qbParser = new QbParser(100, Constants.RDF_TYPE, graphDbUrl, firstVersionName);
        qbParser.run();

        var nodeShapes = qbParser.shapesExtractor.getNodeShapes();
        ExtractedShapes extractedShapes = new ExtractedShapes();
        extractedShapes.support = 0;
        extractedShapes.confidence = 0.7;
        extractedShapes.setNodeShapes(nodeShapes, false);

        GraphDbUtils.checkShapesInNewGraph(graphDbUrl, "PeopleV3", extractedShapes.getNodeShapes());

        var sourceFile = System.getProperty("user.dir")+"\\Output\\PeopleV2\\PeopleV2_QSE_0.7_0_SHACL.ttl";
        var copiedFile = System.getProperty("user.dir")+"\\Output\\PeopleV2\\PeopleV2_QSE_0.7_0_SHACL_V3.ttl";
        RegexUtils.copyFile(sourceFile, copiedFile);
        extractedShapes.fileContentPath = copiedFile;
        ComparisonDiff comparisonDiff = new ComparisonDiff();
        var content = RegexUtils.deleteFromFileWithPruning(extractedShapes, comparisonDiff);
        RegexUtils.saveStringAsFile(content, copiedFile);

        assertTrue("Files are not equal", compareFiles(copiedFile,
                System.getProperty("user.dir")+"\\src\\test\\expected_test_results\\PeopleV2_QSE_-1.0_2_SHACL.ttl"));
    }

    @Test
    public void testDeleteMinCountWithPeople() {
        firstVersionName = "PeopleV2";
        outputPath = System.getProperty("user.dir")+"\\Output\\"+firstVersionName+"\\";
        prepareTest();
        Main.setPruningThresholds("{(-1,0)}");

        QbParser qbParser = new QbParser(100, Constants.RDF_TYPE, graphDbUrl, firstVersionName);
        qbParser.run();

        var nodeShapes = qbParser.shapesExtractor.getNodeShapes();
        ExtractedShapes extractedShapes = new ExtractedShapes();
        extractedShapes.support = 0;
        extractedShapes.confidence = 0.0;
        extractedShapes.setNodeShapes(nodeShapes, false);

        GraphDbUtils.checkShapesInNewGraph(graphDbUrl, "PeopleV3", extractedShapes.getNodeShapes());

        var sourceFile = System.getProperty("user.dir")+"\\Output\\PeopleV2\\PeopleV2_QSE_FULL_SHACL.ttl";
        var copiedFile = System.getProperty("user.dir")+"\\Output\\PeopleV2\\PeopleV2_QSE_FULL_SHACL_V3.ttl";
        RegexUtils.copyFile(sourceFile, copiedFile);
        extractedShapes.fileContentPath = copiedFile;
        ComparisonDiff comparisonDiff = new ComparisonDiff();
        var content = RegexUtils.deleteFromFileWithPruning(extractedShapes, comparisonDiff);
        RegexUtils.saveStringAsFile(content, copiedFile);
        var shape = RegexUtils.getShapeAsString("http://shaclshapes.org/colorCatShapeProperty", content);
        var expected = "\n" +
                "<http://shaclshapes.org/colorCatShapeProperty> rdf:type <http://www.w3.org/ns/shacl#PropertyShape> ;\n" +
                "  <http://shaclshapes.org/confidence> 1E0 ;\n" +
                "  <http://shaclshapes.org/support> \"3\"^^xsd:int ;\n" +
                "  <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#Literal> ;\n" +
                "  <http://www.w3.org/ns/shacl#datatype> xsd:string ;\n" +
                "  <http://www.w3.org/ns/shacl#path> <http://example.org/color> .\n";

        assertEquals("Shapes are not equal", expected, shape);
    }

    @Test
    public void testGetShape() {
        String text = "\n" +
                "<http://shaclshapes.org/aliasPersonShapeProperty> rdf:type <http://www.w3.org/ns/shacl#PropertyShape> ;\n" +
                "  <http://shaclshapes.org/confidence> 8,8889E-1 ;\n" +
                "  <http://shaclshapes.org/support> \"8\"^^xsd:int ;\n" +
                "  <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#Literal> ;\n" +
                "  <http://www.w3.org/ns/shacl#datatype> rdf:langString ;\n" +
                "  <http://www.w3.org/ns/shacl#or> ( [\n" +
                "    <http://shaclshapes.org/confidence> 5,4545E-1 ;\n" +
                "    <http://shaclshapes.org/support> \"6\"^^xsd:int ;\n" +
                "    <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#Literal> ;\n" +
                "    <http://www.w3.org/ns/shacl#datatype> rdf:langString ;\n" +
                "  ] [\n" +
                "    <http://shaclshapes.org/confidence> 1,8182E-1 ;\n" +
                "    <http://shaclshapes.org/support> \"2\"^^xsd:int ;\n" +
                "    <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#Literal> ;\n" +
                "    <http://www.w3.org/ns/shacl#datatype> xsd:string ;\n" +
                "  ] ) ;\n" +
                "  <http://www.w3.org/ns/shacl#or> ( [\n" +
                "    <http://shaclshapes.org/confidence> 5,4545E-1 ;\n" +
                "    <http://shaclshapes.org/support> \"6\"^^xsd:int ;\n" +
                "    <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#Literal> ;\n" +
                "    <http://www.w3.org/ns/shacl#datatype> rdf:langString ;\n" +
                "  ] [\n" +
                "    <http://shaclshapes.org/confidence> 1,8182E-1 ;\n" +
                "    <http://shaclshapes.org/support> \"2\"^^xsd:int ;\n" +
                "    <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#Literal> ;\n" +
                "    <http://www.w3.org/ns/shacl#datatype> xsd:string ;\n" +
                "  ] ) ;\n" +
                "  <http://www.w3.org/ns/shacl#path> <http://dbpedia.org/ontology/alias> .\n" +
                "\n" +
                "<http://shaclshapes.org/aliasQ13474373ShapeProperty> rdf:type <http://www.w3.org/ns/shacl#PropertyShape> ;\n" +
                "  <http://shaclshapes.org/confidence> 1E0 ;\n" +
                "  <http://shaclshapes.org/support> \"1\"^^xsd:int ;\n" +
                "  <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#Literal> ;\n" +
                "  <http://www.w3.org/ns/shacl#datatype> rdf:langString ;\n" +
                "  <http://www.w3.org/ns/shacl#minCount> 1 ;\n" +
                "  <http://www.w3.org/ns/shacl#path> <http://dbpedia.org/ontology/alias> .\n";
        String property = "http://shaclshapes.org/aliasPersonShapeProperty";
        String shape = RegexUtils.getShapeAsString(property, text);
        assertEquals(shape, "\n<http://shaclshapes.org/aliasPersonShapeProperty> rdf:type <http://www.w3.org/ns/shacl#PropertyShape> ;\n" +
                "  <http://shaclshapes.org/confidence> 8,8889E-1 ;\n" +
                "  <http://shaclshapes.org/support> \"8\"^^xsd:int ;\n" +
                "  <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#Literal> ;\n" +
                "  <http://www.w3.org/ns/shacl#datatype> rdf:langString ;\n" +
                "  <http://www.w3.org/ns/shacl#or> ( [\n" +
                "    <http://shaclshapes.org/confidence> 5,4545E-1 ;\n" +
                "    <http://shaclshapes.org/support> \"6\"^^xsd:int ;\n" +
                "    <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#Literal> ;\n" +
                "    <http://www.w3.org/ns/shacl#datatype> rdf:langString ;\n" +
                "  ] [\n" +
                "    <http://shaclshapes.org/confidence> 1,8182E-1 ;\n" +
                "    <http://shaclshapes.org/support> \"2\"^^xsd:int ;\n" +
                "    <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#Literal> ;\n" +
                "    <http://www.w3.org/ns/shacl#datatype> xsd:string ;\n" +
                "  ] ) ;\n" +
                "  <http://www.w3.org/ns/shacl#or> ( [\n" +
                "    <http://shaclshapes.org/confidence> 5,4545E-1 ;\n" +
                "    <http://shaclshapes.org/support> \"6\"^^xsd:int ;\n" +
                "    <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#Literal> ;\n" +
                "    <http://www.w3.org/ns/shacl#datatype> rdf:langString ;\n" +
                "  ] [\n" +
                "    <http://shaclshapes.org/confidence> 1,8182E-1 ;\n" +
                "    <http://shaclshapes.org/support> \"2\"^^xsd:int ;\n" +
                "    <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#Literal> ;\n" +
                "    <http://www.w3.org/ns/shacl#datatype> xsd:string ;\n" +
                "  ] ) ;\n" +
                "  <http://www.w3.org/ns/shacl#path> <http://dbpedia.org/ontology/alias> .\n");
    }

    public static boolean compareFiles(String filePath1, String filePath2) {
        try (BufferedReader reader1 = new BufferedReader(new FileReader(filePath1));
             BufferedReader reader2 = new BufferedReader(new FileReader(filePath2))) {

            String line1 = "";
            String line2 = "";

            while ((line1 = reader1.readLine()) != null && (line2 = reader2.readLine()) != null) {
                if (!line1.trim().equals(line2.trim())) {
                    return false;
                }
            }

            return (line1 == null || line1.isEmpty()) && (line2 == null || line2.isEmpty());

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

}
