package shape_comparator.views.generateshapes;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import cs.Main;
import cs.qse.common.encoders.StringEncoder;
import cs.qse.common.structure.NS;
import cs.qse.filebased.Parser;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import shape_comparator.shactor_utils.PruningUtil;
import shape_comparator.data.ExtractedShapes;
import shape_comparator.data.Graph;
import shape_comparator.data.QseType;
import shape_comparator.data.Version;
import shape_comparator.services.*;
import shape_comparator.services.Utils.ComboBoxItem;
import shape_comparator.views.MainLayout;
import shape_comparator.views.shapes.ShapesView;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@PageTitle("Generate Shapes")
@Route(value = "generate-shapes", layout = MainLayout.class)
@Uses(Icon.class)
public class GenerateShapesView extends Composite<VerticalLayout> {

    @Autowired()
    private VersionService versionService;
    @Autowired()
    private GraphService graphService;

    @Autowired()
    private ShapesService shapeService;

    Select<ComboBoxItem> comboBoxGraph;
    Select<ComboBoxItem> comboBoxVersion;
    Long currentVersionId;
    Grid classesGrid;
    Parser parser;
    H5 graphInfo;
    TextField searchField;
    List<String> chosenClasses;
    Set<Integer> chosenClassesEncoded;
    Button buttonPrimary;
    String prunedFileAddress = "";
    Checkbox checkboxUseDefaultShapes;
    NumberField confidence;
    IntegerField support;
    Graph currentGraph;
    Version currentVersion;
    RadioButtonGroup radioGroupQseType;
    DecimalFormat decimalFormatter = new DecimalFormat("#,###");


