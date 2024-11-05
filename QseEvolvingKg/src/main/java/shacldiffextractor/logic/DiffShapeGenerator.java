package shacldiffextractor.logic;

import cs.Main;
import cs.qse.filebased.Parser;
import cs.qse.filebased.SupportConfidence;
import cs.utils.Constants;
import cs.utils.Tuple3;
import cs.utils.Utils;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import shape_comparator.data.ExtractedShapes;
import shape_comparator.data.PropertyShape;
import sparqlshapechecker.comparator.EditedShapesComparisonObject;
import sparqlshapechecker.utils.RegexUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

//uses internal QSE objects prepared by the DiffExtractor to generate new version of shapes
public class DiffShapeGenerator {
    Map<Integer, Map<Integer, Set<Integer>>> oldShapesMap;
    Map<Integer, Map<Integer, Set<Integer>>> newShapesMap;
    Parser parser;
    public Map<Integer, Map<Integer, Set<Integer>>> updatedShapes;
    public ExtractedShapes updatedExtractedShapes;
    HashMap<Integer, Set<Integer>> editedShapes;
    public HashMap<Integer, Set<Integer>> deletedShapes = new HashMap<>();
    public ExtractedShapes resultExtractedShapes;
    public ArrayList<String> deletedNodeShapeNames = new ArrayList<>();
    public ArrayList<String> deletedPropertyShapeNames = new ArrayList<>();
    public ArrayList<String> addedNodeShapeNames = new ArrayList<>();
    public ArrayList<String> addedPropertyShapeNames = new ArrayList<>();
    public ArrayList<EditedShapesComparisonObject> editedNodeShapes = new ArrayList<>();
    public ArrayList<EditedShapesComparisonObject> editPropertyShapes = new ArrayList<>();
    public Set<String> propertyShapeNamesToCheckForEditedShapes = new HashSet<>();
    public Set<String> nodeShapeNamesToCheckForEditedShapes = new HashSet<>();
    public String newVersionName;
    public String oldVersionName;
    int support;
    double confidence;

    public DiffShapeGenerator(DiffExtractor diffExtractor) {
        this.oldShapesMap = diffExtractor.originalClassToPropWithObjTypes;
        this.newShapesMap = DiffExtractor.deepClone(diffExtractor.parser.classToPropWithObjTypes);
        this.parser = diffExtractor.parser;
        this.editedShapes = diffExtractor.editedShapesMap;
        this.resultExtractedShapes = diffExtractor.originalExtractedShapes;
        this.support = diffExtractor.supportThreshold;
        this.confidence = diffExtractor.confidenceThreshold;
    }

    public ExtractedShapes generateUpdatedShapesWithQse() {
        parser.classToPropWithObjTypes = this.updatedShapes;
        var oldDataSetName = Main.datasetName;
        var oldOutputPath = Main.outputFilePath;
        Main.datasetName = oldVersionName+"_DiffTo"+newVersionName;
        Path path = Paths.get(Main.outputFilePath).getParent();
        var diffPath = path.toAbsolutePath() + File.separator + "Diff" + File.separator;
        DiffManager.clearOutputDirectory(Paths.get(diffPath + File.separator + "db_default"));
        Main.outputFilePath = diffPath;
        Main.setPruningThresholds("{("+confidence+","+support+")}");
        parser.extractSHACLShapes(support != 0 || confidence != 0, false);
        updatedExtractedShapes = new ExtractedShapes();
        updatedExtractedShapes.support = support;
        updatedExtractedShapes.confidence = confidence *100;
        updatedExtractedShapes.setNodeShapesWithPruning(parser.shapesExtractor.getNodeShapes());
        updatedExtractedShapes.fileContentPath = parser.shapesExtractor.getOutputFileAddress();
        updatedExtractedShapes.getFileAsString(true);
        Main.datasetName = oldDataSetName;
        Main.outputFilePath = oldOutputPath;
        return updatedExtractedShapes;
    }

