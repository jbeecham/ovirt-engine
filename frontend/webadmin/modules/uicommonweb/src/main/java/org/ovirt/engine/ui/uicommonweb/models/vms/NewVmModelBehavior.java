package org.ovirt.engine.ui.uicommonweb.models.vms;

import java.util.ArrayList;

import org.ovirt.engine.core.common.businessentities.DisplayType;
import org.ovirt.engine.core.common.businessentities.QuotaEnforcementTypeEnum;
import org.ovirt.engine.core.common.businessentities.StoragePoolStatus;
import org.ovirt.engine.core.common.businessentities.StorageType;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.businessentities.VmType;
import org.ovirt.engine.core.common.businessentities.storage_domains;
import org.ovirt.engine.core.common.businessentities.storage_pool;
import org.ovirt.engine.core.compat.NGuid;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.SystemTreeItemModel;
import org.ovirt.engine.ui.uicommonweb.models.SystemTreeItemType;

public class NewVmModelBehavior extends VmModelBehaviorBase
{
    @Override
    public void Initialize(SystemTreeItemModel systemTreeSelectedItem)
    {
        super.Initialize(systemTreeSelectedItem);
        AsyncDataProvider.GetDataCenterList(new AsyncQuery(getModel(),
                new INewAsyncCallback() {
                    @Override
                    public void OnSuccess(Object target, Object returnValue) {

                        UnitVmModel model = (UnitVmModel) target;
                        ArrayList<storage_pool> list = new ArrayList<storage_pool>();
                        for (storage_pool a : (ArrayList<storage_pool>) returnValue)
                        {
                            if (a.getstatus() == StoragePoolStatus.Up)
                            {
                                list.add(a);
                            }
                        }
                        model.SetDataCenter(model, list);

                    }
                }, getModel().getHash()));
    }

    @Override
    public void DataCenter_SelectedItemChanged()
    {
        storage_pool dataCenter = (storage_pool) getModel().getDataCenter().getSelectedItem();

        getModel().setIsHostAvailable(dataCenter.getstorage_pool_type() != StorageType.LOCALFS);

        AsyncDataProvider.GetClusterList(new AsyncQuery(new Object[] { this, getModel() },
                new INewAsyncCallback() {
                    @Override
                    public void OnSuccess(Object target, Object returnValue) {

                        Object[] array = (Object[]) target;
                        NewVmModelBehavior behavior = (NewVmModelBehavior) array[0];
                        UnitVmModel model = (UnitVmModel) array[1];
                        ArrayList<VDSGroup> clusters = (ArrayList<VDSGroup>) returnValue;
                        model.SetClusters(model, clusters, null);
                        behavior.InitTemplate();
                        behavior.InitCdImage();

                    }
                }, getModel().getHash()), dataCenter.getId());
        if (dataCenter.getQuotaEnforcementType() != QuotaEnforcementTypeEnum.DISABLED) {
            getModel().getQuota().setIsAvailable(true);
        } else {
            getModel().getQuota().setIsAvailable(false);
        }
    }