    public GenerateShapesView() {
        HorizontalLayout layoutRow = new HorizontalLayout();
        layoutRow.setWidth("100%");
        layoutRow.setHeight("min-content");
        layoutRow.setSpacing(true);
        layoutRow.setAlignItems(FlexComponent.Alignment.BASELINE);
        comboBoxGraph = new Select<>();
        comboBoxVersion = new Select<>();
        radioGroupQseType = new RadioButtonGroup();
        classesGrid = new Grid(Type.class,false);
        support = new IntegerField();
        confidence = new NumberField();
        checkboxUseDefaultShapes = new Checkbox();
        checkboxUseDefaultShapes.setLabel("Use default shapes");
        checkboxUseDefaultShapes.setValue(false);
        buttonPrimary = new Button();
        graphInfo = new H5();
        searchField = new TextField();
        searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        getContent().setWidth("100%");
        getContent().getStyle().set("flex-grow", "1");
        comboBoxGraph.setLabel("Graph");
        comboBoxGraph.setWidth("min-content");
        comboBoxVersion.setLabel("Version");
        comboBoxVersion.setWidth("min-content");
        radioGroupQseType.setLabel("QSE-Type");
        radioGroupQseType.setWidth("min-content");
        radioGroupQseType.setItems(Arrays.stream(QseType.values())
                .map(Enum::name)
                .collect(Collectors.toList()));
        radioGroupQseType.setEnabled(false); //TODO remove
        radioGroupQseType.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
        radioGroupQseType.setValue(QseType.EXACT.toString());
        classesGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        classesGrid.setWidth("100%");
        classesGrid.getStyle().set("flex-grow", "0");
        classesGrid.addColumn("name").setHeader("Class IRI").setSortable(true);
        classesGrid.addColumn("instanceCount").setHeader("Class Instance Count").setSortable(true);
        classesGrid.getColumns().forEach(column -> ((Grid.Column)column).setResizable(true));
        classesGrid.addSelectionListener(selection -> buttonPrimary.setEnabled(selection.getAllSelectedItems().size() > 0));
        support.setLabel("Support");
        support.setWidth("min-content");
        support.setMin(0);
        support.setValue(10);
        support.setTooltipText("Only shapes will be generated, which have a higher support (>), not higher or equal (>=)");
        confidence.setLabel("Confidence (in %)");
        confidence.setValue(25.0);
        confidence.setWidth("min-content");
        confidence.setTooltipText("Only shapes will be generated, which have a higher confidence (>), not higher or equal (>=)");
        confidence.setMin(0);
        confidence.setMax(100);
        support.setEnabled(true);
        confidence.setEnabled(true);
        buttonPrimary.setText("Generate");
        buttonPrimary.setTooltipText("Please be patient, this can take a while :)");
        buttonPrimary.setWidth("min-content");
        buttonPrimary.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getContent().add(layoutRow);
        layoutRow.add(comboBoxGraph);
        layoutRow.add(comboBoxVersion);
        getContent().add(radioGroupQseType);
        getContent().add(graphInfo);
        getContent().add(searchField);
        getContent().add(classesGrid);
        getContent().add(checkboxUseDefaultShapes);
        getContent().add(support);
        getContent().add(confidence);
        getContent().add(buttonPrimary);
        comboBoxGraph.addValueChangeListener(event -> {
            if(event.getValue() != null) {
                Long selectedValue = event.getValue().id;
                VaadinSession.getCurrent().setAttribute("shapes_currentGraphId", selectedValue);
                currentGraph = graphService.get(event.getValue().id).get();
                Utils.setComboBoxVersionsData(selectedValue, versionService, comboBoxVersion);
            }
        });
        comboBoxVersion.addValueChangeListener(event -> {
            if(event.getValue() != null) {
                currentVersionId = event.getValue().id;
                VaadinSession.getCurrent().setAttribute("shapes_currentVersionId", currentVersionId);
                setClassesGridData();
                currentVersion = versionService.get(event.getValue().id).get();
                Main.setDataSetNameForJar(currentGraph.getName() + "-" + event.getValue().label.replace(" ",""));
            }
        });
        checkboxUseDefaultShapes.addValueChangeListener(event -> {
            if(event.getValue()) {
                support.setEnabled(false);
                confidence.setEnabled(false);
            }
            else
            {
                support.setEnabled(true);
                confidence.setEnabled(true);
            }
        });
        buttonPrimary.addClickListener(buttonClickEvent -> {
            try {
                completeFileBasedShapesExtraction();
                getUI().ifPresent(ui -> ui.navigate(ShapesView.class));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        addAttachListener(event -> {
            setPaths();
            Utils.setComboBoxGraphData(graphService,comboBoxGraph);
        });
    }


    private void setClassesGridData() {
        //Copied from Shactor
        Version version = versionService.get(currentVersionId).get();
        parser = new Parser(version.getPath(), 50, 5000, "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>");
        parser.entityExtraction();
        String info = "No. of entities: " + decimalFormatter.format(parser.entityDataHashMap.size()) + " ; " + "No. of classes: " + decimalFormatter.format(parser.classEntityCount.size()) + ". Please select the classes from the table below for which you want to extract shapes. Don't select classes with the same name but different IRI, this will lead to inconsistencies! e.g. <http://dbpedia.org/ontology/Agent>, <http://www.ontologydesignpatterns.org/ont/dul/DUL.owl#Agent>";
        graphInfo.setText(info);

        var classes = getClasses(parser.classEntityCount, parser.getStringEncoder());
        var dataView = classesGrid.setItems(classes);
        searchField.addValueChangeListener(e -> dataView.refreshAll());

        dataView.addFilter(type -> {
            String searchTerm = searchField.getValue().trim();
            if (searchTerm.isEmpty())
                return true;
            Type t = (Type)type;
            return t.getName().toLowerCase().contains(searchTerm.toLowerCase());
        });

        classes.forEach(item -> classesGrid.select(item));
        if(classes == null || classes.isEmpty())
            Notification.show("Something went wrong! Please check, if the graph file contains data. Comments and empty lines will lead to errors.");
    }

    private void completeFileBasedShapesExtraction() throws Exception {
        chosenClasses = new ArrayList<>();
        chosenClassesEncoded = new HashSet<>();

        classesGrid.getSelectedItems().forEach(item -> {
            Type t = (Type) item;
            chosenClasses.add(t.getName());
            chosenClassesEncoded.add(parser.getStringEncoder().encode(t.getName()));
        });

        parser.entityConstraintsExtraction();
        parser.computeSupportConfidence();

        //default shapes must be computed first
        System.out.println(chosenClasses);
        String outputAddress = parser.extractSHACLShapes(!chosenClasses.isEmpty(), chosenClasses);
        List<NS> nodeShapes = parser.shapesExtractor.getNodeShapes();
        ExtractedShapes extractedShapes = new ExtractedShapes();
        extractedShapes.setVersionEntity(currentVersion);

        if(!checkboxUseDefaultShapes.getValue()) {
            int supportValue = support.getValue();
            double confidenceValue = confidence.getValue()/100;
            extractedShapes.setFileContentDefaultShapesPath(outputAddress);
            extractedShapes.setNodeShapesDefault(nodeShapes);

            outputAddress = parser.extractSHACLShapesWithPruning(!chosenClasses.isEmpty(), confidenceValue, supportValue, chosenClasses); // extract shapes with pruning
            System.out.println(prunedFileAddress);
            nodeShapes = parser.shapesExtractor.getNodeShapes();
            assert nodeShapes != null;
            PruningUtil pruningUtil = new PruningUtil();
            pruningUtil.applyPruningFlags(nodeShapes, supportValue, confidenceValue);
            pruningUtil.getDefaultStats(nodeShapes);
            pruningUtil.getStatsBySupport(nodeShapes);
            pruningUtil.getStatsByConfidence(nodeShapes);
            pruningUtil.getStatsByBoth(nodeShapes);
        }

        long startTimeMillis = System.currentTimeMillis();

        var classes = new ArrayList<String>();
        for (var i: classesGrid.getSelectedItems()) {
            classes.add(((Type)i).className);
        }
        extractedShapes.setClasses(classes);
        extractedShapes.setConfidence(confidence.isEnabled() ? confidence.getValue() : 0.0);
        extractedShapes.setSupport(support.isEnabled() ? support.getValue() : 0);
        extractedShapes.setQseType(QseType.valueOf(radioGroupQseType.getValue().toString()));
        extractedShapes.setCreatedAt(LocalDateTime.now());
        extractedShapes.setFileContentPath(outputAddress);
        if(extractedShapes.getModel().isEmpty()) {
            Notification.show("Caution, there were no shapes extracted. Please try lower values for support or confidence.");
            //Cleanup
            try {
                Files.delete(Paths.get(extractedShapes.getFileContentPath()));
                Files.delete(Paths.get(extractedShapes.getFileContentDefaultShapesPath()));
            } catch (Exception ex) {
                //ignore
            }
            throw new Exception("Nothing exported");
        }
        extractedShapes.setNodeShapes(nodeShapes, true);
        extractedShapes.generateComboBoxString();
        shapeService.insert(extractedShapes);
        long durationMillis = System.currentTimeMillis() - startTimeMillis;
        System.out.println("Shapes inserted, duration: "+durationMillis);
    }

    private static List<Type> getClasses(Map<Integer, Integer> classEntityCountMap, StringEncoder stringEncoder) {
        List<Type> types = new ArrayList<>();
        classEntityCountMap.forEach((k, v) -> {
            Type t = new Type();
            t.setName(stringEncoder.decode(k));
            t.setEncodedKey(v);
            t.setInstanceCount(v);
            types.add(t);
        });
        types.sort((d1, d2) -> d2.getInstanceCount() - d1.getInstanceCount());
        return types;
    }

    private static void setPaths() {
        try {
            String jarDir = System.getProperty("user.dir");
            Main.setOutputFilePathForJar(jarDir + "/Output/");
            Main.setConfigDirPathForJar(jarDir + "/config/");
            Main.setResourcesPathForJar(jarDir + "/resources/");
            Main.qseFromSpecificClasses = false;

            //Clean output directory
            File[] filesInOutputDir = new File(jarDir + "/Output/").listFiles();
            assert filesInOutputDir != null;
            for (File file : filesInOutputDir) {
                if (!file.getName().equals(".keep")) {
                    boolean deleted = file.delete();
                    if (deleted) {
                        System.out.println("Deleted already existing file: " + file.getPath());
                    }
                }
                if (file.isDirectory()) {
                    FileUtils.forceDelete(file);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
