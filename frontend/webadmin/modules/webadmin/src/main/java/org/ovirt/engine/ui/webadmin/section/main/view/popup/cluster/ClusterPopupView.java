package org.ovirt.engine.ui.webadmin.section.main.view.popup.cluster;

import org.ovirt.engine.core.common.businessentities.ServerCpu;
import org.ovirt.engine.core.common.businessentities.storage_pool;
import org.ovirt.engine.core.common.mode.ApplicationMode;
import org.ovirt.engine.core.compat.Event;
import org.ovirt.engine.core.compat.EventArgs;
import org.ovirt.engine.core.compat.IEventListener;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.view.popup.AbstractModelBoundPopupView;
import org.ovirt.engine.ui.common.widget.Align;
import org.ovirt.engine.ui.common.widget.dialog.SimpleDialogPanel;
import org.ovirt.engine.ui.common.widget.dialog.tab.DialogTab;
import org.ovirt.engine.ui.common.widget.editor.EntityModelCheckBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.EntityModelPasswordBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.EntityModelRadioButtonEditor;
import org.ovirt.engine.ui.common.widget.editor.EntityModelTextAreaLabelEditor;
import org.ovirt.engine.ui.common.widget.editor.EntityModelTextBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.ListModelListBoxEditor;
import org.ovirt.engine.ui.common.widget.renderer.NullSafeRenderer;
import org.ovirt.engine.ui.uicommonweb.models.ApplicationModeHelper;
import org.ovirt.engine.ui.uicommonweb.models.clusters.ClusterModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationMessages;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.cluster.ClusterPopupPresenterWidget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

public class ClusterPopupView extends AbstractModelBoundPopupView<ClusterModel> implements ClusterPopupPresenterWidget.ViewDef {

    interface Driver extends SimpleBeanEditorDriver<ClusterModel, ClusterPopupView> {
        Driver driver = GWT.create(Driver.class);
    }

    interface ViewUiBinder extends UiBinder<SimpleDialogPanel, ClusterPopupView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    interface ViewIdHandler extends ElementIdHandler<ClusterPopupView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @UiField
    WidgetStyle style;

    @UiField
    @WithElementId
    DialogTab generalTab;

    @UiField
    FlowPanel dataCenterPanel;

    @UiField(provided = true)
    @Path(value = "dataCenter.selectedItem")
    @WithElementId
    ListModelListBoxEditor<Object> dataCenterEditor;

    @UiField
    @Path(value = "name.entity")
    @WithElementId
    EntityModelTextBoxEditor nameEditor;

    @UiField
    @Path(value = "description.entity")
    @WithElementId
    EntityModelTextBoxEditor descriptionEditor;

    @UiField(provided = true)
    @Path(value = "cPU.selectedItem")
    @WithElementId
    ListModelListBoxEditor<Object> cPUEditor;

    @UiField(provided = true)
    @Path(value = "version.selectedItem")
    @WithElementId
    ListModelListBoxEditor<Object> versionEditor;

    @UiField
    @Path(value = "enableOvirtService.entity")
    @WithElementId("enableOvirtService")
    EntityModelCheckBoxEditor enableOvirtServiceEditor;

    @UiField
    @Path(value = "enableGlusterService.entity")
    @WithElementId("enableGlusterService")
    EntityModelCheckBoxEditor enableGlusterServiceEditor;

    @UiField(provided = true)
    @Path(value = "isImportGlusterConfiguration.entity")
    @WithElementId("isImportGlusterConfiguration")
    EntityModelCheckBoxEditor importGlusterConfigurationEditor;

    @UiField
    @Ignore
    Label importGlusterExplanationLabel;

    @UiField
    @Path(value = "glusterHostAddress.entity")
    @WithElementId
    EntityModelTextBoxEditor glusterHostAddressEditor;

    @UiField
    @Path(value = "glusterHostFingerprint.entity")
    @WithElementId
    EntityModelTextAreaLabelEditor glusterHostFingerprintEditor;

    @UiField
    @Path(value = "glusterHostPassword.entity")
    @WithElementId
    EntityModelPasswordBoxEditor glusterHostPasswordEditor;

    @UiField
    @Ignore
    Label messageLabel;

    @UiField
    @WithElementId
    DialogTab memoryOptimizationTab;

    @UiField(provided = true)
    @Path(value = "optimizationNone_IsSelected.entity")
    @WithElementId
    EntityModelRadioButtonEditor optimizationNoneEditor;

    @UiField
    @Ignore
    Label optimizationNoneExplanationLabel;

