package org.ovirt.engine.core.vdsbroker.vdsbroker;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.common.businessentities.Network;
import org.ovirt.engine.core.common.errors.VdcBllErrors;
import org.ovirt.engine.core.common.vdscommands.NetworkVdsmVDSCommandParameters;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;
import org.ovirt.engine.core.utils.transaction.RollbackHandler;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;

public class AddNetworkVDSCommand<P extends NetworkVdsmVDSCommandParameters> extends VdsBrokerCommand<P> implements RollbackHandler {
    public AddNetworkVDSCommand(P parameters) {
        super(parameters);
    }

    @Override
    protected void ExecuteVdsBrokerCommand() {
        if (TransactionSupport.current() != null) {
            TransactionSupport.registerRollbackHandler(this);
        }
        String networkName = (getParameters().getNetworkName() == null) ? "" : getParameters()
                .getNetworkName();
        String vlanId = (getParameters().getVlanId() != null) ? getParameters().getVlanId().toString()
                : "";
        String bond = (getParameters().getBondName() == null) ? "" : getParameters().getBondName();
        String[] nics = (getParameters().getNics() == null) ? new String[] {} : getParameters().getNics();
        Map<String, String> options = new HashMap<String, String>();

        switch (getParameters().getBootProtocol()) {
        case Dhcp:
            options.put(VdsProperties.bootproto, VdsProperties.dhcp);
            break;
        case StaticIp:
            if (!StringUtils.isEmpty(getParameters().getInetAddr())) {
                options.put(VdsProperties.ipaddr, getParameters().getInetAddr());
            }
            if (!StringUtils.isEmpty(getParameters().getNetworkMask())) {
                options.put(VdsProperties.netmask, getParameters().getNetworkMask());
            }
            if (!StringUtils.isEmpty(getParameters().getGateway())) {
                options.put(VdsProperties.gateway, getParameters().getGateway());
            }
            break;
        }

        options.put(VdsProperties.stp, (getParameters().getStp()) ? "yes" : "no");

        if (!StringUtils.isEmpty(getParameters().getBondingOptions())) {
            options.put(VdsProperties.bonding_opts, getParameters().getBondingOptions());
        }

        options.put(VdsProperties.stp, (getParameters().getStp()) ? "yes" : "no");
        // options[VdsProperties.force] = "true";

        options.put("bridged", Boolean.toString(getParameters().isVmNetwork()));

        Network network = getParameters().getNetwork();
        if (network != null && network.getMtu() != 0) {
            options.put("mtu", String.valueOf(network.getMtu()));
        }

        status = getBroker().addNetwork(networkName, vlanId, bond, nics, options);
        ProceedProxyReturnValue();
    }

    @Override
    public void rollback() {
        try {
            // We check for "Done" status because we want to be sure that we made the net change, or in case of empty
            // response (which means the call to VDSM failed on timeout).
            // 1. If we failed VDSM revert the change so we don't need to do anything.
            // 2. If we are in transaction first command was AddNetworkCommand (end successfully), second command fails,
            // we want to revert the network change (that is why we check for Done).
            // 3. If the call to VDSM timeout out we assume it had succeeded and try to remove the network.
            // 3.1. If the timeout was a failure to call the VDSM in the first place, then probably the call to delete
            // the network will timeout also.
            if (getReturnValueFromBroker() == null ||
                    VdcBllErrors.Done == GetReturnValueFromStatus(getReturnStatus())) {
                String network = (getParameters().getNetworkName() == null) ? "" : getParameters()
                        .getNetworkName();
                String vlanId = (getParameters().getVlanId() != null) ? getParameters().getVlanId()
                        .toString() : "";
                String bond = (getParameters().getBondName() == null) ? "" : getParameters()
                        .getBondName();
                String[] nics = (getParameters().getNics() == null) ? new String[] {} : getParameters()
                        .getNics();
                status = getBroker().delNetwork(network, vlanId, bond, nics);
            }
        } catch (RuntimeException ex) {
            log.error("Exception in Rollback ExecuteVdsBrokerCommand", ex);
        }
    }

    private static final Log log = LogFactory.getLog(AddNetworkVDSCommand.class);
}
