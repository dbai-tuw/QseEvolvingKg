package shape_comparator.views.comparisondetails;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.beans.factory.annotation.Autowired;
import shape_comparator.data.NodeShape;
import shape_comparator.services.ComparisonTreeViewItem;
import shape_comparator.services.ShapesService;
import shape_comparator.services.Utils;
import shape_comparator.views.MainLayout;

import java.util.ArrayList;
import java.util.Set;

@Route(value = "comparison-details", layout = MainLayout.class)
@Uses(Icon.class)
public class ComparisonDetailsView extends Composite<VerticalLayout> implements HasDynamicTitle {
    private ComparisonDiv comparisonDiv;
    private Select<Utils.ComboBoxItem> selectItemOld;
    private Select<Utils.ComboBoxItem> selectItemNew;
    private String oldText;
    private String newText;
    private ComparisonTreeViewItem treeViewItem;
    private Paragraph infoParagraph;
    private Long oldSelectItemIdExtractedShapes;
    private Long newSelectItemIdExtractedShapes;


    @Autowired
    ShapesService shapesService;

    public ComparisonDetailsView() {
        HorizontalLayout layoutRow = new HorizontalLayout();
        VerticalLayout layout = new VerticalLayout();
        layout.setWidth("100%");
        getContent().add(layout);
        layout.add(layoutRow);
        selectItemOld = new Select<>();
        selectItemOld.setLabel("First version to compare");
        selectItemOld.setWidth("100%");
        selectItemNew = new Select<>();
        selectItemNew.setLabel("Second version to compare");
        selectItemNew.setWidth("100%");
        layoutRow.setWidth("100%");
        layoutRow.getStyle().set("justify-content", "space-between");
        layoutRow.add(selectItemOld);
        layoutRow.add(selectItemNew);
        treeViewItem = (ComparisonTreeViewItem)VaadinSession.getCurrent().getAttribute("currentCompareObject");

        var comboBoxItems = (Set<Utils.ComboBoxItem>)VaadinSession.getCurrent().getAttribute("currentComboBoxItems");
        if(comboBoxItems != null) {
            selectItemOld.setItems(comboBoxItems);
            selectItemOld.setItemLabelGenerator(item -> item.label);
            selectItemNew.setItems(comboBoxItems);
            selectItemNew.setItemLabelGenerator(item -> item.label);

            var list = selectItemOld.getDataProvider().fetch(new Query<>()).toList();
            selectItemOld.setValue(list.get(0));
            oldSelectItemIdExtractedShapes = list.get(0).id;
            selectItemNew.setValue(list.get(list.size() - 1));
            newSelectItemIdExtractedShapes = list.get(list.size() - 1).id;
            if(treeViewItem != null) {
                oldText = Utils.escapeNew(getText(list.get(0).id));
                newText = Utils.escapeNew(getText(list.get(list.size() - 1).id));
            }
        }
        infoParagraph = new Paragraph();
        infoParagraph.getElement().getStyle().set("font-style", "italic");
        infoParagraph.getElement().getStyle().set("padding", "0");
        infoParagraph.getElement().getStyle().setDisplay(Style.Display.NONE);
        layout.add(infoParagraph);
        comparisonDiv = new ComparisonDiv(oldText, newText);
        updateColorCombobox(oldText,newText);
        layout.add(comparisonDiv);

        selectItemOld.addValueChangeListener(e -> {
            oldText = Utils.escapeNew(getText(e.getValue().id));
            oldSelectItemIdExtractedShapes = e.getValue().id;
            comparisonDiv.updateTextDifferences(oldText,newText);
            updateInfoParagraph();
            updateColorCombobox(oldText,newText);
        });
        selectItemNew.addValueChangeListener(e -> {
            newText = Utils.escapeNew(getText(e.getValue().id));
            newSelectItemIdExtractedShapes = e.getValue().id;
            comparisonDiv.updateTextDifferences(oldText,newText);
            updateInfoParagraph();
            updateColorCombobox(oldText,newText);
        });

        getContent().addAttachListener(e -> {
            layout.add(new H2("All SHACL shapes"));
            if(this.treeViewItem != null) {
                for (var extractedShapes : selectItemOld.getDataProvider().fetch(new Query<>()).toList()) {
                    layout.add(new H4(Utils.getComboBoxLabelForExtractedShapes(shapesService.get(extractedShapes.id).get())));
                    Div div = new Div();
                    if(treeViewItem.isNodeShapeLine()) {
                        if(treeViewItem.getNodeShapeList().containsKey(extractedShapes.id)) {
                            addText(layout, extractedShapes, div);
                        }
                    }
                    else {
                        if(treeViewItem.getPropertyShapeList().containsKey(extractedShapes.id)) {
                            addText(layout, extractedShapes, div);
                        }
                    }
                }
                updateInfoParagraph();
            }
        });
    }

    private void updateColorCombobox(String oldText, String newText) {
        if(oldText != null && !oldText.isEmpty() && newText != null && !newText.isEmpty() && !oldText.equals(newText)) {
            selectItemOld.getStyle().set("--vaadin-input-field-border-width", "1px");
            selectItemOld.getStyle().set("--vaadin-input-field-border-color", "#FF0000");
            selectItemNew.getStyle().set("--vaadin-input-field-border-width", "1px");
            selectItemNew.getStyle().set("--vaadin-input-field-border-color", "#008000");
        }
        else {
            selectItemNew.getStyle().set("--vaadin-input-field-border-width", "0px");
            selectItemOld.getStyle().set("--vaadin-input-field-border-width", "0px");
        }

    }