    public void computeDeletedShapes() {
        deletedShapes = new HashMap<>();
        for(var nodeShapeEntry : oldShapesMap.entrySet()) {
            var nodeShapeKey = nodeShapeEntry.getKey();
            boolean areThresholdsNotMet = !parser.classEntityCount.containsKey(nodeShapeKey) || parser.classEntityCount.get(nodeShapeKey) < this.support;
            if(!newShapesMap.containsKey(nodeShapeKey) || areThresholdsNotMet) {
                deletedShapes.put(nodeShapeKey, nodeShapeEntry.getValue().keySet());
            }
            else {
                for(var propertyShapeId : nodeShapeEntry.getValue().keySet()) {
                    var keyForShapeTripletSupport = parser.statsComputer.shapeTripletSupport.keySet().stream().filter(k -> k._1.equals(nodeShapeKey) && k._2.equals(propertyShapeId)).toList();
                    areThresholdsNotMet = checkPruningThresholds(keyForShapeTripletSupport);
                    if(!newShapesMap.get(nodeShapeKey).containsKey(propertyShapeId) || areThresholdsNotMet) {
                        if(!deletedShapes.containsKey(nodeShapeKey)) {
                            deletedShapes.put(nodeShapeKey, new HashSet<>());
                        }
                        var propEntryDeleted = deletedShapes.get(nodeShapeKey);
                        propEntryDeleted.add(propertyShapeId);
                    }
                }
            }
        }
    }

    private boolean checkPruningThresholds(List<Tuple3<Integer, Integer, Integer>> keysForShapeTripletSupport) {
        SupportConfidence maxConfidenceItem = null;
        for(var key : keysForShapeTripletSupport) {
            var item = parser.statsComputer.shapeTripletSupport.get(key);
            if (maxConfidenceItem == null) {
                maxConfidenceItem = item;
            }
            if (item.getConfidence() > maxConfidenceItem.getConfidence()) {
                maxConfidenceItem = item;
            }
        }

        if(maxConfidenceItem == null)
            return false;

        return maxConfidenceItem.getSupport() <= this.support || maxConfidenceItem.getConfidence() <= this.confidence;
    }


    public void generateUpdatesMap() {
        updatedShapes = new HashMap<>();

        //add edited shapes
        for(var nodeShapeEntry : newShapesMap.entrySet()) {
            var nodeShapeKey = nodeShapeEntry.getKey();
            if(!oldShapesMap.containsKey(nodeShapeKey)) {
                updatedShapes.put(nodeShapeKey, nodeShapeEntry.getValue());
            }
            else {
                for(var propertyShapeEntry : nodeShapeEntry.getValue().entrySet()) {
                    if(!oldShapesMap.get(nodeShapeKey).containsKey(propertyShapeEntry.getKey())) {
                        if(!updatedShapes.containsKey(nodeShapeKey)) {
                            updatedShapes.put(nodeShapeKey, new HashMap<>());
                        }
                        var propEntry = updatedShapes.get(nodeShapeKey);
                        propEntry.put(propertyShapeEntry.getKey(), propertyShapeEntry.getValue());
                    }
                }
            }
        }

        //add edited shapes
        for(var nodeShapeEntry : editedShapes.entrySet()) {
            var nodeShapeKey = nodeShapeEntry.getKey();
            if(!updatedShapes.containsKey(nodeShapeKey)) {
                updatedShapes.put(nodeShapeKey, new HashMap<>());
            }
            var propEntryUpdate = updatedShapes.get(nodeShapeKey);
            var propEntryDeleted = deletedShapes.getOrDefault(nodeShapeKey, new HashSet<>());

            for(var propertyShapeId : nodeShapeEntry.getValue()) {
                if(!propEntryUpdate.containsKey(propertyShapeId) && !propEntryDeleted.contains(propertyShapeId)) {
                    var propEntryNew = newShapesMap.get(nodeShapeKey).get(propertyShapeId);
                    propEntryUpdate.put(propertyShapeId, propEntryNew);
                }
            }
            if(propEntryUpdate.isEmpty())
                updatedShapes.remove(nodeShapeKey);
        }
    }

