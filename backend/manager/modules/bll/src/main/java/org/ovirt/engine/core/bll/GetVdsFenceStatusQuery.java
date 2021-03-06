package org.ovirt.engine.core.bll;

import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.businessentities.FenceActionType;
import org.ovirt.engine.core.common.businessentities.FenceStatusReturnValue;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.queries.VdsIdParametersBase;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AlertDirector;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogDirector;

public class GetVdsFenceStatusQuery<P extends VdsIdParametersBase> extends FencingQueryBase<P> {

    public GetVdsFenceStatusQuery(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeQueryCommand() {
        String msg = "";
        VDS vds = DbFacade.getInstance().getVdsDao().get(getParameters().getVdsId());
        setVdsId(vds.getId());
        setVdsName(vds.getvds_name());
        FencingExecutor executor = new FencingExecutor(vds, FenceActionType.Status);
        VDSReturnValue returnValue = null;
        if (executor.FindVdsToFence()) {
            returnValue = executor.Fence();
            if (returnValue.getReturnValue() != null) {
                if (returnValue.getSucceeded()) {
                    // Remove all alerts including NOT CONFIG alert
                    AlertDirector.RemoveAllVdsAlerts(getVdsId(), true);
                    getQueryReturnValue().setReturnValue(returnValue.getReturnValue());
                } else {
                    msg = ((FenceStatusReturnValue) returnValue.getReturnValue()).getMessage();
                    getQueryReturnValue().setReturnValue(new FenceStatusReturnValue("unknown", msg));
                    AlertPowerManagementStatusFailed(msg);
                }
            }
        } else {
            msg = String.format(
                    "Failed to run Power Management command on Host %1$s, no running proxy Host was found.",
                    vds.getvds_name());
            getQueryReturnValue().setReturnValue(new FenceStatusReturnValue("unknown", msg));
            AlertPowerManagementStatusFailed(AuditLogDirector.GetMessage(AuditLogType.VDS_ALERT_FENCING_NO_PROXY_HOST));
        }

    }
}