    private void updateInfoParagraph() {
        if(oldText == null || oldText.isEmpty() || newText == null || newText.isEmpty()) {
            infoParagraph.getElement().getStyle().setDisplay(Style.Display.BLOCK);
            int supportThreshold = treeViewItem.getSupportThreshold();
            int confidenceThreshold = treeViewItem.getConfidenceThreshold();
            int support = 0;
            int confidence = 0;
            comparisonDiv.getElement().getStyle().setDisplay(Style.Display.NONE);

            //shape has been deleted
            if(oldText != null && !oldText.isEmpty() && (newText == null || newText.isEmpty())) {
                if(treeViewItem.usesDefaultShapes()) {
                    infoParagraph.setText("This shape was deleted because there were no nodes of this class found (default shapes were compared)");
                }
                else {
                    try {
                        var shapeNamesToFetch = new ArrayList<String>();
                        shapeNamesToFetch.add(treeViewItem.getShapeName());
                        if (treeViewItem.getParentShape() != null) {
                            shapeNamesToFetch.add(treeViewItem.getParentShape().getShapeName());
                        }
                        var extractedShapes = shapesService.getWithNodeShapesDefault(newSelectItemIdExtractedShapes, shapeNamesToFetch);

                        if (treeViewItem.isNodeShapeLine()) {
                            var nodeShape = extractedShapes.getNodeShapesDefault().stream().filter(ns -> ns.getIri().getLocalName().equals(treeViewItem.getShapeName())).findFirst();
                            support = nodeShape.isPresent() ? nodeShape.get().getSupport() : 0;
                        } else {
                            try {
                                var nodeShape = extractedShapes.getNodeShapesDefault().stream().filter(ns -> ns.getIri().getLocalName().equals(treeViewItem.getParentShape().getShapeName())).findFirst().get();
                                var propertyShape = nodeShape.getPropertyShapes()
                                        .stream().filter(ps -> ps.getIri().getLocalName().equals(treeViewItem.getShapeName())).findFirst().get();
                                support = propertyShape.getSupport();
                                confidence = (int) Math.round(propertyShape.getConfidence() * 100);
                            } catch (Exception ex) {
                                //ignore, values are 0 anyways
                            }
                        }

                        if (supportThreshold != 0 && support <= supportThreshold) {
                            infoParagraph.setText(String.format("This shape was deleted because there were less (<=) shapes (%d) than defined by the support-parameter (%d)", support, supportThreshold));
                        } else if (confidenceThreshold != 0 && confidence <= confidenceThreshold) {
                            infoParagraph.setText(String.format("This shape was deleted because the confidence (%d %%) was less(<=) than defined by the confidence-parameter (%d %%)", confidence, confidenceThreshold));
                        }
                    }
                    catch(Exception ex) {
                        infoParagraph.setText("This shape was deleted!");
                        ex.printStackTrace();
                    }
                }
            }
            //shape was added
            else if(oldText == null || oldText.isEmpty()) {
                infoParagraph.setText("This shape was newly added!");
            }
        }
        else {
            infoParagraph.getElement().getStyle().setDisplay(Style.Display.NONE);
            comparisonDiv.getElement().getStyle().setDisplay(Style.Display.BLOCK);
        }
    }

    private void addText(VerticalLayout layout, Utils.ComboBoxItem extractedShapes, Div div) {
        var allText = "";
        var supportText = "";
        if(treeViewItem.isNodeShapeLine()) {
            NodeShape ns = treeViewItem.getNodeShapeList().get(extractedShapes.id);
            if(ns != null) {
                supportText = String.format("Support: %d", ns.getSupport());
                allText =  ns.getGeneratedText();
            }
        }
        else {
            var ps = treeViewItem.getPropertyShapeList().get(extractedShapes.id);
            if(ps != null) {
                int confidence;
                confidence = (int) Math.round(ps.getConfidence() * 100);
                supportText = String.format("Support: %d, Confidence: %s", ps.getSupport(),
                        confidence + "%");
                allText =  ps.getGeneratedText();
            }
        }
        div.getElement().setProperty("innerHTML", convertNewlinesToHtmlBreaks(allText));
        layout.add(div);
        Paragraph paragraph = new Paragraph(supportText);
        paragraph.getElement().getStyle().set("font-style", "italic");
        paragraph.getElement().getStyle().set("padding", "0");
        layout.add(new Paragraph(paragraph));
    }

    private String getText(Long extractedShapesId) {
        if(treeViewItem.isNodeShapeLine()) {
            NodeShape ns = treeViewItem.getNodeShapeList().get(extractedShapesId);
            if(ns == null)
                return "";
            return ns.getGeneratedText();
        }
        else {
            var ps = treeViewItem.getPropertyShapeList().get(extractedShapesId);
            if(ps == null)
                return "";
            return ps.getGeneratedText();
        }
    }
    public static String convertNewlinesToHtmlBreaks(String input) {
        String s = ComparisonDiv.escapeHtmlCharacters(input);
        if(Utils.usePrettyFormatting)
            return s.replaceAll("\r","").replaceAll("\n", "<br>");
        else
            return s.replaceAll("\\\\\\\\n", "<br>");
    }

    @Override
    public String getPageTitle() {
        if(treeViewItem == null)
            return "Comparison Detail";
        return "Comparison Detail - " + treeViewItem.getShapeName();
    }
}