    public String deleteShapesFromFile(String fileContent) {
        for (var nodeShapeClass : deletedShapes.entrySet()) {
            var nodeShape = resultExtractedShapes.nodeShapes.stream().filter(ns -> ns.targetClass.toString().equals(parser.getStringEncoder().decode(nodeShapeClass.getKey()))).findFirst();
            if (nodeShape.isPresent()) {
                for (var propertyShapeClass : nodeShapeClass.getValue()) {
                    var propertyShape = nodeShape.get().propertyShapes.stream().filter(ps -> ps.pathAsIri.toString().equals(parser.getStringEncoder().decode(propertyShapeClass))).findFirst();
                    if (propertyShape.isPresent()) {
                        fileContent = RegexUtils.deleteIriFromString(propertyShape.get().iri.toString(), fileContent, false);
                        fileContent = RegexUtils.deletePropertyShapeReferenceWithIriFromString(propertyShape.get().iri.toString(), fileContent, false);
                        nodeShape.get().propertyShapes.remove(propertyShape.get());
                        deletedPropertyShapeNames.add(propertyShape.get().iri.toString());
                        nodeShapeNamesToCheckForEditedShapes.add(nodeShape.get().getIri().toString());
                    }
                }
                //delete whole nodeshape
                if (nodeShape.get().propertyShapes.isEmpty()) {
                    fileContent = RegexUtils.deleteIriFromString(nodeShape.get().getIri().toString(), fileContent, false);
                    resultExtractedShapes.nodeShapes.remove(nodeShape.get());
                    deletedNodeShapeNames.add(nodeShape.get().getIri().toString());
                    nodeShapeNamesToCheckForEditedShapes.remove(nodeShape.get().getIri().toString());
                }
            }
        }
        return fileContent;
    }

    public String mergeAddedShapesToOrginialFileAsString() {
        StringBuilder originalFile = new StringBuilder(resultExtractedShapes.getFileAsString(false));
        originalFile.append(updatedExtractedShapes.prefixLines);

        for(var addedNodeShape : updatedExtractedShapes.getNodeShapes()) {
            var existingNodeShape = resultExtractedShapes.getNodeShapes().stream().filter(ns -> ns.getIri().equals(addedNodeShape.getIri())).findFirst();
            if(existingNodeShape.isPresent()) {
                var nodeShape = existingNodeShape.get();
                nodeShape.support = addedNodeShape.support;
                var nodeShapeAsText = RegexUtils.getShapeAsString(nodeShape.getIri().toString(), resultExtractedShapes.getFileAsString(false));
                var nodeShapeAsTextUpdated = nodeShapeAsText;

                for(var newPropertyShape : addedNodeShape.propertyShapes) {
                    var existingPropertyShape = existingNodeShape.get().propertyShapes.stream().filter(ps -> ps.iri.equals(newPropertyShape.iri)).findFirst();
                    if(existingPropertyShape.isPresent()) {
                        nodeShape.propertyShapes.remove(existingPropertyShape.get());
                        nodeShape.propertyShapes.add(newPropertyShape);
                        originalFile = new StringBuilder(RegexUtils.deleteIriFromString(existingPropertyShape.get().iri.toString(), originalFile.toString(), false));
                        var propertyShapeText = getPropertyShapeWithNodeTriple(newPropertyShape);
                        originalFile.append(propertyShapeText);
                        propertyShapeNamesToCheckForEditedShapes.add(newPropertyShape.iri.toString());
                    }
                    else {
                        nodeShape.propertyShapes.add(newPropertyShape);
                        addedPropertyShapeNames.add(newPropertyShape.iri.toString());
                        var propertyShapeText = getPropertyShapeWithNodeTriple(newPropertyShape);
                        originalFile.append(propertyShapeText);
                        nodeShapeAsTextUpdated = RegexUtils.insertAfter(nodeShapeAsTextUpdated, ";", "<http://www.w3.org/ns/shacl#property> <"+newPropertyShape.iri.toString()+"> ;");
                    }
                }

                originalFile = new StringBuilder(RegexUtils.deleteIriFromString(nodeShape.getIri().toString(), originalFile.toString(), nodeShape.errorDuringGeneration));
                originalFile.append(nodeShapeAsTextUpdated);
                nodeShapeNamesToCheckForEditedShapes.add(nodeShape.getIri().toString());
            }
            else {
                resultExtractedShapes.nodeShapes.add(addedNodeShape);
                addedNodeShapeNames.add(addedNodeShape.getIri().toString());
                originalFile.append(RegexUtils.getShapeAsString(addedNodeShape.getIri().toString(), updatedExtractedShapes.getFileAsString(false)));
                for (var ps : addedNodeShape.propertyShapes) {
                    originalFile.append(RegexUtils.getShapeAsString(ps.iri.toString(), updatedExtractedShapes.getFileAsString(false)));
                    addedPropertyShapeNames.add(ps.iri.toString());
                }
            }
        }

        var newFilePath = Main.outputFilePath+"merged"+newVersionName+".ttl";
        var formattedFile = RegexUtils.replaceCommaWithDotInConfidenceValues(originalFile.toString());
        RegexUtils.saveStringAsFile(formattedFile, newFilePath);
        parser.shapesExtractor.prettyFormatTurtle(newFilePath);
        return parser.shapesExtractor.getOutputFileAddress();
    }

