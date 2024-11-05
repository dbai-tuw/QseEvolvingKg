package sparqlshapechecker.utils;

import org.jetbrains.annotations.NotNull;
import shape_comparator.data.ExtractedShapes;
import shape_comparator.data.NodeShape;
import shape_comparator.data.PropertyShape;
import sparqlshapechecker.SparqlShapeChecker;
import sparqlshapechecker.comparator.ComparisonDiff;
import shape_comparator.data.ShaclOrListItem;

import java.io.*;
import java.nio.file.*;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static shape_comparator.services.Utils.getShapeAsStringFormatted;

public class RegexUtils {
    private static final Logger LOGGER = Logger.getLogger(SparqlShapeChecker.class.getName());

    public static String deleteFromFileWithPruning(ExtractedShapes extractedShapes, ComparisonDiff comparisonDiff) {
        String fileContent = extractedShapes.getFileAsString(false);
        int supportThreshold = extractedShapes.support;
        double confidenceThreshold = extractedShapes.confidence;
        for (var nodeShape : extractedShapes.getNodeShapes()) {
            if(nodeShape.support <= supportThreshold) { //QSE also compares with <=, not with <
                //special case for multiple nodeshapes (different targetclasses) -> don't delete nodeshape if other nodeshape object still exist
                var otherNodeShapes = extractedShapes.getNodeShapes().stream().filter(ns -> ns.getIri().toString().equals(nodeShape.getIri().toString()) && !ns.targetClass.equals(nodeShape.targetClass));
                if(otherNodeShapes.findAny().isEmpty()) { //special case for multiple node shapes with different target classes
                    comparisonDiff.deletedNodeShapes.add(nodeShape.getIri().toString());
                    fileContent = deleteIriFromString(nodeShape.getIri().toString(), fileContent, nodeShape.errorDuringGeneration);
                    for (var propertyShape : nodeShape.propertyShapes) {
                        comparisonDiff.deletedPropertyShapes.add(propertyShape.iri.toString());
                        fileContent = deleteIriFromString(propertyShape.iri.toString(), fileContent, propertyShape.errorDuringGeneration);
                    }
                }
            }
            else {
                for(var propertyShape : nodeShape.propertyShapes) {
                    var allOrItemsUnderThreshold = propertyShape.orItems != null && !propertyShape.orItems.isEmpty()
                            && propertyShape.orItems.stream().allMatch(o -> o.support <= supportThreshold || o.confidence <= confidenceThreshold);

                    if ((propertyShape.support <= supportThreshold || propertyShape.confidence <= confidenceThreshold) && (propertyShape.orItems == null || propertyShape.orItems.isEmpty()) || allOrItemsUnderThreshold) {
                        comparisonDiff.deletedPropertyShapes.add(propertyShape.iri.toString());
                        fileContent = deleteIriFromString(propertyShape.iri.toString(), fileContent, propertyShape.errorDuringGeneration);
                        fileContent = deletePropertyShapeReferenceWithIriFromString(propertyShape.iri.toString(), fileContent, propertyShape.errorDuringGeneration);
                    } else if (propertyShape.support > supportThreshold && propertyShape.confidence > confidenceThreshold) {
                        if(propertyShape.orItems != null) {
                            var numberOfOrItemsLeft = propertyShape.orItems.stream().filter(o -> o.support > supportThreshold && o.confidence > confidenceThreshold).count();
                            String originalShape = getShapeAsString(propertyShape.iri.toString(), fileContent);
                            if (originalShape.contains("<http://www.w3.org/ns/shacl#or>")) {
                                String modifiedShape = originalShape;
                                for (var orItem : propertyShape.orItems) {
                                    if (orItem.support <= supportThreshold || orItem.confidence <= confidenceThreshold) {
                                        modifiedShape = deleteShaclOrItemWithIriFromString(orItem, modifiedShape, false);
                                    }
                                }
                                if (numberOfOrItemsLeft == 1 && propertyShape.orItems.size() != 1) {
                                    var item = propertyShape.orItems.stream().filter(o -> o.support > supportThreshold && o.confidence > confidenceThreshold).findFirst().get();
                                    int newSupport = item.support;
                                    double newConfidence = item.confidence;

                                    modifiedShape = extractedShapes.prefixLines + modifiedShape;
                                    modifiedShape = GraphDbUtils.deleteOrListAndConnectToParentNode(modifiedShape, propertyShape.iri.toString(), newSupport, newConfidence);
                                    modifiedShape = RegexUtils.removeLinesWithPrefix(modifiedShape);
                                }
                                fileContent = fileContent.replace(originalShape, modifiedShape);
                            }
                        }

                        fileContent = removeMinCount(nodeShape, propertyShape, fileContent);
                    }
                }
            }
        }
        return fileContent;
    }
    private static String removeMinCount(NodeShape nodeShape, PropertyShape propertyShape, String fileContent) {
        if(!Objects.equals(propertyShape.support, nodeShape.support)) {
            String shape = getShapeAsString(propertyShape.iri.toString(), fileContent);
            String minCountString = "<http://www.w3.org/ns/shacl#minCount>";
            if(shape.contains(minCountString)) {
                String regexPattern = "\n  "+minCountString+" 1 ;";
                String newShape = getReplacedFileWithRegex("MinCount " + propertyShape.iri.toString(), shape, regexPattern);
                fileContent = fileContent.replace(shape, newShape);
            }
        }
        return fileContent;
    }

