package org.ovirt.engine.ui.userportal.section.main.presenter.tab.extended;

import java.util.List;

import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.ui.common.widget.HasEditorDriver;
import org.ovirt.engine.ui.uicommonweb.models.resources.ResourcesModel;
import org.ovirt.engine.ui.userportal.gin.ClientGinjector;
import org.ovirt.engine.ui.userportal.place.ApplicationPlaces;
import org.ovirt.engine.ui.userportal.section.main.presenter.AbstractModelActivationPresenter;
import org.ovirt.engine.ui.userportal.section.main.presenter.tab.MainTabExtendedPresenter;
import org.ovirt.engine.ui.userportal.uicommon.model.UserPortalDataBoundModelProvider.DataChangeListener;
import org.ovirt.engine.ui.userportal.uicommon.model.resources.ResourcesModelProvider;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.TabData;
import com.gwtplatform.mvp.client.TabDataBasic;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.annotations.TabInfo;
import com.gwtplatform.mvp.client.proxy.RevealContentEvent;
import com.gwtplatform.mvp.client.proxy.TabContentProxyPlace;

public class SideTabExtendedResourcePresenter extends AbstractModelActivationPresenter<VM, ResourcesModel, SideTabExtendedResourcePresenter.ViewDef, SideTabExtendedResourcePresenter.ProxyDef> implements DataChangeListener<VM> {

    private final ResourcesModelProvider modelProvider;

    @ProxyCodeSplit
    @NameToken(ApplicationPlaces.extendedResourceSideTabPlace)
    public interface ProxyDef extends TabContentProxyPlace<SideTabExtendedResourcePresenter> {
    }

    public interface ViewDef extends View, HasEditorDriver<ResourcesModel> {
    }

    @TabInfo(container = MainTabExtendedPresenter.class)
    static TabData getTabData(ClientGinjector ginjector) {
        return new TabDataBasic(ginjector.getApplicationConstants().extendedResourceSideTabLabel(), 2);
    }

    @Inject
    public SideTabExtendedResourcePresenter(EventBus eventBus,
            ViewDef view,
            ProxyDef proxy,
            final ResourcesModelProvider modelProvider) {
        super(eventBus, view, proxy, modelProvider);
        this.modelProvider = modelProvider;
        modelProvider.setDataChangeListener(this);
    }

    @Override
    protected void revealInParent() {
        RevealContentEvent.fire(this, MainTabExtendedPresenter.TYPE_SetTabContent, this);
    }

    @Override
    public void onDataChange(List<VM> items) {
        getView().edit(modelProvider.getModel());
    }

}
