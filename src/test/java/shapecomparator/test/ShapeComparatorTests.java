package shapecomparator.test;

import cs.Main;
import cs.qse.filebased.Parser;
import cs.qse.querybased.nonsampling.QbParser;
import cs.utils.Constants;
import de.atextor.turtle.formatter.FormattingStyle;
import de.atextor.turtle.formatter.TurtleFormatter;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.*;
import shape_comparator.services.Utils;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cs.Main.setOutputFilePathForJar;

//Just used for debugging, not for testing
public class ShapeComparatorTests {

    @org.junit.Test
    public void testOrItems() {
        var testshape = "<http://shaclshapes.org/religion_1PersonShapeProperty> rdf:type <http://www.w3.org/ns/shacl#PropertyShape> ;\n" +
                "  <http://www.w3.org/ns/shacl#or> ( [\n" +
                "    <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#Literal> ;\n" +
                "    <http://www.w3.org/ns/shacl#datatype> xrdf:langString ;\n" +
                "  ] [\n" +
                "    <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#IRI> ;\n" +
                "  ] ) ;\n" +
                "  <http://www.w3.org/ns/shacl#or> ( [\n" +
                "    <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#Literal> ;\n" +
                "    <http://www.w3.org/ns/shacl#datatype> xsd:gMonthDay ;\n" +
                "  ] [\n" +
                "    <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#Literal> ;\n" +
                "    <http://www.w3.org/ns/shacl#datatype> rdf:langString ;\n" +
                "  ] [\n" +
                "    <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#IRI> ;\n" +
                "  ] ) ;\n" +
                "  <http://www.w3.org/ns/shacl#or> ( [\n" +
                "    <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#Literal> ;\n" +
                "    <http://www.w3.org/ns/shacl#datatype> xsd:gMonthDay ;\n" +
                "  ] [\n" +
                "    <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#Literal> ;\n" +
                "    <http://www.w3.org/ns/shacl#datatype> rdf:langString ;\n" +
                "  ] [\n" +
                "    <http://www.w3.org/ns/shacl#NodeKind> <ahttp://www.w3.org/ns/shacl#IRI> ;\n" +
                "  ] ) ;\n" +
                "  <http://www.w3.org/ns/shacl#path> <http://dbpedia.org/property/religion> .\n";
        System.out.println(Utils.reOrderOrItems(testshape));
    }
    @org.junit.Test
    public void testFileBased() {
        Main.setResourcesPathForJar(System.getProperty("user.dir")+"\\resources");
        setOutputFilePathForJar("/Users/evapu/Documents/GitHub/QseEvolvingKg/qse/Output/TEMP/fileBased/");
//        Main.setPruningThresholds("{(0,10)}"); //todo
//        Main.annotateSupportConfidence = "true";
        String path = "/Users/evapu/Downloads/alldata.IC.nt/000001.nt/000001.nt";
//        String path = "/Users/evapu/Downloads/qse-main/qse-main/src/main/resources/lubm-mini.nt";
//        String path = "/Users/evapu/Documents/GitHub/QseEvolvingKg/QseEvolvingKgWebApp/graphs/pre_configured/film.nt";
        Main.datasetName = "bearb";

        Parser parser = new Parser(path, 100, 1000000, "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>");
        parser.run();
    }
    @org.junit.Test
    public void testQueryBased() {
        Main.setResourcesPathForJar("/Users/evapu/Downloads/qse-main/qse-main/src/main/resources");
        setOutputFilePathForJar("/Users/evapu/Documents/GitHub/QseEvolvingKg/qse/Output/TEMP/qb/");
//        Main.setPruningThresholds("{(-1,10)}"); //todo
//        Main.annotateSupportConfidence = "true";
        var repoName = "Bear-B";
        Main.datasetName = repoName;

        QbParser qbParser = new QbParser(10, Constants.RDF_TYPE, "http://localhost:7201/",repoName);
        qbParser.run();
    }
    @org.junit.Test
    public void reorderOR() {
        String shape = "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
                "\n" +
                "<http://shaclshapes.org/attendanceThingShapeProperty> rdf:type <http://www.w3.org/ns/shacl#PropertyShape> ;\n" +
                "<http://www.w3.org/ns/shacl#or> ( [\n" +
                "<http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#Literal> ;\n" +
                "<http://www.w3.org/ns/shacl#datatype> xsd:integer ;\n" +
                "] [\n" +
                "<http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#Literal> ;\n" +
                "<http://www.w3.org/ns/shacl#datatype> rdf:langString ;\n" +
                "] ) ;\n" +
                "<http://www.w3.org/ns/shacl#path> <http://dbpedia.org/property/attendance> .";
        System.out.println(Utils.reOrderOrItems(shape));
    }

