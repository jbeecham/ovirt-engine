package org.ovirt.engine.core.vdsbroker.gluster;

import org.ovirt.engine.core.common.vdscommands.VdsIdVDSCommandParametersBase;
import org.ovirt.engine.core.vdsbroker.irsbroker.GlusterServersListReturnForXmlRpc;
import org.ovirt.engine.core.vdsbroker.vdsbroker.StatusForXmlRpc;

public class GlusterServersListVDSCommand<P extends VdsIdVDSCommandParametersBase> extends AbstractGlusterBrokerCommand<P> {

    private GlusterServersListReturnForXmlRpc glusterServers;

    public GlusterServersListVDSCommand(P parameters) {
        super(parameters);
    }

    @Override
    protected void ExecuteVdsBrokerCommand() {
        glusterServers = getBroker().glusterServersList();

        ProceedProxyReturnValue();
        if (getVDSReturnValue().getSucceeded()) {
            setReturnValue(glusterServers.getServers());
        }


    }

    @Override
    protected StatusForXmlRpc getReturnStatus() {
        return glusterServers.mStatus;
    }

}