    @Override
    public void Template_SelectedItemChanged()
    {
        VmTemplate template = (VmTemplate) getModel().getTemplate().getSelectedItem();

        if (template != null)
        {
            // Copy VM parameters from template.
            getModel().getOSType().setSelectedItem(template.getos());
            getModel().getTotalCPUCores().setEntity(Integer.toString(template.getnum_of_cpus()));
            getModel().getNumOfSockets().setSelectedItem(template.getnum_of_sockets());
            getModel().getNumOfMonitors().setSelectedItem(template.getnum_of_monitors());
            getModel().getDomain().setSelectedItem(template.getdomain());
            getModel().getMemSize().setEntity(template.getmem_size_mb());
            getModel().getUsbPolicy().setSelectedItem(template.getusb_policy());
            getModel().setBootSequence(template.getdefault_boot_sequence());
            getModel().getIsHighlyAvailable().setEntity(template.getauto_startup());

            updateHostPinning(template.getMigrationSupport());
            doChangeDefautlHost(template.getdedicated_vm_for_vds());

            if (getModel().getVmType() == VmType.Desktop) {
                getModel().getIsStateless().setEntity(template.getis_stateless());
                getModel().getAllowConsoleReconnect().setEntity(template.getAllowConsoleReconnect());
            }

            boolean hasCd = !StringHelper.isNullOrEmpty(template.getiso_path());

            getModel().getCdImage().setIsChangable(hasCd);
            getModel().getCdAttached().setEntity(hasCd);
            if (hasCd) {
                getModel().getCdImage().setSelectedItem(template.getiso_path());
            }

            if (!StringHelper.isNullOrEmpty(template.gettime_zone()))
            {
                updateTimeZone(template.gettime_zone());
            }
            else
            {
                UpdateDefaultTimeZone();
            }

            // Update domain list
            UpdateDomain();

            ArrayList<VDSGroup> clusters = (ArrayList<VDSGroup>) getModel().getCluster().getItems();
            VDSGroup selectCluster =
                    Linq.FirstOrDefault(clusters, new Linq.ClusterPredicate(template.getvds_group_id()));

            getModel().getCluster().setSelectedItem((selectCluster != null) ? selectCluster
                    : Linq.FirstOrDefault(clusters));

            // Update display protocol selected item
            EntityModel displayProtocol = null;
            boolean isFirst = true;
            for (Object item : getModel().getDisplayProtocol().getItems())
            {
                EntityModel a = (EntityModel) item;
                if (isFirst)
                {
                    displayProtocol = a;
                    isFirst = false;
                }
                DisplayType dt = (DisplayType) a.getEntity();
                if (dt == template.getdefault_display_type())
                {
                    displayProtocol = a;
                    break;
                }
            }
            getModel().getDisplayProtocol().setSelectedItem(displayProtocol);

            // By default, take kernel params from template.
            getModel().getKernel_path().setEntity(template.getkernel_url());
            getModel().getKernel_parameters().setEntity(template.getkernel_params());
            getModel().getInitrd_path().setEntity(template.getinitrd_url());

            if (!template.getId().equals(NGuid.Empty))
            {
                getModel().getStorageDomain().setIsChangable(true);
                getModel().getProvisioning().setIsChangable(true);

                getModel().setIsBlankTemplate(false);
                InitDisks();
            }
            else
            {
                getModel().getStorageDomain().setIsChangable(false);
                getModel().getProvisioning().setIsChangable(false);

                getModel().setIsBlankTemplate(true);
                getModel().setIsDisksAvailable(false);
                getModel().setDisks(null);
            }

            InitPriority(template.getpriority());
            InitStorageDomains();
            UpdateMinAllocatedMemory();
        }
    }

    @Override
    public void Cluster_SelectedItemChanged()
    {
        UpdateDefaultHost();
        UpdateCustomProperties();
        UpdateMinAllocatedMemory();
        UpdateNumOfSockets();
        updateQuotaByCluster(null, "");
        updateCpuPinningVisibility();
    }

    @Override
    protected void UpdateCustomProperties() {
        super.UpdateCustomProperties();

        updateCustomPropertySheet();
    }

    @Override
    public void DefaultHost_SelectedItemChanged()
    {
        UpdateCdImage();
    }

    @Override
    public void Provisioning_SelectedItemChanged()
    {
        boolean provisioning = (Boolean) getModel().getProvisioning().getEntity();
        getModel().getProvisioningThin_IsSelected().setEntity(!provisioning);
        getModel().getProvisioningClone_IsSelected().setEntity(provisioning);
        getModel().getDisksAllocationModel().setIsVolumeFormatAvailable(true);
        getModel().getDisksAllocationModel().setIsVolumeFormatChangable(provisioning);
        getModel().getDisksAllocationModel().setIsAliasChangable(true);

        InitStorageDomains();
    }