    public void generateText_includeSpecialCharacters() {

        StringBuilder fileContent = new StringBuilder();
        StringBuilder prefixLines = new StringBuilder();

        // Open the text file for reading
        try (BufferedReader reader = new BufferedReader(new FileReader("C:\\Users\\evapu\\Documents\\GitHub\\QseEvolvingKg\\QseEvolvingKgWebApp\\Output\\bearb-1-Original_QSE_FULL_SHACL.ttl"))) {
            String line;

            // Read the file line by line and append to StringBuilder
            while ((line = reader.readLine()) != null) {
                fileContent.append(line).append("\n");
                if (line.contains("@prefix"))
                    prefixLines.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        Resource iri = ResourceFactory.createResource("http://shaclshapes.org/topScorer(s)_Q476028ShapeProperty");
        String iriWithEscapedChars = iri.toString().replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)");
        String regexPattern = String.format("\n<%s>.*? \\.", iriWithEscapedChars);

        // Compile the regular expression pattern
        Pattern pattern = Pattern.compile(regexPattern, Pattern.DOTALL);

        Matcher matcher = pattern.matcher(fileContent.toString());

        matcher.find();
        String match = matcher.group();
//        System.out.println(match);

        var model = ModelFactory.createDefaultModel();
        model.read(new java.io.StringReader(prefixLines + match), null, "TURTLE"); // Assuming Turtle format, change as needed

        Resource iriSupport = ResourceFactory.createResource("http://shaclshapes.org/support");
        Resource iriConfidence = ResourceFactory.createResource("http://shaclshapes.org/confidence");


        String queryString = String.format("CONSTRUCT {?s ?p ?o} WHERE { ?s ?p ?o. FILTER (?p != <%s> && ?p != <%s>)}", iriSupport, iriConfidence);

        var query = QueryFactory.create(queryString);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
            org.apache.jena.rdf.model.Model jenaModel = qexec.execConstruct();
            TurtleFormatter formatter = new TurtleFormatter(FormattingStyle.DEFAULT);
            OutputStream outputStream = new ByteArrayOutputStream();
            formatter.accept(jenaModel, outputStream);
            System.out.println(outputStream.toString().replaceAll("\n+$", ""));
        }
    }

    public void test() {
        StringBuilder fileContent = new StringBuilder();
        StringBuilder prefixLines = new StringBuilder();

        // Open the text file for reading
        try (BufferedReader reader = new BufferedReader(new FileReader("C:\\Users\\evapu\\Documents\\GitHub\\QseEvolvingKg\\QseEvolvingKgWebApp\\Output\\film-1-Original_QSE_FULL_SHACL.ttl"))) {
            String line;

            // Read the file line by line and append to StringBuilder
            while ((line = reader.readLine()) != null) {
                fileContent.append(line).append("\n");
                if(line.contains("@prefix"))
                    prefixLines.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        String regexPattern = "\n<http://shaclshapes.org/rangeDatatypePropertyShapeProperty>.*? \\.";

        // Compile the regular expression pattern
        Pattern pattern = Pattern.compile(regexPattern, Pattern.DOTALL);

        Matcher matcher = pattern.matcher(fileContent.toString());

        matcher.find();
        String match = matcher.group();
//        System.out.println(match);

        var model = ModelFactory.createDefaultModel();
        model.read(new java.io.StringReader(prefixLines+match), null, "TURTLE"); // Assuming Turtle format, change as needed

        Resource iri = ResourceFactory.createResource("http://shaclshapes.org/rangeDatatypePropertyShapeProperty");
        Resource iriSupport = ResourceFactory.createResource("http://shaclshapes.org/support");
        Resource iriConfidence = ResourceFactory.createResource("http://shaclshapes.org/confidence");


        String queryString = String.format("CONSTRUCT {?s ?p ?o} WHERE { ?s ?p ?o. FILTER (?p != <%s> && ?p != <%s>)}", iriSupport, iriConfidence);

        var query = QueryFactory.create(queryString);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
            org.apache.jena.rdf.model.Model jenaModel = qexec.execConstruct();
            TurtleFormatter formatter = new TurtleFormatter(FormattingStyle.DEFAULT);
            OutputStream outputStream = new ByteArrayOutputStream();
            formatter.accept(jenaModel, outputStream);
            System.out.println(outputStream.toString().replaceAll("\n+$", ""));
        }
    }

}
