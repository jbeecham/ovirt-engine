package org.ovirt.engine.ui.webadmin.section.main.presenter.popup.vm;

import org.ovirt.engine.ui.common.presenter.AbstractModelBoundPopupPresenterWidget;
import org.ovirt.engine.ui.common.uicommon.model.DeferredModelCommandInvoker;
import org.ovirt.engine.ui.uicommonweb.models.vms.DiskModel;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;

public class VmDiskPopupPresenterWidget extends AbstractModelBoundPopupPresenterWidget<DiskModel, VmDiskPopupPresenterWidget.ViewDef> {

    public interface ViewDef extends AbstractModelBoundPopupPresenterWidget.ViewDef<DiskModel> {
        boolean handleEnterKeyDisabled();
    }

    @Inject
    public VmDiskPopupPresenterWidget(EventBus eventBus, ViewDef view) {
        super(eventBus, view);
    }

    @Override
    protected void handleEnterKey(DeferredModelCommandInvoker commandInvoker) {
        if (!getView().handleEnterKeyDisabled()) {
            super.handleEnterKey(commandInvoker);
        }
    }
}
