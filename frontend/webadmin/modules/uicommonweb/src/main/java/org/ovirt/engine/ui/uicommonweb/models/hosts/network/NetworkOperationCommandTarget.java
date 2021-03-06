package org.ovirt.engine.ui.uicommonweb.models.hosts.network;

import java.util.List;

import org.ovirt.engine.core.common.businessentities.VdsNetworkInterface;
import org.ovirt.engine.ui.uicommonweb.ICommandTarget;
import org.ovirt.engine.ui.uicommonweb.UICommand;

/**
 * An {@link ICommandTarget} for Network Commands
 */
public abstract class NetworkOperationCommandTarget implements ICommandTarget {

    @Override
    public void ExecuteCommand(UICommand uiCommand) {
        ExecuteCommand(uiCommand, new Object[0]);
    }

    @Override
    public void ExecuteCommand(UICommand uiCommand, Object... params) {
        NetworkCommand command = (NetworkCommand) uiCommand;
        NetworkItemModel<?> op1 = command.getOp1();
        NetworkItemModel<?> op2 = command.getOp2();
        List<VdsNetworkInterface> allNics = command.getAllNics();
        ExecuteNetworkCommand(op1, op2, allNics, params);

    }

    protected abstract void ExecuteNetworkCommand(NetworkItemModel<?> op1,
            NetworkItemModel<?> op2,
            List<VdsNetworkInterface> allNics, Object... params);
}
