package org.ovirt.engine.ui.common.widget.dialog;

import org.ovirt.engine.ui.common.CommonApplicationConstants;
import org.ovirt.engine.ui.common.CommonApplicationResources;
import org.ovirt.engine.ui.common.CommonApplicationTemplates;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ToggleButton;

public class AdvancedParametersExpander extends Composite {

    @UiField
    ToggleButton expander;

    private Element expanderContent;

    private static final CommonApplicationResources resources = GWT.create(CommonApplicationResources.class);

    private static final CommonApplicationTemplates templates = GWT.create(CommonApplicationTemplates.class);

    private static final CommonApplicationConstants constants = GWT.create(CommonApplicationConstants.class);

    interface ViewUiBinder extends UiBinder<ToggleButton, AdvancedParametersExpander> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    public AdvancedParametersExpander() {
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
    }

    public void initWithContent(Element expanderContent) {
        this.expanderContent = expanderContent;

        initStyle();
        initListener();
        initContent();
    }

    private void initStyle() {
        SafeHtml expandImage =
                SafeHtmlUtils.fromTrustedString(AbstractImagePrototype.create(resources.expanderImage()).getHTML());
        expander.getUpFace().setHTML(templates.imageTextButton(expandImage, constants.advancedParameters()));

        SafeHtml collapseImage =
                SafeHtmlUtils.fromTrustedString(AbstractImagePrototype.create(resources.expanderDownImage()).getHTML());
        expander.getDownFace().setHTML(templates.imageTextButton(collapseImage, constants.advancedParameters()));

    }

    private void initContent() {
        expanderContent.getStyle().setDisplay(Style.Display.NONE);
    }

    private void initListener() {
        expander.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                expanderContent.getStyle().setDisplay(expander.isDown() ? Style.Display.BLOCK : Style.Display.NONE);
            }
        });
    }
}
