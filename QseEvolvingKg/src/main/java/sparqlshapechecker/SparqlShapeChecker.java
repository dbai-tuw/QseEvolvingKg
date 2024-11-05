package sparqlshapechecker;

import sparqlshapechecker.comparator.*;
import sparqlshapechecker.utils.ConfigManager;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class SparqlShapeChecker {
    private static final Logger LOGGER = Logger.getLogger(SparqlShapeChecker.class.getName());
    static String fileName = "ConfigSparqlShapeChecker.properties";
    public static void main(String[] args) {
        setupLogger();

        MetaComparator metaComparator = new MetaComparator();
        String dataSetName1 = ConfigManager.getProperty("dataSetNameQSE", fileName);
        String dataSets = ConfigManager.getProperty("dataSetsToCheck", fileName);
        var dataSetsToCheck = dataSets.split(",");
        String pruningThresholds = ConfigManager.getProperty("pruningThresholds", fileName);
        String graphDbUrl = ConfigManager.getProperty("graphDbUrl", fileName);
        String parentDirectory = System.getProperty("user.dir")+ File.separator;
        String logPath = parentDirectory + "Output" + File.separator + "compareLogs" + File.separator;
        var doMetaComparison = Boolean.parseBoolean(ConfigManager.getProperty("doMetaComparison", fileName));
        if(doMetaComparison) {
            ShapeComparatorQSEQueryBased comparatorQSETwice = new ShapeComparatorQSEQueryBased(graphDbUrl, dataSetName1, "", logPath);
            ComparisonDiff comparisonDiff = comparatorQSETwice.runQseFirstTime(pruningThresholds);
            ShapeComparatorSparql comparatorSparql = new ShapeComparatorSparql(graphDbUrl, dataSetName1, "", logPath);
            for (var dataSetName2 : dataSetsToCheck) {
                comparatorQSETwice.dataSetName2 = dataSetName2;
                comparatorSparql.dataSetName2 = dataSetName2;

                metaComparator.diffQse = comparatorQSETwice.doComparisonForFollowingVersion(pruningThresholds, comparisonDiff);
                metaComparator.diffAlgorithm = comparatorSparql.doComparison(pruningThresholds, comparatorQSETwice);
                MetaComparator.nameToCompare = "SPARQL-ShapeChecker";
                ComparatorUtils.exportComparisonToFile(logPath+dataSetName1+"_"+dataSetName2+ File.separator + "Meta", metaComparator.compareAll());
            }
        }
        else {
            ShapeComparatorSparql comparatorSparql = new ShapeComparatorSparql(graphDbUrl, dataSetName1, "", logPath);
            comparatorSparql.doFullComparisonForMultipleVersions(pruningThresholds, dataSetsToCheck);
        }

    }

    static void setupLogger() {
        try {
            FileHandler fileHandler;
            fileHandler = new FileHandler("application.log");
            fileHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fileHandler);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}