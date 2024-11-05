package shape_comparator.views.compareshapes;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility.Gap;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import shape_comparator.data.ExtractedShapes;
import shape_comparator.data.NodeShape;
import shape_comparator.data.PropertyShape;
import shape_comparator.services.*;
import shape_comparator.views.MainLayout;
import shape_comparator.views.comparisondetails.ComparisonDetailsView;

@PageTitle("Compare Shapes")
@Route(value = "compare-shapes", layout = MainLayout.class)
@Uses(Icon.class)
@CssImport(
        themeFor = "vaadin-grid",
        value = "./themes/shape_comparator/components/treeGridCustomCellBackground.css"
)
public class CompareShapesView extends Composite<VerticalLayout> {
    @Autowired()
    private ShapesService shapeService;

    MultiSelectComboBox<Utils.ComboBoxItem> multiSelectShapes;
    TreeGrid<ComparisonTreeViewItem> treeViewComparison;
    TextField filterField = new TextField("Filter");
    RadioButtonGroup<String> radioGroupFilter = new RadioButtonGroup<>();
    List<ExtractedShapes> shapesCache;
    public CompareShapesView() {
        shapesCache = new ArrayList<>();
        HorizontalLayout layoutRowComboBox = new HorizontalLayout();
        HorizontalLayout layoutRowFilter = new HorizontalLayout();
        multiSelectShapes = new MultiSelectComboBox();
        treeViewComparison = new TreeGrid<>();
        getContent().setWidth("100%");
        getContent().getStyle().set("flex-grow", "1");
        getContent().setSpacing(false);
        layoutRowComboBox.addClassName(Gap.MEDIUM);
        layoutRowComboBox.setWidth("100%");
        layoutRowComboBox.setHeight("min-content");
        layoutRowFilter.addClassName(Gap.MEDIUM);
        layoutRowFilter.setWidth("100%");
        layoutRowFilter.setHeight("min-content");
        filterField.setHeight("min-content");
        multiSelectShapes.setLabel("Shapes");
        multiSelectShapes.setWidthFull();
        treeViewComparison.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_NO_BORDER,
                GridVariant.LUMO_NO_ROW_BORDERS);
        treeViewComparison.setWidth("100%");
        treeViewComparison.getStyle().set("flex-grow", "0");
        treeViewComparison.setHeight("70vh");
        radioGroupFilter.setItems(Arrays.stream(FilterEnum.values()).map(FilterEnum::getLabel).collect(Collectors.toList()));
        radioGroupFilter.setValue(FilterEnum.ALL.getLabel());
        var currentSearchFilter = (String)VaadinSession.getCurrent().getAttribute("comparison_searchValue");
        if(currentSearchFilter != null)
            filterField.setValue(currentSearchFilter);
        var currentFilter = (String)VaadinSession.getCurrent().getAttribute("comparison_filterValue");
        if(currentFilter != null)
            radioGroupFilter.setValue(currentFilter);
        layoutRowFilter.setAlignItems(FlexComponent.Alignment.BASELINE);
        getContent().add(layoutRowComboBox);
        layoutRowComboBox.add(multiSelectShapes);
        getContent().add(layoutRowFilter);
        layoutRowFilter.add(filterField);
        layoutRowFilter.add(radioGroupFilter);
        getContent().add(treeViewComparison);

        filterField.setValueChangeMode(ValueChangeMode.EAGER);
        filterField.addValueChangeListener(event -> applyFilters());

        radioGroupFilter.addValueChangeListener(event -> applyFilters());

        multiSelectShapes.addValueChangeListener(e -> {
            var extractedShapes = new ArrayList<ExtractedShapes>();
            for(var i : multiSelectShapes.getSelectedItems()) {
                extractedShapes.add(shapeService.get(i.id).get());
            }
            if(extractedShapes.stream().map(ExtractedShapes::getSupport).distinct().count() > 1)
                Notification.show("Caution, the compared items do not have the same support-value!");
            if(extractedShapes.stream().map(ExtractedShapes::getConfidence).distinct().count() > 1)
                Notification.show("Caution, the compared items do not have the same confidence-value!");
            if(extractedShapes.stream().map(ExtractedShapes::getClassesAsString).distinct().count() > 1)
                Notification.show("Caution, the compared items were not analyzed for the same classes!");

            setTreeViewData();
            VaadinSession.getCurrent().setAttribute("currentComboBoxItems", multiSelectShapes.getSelectedItems());
            applyFilters();
        });
        treeViewComparison.addItemClickListener(event -> {
            VaadinSession.getCurrent().setAttribute("currentCompareObject", event.getItem());
            getUI().ifPresent(ui -> ui.navigate(ComparisonDetailsView.class));
        });