    @Override
    public void UpdateMinAllocatedMemory()
    {
        VDSGroup cluster = (VDSGroup) getModel().getCluster().getSelectedItem();
        if (cluster == null)
        {
            return;
        }

        double overCommitFactor = 100.0 / cluster.getmax_vds_memory_over_commit();
        getModel().getMinAllocatedMemory()
                .setEntity((int) ((Integer) getModel().getMemSize().getEntity() * overCommitFactor));
    }

    private void InitTemplate()
    {
        storage_pool dataCenter = (storage_pool) getModel().getDataCenter().getSelectedItem();

        // Filter according to system tree selection.
        if (getSystemTreeSelectedItem() != null && getSystemTreeSelectedItem().getType() == SystemTreeItemType.Storage)
        {
            storage_domains storage = (storage_domains) getSystemTreeSelectedItem().getEntity();

            AsyncDataProvider.GetTemplateListByDataCenter(new AsyncQuery(new Object[] { this, storage },
                    new INewAsyncCallback() {
                        @Override
                        public void OnSuccess(Object target1, Object returnValue1) {

                            Object[] array1 = (Object[]) target1;
                            NewVmModelBehavior behavior1 = (NewVmModelBehavior) array1[0];
                            storage_domains storage1 = (storage_domains) array1[1];
                            AsyncDataProvider.GetTemplateListByStorage(new AsyncQuery(new Object[] { behavior1,
                                    returnValue1 },
                                    new INewAsyncCallback() {
                                        @Override
                                        public void OnSuccess(Object target2, Object returnValue2) {

                                            Object[] array2 = (Object[]) target2;
                                            NewVmModelBehavior behavior2 = (NewVmModelBehavior) array2[0];
                                            ArrayList<VmTemplate> templatesByDataCenter =
                                                    (ArrayList<VmTemplate>) array2[1];
                                            ArrayList<VmTemplate> templatesByStorage =
                                                    (ArrayList<VmTemplate>) returnValue2;
                                            VmTemplate blankTemplate =
                                                    Linq.FirstOrDefault(templatesByDataCenter,
                                                            new Linq.TemplatePredicate(NGuid.Empty));
                                            if (blankTemplate != null)
                                            {
                                                templatesByStorage.add(0, blankTemplate);
                                            }
                                            behavior2.PostInitTemplate((ArrayList<VmTemplate>) returnValue2);

                                        }
                                    }),
                                    storage1.getId());

                        }
                    }, getModel().getHash()),
                    dataCenter.getId());
        }
        else
        {
            AsyncDataProvider.GetTemplateListByDataCenter(new AsyncQuery(this,
                    new INewAsyncCallback() {
                        @Override
                        public void OnSuccess(Object target, Object returnValue) {

                            NewVmModelBehavior behavior = (NewVmModelBehavior) target;
                            behavior.PostInitTemplate((ArrayList<VmTemplate>) returnValue);

                        }
                    }, getModel().getHash()), dataCenter.getId());
        }
    }

    private void PostInitTemplate(ArrayList<VmTemplate> templates)
    {
        // If there was some template selected before, try select it again.
        VmTemplate oldTemplate = (VmTemplate) getModel().getTemplate().getSelectedItem();

        getModel().getTemplate().setItems(templates);

        getModel().getTemplate().setSelectedItem(Linq.FirstOrDefault(templates,
                oldTemplate != null ? new Linq.TemplatePredicate(oldTemplate.getId())
                        : new Linq.TemplatePredicate(NGuid.Empty)));

        UpdateIsDisksAvailable();
    }

    public void InitCdImage()
    {
        updateUserCdImage(((storage_pool) getModel().getDataCenter().getSelectedItem()).getId());
    }

    @Override
    public void UpdateIsDisksAvailable()
    {
        getModel().setIsDisksAvailable(getModel().getDisks() != null
                && getModel().getProvisioning().getIsChangable());
    }

}
