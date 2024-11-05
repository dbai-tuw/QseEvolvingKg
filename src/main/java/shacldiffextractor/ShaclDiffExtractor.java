package shacldiffextractor;

import shacldiffextractor.comparator.ShapeComparatorQseFileBased;
import shacldiffextractor.comparator.ShapeComparatorDiff;
import sparqlshapechecker.comparator.ComparatorUtils;
import sparqlshapechecker.comparator.ComparisonDiff;
import sparqlshapechecker.comparator.MetaComparator;
import sparqlshapechecker.utils.ConfigManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ShaclDiffExtractor {
    private static final Logger LOGGER = Logger.getLogger(ShaclDiffExtractor.class.getName());
    static String fileName = "ConfigShaclDiffExtractor.properties";
    public static String filePathInitialVersion;
    public static String initialVersionName;

    public static String pruningThresholds;
    public static boolean doMetaComparison;
    public static List<ConfigVersionElement> versionElements = new ArrayList<>();
    public static void main(String[] args) {
        readConfigFromFile();

        run();
    }

    public static void run() {
        setupLogger();

        MetaComparator metaComparator = new MetaComparator();
        String parentDirectory = System.getProperty("user.dir")+ File.separator;
        String logPath = parentDirectory + "Output" + File.separator + "compareLogsDiff" + File.separator;
        var outputPath = Paths.get("Output", "DiffExtractor");
        ShapeComparatorQseFileBased.prepareQsePath(initialVersionName, outputPath);
        if(doMetaComparison) {
            ShapeComparatorQseFileBased comparatorQSETwice = new ShapeComparatorQseFileBased(filePathInitialVersion,"",initialVersionName, "",logPath);
            ComparisonDiff comparisonDiff = comparatorQSETwice.runQseFirstTime(pruningThresholds, outputPath);
            ShapeComparatorDiff comparatorDiff = new ShapeComparatorDiff(filePathInitialVersion, "", "", "", initialVersionName, "", logPath);
            for (var versionElement : versionElements) {
                comparatorQSETwice.filePath2 = versionElement.filePathFullVersion;
                comparatorQSETwice.dataSetName2 = versionElement.versionName;
                comparatorDiff.filePath2=versionElement.filePathFullVersion;
                comparatorDiff.filePathAdded=versionElement.filePathAdded;
                comparatorDiff.filePathDeleted=versionElement.filePathDeleted;
                comparatorDiff.dataSetName2 = versionElement.versionName;

                metaComparator.diffQse = comparatorQSETwice.doComparisonForFollowingVersion(pruningThresholds, comparisonDiff);
                metaComparator.diffAlgorithm = comparatorDiff.doComparison(pruningThresholds, comparatorQSETwice);
                MetaComparator.nameToCompare = "SHACL-DiffExtractor";
                ComparatorUtils.exportComparisonToFile(logPath+initialVersionName+"_"+versionElement.versionName+ File.separator + "Meta", metaComparator.compareAll());

                if(versionElements.indexOf(versionElement) != versionElements.size()-1) {
                    comparatorQSETwice = new ShapeComparatorQseFileBased(versionElement.filePathFullVersion,"",versionElement.versionName, "",logPath);
                    comparisonDiff = comparatorQSETwice.runQseFirstTime(pruningThresholds, outputPath);
                    comparatorDiff = new ShapeComparatorDiff(versionElement.filePathFullVersion, "", "", "", versionElement.versionName, "", logPath);
                    initialVersionName = versionElement.versionName;
                }
            }
        }
        else {
            ShapeComparatorDiff comparatorDiff = new ShapeComparatorDiff(filePathInitialVersion, "", "", "", initialVersionName, "", logPath);
            comparatorDiff.doFullComparisonForMultipleVersions(pruningThresholds, versionElements);
        }
    }

    static void readConfigFromFile() {
        pruningThresholds = ConfigManager.getProperty("pruningThresholds", fileName);
        filePathInitialVersion = ConfigManager.getProperty("filePathInitialVersion", fileName);
        initialVersionName = ConfigManager.getProperty("initialVersionName", fileName);
        if(initialVersionName == null)
            initialVersionName = new File(filePathInitialVersion).getName().replaceFirst("[.][^.]+$", ""); //removes file ending
        doMetaComparison = Boolean.parseBoolean(ConfigManager.getProperty("doMetaComparison", fileName));

        var groupNames = ConfigManager.getPropertiesGroupNames(fileName);
        for (String key : groupNames) {
            ConfigVersionElement configVersionElement = new ConfigVersionElement();
            configVersionElement.versionName = key;
            configVersionElement.filePathAdded = ConfigManager.getProperty(key+".filePathAdded", fileName);
            configVersionElement.filePathDeleted = ConfigManager.getProperty(key+".filePathDeleted", fileName);
            configVersionElement.filePathFullVersion = ConfigManager.getProperty(key+".filePathFullVersion", fileName);
            versionElements.add(configVersionElement);
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