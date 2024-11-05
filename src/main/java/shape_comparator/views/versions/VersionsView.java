package shape_comparator.views.versions;

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
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import shape_comparator.data.Version;
import shape_comparator.services.Utils;
import shape_comparator.services.Utils.ComboBoxItem;
import shape_comparator.services.GraphService;
import shape_comparator.services.VersionService;
import shape_comparator.views.MainLayout;
import shape_comparator.views.utils.ValidationMessage;
import shape_comparator.views.newversion.NewVersionView;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@PageTitle("Versions")
@Route(value = "versions", layout = MainLayout.class)
@Uses(Icon.class)
public class VersionsView extends Composite<VerticalLayout>  {
    @Autowired()
    private VersionService versionService;
    @Autowired()
    private GraphService graphService;

    private Grid gridVersions;
    private Long currentGraphId;

    public VersionsView() {
        HorizontalLayout horizontalLayout = new HorizontalLayout();
        Select<ComboBoxItem> comboBoxGraphs = new Select<>();
        Button buttonNewVersion = new Button();
        gridVersions = new Grid(Version.class);
        getContent().setWidth("100%");
        getContent().getStyle().set("flex-grow", "1");
        horizontalLayout.setWidth("100%");
        horizontalLayout.setHeight("min-content");
        horizontalLayout.setSpacing(true);
        horizontalLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
        comboBoxGraphs.setLabel("Graph");
        comboBoxGraphs.setWidth("min-content");
        gridVersions.setWidth("100%");
        gridVersions.setHeight("70vh");
        gridVersions.getStyle().set("flex-grow", "0");
        gridVersions.setColumns("versionNumber", "name", "createdAt","path");
        gridVersions.getColumnByKey("path").setTooltipGenerator(v -> ((Version)v).getPathWithSpacesForTooltip());
        gridVersions.getColumnByKey("createdAt").setRenderer(new TextRenderer<>(e -> ((Version)e).getCreatedAt().format(Utils.formatter)));
        gridVersions.addColumn(new ComponentRenderer<>(Button::new, (button, ver) -> {
            button.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_TERTIARY);
            button.addClickListener(e -> {
                Version version = (Version) ver;
                if(!version.getPath().contains(Utils.preconfiguredFolderName)) {
                    try {
                        Files.delete(Paths.get(version.getPath()));
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
                versionService.delete(version.getId());
                fillGrid();
            });
            button.setIcon(new Icon(VaadinIcon.TRASH));
        })).setHeader("").setAutoWidth(true).setFlexGrow(0);
        gridVersions.getColumns().forEach(column -> ((Grid.Column)column).setResizable(true));
        buttonNewVersion.setText("New Version");
        buttonNewVersion.setWidth("min-content");
        buttonNewVersion.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        buttonNewVersion.addClickListener(event -> getUI().ifPresent(ui -> ui.navigate(NewVersionView.class,comboBoxGraphs.getValue().id)));

        Binder<Version> binder = new Binder<>(Version.class);
        Editor<Version> editor = gridVersions.getEditor();
        editor.setBinder(binder);
        ValidationMessage nameValidationMessage = new ValidationMessage();

        TextField nameField = new TextField();
        nameField.setWidthFull();
        addCloseHandler(nameField, editor);
        binder.forField(nameField)
                .asRequired("Name must not be empty")
                .withStatusLabel(nameValidationMessage)
                .bind(Version::getName, Version::setName);
        gridVersions.getColumnByKey("name").setEditorComponent(nameField);

        gridVersions.addItemDoubleClickListener(e -> {
            var event = (ItemDoubleClickEvent)e;
            editor.editItem((Version) event.getItem());
            Component editorComponent = event.getColumn().getEditorComponent();
            if (editorComponent instanceof Focusable) {
                ((Focusable) editorComponent).focus();
            }
        });

        editor.addCancelListener(e -> nameValidationMessage.setText(""));
        editor.addCloseListener(e -> versionService.update(e.getItem()));

        getContent().add(horizontalLayout);
        horizontalLayout.add(comboBoxGraphs);
        horizontalLayout.add(buttonNewVersion);
        getContent().add(nameValidationMessage);
        getContent().add(gridVersions);
        comboBoxGraphs.addValueChangeListener(event -> {
            if(event.getValue() != null) {
                currentGraphId = event.getValue().id;
                VaadinSession.getCurrent().setAttribute("versions_currentGraphId", currentGraphId);
                fillGrid();
            }
        });

        addAttachListener(event -> setGraphs(comboBoxGraphs));
    }

    private static void addCloseHandler(Component textField,
                                        Editor<Version> editor) {
        textField.getElement().addEventListener("keydown", e -> editor.cancel())
                .setFilter("event.code === 'Escape'");
    }

    private void setGraphs(Select<ComboBoxItem> comboBox) {
        List<ComboBoxItem> comboBoxItemList = graphService.listAll().stream()
                .map(graph -> new ComboBoxItem(graph.getName(),graph.getId()))
                .collect(Collectors.toList());
        comboBox.setItems(comboBoxItemList);
        comboBox.setItemLabelGenerator(item -> item.label);
        if (comboBoxItemList.size() > 0) {
            var currentGraphId = (Long)VaadinSession.getCurrent().getAttribute("versions_currentGraphId");
            var selectedGraph = comboBoxItemList.stream().filter(c -> c.id.equals(currentGraphId)).findFirst();
            if(selectedGraph.isPresent())
                comboBox.setValue(selectedGraph.get());
            else {
                var firstItem = comboBoxItemList.stream().findFirst();
                comboBox.setValue(firstItem.get());
            }
        }
    }

    private void fillGrid() {
        gridVersions.setItems(query -> versionService.listByGraphId(
                        PageRequest.of(query.getPage(), query.getPageSize(), VaadinSpringDataHelpers.toSpringDataSort(query)), currentGraphId)
                .stream());
    }
}

