package sparqlshapechecker.comparator;

import java.util.ArrayList;
import java.util.List;

public class MetaComparator {
    public ComparisonDiff diffQse;
    public ComparisonDiff diffAlgorithm;
    public static String nameToCompare = "Algorithm";

    public String compareAll() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n==== Comparison of Compare-Methods ====\n");

        sb.append("=== Added Node Shapes ===\n");
        List<String> uniqueToFirstList = getDifference(diffQse.addedNodeShapes, diffAlgorithm.addedNodeShapes);
        List<String> uniqueToSecondList = getDifference(diffAlgorithm.addedNodeShapes, diffQse.addedNodeShapes);
        appendUniqueNamesToStringBuilder(uniqueToFirstList, sb, uniqueToSecondList);

        sb.append("=== Added Property Shapes ===\n");
        uniqueToFirstList = getDifference(diffQse.addedPropertyShapes, diffAlgorithm.addedPropertyShapes);
        uniqueToSecondList = getDifference(diffAlgorithm.addedPropertyShapes, diffQse.addedPropertyShapes);
        appendUniqueNamesToStringBuilder(uniqueToFirstList, sb, uniqueToSecondList);

        sb.append("=== Deleted Node Shapes ===\n");
        uniqueToFirstList = getDifference(diffQse.deletedNodeShapes, diffAlgorithm.deletedNodeShapes);
        uniqueToSecondList = getDifference(diffAlgorithm.deletedNodeShapes, diffQse.deletedNodeShapes);
        appendUniqueNamesToStringBuilder(uniqueToFirstList, sb, uniqueToSecondList);

        sb.append("=== Deleted Property Shapes ===\n");
        uniqueToFirstList = getDifference(diffQse.deletedPropertyShapes, diffAlgorithm.deletedPropertyShapes);
        uniqueToSecondList = getDifference(diffAlgorithm.deletedPropertyShapes, diffQse.deletedPropertyShapes);
        appendUniqueNamesToStringBuilder(uniqueToFirstList, sb, uniqueToSecondList);

        sb.append("=== Edited Node Shape Names ===\n");
        if(diffQse.editedNodeShapes != null && diffAlgorithm.editedNodeShapes != null) {
            var uniqueToFirstListObjects = getDifferenceBetweenObjectLists(diffQse.editedNodeShapes, diffAlgorithm.editedNodeShapes);
            var uniqueToSecondListObjects = getDifferenceBetweenObjectLists(diffAlgorithm.editedNodeShapes, diffQse.editedNodeShapes);
            appendUniqueNamesToStringBuilderObjects(uniqueToFirstListObjects, sb, uniqueToSecondListObjects);
        }
        sb.append("=== Edited Property Shape Names ===\n");
        if(diffQse.editedPropertyShapes != null && diffAlgorithm.editedPropertyShapes != null) {
            var uniqueToFirstListObjects = getDifferenceBetweenObjectLists(diffQse.editedPropertyShapes, diffAlgorithm.editedPropertyShapes);
            var uniqueToSecondListObjects = getDifferenceBetweenObjectLists(diffAlgorithm.editedPropertyShapes, diffQse.editedPropertyShapes);
            appendUniqueNamesToStringBuilderObjects(uniqueToFirstListObjects, sb, uniqueToSecondListObjects);
        }
        sb.append("Execution Time QSE Total: ").append(diffQse.durationTotal.getSeconds()).append(" seconds\nExecution Time ").append(nameToCompare).append(" Total: ").append(diffAlgorithm.durationTotal.getSeconds()).append(" seconds");
        return sb.toString();
    }

    private static void appendUniqueNamesToStringBuilderObjects(List<EditedShapesComparisonObject> uniqueToFirstListObjects, StringBuilder sb, List<EditedShapesComparisonObject> uniqueToSecondListObjects) {
        if (!uniqueToFirstListObjects.isEmpty())
            sb.append("== Unique in QSE-Comparison (Count = ").append(uniqueToFirstListObjects.size()).append(") ==\n");
        uniqueToFirstListObjects.forEach(s -> sb.append(s.shapeName).append("\n"));
        if (!uniqueToSecondListObjects.isEmpty())
            sb.append("== Unique in ").append(nameToCompare).append("-Comparison (Count = ").append(uniqueToSecondListObjects.size()).append(") ==\n");
        uniqueToSecondListObjects.forEach(s -> sb.append(s.shapeName).append("\n"));
    }

    private static void appendUniqueNamesToStringBuilder(List<String> uniqueToFirstList, StringBuilder sb, List<String> uniqueToSecondList) {
        if (!uniqueToFirstList.isEmpty())
            sb.append("== Unique in QSE-Comparison (Count = ").append(uniqueToFirstList.size()).append(") ==\n");
        uniqueToFirstList.forEach(s -> sb.append(s).append("\n"));
        if (!uniqueToSecondList.isEmpty())
            sb.append("== Unique in ").append(nameToCompare).append("-Comparison (Count = ").append(uniqueToSecondList.size()).append(") ==\n");
        uniqueToSecondList.forEach(s -> sb.append(s).append("\n"));
    }

    public static List<String> getDifference(List<String> list1, List<String> list2) {
        List<String> difference = new ArrayList<>(list1);
        difference.removeAll(list2);
        return difference;
    }

    public static List<EditedShapesComparisonObject> getDifferenceBetweenObjectLists(List<EditedShapesComparisonObject> list1, List<EditedShapesComparisonObject> list2) {
        var difference = new ArrayList<>(list1);
        difference.removeAll(list2);
        return difference;
    }
}
