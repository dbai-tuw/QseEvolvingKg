package sparqldiffchecker.test;

import cs.Main;
import cs.qse.querybased.nonsampling.QbParser;
import cs.utils.Constants;
import org.junit.Test;
import sparqlshapechecker.comparator.ComparatorUtils;
import sparqlshapechecker.comparator.MetaComparator;
import sparqlshapechecker.comparator.ShapeComparatorQSEQueryBased;
import sparqlshapechecker.comparator.ShapeComparatorSparql;
import sparqlshapechecker.utils.ConfigManager;

import java.io.File;

//Tests are only used for local execution
public class SparqlShapeCheckerComparisonTests {

    public static final String firstVersionName = "film";

    public static final String outputPath = System.getProperty("user.dir")+"\\Output\\"+firstVersionName+"\\";
    //QSE QueryBases does not calculate confidence, therefore it is always 0 and filtering works with > 0 -> filter to -1
    public static final String pruningThresholdsDefault = "{(-1,0)}"; //only set one threshold - {(<confidence 10% is 0.1>,<support>)}
    public static final String graphDbUrl = "http://localhost:7201/";
    public static final String logPath = System.getProperty("user.dir")+"\\Output\\compareLogs\\";


    @Test
    public void basicQseTestWithFilm() {
        ShapeComparatorQSEQueryBased comparatorQSETwice = new ShapeComparatorQSEQueryBased(graphDbUrl, "film", "Film-NoGender", logPath);
        comparatorQSETwice.doComparisonSparql(pruningThresholdsDefault);
    }
    @Test
    public void basicQseTestWithFilmSparql() {
        ShapeComparatorSparql comparatorSparql = new ShapeComparatorSparql(graphDbUrl, "film", "Film-NoGender", logPath);
        comparatorSparql.doFullComparison(pruningThresholdsDefault);
    }

    @Test
    public void basicTestWithCustomGraphSparql() {
        ShapeComparatorSparql comparatorSparql = new ShapeComparatorSparql(graphDbUrl, "testKilometre", "testKilometre", logPath);
        comparatorSparql.doFullComparison(pruningThresholdsDefault);
    }

    @Test
    public void basicFilmTest() {
        MetaComparator metaComparator = new MetaComparator();
        String dataSetName1 = "film";
        String dataSetName2 = "film3";
        ShapeComparatorQSEQueryBased comparatorQSETwice = new ShapeComparatorQSEQueryBased(graphDbUrl, dataSetName1, dataSetName2, logPath);
        metaComparator.diffQse = comparatorQSETwice.doComparisonSparql(pruningThresholdsDefault);
        ShapeComparatorSparql comparatorSparql = new ShapeComparatorSparql(graphDbUrl, dataSetName1, dataSetName2, logPath);
        metaComparator.diffAlgorithm = comparatorSparql.doFullComparison(pruningThresholdsDefault);
        System.out.println(metaComparator.compareAll());
        ComparatorUtils.exportComparisonToFile(logPath+dataSetName1+"_"+dataSetName2+ File.separator + "Meta", metaComparator.compareAll());
    }

    @Test
    public void bearBV1V2Test() {
        MetaComparator metaComparator = new MetaComparator();
        String dataSetName1 = "Bear-B-1";
        String dataSetName2 = "Bear-B87";
        String pruningThresholds =  "{(-1,0)}";
//        ShapeComparatorQSE comparatorQSETwice = new ShapeComparatorQSE(graphDbUrl, dataSetName1, dataSetName2, logPath);
//        metaComparator.diffQse = comparatorQSETwice.doComparison(pruningThresholds);
        ShapeComparatorSparql comparatorSparql = new ShapeComparatorSparql(graphDbUrl, dataSetName1, dataSetName2, logPath);
        metaComparator.diffAlgorithm = comparatorSparql.doFullComparison(pruningThresholds);
        System.out.println(metaComparator.compareAll());
        ComparatorUtils.exportComparisonToFile(logPath+dataSetName1+"_"+dataSetName2+ File.separator + "Meta", metaComparator.compareAll());
    }

