package shape_comparator.services;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.server.VaadinSession;
import de.atextor.turtle.formatter.FormattingStyle;
import de.atextor.turtle.formatter.TurtleFormatter;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.jetbrains.annotations.NotNull;
import shape_comparator.data.ExtractedShapes;
import shape_comparator.data.Graph;
import shape_comparator.data.Version;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Utils {
    private static final Logger LOGGER = Logger.getLogger(Utils.class.getName());

    public static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final String preconfiguredFolderName = "pre_configured";
    public static final String shapesPath = "shapes";


    public static String getGraphDirectory() {
        String projectDirectory = System.getProperty("user.dir");
        projectDirectory = projectDirectory + File.separator + "graphs" + File.separator;
        return projectDirectory;
    }

    public static List<ComboBoxItem> getAllGraphs(GraphService graphService) {
        return graphService.listAll().stream()
                .map(graph -> new Utils.ComboBoxItem(graph.getName(), graph.getId()))
                .collect(Collectors.toList());
    }

    public static List<ComboBoxItem> getAllVersions(VersionService versionService, Long graphId) {
        return versionService.listByGraphId(graphId).stream()
                .map(version -> new Utils.ComboBoxItem(version.getVersionNumber() + " - " + version.getName(), version.getId()))
                .collect(Collectors.toList());
    }

    public static class ComboBoxItem {
        public String label;
        public Long id;

        public ComboBoxItem(String label, Long id) {
            this.label = label;
            this.id = id;
        }

        public ComboBoxItem() {

        }
    }

    public static void setComboBoxGraphData(GraphService graphService, Select<ComboBoxItem> selectItemGraph) {
        List<Utils.ComboBoxItem> graphs = Utils.getAllGraphs(graphService);
        selectItemGraph.setItems(graphs);
        selectItemGraph.setItemLabelGenerator(item -> item.label);
        var selectedGraphId = (Long) VaadinSession.getCurrent().getAttribute("shapes_currentGraphId");
        var firstItem = graphs.stream().findFirst();

        if (selectedGraphId != null) {
            var graphItem = selectItemGraph.getDataProvider().fetch(new Query<>()).filter(g -> g.id.equals(selectedGraphId)).findFirst();
            if (graphItem.isPresent())
                selectItemGraph.setValue(graphItem.get());
            else if (firstItem.isPresent())
                selectItemGraph.setValue(firstItem.get());
        } else if (firstItem.isPresent())
            selectItemGraph.setValue(firstItem.get());
    }

    public static void setComboBoxVersionsData(Long graphId, VersionService versionService, Select<ComboBoxItem> selectItemVersion) {
        List<Utils.ComboBoxItem> versions = Utils.getAllVersions(versionService, graphId);
        selectItemVersion.setItems(versions);
        selectItemVersion.setItemLabelGenerator(item -> item.label);
        var currentVersionId = (Long) VaadinSession.getCurrent().getAttribute("shapes_currentVersionId");
        var firstItem = versions.stream().findFirst();

        if (currentVersionId != null) {
            var graphItem = selectItemVersion.getDataProvider().fetch(new Query<>()).filter(v -> v.id.equals(currentVersionId)).findFirst();
            if (graphItem.isPresent())
                selectItemVersion.setValue(graphItem.get());
            else if (firstItem.isPresent())
                selectItemVersion.setValue(firstItem.get());
        } else if (firstItem.isPresent())
            selectItemVersion.setValue(firstItem.get());
    }

    public static Boolean usePrettyFormatting = true; //debugging

    public static String generateTTLFromRegex(IRI iri, String fileContent, String prefixLines) {
        //TODO maybe also replace other characters
        String iriWithEscapedChars = iri.toString().replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)");
        String regexPattern = String.format("\n<%s>.*? \\.", iriWithEscapedChars);
        Pattern pattern = Pattern.compile(regexPattern, Pattern.DOTALL);

        Matcher matcher = pattern.matcher(fileContent);

        if (!matcher.find()) {
            System.out.println("No text generated for " + iri.getLocalName());
            return "";
        }
        return getShapeAsStringFormatted(prefixLines, matcher);
    }

    @NotNull
    public static String getShapeAsStringFormatted(String prefixLines, Matcher matcher) {
        String match = matcher.group();

        var model = org.apache.jena.rdf.model.ModelFactory.createDefaultModel();
        model.read(new StringReader(prefixLines + match), null, "TURTLE"); // Assuming Turtle format, change as needed

        org.apache.jena.rdf.model.Resource iriSupport = ResourceFactory.createResource("http://shaclshapes.org/support");
        org.apache.jena.rdf.model.Resource iriConfidence = ResourceFactory.createResource("http://shaclshapes.org/confidence");

        String queryString = String.format("CONSTRUCT {?s ?p ?o} WHERE { ?s ?p ?o. FILTER (?p != <%s> && ?p != <%s>)}", iriSupport, iriConfidence);

        var query = QueryFactory.create(queryString);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
            org.apache.jena.rdf.model.Model jenaModel = qexec.execConstruct();
            TurtleFormatter formatter = new TurtleFormatter(FormattingStyle.DEFAULT);
            OutputStream outputStream = new ByteArrayOutputStream();
            formatter.accept(jenaModel, outputStream);
            String cleanedString = reorderShaclInItems(outputStream.toString());
            String cleanedStringOrItems = reOrderOrItems(cleanedString);
            return cleanedStringOrItems.replaceAll("\n+$", "");
        }
    }

    public static String reorderShaclInItems(String input) {
        String searchString = "shacl#in";
        if (input.contains(searchString) && input.indexOf(searchString) != input.lastIndexOf(searchString)) {
            String[] lines = input.split("\n");
            List<String> inLines = new ArrayList<>();
            for (String line : lines) {
                if (line.contains(searchString))
                    inLines.add(line.trim());
            }
            Collections.sort(inLines);
            List<String> orderedLines = new ArrayList<>();
            int remainingIndex = 0;
            for (String line : lines) {
                if (line.contains(searchString)) {
                    orderedLines.add(inLines.get(remainingIndex));
                    remainingIndex++;
                } else
                    orderedLines.add(line);
            }

            return String.join("\n", orderedLines);
        } else
            return input;
    }

    public static String reOrderOrItems(String input) {
        try {
            String orItemString = "<http://www.w3.org/ns/shacl#or>"; //highly dependent on turtlePrettyFormatter
            String patternString = orItemString + " \\([^\\)]*\\) ;"; //would not work for names with '('
            Pattern patternOrParent = Pattern.compile(patternString, Pattern.DOTALL);
            Matcher matcherOrParent = patternOrParent.matcher(input);
            var inputCopy = input;
            List<String> orObjects = new ArrayList<>();
            while(matcherOrParent.find()) {
                var firstResultParent = matcherOrParent.group();
                var patternOrObjects = Pattern.compile("\\[[^\\]]*\\]", Pattern.DOTALL);
                var matcherObjects = patternOrObjects.matcher(firstResultParent);
                List<String> objects = new ArrayList<>();
                while (matcherObjects.find()) {
                    String object = matcherObjects.group().trim(); // Extract contents of brackets and trim whitespace
                    objects.add(object);
                }
                objects.sort(Comparator.comparing(o -> o));
                var newString = firstResultParent;
                StringBuilder newOrItems = new StringBuilder();
                for (var m : objects) {
                    newString = newString.replace(m, "");
                    newOrItems.append(m).append(" ");
                }
                var newOrItemString = insertAfter(newString, orItemString + " ( ", newOrItems.toString());
                inputCopy = inputCopy.replace(firstResultParent, newOrItemString);
                orObjects.add(newOrItemString);
            }

            //reorder or-objects in general (in case of multiple
            orObjects.sort(Comparator.comparing(o -> o));
            StringBuilder newOrItems = new StringBuilder();
            for (var m : orObjects) {
                inputCopy = inputCopy.replace(m, "");
                newOrItems.append(m).append(" \n  ");
            }
            int index = input.indexOf(orItemString);
            if (index == -1)
                return input;
            inputCopy = insertAfter(inputCopy, input.substring(0, index), newOrItems.toString());
            return inputCopy;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Exception occurred", ex);
            return input;
        }
    }

    public static String insertAfter(String original, String searchString, String toInsert) {
        int index = original.indexOf(searchString);
        if (index == -1) {
            return original;
        }
        String part1 = original.substring(0, index + searchString.length());
        String part2 = original.substring(index + searchString.length()).trim();

        return part1 + toInsert + part2;
    }

    public static String escapeNew(String input) {
        if (usePrettyFormatting) {
            return input.replaceAll("\r", "").replaceAll("\n", "\\\\n");
        } else {
            input = input.replaceFirst("\r\n", "");
            return input.replaceAll("\r\n", "\\\\\\\\n");
        }
    }

    public static String getComboBoxLabelForExtractedShapes(ExtractedShapes shape) {
        if (shape.getComboBoxString().isEmpty())
            shape.generateComboBoxString();
        return shape.getComboBoxString();
    }

    public static void handleSaveFile(Graph graph, VersionService versionService, InputStream inputStream, String versionName, String preConfiguredGraphPath) {
        Version version = versionService.generateNewVersion(graph);
        if (preConfiguredGraphPath.isEmpty()) {
            var dir = Utils.getGraphDirectory();
            String directory = dir + graph.getName() + File.separator;
            String generatedFileName = graph.getName() + "_" + version.getVersionNumber() + ".nt";
            String filePath = directory + generatedFileName;
            version.setPath(filePath);
            File file = new File(directory);

            if (!file.exists()) {
                file.mkdirs();
            }

            File outputFile = new File(directory, generatedFileName);

            try (OutputStream outputStream = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            version.setPath(preConfiguredGraphPath);
        }
        version.setName(versionName);
        versionService.update(version);
    }

    public static void setGraphOrVersionGuiFields(TextField textFieldGraphName, Button buttonSave, Upload uploadGraphFile, Select<String> preconfiguredGraphs) {
        textFieldGraphName.setHeight("min-content");

        textFieldGraphName.setLabel("Name");
        textFieldGraphName.setWidth("min-content");
        buttonSave.setWidth("min-content");
        buttonSave.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        textFieldGraphName.setRequiredIndicatorVisible(true);
        uploadGraphFile.setUploadButton(new Button("Or upload .nt file"));
        buttonSave.setTooltipText("This will copy the file to the project directory");

        uploadGraphFile.setAcceptedFileTypes(".nt");

        var files = Utils.listFilesInStaticGraphDirectory();
        preconfiguredGraphs.setItems(files);
        preconfiguredGraphs.setItemLabelGenerator(item -> item == null ? "" : item.substring(item.indexOf(Utils.preconfiguredFolderName) + Utils.preconfiguredFolderName.length() + 1));
        preconfiguredGraphs.setLabel("Select pre-configured graph");
        preconfiguredGraphs.setEmptySelectionAllowed(true);
    }

    public static Boolean isEmptyItemSelected(Select<String> preconfiguredGraphs) {
        var value = preconfiguredGraphs.getValue();
        return preconfiguredGraphs.getValue() == null || preconfiguredGraphs.getValue().isEmpty();
    }

    public static List<String> listFilesInStaticGraphDirectory() {
        Path projectDirectory = Paths.get("").toAbsolutePath().resolve("graphs" + File.separator + preconfiguredFolderName);
        try {
            if (!Files.exists(projectDirectory))
                Files.createDirectory(projectDirectory);
            return Files.walk(projectDirectory)
                    .filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".nt"))
                    .map(Path::toString)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static IRI getIRI(String iri) {
        try {
            return iri == null || iri.isEmpty() || iri.isBlank() ? null :
                    Values.iri(iri.replace("<", "").replace(">", ""));
        } catch (Exception ex) {
            LOGGER.warning("Problem converting " + iri + " to IRI");
        }
        return null;
    }

}