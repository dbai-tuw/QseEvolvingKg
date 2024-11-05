package shape_comparator.views.newgraph;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;
import shape_comparator.data.Graph;
import shape_comparator.services.GraphService;
import shape_comparator.services.Utils;
import shape_comparator.services.VersionService;
import shape_comparator.views.MainLayout;

import java.io.*;
import java.time.LocalDateTime;

@PageTitle("New Graph")
@Route(value = "new-graph", layout = MainLayout.class)
@Uses(Icon.class)
public class NewGraphView extends Composite<VerticalLayout> {
    @Autowired()
    private GraphService graphService;

    @Autowired()
    private VersionService versionService;

    private TextField textFieldGraphName;
    private Upload uploadGraphFile;
    private Graph existingGraph;
    private Button buttonSave;
    private Select<String> preconfiguredGraphs;

    public NewGraphView() {
        textFieldGraphName = new TextField();
        buttonSave = new Button();
        getContent().setWidth("100%");
        getContent().getStyle().set("flex-grow", "1");
        buttonSave.setText("Save Graph");
        getContent().add(textFieldGraphName);
        MemoryBuffer buffer = new MemoryBuffer();
        uploadGraphFile = new Upload(buffer);
        preconfiguredGraphs = new Select<>();
        Utils.setGraphOrVersionGuiFields(textFieldGraphName,buttonSave,uploadGraphFile, preconfiguredGraphs);

        buttonSave.addClickListener(event -> {
            if (textFieldGraphName.getValue().isEmpty()) {
                Notification.show("Name field cannot be empty");
            }
            else if(buffer.getFileName().isEmpty() && Utils.isEmptyItemSelected(preconfiguredGraphs)) {
                Notification.show("Please choose a pre-configured graph or upload a file");
            }
            else if(!buffer.getFileName().isEmpty() && !Utils.isEmptyItemSelected(preconfiguredGraphs))
                Notification.show("Please either deselect the pre-configured graph or remove the uploaded .nt file");
            else if (!buffer.getFileName().isEmpty() || !Utils.isEmptyItemSelected(preconfiguredGraphs)) {
                if(!buffer.getFileName().isEmpty()) {
                    try (InputStream inputStream = buffer.getInputStream()) {
                        saveFile(inputStream, textFieldGraphName.getValue(), "");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    saveFile(null, textFieldGraphName.getValue(), preconfiguredGraphs.getValue());
                }
                Notification.show("Graph saved!");
                getUI().ifPresent(ui -> ui.navigate("graphs"));
            }
        });
        getContent().add(preconfiguredGraphs);
        getContent().add(uploadGraphFile);
        getContent().add(buttonSave);
    }

    private void saveFile(InputStream inputStream, String graphName, String preconfiguredGraphPath)  {
        Graph graph = new Graph();
        graph.setName(graphName);
        graph.setCreatedAt(LocalDateTime.now());
        graph = graphService.insert(graph);

        Utils.handleSaveFile(graph, versionService, inputStream, "Original", preconfiguredGraphPath);
    }
}