    @UiField(provided = true)
    @Path(value = "optimizationForServer_IsSelected.entity")
    @WithElementId
    EntityModelRadioButtonEditor optimizationForServerEditor;

    @UiField
    @Ignore
    Label optimizationForServerExplanationLabel;

    @UiField(provided = true)
    @Path(value = "optimizationForDesktop_IsSelected.entity")
    @WithElementId
    EntityModelRadioButtonEditor optimizationForDesktopEditor;

    @UiField
    @Ignore
    Label optimizationForDesktopExplanationLabel;

    @UiField(provided = true)
    @Path(value = "optimizationCustom_IsSelected.entity")
    @WithElementId
    EntityModelRadioButtonEditor optimizationCustomEditor;

    @UiField(provided = true)
    @Ignore
    Label optimizationCustomExplanationLabel;

    @UiField
    @WithElementId
    DialogTab resiliencePolicyTab;

    @UiField(provided = true)
    @Path(value = "migrateOnErrorOption_YES.entity")
    @WithElementId
    EntityModelRadioButtonEditor migrateOnErrorOption_YESEditor;

    @UiField(provided = true)
    @Path(value = "migrateOnErrorOption_HA_ONLY.entity")
    @WithElementId
    EntityModelRadioButtonEditor migrateOnErrorOption_HA_ONLYEditor;

    @UiField(provided = true)
    @Path(value = "migrateOnErrorOption_NO.entity")
    @WithElementId
    EntityModelRadioButtonEditor migrateOnErrorOption_NOEditor;

    private ApplicationMessages messages;

    @Inject
    public ClusterPopupView(EventBus eventBus, ApplicationResources resources, ApplicationConstants constants, ApplicationMessages messages) {
        super(eventBus, resources);
        this.messages = messages;
        initListBoxEditors();
        initRadioButtonEditors();
        initCheckBoxEditors();
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        ViewIdHandler.idHandler.generateAndSetIds(this);
        addStyles();
        localize(constants);
        Driver.driver.initialize(this);
        applyModeCustomizations();
    }

    private void addStyles() {
        importGlusterConfigurationEditor.addContentWidgetStyleName(style.editorContentWidget());
        migrateOnErrorOption_NOEditor.addContentWidgetStyleName(style.label());
        migrateOnErrorOption_YESEditor.addContentWidgetStyleName(style.label());
        migrateOnErrorOption_HA_ONLYEditor.addContentWidgetStyleName(style.label());
    }

    private void localize(ApplicationConstants constants) {
        generalTab.setLabel(constants.clusterPopupGeneralTabLabel());

        dataCenterEditor.setLabel(constants.clusterPopupDataCenterLabel());
        nameEditor.setLabel(constants.clusterPopupNameLabel());
        descriptionEditor.setLabel(constants.clusterPopupDescriptionLabel());
        cPUEditor.setLabel(constants.clusterPopupCPULabel());
        versionEditor.setLabel(constants.clusterPopupVersionLabel());
        enableOvirtServiceEditor.setLabel(constants.clusterEnableOvirtServiceLabel());
        enableGlusterServiceEditor.setLabel(constants.clusterEnableGlusterServiceLabel());

        importGlusterConfigurationEditor.setLabel(constants.clusterImportGlusterConfigurationLabel());
        importGlusterExplanationLabel.setText(constants.clusterImportGlusterConfigurationExplanationLabel());
        glusterHostAddressEditor.setLabel(constants.hostPopupHostAddressLabel());
        glusterHostFingerprintEditor.setLabel(constants.hostPopupHostFingerprintLabel());
        glusterHostPasswordEditor.setLabel(constants.hostPopupRootPasswordLabel());

        memoryOptimizationTab.setLabel(constants.clusterPopupMemoryOptimizationTabLabel());

        optimizationNoneEditor.setLabel(constants.clusterPopupOptimizationNoneLabel());
        optimizationForServerEditor.setLabel(constants.clusterPopupOptimizationForServerLabel());
        optimizationForDesktopEditor.setLabel(constants.clusterPopupOptimizationForDesktopLabel());
        optimizationCustomEditor.setLabel(constants.clusterPopupOptimizationCustomLabel());

        optimizationNoneExplanationLabel.setText(constants.clusterPopupOptimizationNoneExplainationLabel());

        resiliencePolicyTab.setLabel(constants.clusterPopupResiliencePolicyTabLabel());

        migrateOnErrorOption_YESEditor.setLabel(constants.clusterPopupMigrateOnError_YesLabel());
        migrateOnErrorOption_HA_ONLYEditor.setLabel(constants.clusterPopupMigrateOnError_HaLabel());
        migrateOnErrorOption_NOEditor.setLabel(constants.clusterPopupMigrateOnError_NoLabel());

    }

