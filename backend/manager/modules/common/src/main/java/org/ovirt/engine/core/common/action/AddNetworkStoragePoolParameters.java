package org.ovirt.engine.core.common.action;

import javax.validation.Valid;

import org.ovirt.engine.core.common.businessentities.Network;
import org.ovirt.engine.core.compat.Guid;

public class AddNetworkStoragePoolParameters extends StoragePoolParametersBase {
    private static final long serialVersionUID = -7392121807419409051L;

    @Valid
    private Network _network;

    public AddNetworkStoragePoolParameters(Guid storagePoolId, Network net) {
        super(storagePoolId);
        _network = net;
    }

    public Network getNetwork() {
        return _network;
    }

    public AddNetworkStoragePoolParameters() {
    }
}
