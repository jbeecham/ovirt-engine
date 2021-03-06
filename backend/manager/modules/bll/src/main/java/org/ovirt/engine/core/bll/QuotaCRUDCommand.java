package org.ovirt.engine.core.bll;

import java.util.List;

import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.bll.quota.Quotable;
import org.ovirt.engine.core.common.action.QuotaCRUDParameters;
import org.ovirt.engine.core.common.businessentities.Quota;
import org.ovirt.engine.core.common.businessentities.QuotaStorage;
import org.ovirt.engine.core.common.businessentities.QuotaVdsGroup;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.VdcBllMessages;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.CustomLogField;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.CustomLogFields;
import org.ovirt.engine.core.dao.QuotaDAO;

@CustomLogFields({ @CustomLogField("QuotaName") })
public abstract class QuotaCRUDCommand extends CommandBase<QuotaCRUDParameters> implements Quotable {

    private Quota quota;

    private static final long serialVersionUID = 8037321564997412357L;

    public QuotaCRUDCommand(QuotaCRUDParameters parameters) {
        super(parameters);
    }

    public Quota getQuota() {
        if (quota == null) {
            setQuota(getQuotaDAO().getById(getParameters().getQuotaId()));
        }
        return quota;
    }

    public void setQuota(Quota quota) {
        this.quota = quota;
    }

    protected boolean checkQuotaValidationCommon(Quota quota, List<String> messages) {
        if (quota == null) {
            messages.add(VdcBllMessages.ACTION_TYPE_FAILED_QUOTA_IS_NOT_VALID.toString());
            return false;
        }

        // Check if quota name exists or
        // If specific Quota for storage is specified or
        // If specific Quota for cluster is specific
        if (!checkQuotaNameExisting(quota, messages) ||
                !validateQuotaStorageLimitation(quota, messages) ||
                !validateQuotaVdsGroupLimitation(quota, messages)) {
            return false;
        }

        return true;
    }

    public boolean checkQuotaNameExisting(Quota quota, List<String> messages) {
        Quota quotaByName = getQuotaDAO().getQuotaByQuotaName(quota.getQuotaName());

        // Check if there is no quota with the same name that already exists.
        if ((quotaByName != null) && (!quotaByName.getId().equals(quota.getId()))) {
            messages.add(VdcBllMessages.ACTION_TYPE_FAILED_QUOTA_NAME_ALREADY_EXISTS.toString());
            return false;
        }
        return true;
    }

    /**
     * Validate Quota storage restrictions.
     *
     * @param quota
     * @param messages
     * @return
     */
    private static boolean validateQuotaStorageLimitation(Quota quota, List<String> messages) {
        boolean isValid = true;
        List<QuotaStorage> quotaStorageList = quota.getQuotaStorages();
        if (quota.isGlobalStorageQuota() && (quotaStorageList != null && !quotaStorageList.isEmpty())) {
            messages.add(VdcBllMessages.ACTION_TYPE_FAILED_QUOTA_LIMIT_IS_SPECIFIC_AND_GENERAL.toString());
            isValid = false;
        }
        return isValid;
    }

    /**
     * Validate Quota vds group restrictions.
     *
     * @param quota
     *            - Quota we validate
     * @param messages
     *            - Messages of can do action.
     * @return Boolean value if the quota is valid or not.
     */
    private static boolean validateQuotaVdsGroupLimitation(Quota quota, List<String> messages) {
        boolean isValid = true;
        List<QuotaVdsGroup> quotaVdsGroupList = quota.getQuotaVdsGroups();
        if (quotaVdsGroupList != null && !quotaVdsGroupList.isEmpty()) {
            boolean isSpecificVirtualCpu = false;
            boolean isSpecificVirtualRam = false;

            for (QuotaVdsGroup quotaVdsGroup : quotaVdsGroupList) {
                isSpecificVirtualCpu = quotaVdsGroup.getVirtualCpu() != null;
                isSpecificVirtualRam = quotaVdsGroup.getMemSizeMB() != null;
            }

            // if the global vds group limit was not specified, then specific limitation must be specified.
            if (quota.isGlobalVdsGroupQuota() && (isSpecificVirtualRam || isSpecificVirtualCpu)) {
                messages.add(VdcBllMessages.ACTION_TYPE_FAILED_QUOTA_LIMIT_IS_SPECIFIC_AND_GENERAL.toString());
                isValid = false;
            }
        }
        return isValid;
    }

    @Override
    public boolean validateAndSetQuota() {
        return true;
    }

    @Override
    public void rollbackQuota() {
    }

    @Override
    public Guid getQuotaId() {
        return getQuota().getId();
    }

    @Override
    public void addQuotaPermissionSubject(List<PermissionSubject> quotaPermissionList) {
    }

    protected QuotaDAO getQuotaDAO() {
        return getDbFacade().getQuotaDao();
    }

    public String getQuotaName() {
        return quota.getQuotaName();
    }

}