    private void initRadioButtonEditors() {
        optimizationNoneEditor = new EntityModelRadioButtonEditor("1"); //$NON-NLS-1$
        optimizationForServerEditor = new EntityModelRadioButtonEditor("1"); //$NON-NLS-1$
        optimizationForDesktopEditor = new EntityModelRadioButtonEditor("1"); //$NON-NLS-1$
        optimizationCustomEditor = new EntityModelRadioButtonEditor("1"); //$NON-NLS-1$

        migrateOnErrorOption_YESEditor = new EntityModelRadioButtonEditor("2"); //$NON-NLS-1$
        migrateOnErrorOption_HA_ONLYEditor = new EntityModelRadioButtonEditor("2"); //$NON-NLS-1$
        migrateOnErrorOption_NOEditor = new EntityModelRadioButtonEditor("2"); //$NON-NLS-1$

        optimizationCustomExplanationLabel = new Label();
        optimizationCustomExplanationLabel.setVisible(false);
    }

    private void initListBoxEditors() {
        dataCenterEditor = new ListModelListBoxEditor<Object>(new NullSafeRenderer<Object>() {
            @Override
            public String renderNullSafe(Object object) {
                return ((storage_pool) object).getname();
            }
        });

        cPUEditor = new ListModelListBoxEditor<Object>(new NullSafeRenderer<Object>() {
            @Override
            public String renderNullSafe(Object object) {
                return ((ServerCpu) object).getCpuName();
            }
        });

        versionEditor = new ListModelListBoxEditor<Object>(new NullSafeRenderer<Object>() {
            @Override
            public String renderNullSafe(Object object) {
                return ((Version) object).toString();
            }
        });

    }

    private void initCheckBoxEditors()
    {
        importGlusterConfigurationEditor = new EntityModelCheckBoxEditor(Align.RIGHT);
    }

    private void applyModeCustomizations() {
        if (ApplicationModeHelper.getUiMode() == ApplicationMode.GlusterOnly)
        {
            memoryOptimizationTab.setVisible(false);
            resiliencePolicyTab.setVisible(false);
            dataCenterPanel.addStyleName(style.generalTabTopDecoratorEmpty());
        }
    }

    @Override
    public void focusInput() {
        nameEditor.setFocus(true);
    }

    @Override
    public void edit(final ClusterModel object) {
        Driver.driver.edit(object);

        object.getOptimizationForServer().getEntityChangedEvent().addListener(new IEventListener() {

            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                optimizationForServerExplanationLabel.setText(messages.clusterPopupOptimizationForServerExplainationLabel( object.getOptimizationForServer().getEntity().toString() + "%")); //$NON-NLS-1$
            }
        });
        object.getOptimizationForDesktop().getEntityChangedEvent().addListener(new IEventListener() {

            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                optimizationForDesktopExplanationLabel.setText(messages.clusterPopupOptimizationForDesktopExplainationLabel(object.getOptimizationForDesktop().getEntity().toString() + "%")); //$NON-NLS-1$
            }
        });
        object.getOptimizationCustom_IsSelected().getEntityChangedEvent().addListener(new IEventListener() {

            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                if ((Boolean) object.getOptimizationCustom_IsSelected().getEntity()) {
                    optimizationCustomExplanationLabel.setText(messages.clusterPopupOptimizationCustomExplainationLabel(object.getOptimizationCustom().getEntity().toString() + "%")); //$NON-NLS-1$
                    optimizationCustomExplanationLabel.setVisible(true);
                }
            }
        });
        object.getDataCenter().getSelectedItemChangedEvent().addListener(new IEventListener() {

            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                resiliencePolicyTab.setVisible(object.getisResiliencePolicyTabAvailable());
                applyModeCustomizations();
            }
        });
        object.getEnableGlusterService().getEntityChangedEvent().addListener(new IEventListener() {

            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                importGlusterExplanationLabel.setVisible((Boolean) object.getEnableGlusterService().getEntity()
                        && object.getIsNew());
            }
        });
        importGlusterExplanationLabel.setVisible((Boolean) object.getEnableGlusterService().getEntity()
                && object.getIsNew());
    }

    @Override
    public void setMessage(String message) {
        super.setMessage(message);
        messageLabel.setText(message);
    }

    @Override
    public ClusterModel flush() {
        return Driver.driver.flush();
    }

    interface WidgetStyle extends CssResource {
        String label();

        String generalTabTopDecoratorEmpty();

        String editorContentWidget();
    }

}
