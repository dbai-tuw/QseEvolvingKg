package shape_comparator.views.shapes;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility.Gap;
import org.springframework.beans.factory.annotation.Autowired;
import shape_comparator.data.ExtractedShapes;
import shape_comparator.data.Graph;
import shape_comparator.data.Version;
import shape_comparator.services.GraphService;
import shape_comparator.services.ShapesService;
import shape_comparator.services.Utils;
import shape_comparator.services.VersionService;
import shape_comparator.views.MainLayout;
import shape_comparator.views.generateshapes.GenerateShapesView;

import java.nio.file.Files;
import java.nio.file.Paths;

@PageTitle("Shapes")
@Route(value = "shapes", layout = MainLayout.class)
@Uses(Icon.class)
public class ShapesView extends Composite<VerticalLayout>{

    @Autowired()
    private VersionService versionService;
    @Autowired()
    private GraphService graphService;
    @Autowired()
    private ShapesService shapeService;
    Select<Utils.ComboBoxItem> selectItemGraph;
    Select<Utils.ComboBoxItem> selectItemVersion;
    Grid gridShapes;
    Graph currentGraph;
    Version currentVersion;
    Long currentVersionId;
    public ShapesView() {
        HorizontalLayout layoutRow = new HorizontalLayout();
        selectItemGraph = new Select<>();
        selectItemVersion = new Select<>();
        Button buttonGenerateShapes = new Button();
        gridShapes = new Grid(ExtractedShapes.class, false);
        getContent().setWidth("100%");
        getContent().getStyle().set("flex-grow", "1");
        layoutRow.addClassName(Gap.MEDIUM);
        layoutRow.setWidth("100%");
        layoutRow.setHeight("min-content");
        layoutRow.setVerticalComponentAlignment(FlexComponent.Alignment.END, buttonGenerateShapes);
        selectItemGraph.setLabel("Graph");
        selectItemGraph.setWidth("min-content");
        selectItemVersion.setLabel("Version");
        selectItemVersion.setWidth("min-content");
        buttonGenerateShapes.setText("Generate Shapes");
        buttonGenerateShapes.setWidth("min-content");
        buttonGenerateShapes.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        gridShapes.setWidth("100%");
        gridShapes.setHeight("70vh");
        gridShapes.getStyle().set("flex-grow", "0");

        getContent().add(layoutRow);
        layoutRow.add(selectItemGraph);
        layoutRow.add(selectItemVersion);
        layoutRow.add(buttonGenerateShapes);
        getContent().add(gridShapes);

        selectItemGraph.addValueChangeListener(event -> {
            if(event.getValue() != null) {
                Long selectedValue = event.getValue().id;
                VaadinSession.getCurrent().setAttribute("shapes_currentGraphId", selectedValue);
                currentGraph = graphService.get(selectedValue).get();
                Utils.setComboBoxVersionsData(selectedValue, versionService, selectItemVersion);
            }
        });

        selectItemVersion.addValueChangeListener(event -> {
            if(event.getValue() != null) {
                currentVersionId = event.getValue().id;
                VaadinSession.getCurrent().setAttribute("shapes_currentVersionId", currentVersionId);
                currentVersion = versionService.get(currentVersionId).get();
                setGridData();
            }
        });

        buttonGenerateShapes.addClickListener(buttonClickEvent -> getUI().ifPresent(ui -> ui.navigate(GenerateShapesView.class)));

        addAttachListener(event -> Utils.setComboBoxGraphData(graphService, selectItemGraph));
    }

    private void setGridData() {
        gridShapes.removeAllColumns();
        gridShapes.addColumn(o -> ((ExtractedShapes) o).getCreatedAt()).setHeader("Created At")
        .setRenderer(new TextRenderer<>(e -> ((ExtractedShapes)e).getCreatedAt().format(Utils.formatter))).setResizable(true);
        gridShapes.addColumn(o -> ((ExtractedShapes) o).getQseType()).setHeader("QSE Type").setResizable(true);
        gridShapes.addColumn(o -> ((ExtractedShapes) o).getSupport()).setHeader("Support").setResizable(true);
        gridShapes.addColumn(o -> ((ExtractedShapes) o).getConfidence()).setHeader("Confidence").setResizable(true);
        gridShapes.addColumn(o -> ((ExtractedShapes) o).getClassesAsString())
                .setTooltipGenerator(s -> ((ExtractedShapes)s).getClassesAsString())
                .setHeader("Classes").setResizable(true);
        gridShapes.addColumn(new ComponentRenderer<>(Button::new, (button, es) -> {
            button.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_TERTIARY);
            button.addClickListener(e -> {
                ExtractedShapes extractedShapes = (ExtractedShapes)es;
                try {
                    Files.delete(Paths.get(extractedShapes.getFileContentPath()));
                    Files.delete(Paths.get(extractedShapes.getFileContentDefaultShapesPath()));
                } catch (Exception ex) {
                    //ignore
                }
                shapeService.delete(extractedShapes.getId());
                setGridData();
            });
            button.setIcon(new Icon(VaadinIcon.TRASH));
        })).setHeader("").setAutoWidth(true).setFlexGrow(0);
        var items = shapeService.listByVersionId(currentVersionId);

        gridShapes.setItems(items);
    }
}
