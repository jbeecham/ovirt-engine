package org.ovirt.engine.ui.webadmin.section.main.presenter.popup.vm;

import org.ovirt.engine.ui.common.presenter.AbstractModelBoundPopupPresenterWidget;
import org.ovirt.engine.ui.uicommonweb.models.vms.UnitVmModel;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;

public class VmDesktopNewPopupPresenterWidget extends AbstractModelBoundPopupPresenterWidget<UnitVmModel, VmDesktopNewPopupPresenterWidget.ViewDef> {
    public interface ViewDef extends AbstractModelBoundPopupPresenterWidget.ViewDef<UnitVmModel> {

    }

    @Inject
    public VmDesktopNewPopupPresenterWidget(EventBus eventBus, ViewDef view) {
        super(eventBus, view);
    }
}