    private String getPropertyShapeWithNodeTriple(PropertyShape newPropertyShape) {
        var text = RegexUtils.getShapeAsString(newPropertyShape.iri.toString(), updatedExtractedShapes.getFileAsString(false));

        //algorithm for PostConstraintsAnnotator-Class from QSE which creates node-triple

        //without Or-Items
        if((newPropertyShape.orItems == null || newPropertyShape.orItems.isEmpty()) &&
                newPropertyShape.getNodeKind() != null && newPropertyShape.getNodeKind().equals("IRI") ) {
            //check if nodeShape is present
            if(newPropertyShape.dataTypeOrClassAsIri!=null) {
                var classIRI = newPropertyShape.dataTypeOrClassAsIri.toString();
                if(Utils.isValidIRI(classIRI) && !classIRI.equals(Constants.OBJECT_UNDEFINED_TYPE)) {
                    if(!checkIfTargetClassIsAmongDeletedShapes(classIRI)) {
                        var existingNodeShapeIri = getTargetClassFromAmongResultOrAddedShapes(classIRI);
                        if(existingNodeShapeIri != null) {
                            var propertyShapeTextWithOutNodeTriple = RegexUtils.removeNodeTripleFromShape(text);
                            var nodeTriple = "<"+SHACL.NODE+"> <" + existingNodeShapeIri + "> ; ";
                            var textWithNodeTriple = RegexUtils.insertAfter(propertyShapeTextWithOutNodeTriple, "PropertyShape> ;", nodeTriple);
                            return textWithNodeTriple;
                        }
                    }
                }
            }
        }

        //with or-items
        if(newPropertyShape.orItems != null && !newPropertyShape.orItems.isEmpty()) {
            var maxSupport = 0;
            var nodeTriple = "";
            for(var orItem : newPropertyShape.orItems) {
                //check if nodeShape is present
                if(orItem.dataTypeOrClass!=null && orItem.nodeKind.equals(SHACL.IRI)) {
                    if(orItem.dataTypeOrClass != null) {
                        var classIRI = orItem.dataTypeOrClass.toString();
                        if(Utils.isValidIRI(classIRI) && !classIRI.equals(Constants.OBJECT_UNDEFINED_TYPE)) {
                            if(!checkIfTargetClassIsAmongDeletedShapes(classIRI)) {
                                var existingNodeShapeIri = getTargetClassFromAmongResultOrAddedShapes(classIRI);
                                if(existingNodeShapeIri != null) {
                                    if(orItem.support > maxSupport) {
                                        maxSupport = orItem.support;
                                        nodeTriple =  "<"+SHACL.NODE+"> <" + existingNodeShapeIri + "> ; ";
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if(!nodeTriple.isEmpty()) {
                var propertyShapeTextWithOutNodeTriple = RegexUtils.removeNodeTripleFromShape(text);
                var textWithNodeTriple = RegexUtils.insertAfter(propertyShapeTextWithOutNodeTriple, "PropertyShape> ;", nodeTriple);
                return textWithNodeTriple;
            }
        }
        return text;
    }

    public boolean checkIfTargetClassIsAmongDeletedShapes(String targetClass) {
        var nodeShape = resultExtractedShapes.nodeShapes.stream().filter(ns -> ns.targetClass.toString().equals(targetClass)).findFirst();
        var targetClassEncoded = parser.getStringEncoder().encode(targetClass);
        var deletedShape = deletedShapes.getOrDefault(targetClassEncoded, null);
        if(nodeShape.isPresent() && deletedShape != null && (nodeShape.get().propertyShapes.isEmpty() || nodeShape.get().propertyShapes.size() == deletedShape.size()))
            return true;
        return false;
    }

    private String getTargetClassFromAmongResultOrAddedShapes(String targetClass) {
        var existsInUpdatedShapes = updatedExtractedShapes.getNodeShapes().stream().filter(ns -> ns.targetClass.toString().equals(targetClass)).findAny();
        if (existsInUpdatedShapes.isPresent())
            return existsInUpdatedShapes.get().getIri().toString();
        var existsInOriginalShapes = resultExtractedShapes.getNodeShapes().stream().filter(ns -> ns.targetClass.toString().equals(targetClass)).findAny();
        if (existsInOriginalShapes.isPresent())
            return existsInOriginalShapes.get().getIri().toString();
        return null;
    }

}