    public static String removeLinesWithPrefix(String input) {
        StringBuilder result = new StringBuilder();
        String[] lines = input.split("\n");

        for (String line : lines) {
            if (!line.startsWith("@prefix")) {
                result.append(line).append("\n");
            }
        }
        return result.toString();
    }

    public static void saveStringAsFile(String content, String filePath) {
        byte[] bytes = content.getBytes();

        try {
            Path path = Paths.get(filePath);
            Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            LOGGER.severe("Failed to save the file: " + e.getMessage());
        }
    }

    public static void copyFile(String sourceFilePath, String destinationFilePath) {
        Path sourcePath = Paths.get(sourceFilePath);
        Path destinationPath = Paths.get(destinationFilePath);
        try {
            Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.severe("Failed to copy the file: " + e.getMessage());
        }
    }

    public static String deleteIriFromString(String iri, String file, boolean errorDuringGeneration) {
        if(errorDuringGeneration)
            return file;
        String iriWithEscapedChars = iri.replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)");
        String regexPattern = String.format("\n<%s>.*? \\.\n", iriWithEscapedChars);
        return getReplacedFileWithRegex(iri, file, regexPattern);
    }

    public static String deletePropertyShapeReferenceWithIriFromString(String iri, String file, boolean errorDuringGeneration) {
        if(errorDuringGeneration)
            return file;
        String iriWithEscapedChars = iri.replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)");
        String regexPattern = String.format("  <http://www.w3.org/ns/shacl#property> <%s> \\;\n", iriWithEscapedChars);
        return getReplacedFileWithRegex(iri, file, regexPattern);
    }

    @NotNull
    private static String getReplacedFileWithRegex(String iri, String file, String regexPattern) {
        Pattern pattern = Pattern.compile(regexPattern, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(file);
        if (!matcher.find()) {
            LOGGER.warning("Delete did not work for " + iri);
            return file;
        }
        String match = matcher.group();
        return file.replace(match, "");
    }

    //only works if at least one or item stays
    public static String deleteShaclOrItemWithIriFromString(ShaclOrListItem orItem, String shape, boolean errorDuringGeneration) {
        if(errorDuringGeneration)
            return shape;

        String regexPart = "";
        if (orItem.nodeKind.toString().equals("http://www.w3.org/ns/shacl#Literal"))
            if(orItem.dataType == null)
                regexPart = String.format(" \\<http://www.w3.org/ns/shacl#datatype> <?[^<]*%s>?", orItem.dataTypeOrClass.getLocalName()); //problem with full name or short name e.g. rdf:label
            else
                regexPart = String.format(" \\<http://www.w3.org/ns/shacl#datatype> <?[^<]*%s>?", orItem.dataType);
        else if(orItem.nodeKind.toString().equals("http://www.w3.org/ns/shacl#IRI")) {
            if(orItem.classIri == null)
                regexPart = String.format(" \\<http://www.w3.org/ns/shacl#class> <?[^<]*%s>?", orItem.dataTypeOrClass.getLocalName());
            else
                regexPart = String.format(" \\<http://www.w3.org/ns/shacl#class> <?[^<]*%s>?", orItem.classIri);
        }

        String regexPattern = String.format(" \\[[^\\[\\]]*?<http://www.w3.org/ns/shacl#NodeKind> <%s>[^\\[\\]]*?%s[^\\]\\[]*?\\]", orItem.nodeKind, regexPart);
        Pattern pattern = Pattern.compile(regexPattern, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(shape);
        if (!matcher.find()) {
            LOGGER.warning("Delete did not work for " + orItem.propertyShape.iri.toString() + ", " + orItem);
            return shape;
        }
        String match = matcher.group();
        return shape.replace(match, "");
    }

    public static String getShapeAsString(String iri, String file) {
        Matcher matcher = getIriWithEscapedCharacters(iri, file);
        return matcher.group();
    }

    private static Matcher getIriWithEscapedCharacters(String iri, String file) {
        String iriWithEscapedChars = iri.replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)");
        String regexPattern = String.format("\n<%s>.*? \\.\n", iriWithEscapedChars);
        Pattern pattern = Pattern.compile(regexPattern, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(file);
        if (!matcher.find()) {
            LOGGER.warning("Could not find shape " + iri);
        }
        return matcher;
    }

    public static String removeNodeTripleFromShape(String shape) {
        return shape.replaceAll("(?m)^\\s*<http://www.w3.org/ns/shacl#node>.*;\\s*$", "");
    }

    public static String getShapeAsStringFormattedFromFile(String iri, String file, String prefixLines) {
        Matcher matcher = getIriWithEscapedCharacters(iri, file);
        return getShapeAsStringFormatted(prefixLines, matcher);
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

    public static String getFileAsString(String path) {
        StringBuilder fileContent = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                fileContent.append(line).append("\n");
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Exception occurred", e);
        }
        return fileContent.toString();
    }

    public static String replaceCommaWithDotInConfidenceValues(String shapeAsString) {
        Pattern pattern = Pattern.compile("(<http://shaclshapes.org/confidence> )([0-9]),([0-9]+.*?);");
        Matcher matcher = pattern.matcher(shapeAsString);

        return matcher.replaceAll("$1$2.$3;");
    }
}