    @Test
    public void bearTest() {
        MetaComparator metaComparator = new MetaComparator();
        String dataSetName1 = "Bear-C1";
        String dataSetName2 = "Bear-C2";
        String pruningThresholds =  "{(0,0)}";
//        ShapeComparatorQSEQueryBased comparatorQSETwice = new ShapeComparatorQSEQueryBased(graphDbUrl, dataSetName1, dataSetName2, logPath);
//        metaComparator.diffQse = comparatorQSETwice.doComparisonSparql(pruningThresholds);
        ShapeComparatorSparql comparatorSparql = new ShapeComparatorSparql(graphDbUrl, dataSetName1, dataSetName2, logPath);
        metaComparator.diffAlgorithm = comparatorSparql.doFullComparison(pruningThresholds);
        System.out.println(metaComparator.compareAll());
        ComparatorUtils.exportComparisonToFile(logPath+dataSetName1+"_"+dataSetName2+ File.separator + "Meta", metaComparator.compareAll());
    }
    @Test
    public void peopleTest() {
        MetaComparator metaComparator = new MetaComparator();
        String dataSetName1 = "PeopleV2";
        String dataSetName2 = "PeopleV3";
        String pruningThresholds =  "{(-1,0)}";
        ShapeComparatorQSEQueryBased comparatorQSETwice = new ShapeComparatorQSEQueryBased(graphDbUrl, dataSetName1, dataSetName2, logPath);
        metaComparator.diffQse = comparatorQSETwice.doComparisonSparql(pruningThresholds);
        ShapeComparatorSparql comparatorSparql = new ShapeComparatorSparql(graphDbUrl, dataSetName1, dataSetName2, logPath);
        metaComparator.diffAlgorithm = comparatorSparql.doFullComparison(pruningThresholds);
        System.out.println(metaComparator.compareAll());
        ComparatorUtils.exportComparisonToFile(logPath+dataSetName1+"_"+dataSetName2+ File.separator + "Meta", metaComparator.compareAll());
    }

    @Test
    public void runQB() {
        Main.setResourcesPathForJar(ConfigManager.getRelativeResourcesPathFromQse());
        Main.annotateSupportConfidence = "true";
        Main.setPruningThresholds("{(-1,0)}");
        File currentDir = new File(System.getProperty("user.dir"));
        File emptyConfig = new File(currentDir, "src/test/expected_test_results/emptyconfig.txt");
        Main.configPath = emptyConfig.getAbsolutePath(); //avoid exceptions in QSE
        Main.datasetName = "Bear-B87";
        Main.setOutputFilePathForJar(outputPath+Main.datasetName+File.separator);

        QbParser qbParser = new QbParser(100, Constants.RDF_TYPE, graphDbUrl, Main.datasetName);
        qbParser.run();
    }

    @Test
    public void peopleDemonstration() {
        MetaComparator metaComparator = new MetaComparator();
        String dataSetName1 = "PeopleV4";
        String dataSetName2 = "PeopleV5";
        String pruningThresholds =  "{(0,0)}";
        ShapeComparatorQSEQueryBased comparatorQSETwice = new ShapeComparatorQSEQueryBased(graphDbUrl, dataSetName1, dataSetName2, logPath);
        metaComparator.diffQse = comparatorQSETwice.doComparisonSparql(pruningThresholds);
        ShapeComparatorSparql comparatorSparql = new ShapeComparatorSparql(graphDbUrl, dataSetName1, dataSetName2, logPath);
        metaComparator.diffAlgorithm = comparatorSparql.doFullComparison(pruningThresholds);
        System.out.println(metaComparator.compareAll());
        ComparatorUtils.exportComparisonToFile(logPath+dataSetName1+"_"+dataSetName2+ File.separator + "Meta", metaComparator.compareAll());

    }
}
