package shape_comparator.views.graphs;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.ItemDoubleClickEvent;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;
import com.vaadin.flow.theme.lumo.LumoUtility.Gap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import shape_comparator.data.Graph;
import shape_comparator.services.GraphService;
import shape_comparator.services.Utils;
import shape_comparator.views.MainLayout;
import shape_comparator.views.utils.ValidationMessage;
import shape_comparator.views.newgraph.NewGraphView;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@PageTitle("Graphs")
@Route(value = "graphs", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@Uses(Icon.class)
public class GraphsView extends Composite<VerticalLayout> {
    @Autowired()
    private GraphService graphService;

    private Grid gridGraphs;
    ValidationMessage nameValidationMessage = new ValidationMessage();

    public GraphsView() {
        HorizontalLayout horizontalLayout = new HorizontalLayout();
        Button buttonNewGraph = new Button();
        gridGraphs = new Grid(Graph.class);
        getContent().setWidth("100%");
        getContent().getStyle().set("flex-grow", "1");
        horizontalLayout.addClassName(Gap.MEDIUM);
        horizontalLayout.setWidth("100%");
        horizontalLayout.setHeight("min-content");
        buttonNewGraph.setText("New Graph");
        buttonNewGraph.setWidth("min-content");
        buttonNewGraph.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        buttonNewGraph.addClickListener(event -> getUI().ifPresent(ui -> ui.navigate(NewGraphView.class)));
        gridGraphs.setWidth("100%");
        gridGraphs.setHeight("80vh");
        gridGraphs.getStyle().set("flex-grow", "0");
        gridGraphs.getColumns().forEach(column -> ((Grid.Column)column).setResizable(true));
        gridGraphs.setColumns("name","createdAt");
        gridGraphs.getColumnByKey("createdAt").setRenderer(new TextRenderer<>(g -> ((Graph)g).getCreatedAt().format(Utils.formatter)));
        gridGraphs.addColumn(new ComponentRenderer<>(Button::new, (button, gr) -> {
            button.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_TERTIARY);
            button.addClickListener(e -> {
                Graph graph = (Graph) gr;
                try {
                    Path directory = Path.of(Utils.getGraphDirectory()+graph.getName());
                    if(Files.exists(directory)) {
                        Files.walk(directory)
                                .map(Path::toFile)
                                .forEach(File::delete);
                        Files.delete(directory);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                graphService.delete(graph.getId());
                setGraphsData();
            });
            button.setIcon(new Icon(VaadinIcon.TRASH));
        })).setHeader("").setAutoWidth(true).setFlexGrow(0);

        //In-line Editing
        Binder<Graph> binder = new Binder<>(Graph.class);
        Editor<Graph> editor = gridGraphs.getEditor();
        editor.setBinder(binder);

        TextField nameField = new TextField();
        nameField.setWidthFull();
        addCloseHandler(nameField, editor);
        binder.forField(nameField)
                .asRequired("Name must not be empty")
                .withStatusLabel(nameValidationMessage)
                .bind(Graph::getName, Graph::setName);
        gridGraphs.getColumnByKey("name").setEditorComponent(nameField);

        gridGraphs.addItemDoubleClickListener(e -> {
            var event = (ItemDoubleClickEvent)e;
            if(event.getItem() != null)
                editor.editItem((Graph)event.getItem());
            Component editorComponent = event.getColumn().getEditorComponent();
            if (editorComponent instanceof Focusable) {
                ((Focusable) editorComponent).focus();
            }
        });
        editor.addCancelListener(e -> nameValidationMessage.setText(""));
        editor.addCloseListener(e -> graphService.update(e.getItem()));


        setGraphsData();
        getContent().add(horizontalLayout);
        horizontalLayout.add(buttonNewGraph);
        getContent().add(gridGraphs);
    }

    private void setGraphsData() {
        gridGraphs.setItems(query -> graphService.list(
                PageRequest.of(query.getPage(), query.getPageSize(), VaadinSpringDataHelpers.toSpringDataSort(query)))
                .stream());
    }

    private static void addCloseHandler(Component textField,
                                        Editor<Graph> editor) {
        textField.getElement().addEventListener("keydown", e -> editor.cancel())
                .setFilter("event.code === 'Escape'");
    }
}
