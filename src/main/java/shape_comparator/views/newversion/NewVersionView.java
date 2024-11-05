package shape_comparator.views.newversion;

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
import com.vaadin.flow.router.*;
import org.springframework.beans.factory.annotation.Autowired;
import shape_comparator.data.Graph;
import shape_comparator.services.GraphService;
import shape_comparator.services.Utils;
import shape_comparator.services.VersionService;
import shape_comparator.views.MainLayout;

import java.io.*;

@Route(value = "new-version", layout = MainLayout.class)
@Uses(Icon.class)
public class NewVersionView extends Composite<VerticalLayout> implements HasUrlParameter<Long>, HasDynamicTitle {
    @Autowired()
    private GraphService graphService;

    @Autowired()
    private VersionService versionService;

    private Long graphId;
    private String graphName;

    public NewVersionView() {
        TextField textField = new TextField();
        Button buttonPrimary = new Button();
        getContent().setWidth("100%");
        getContent().getStyle().set("flex-grow", "1");
        buttonPrimary.setText("Upload Graph Version");
        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        Select<String> preconfiguredGraphs = new Select<>();
        Utils.setGraphOrVersionGuiFields(textField, buttonPrimary, upload, preconfiguredGraphs);

        buttonPrimary.addClickListener(event -> {
            //duplicate code from new graph view
            if (textField.getValue().isEmpty()) {
                Notification.show("Name field cannot be empty");
            }
            else if(buffer.getFileName().isEmpty() && Utils.isEmptyItemSelected(preconfiguredGraphs)) {
                Notification.show("Please choose a pre-configured graph or upload a file");
            }
            else if(!buffer.getFileName().isEmpty() && !Utils.isEmptyItemSelected(preconfiguredGraphs))
                Notification.show("Please either deselect the pre-configured graph or remove the uploaded .nt file");
            else if (!buffer.getFileName().isEmpty() || !Utils.isEmptyItemSelected(preconfiguredGraphs)) {
                if (!buffer.getFileName().isEmpty()) {
                    try (InputStream inputStream = buffer.getInputStream()) {
                        saveFile(inputStream, textField.getValue(), "");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    saveFile(null, textField.getValue(), preconfiguredGraphs.getValue());
                }
                Notification.show("Version saved!");
            }
            getUI().ifPresent(ui -> ui.navigate("versions"));

        });

        getContent().add(textField);
        getContent().add(preconfiguredGraphs);
        getContent().add(upload);
        getContent().add(buttonPrimary);
    }

    private void saveFile(InputStream inputStream, String versionName, String preConfiguredGraphPath) {
        Graph graph = graphService.get(this.graphId).get();
        Utils.handleSaveFile(graph, versionService,inputStream, versionName, preConfiguredGraphPath);
    }

    @Override
    public void setParameter(BeforeEvent beforeEvent, Long aLong) {
        graphId = aLong;
        graphName = graphService.get(graphId).get().getName();
    }

    @Override
    public String getPageTitle() {
        return "New Version for graph " + graphName;
    }
}