        addAttachListener(e -> {
            fillComboBox();
            applyFilters();
        });
    }

    private void applyFilters() {
        var dataProvider = (TreeDataProvider<ComparisonTreeViewItem>) treeViewComparison.getDataProvider();

        // Get values from filter components
        String searchValue = filterField.getValue();
        String selectedRadioGroupFilter = radioGroupFilter.getValue();
        VaadinSession.getCurrent().setAttribute("comparison_searchValue", searchValue);
        VaadinSession.getCurrent().setAttribute("comparison_filterValue", selectedRadioGroupFilter);

        // Define filter predicate based on filter values
        if (searchValue == null && selectedRadioGroupFilter == null) {
            dataProvider.setFilter(null);
        } else {
            dataProvider.setFilter(item -> {
                boolean filterFieldPass = searchValue == null || item.getShapeName().toLowerCase().contains(searchValue.toLowerCase());

                boolean radioGroupPass = selectedRadioGroupFilter == null ||
                        (selectedRadioGroupFilter.equals(FilterEnum.IDENTICAL_PS.getLabel()) && item.areShapesEqual()) ||
                        (selectedRadioGroupFilter.equals(FilterEnum.IDENTICAL_NS.getLabel()) && item.areShapesEqual() && filterParentNodeShape(item)) ||
                        (selectedRadioGroupFilter.equals(FilterEnum.DIFFERENT.getLabel()) && !item.areShapesEqual()) ||
                        (selectedRadioGroupFilter.equals(FilterEnum.ALL.getLabel()));

                return filterFieldPass && radioGroupPass;
            });
            treeViewComparison.expandRecursively(dataProvider.getTreeData().getRootItems(),
                    99);
        }
        treeViewComparison.expandRecursively(dataProvider.getTreeData().getRootItems(), 99);
    }

    private Boolean filterParentNodeShape(ComparisonTreeViewItem child) {
        if(!child.isNodeShapeLine()) {
            var parent = child.getParentShape();
            return parent.areShapesEqual();
        }
        return true;
    }

    private void fillComboBox() {
        var shapes = shapeService.listAll();
        var comboBoxItems = new ArrayList<Utils.ComboBoxItem>();

        for (var shape : shapes) {
            var comboBoxItem = new Utils.ComboBoxItem();
            comboBoxItem.id = shape.getId();
            comboBoxItem.label = Utils.getComboBoxLabelForExtractedShapes(shape);
            comboBoxItems.add(comboBoxItem);
        }
        multiSelectShapes.setItems(comboBoxItems);
        multiSelectShapes.setItemLabelGenerator(item -> item.label);

        var currentComboBoxItems = (Set<Utils.ComboBoxItem>)VaadinSession.getCurrent().getAttribute("currentComboBoxItems");
        if(currentComboBoxItems != null && !currentComboBoxItems.isEmpty()
                && currentComboBoxItems.stream()
                .map(item -> item.id)
                .allMatch(id -> comboBoxItems.stream()
                        .map(comboBoxItem -> comboBoxItem.id)
                        .collect(Collectors.toSet())
                        .contains(id))) {
            for (var cbi :
                    currentComboBoxItems) {
                var newComboBoxItem = comboBoxItems.stream().filter(c -> c.id.equals(cbi.id)).findFirst();
                newComboBoxItem.ifPresent(comboBoxItem -> multiSelectShapes.select(comboBoxItem));
            }
        }
    }

    private void setTreeViewData() {
        var nodeShapesToShow = new ArrayList<ComparisonTreeViewItem>();
        treeViewComparison.removeAllColumns();
        for(var comboBoxItem : multiSelectShapes.getSelectedItems()) {
            var cacheItem = shapesCache.stream().filter(es -> es.getId().equals(comboBoxItem.id)).findFirst();
            ExtractedShapes extractedShapes;

            if(cacheItem.isPresent())
                extractedShapes = cacheItem.get();
            else {
                extractedShapes = shapeService.getWithNodeShapes(comboBoxItem.id);
                shapesCache.add(extractedShapes);
            }

            var nodeShapes = extractedShapes.getNodeShapes();
            var nodeShapesToShowMap = nodeShapesToShow
                    .stream().map(ComparisonTreeViewItem::getShapeName).toList();
            for(var ns : nodeShapes) {
                if(nodeShapesToShowMap.contains(ns.getIri().getLocalName())) {
                    var nodeShapeToShow = nodeShapesToShow.stream().filter(n -> n.getShapeName()
                            .equals(ns.getIri().getLocalName())).findFirst().get();
                    nodeShapeToShow.addNodeShape(ns, comboBoxItem.id);
                }
                else {
                    var newItem = new ComparisonTreeViewItem();
                    newItem.addNodeShape(ns, comboBoxItem.id);
                    nodeShapesToShow.add(newItem);
                }
            }
            treeViewComparison.addHierarchyColumn(o -> getTreeViewTextFromViewItem(o, comboBoxItem.id))
                .setHeader(comboBoxItem.label);
        }

        addEqualInformationNS(nodeShapesToShow);

        treeViewComparison.setItems(nodeShapesToShow, item -> getPropertyShapes(item));

        treeViewComparison.expand(nodeShapesToShow);
        treeViewComparison.setClassNameGenerator(e -> !e.areShapesEqual() ? "warn" : null);
    }

    private String getTreeViewTextFromViewItem(ComparisonTreeViewItem o, Long extractedShapesId) {
        if(o.isNodeShapeLine()) {
            if(o.getNodeShapeList().containsKey(extractedShapesId))
                return o.getShapeName();
            else
                return "-";
        }
        else {
            if(o.getPropertyShapeList().containsKey(extractedShapesId))
                return o.getShapeName();
            else
                return "-";
        }
    }

    private List<ComparisonTreeViewItem> getPropertyShapes(ComparisonTreeViewItem item) {
        var propertyShapesToShow = new ArrayList<ComparisonTreeViewItem>();
        if(item.getPropertyShapeList().isEmpty()) { //important for performance -> only execute this for NodeShapes
            for (var comboBoxItem : multiSelectShapes.getSelectedItems()) {
                var extractedShapes = shapesCache.stream().filter(es -> es.getId().equals(comboBoxItem.id)).findFirst().get();
                var nodeShape = extractedShapes.getNodeShapes()
                        .stream().filter(n -> n.getIri().getLocalName().equals(item.getShapeName())).findFirst();
                if (nodeShape.isPresent()) {
                    var propertyShapesToShowMap = propertyShapesToShow
                            .stream().map(ComparisonTreeViewItem::getShapeName).toList();
                    for (var ps : nodeShape.get().getPropertyShapes()) {
                        if (propertyShapesToShowMap.contains(ps.getIri().getLocalName())) {
                            var propertyShapeToShow = propertyShapesToShow.stream().filter(n -> n.getShapeName()
                                    .equals(ps.getIri().getLocalName())).findFirst().get();
                            propertyShapeToShow.addPropertyShape(ps, comboBoxItem.id);
                        } else {
                            var newItem = new ComparisonTreeViewItem();
                            newItem.addPropertyShape(ps, comboBoxItem.id);
                            newItem.setParentShape(item);
                            propertyShapesToShow.add(newItem);
                        }
                    }
                }

            }
        }
        addEqualInformationPS(propertyShapesToShow);
        return propertyShapesToShow;
    }

    private void addEqualInformationPS(ArrayList<ComparisonTreeViewItem> propertyShapesToShow) {
        for (var comparisonTreeViewItem:
             propertyShapesToShow) {
            comparisonTreeViewItem.setShapesEqual(
                    areAllStringsEqual(
                            comparisonTreeViewItem.getPropertyShapeList().values().stream()
                                    .map(PropertyShape::getGeneratedText).collect(Collectors.toList())));
        }
    }

    private void addEqualInformationNS(ArrayList<ComparisonTreeViewItem> propertyShapesToShow) {
        for (var comparisonTreeViewItem:
                propertyShapesToShow) {
            comparisonTreeViewItem.setShapesEqual(
                    areAllStringsEqual(
                            comparisonTreeViewItem.getNodeShapeList().values().stream()
                                    .map(NodeShape::getGeneratedText).collect(Collectors.toList())));

        }
    }
    private Boolean areAllStringsEqual(List<String> texts) {
        if(texts.size() != multiSelectShapes.getSelectedItems().size())
            return false;
        for (int i = 0; i < texts.size(); i++) {
            for (int j = i + 1; j < texts.size(); j++) {
                if (!texts.get(i).equals(texts.get(j))) {
                    return false;
                }
            }
        }
        return true;
    }

    public enum FilterEnum {
        ALL("All"),
        IDENTICAL_NS("Identical node shapes"),
        IDENTICAL_PS("Identical property shapes"),
        DIFFERENT("Different shapes");

        private final String label;

        FilterEnum(String label) {
            this.label = label;
        }public String getLabel() {
            return label;
        }
    }
}
