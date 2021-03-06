package org.ovirt.engine.ui.common.widget;

import org.ovirt.engine.core.compat.Event;
import org.ovirt.engine.core.compat.EventArgs;
import org.ovirt.engine.core.compat.IEventListener;
import org.ovirt.engine.ui.uicommonweb.UICommand;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.ButtonBase;
import com.google.gwt.user.client.ui.Composite;

/**
 * Abstract button widget bound to UiCommon {@linkplain UICommand command}.
 */
public abstract class AbstractUiCommandButton extends Composite implements HasUiCommandClickHandlers, HasLabel {

    private UICommand command;

    @Override
    public HandlerRegistration addClickHandler(ClickHandler handler) {
        return getButtonWidget().addClickHandler(handler);
    }

    @Override
    public UICommand getCommand() {
        return command;
    }

    @Override
    public void setCommand(UICommand command) {
        this.command = command;

        command.getPropertyChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                updateButton();
            }
        });

        updateButton();
    }

    @Override
    public String getLabel() {
        return getButtonWidget().getText();
    }

    @Override
    public void setLabel(String label) {
        getButtonWidget().setText(label);
    }

    void updateButton() {
        getButtonWidget().setVisible(command.getIsAvailable() && command.getIsVisible());
        getButtonWidget().setEnabled(command.getIsExecutionAllowed());
        getButtonWidget().setTitle(command.getTitle());
    }

    protected abstract ButtonBase getButtonWidget();

}
