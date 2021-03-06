package org.ovirt.engine.core.bll.storage;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.ovirt.engine.core.bll.CommandAssertUtils.checkMessages;
import static org.ovirt.engine.core.bll.CommandAssertUtils.checkSucceeded;

import java.util.ArrayList;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.ovirt.engine.core.bll.interfaces.BackendInternal;
import org.ovirt.engine.core.common.action.DetachStorageDomainFromPoolParameters;
import org.ovirt.engine.core.common.action.RemoveStorageDomainParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.businessentities.StorageDomainSharedStatus;
import org.ovirt.engine.core.common.businessentities.StorageDomainType;
import org.ovirt.engine.core.common.businessentities.StoragePoolIsoMapId;
import org.ovirt.engine.core.common.businessentities.StorageType;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.storage_domain_static;
import org.ovirt.engine.core.common.businessentities.storage_domains;
import org.ovirt.engine.core.common.businessentities.storage_pool;
import org.ovirt.engine.core.common.businessentities.storage_pool_iso_map;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.interfaces.VDSBrokerFrontend;
import org.ovirt.engine.core.common.vdscommands.FormatStorageDomainVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.RemoveVGVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.core.dal.VdcBllMessages;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dao.BusinessEntitySnapshotDAO;
import org.ovirt.engine.core.dao.StorageDomainDAO;
import org.ovirt.engine.core.dao.StorageDomainDynamicDAO;
import org.ovirt.engine.core.dao.StorageDomainStaticDAO;
import org.ovirt.engine.core.dao.StoragePoolDAO;
import org.ovirt.engine.core.dao.StoragePoolIsoMapDAO;
import org.ovirt.engine.core.dao.VdsDAO;
import org.ovirt.engine.core.utils.MockConfigRule;
import org.ovirt.engine.core.utils.transaction.TransactionMethod;

@RunWith(MockitoJUnitRunner.class)
public class RemoveStorageDomainTest {
    private static final Version VDS_COMPATIBILITY_VERSION = new Version(2, 2);

    @Rule
    public static MockConfigRule mcr =
            new MockConfigRule(
                    MockConfigRule.mockConfig(ConfigValues.IsNeedSupportForOldVgAPI,
                            VDS_COMPATIBILITY_VERSION.getValue(),
                            true)
            );

    private final Guid STORAGE_DOMAIN_ID = Guid.NewGuid();
    private final Guid STORAGE_POOL_ID = Guid.NewGuid();
    private final Guid VDS_ID = Guid.NewGuid();

    @Mock
    private DbFacade db;

    @Mock
    private BackendInternal backend;

    protected RemoveStorageDomainCommand<RemoveStorageDomainParameters> createCommand(boolean format) {
        RemoveStorageDomainCommand<RemoveStorageDomainParameters> cmd =
                spy(new RemoveStorageDomainCommand<RemoveStorageDomainParameters>(getParams(format)));
        doReturn(db).when(cmd).getDbFacade();
        doReturn(backend).when(cmd).getBackend();
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return ((TransactionMethod<?>) invocation.getArguments()[0]).runInTransaction();
            }

        }).when(cmd).executeInNewTransaction(any(TransactionMethod.class));
        return cmd;
    }

    @Test
    public void testCanDoAction() {
        expectGetStoragePool(STORAGE_POOL_ID);
        expectGetStorageDomain(STORAGE_DOMAIN_ID, STORAGE_POOL_ID, StorageDomainType.Data, StorageType.NFS);
        expectGetVds(VDS_ID);
        expectGetIsoMap(STORAGE_DOMAIN_ID);

        expectBusinessEntitySnapshotDAO();

        RemoveStorageDomainCommand<RemoveStorageDomainParameters> cmd = createCommand(true);
        assertTrue(cmd.canDoAction());

        checkSucceeded(cmd, false);
        checkMessages(cmd,
                VdcBllMessages.VAR__TYPE__STORAGE__DOMAIN,
                VdcBllMessages.VAR__ACTION__REMOVE);
    }

    @Test
    public void testRemoveNfsData() {
        doTestRemove(StorageDomainType.Data, StorageType.NFS, true, false);
    }

    @Test
    public void testRemoveIscsiData() {
        doTestRemove(StorageDomainType.Data, StorageType.ISCSI, true, false);
    }

    @Test
    public void testRemoveAndFormatNfsIso() {
        doTestRemove(StorageDomainType.ISO, StorageType.NFS, true, false);
    }

    @Test
    public void testRemoveNfsExport() {
        doTestRemove(StorageDomainType.ImportExport, StorageType.NFS, false, false);
    }

    @Test
    public void testRemoveLocalData() {
        doTestRemove(StorageDomainType.Data, StorageType.LOCALFS, true, false);
    }

    @Test
    public void testRemoveFormatFailed() {
        doTestRemove(StorageDomainType.Data, StorageType.NFS, true, true);
    }

    public void doTestRemove(StorageDomainType type, StorageType storageType, boolean format, boolean failure) {
        RemoveStorageDomainCommand<RemoveStorageDomainParameters> cmd = createCommand(format);
        expectGetStoragePool(STORAGE_POOL_ID);
        storage_domains dom = expectGetStorageDomain(STORAGE_DOMAIN_ID, STORAGE_POOL_ID, type, storageType);
        expectGetVds(VDS_ID);

        if (storageType == StorageType.LOCALFS) {
            expectGetIsAttached(STORAGE_DOMAIN_ID, STORAGE_POOL_ID);
            expectDetach();
        }

        if (format || (type != StorageDomainType.ISO && type != StorageDomainType.ImportExport)) {
            setUpStorageHelper(cmd, dom, true, failure);
            VDSBrokerFrontend vdsBroker = expectFormat(setUpVdsBroker(), failure);

            if (storageType == StorageType.ISCSI) {
                expectRemove(vdsBroker);
            }

        } else {
            setUpStorageHelper(cmd, dom, false, false);
        }

        if (!failure) {
            expectRemoveFromDb();
        }

        expectBusinessEntitySnapshotDAO();

        cmd.executeCommand();

        checkSucceeded(cmd, !failure);
        checkMessages(cmd);
    }

    protected VDSBrokerFrontend setUpVdsBroker() {
        VDSBrokerFrontend vdsBroker = mock(VDSBrokerFrontend.class);
        when(backend.getResourceManager()).thenReturn(vdsBroker);
        return vdsBroker;
    }

    protected VDSBrokerFrontend expectRemove(VDSBrokerFrontend vdsBroker) {
        VDSReturnValue ret = new VDSReturnValue();
        ret.setSucceeded(true);
        when(vdsBroker.RunVdsCommand(eq(VDSCommandType.RemoveVG),
                any(RemoveVGVDSCommandParameters.class))).thenReturn(ret);
        return vdsBroker;
    }

    protected void setUpStorageHelper(RemoveStorageDomainCommand<RemoveStorageDomainParameters> cmd,
            storage_domains dom,
            boolean connect,
            boolean failure) {
        IStorageHelper helper = mock(IStorageHelper.class);
        if (connect) {
            when(helper.ConnectStorageToDomainByVdsId(dom, VDS_ID)).thenReturn(true);
            when(helper.DisconnectStorageFromDomainByVdsId(dom, VDS_ID)).thenReturn(true);
        }
        if (!failure) {
            when(helper.StorageDomainRemoved(dom.getStorageStaticData())).thenReturn(true);
        }
        doReturn(helper).when(cmd).getStorageHelper(dom);
    }

    protected RemoveStorageDomainParameters getParams(boolean format) {
        RemoveStorageDomainParameters params = new RemoveStorageDomainParameters(STORAGE_DOMAIN_ID);
        params.setStoragePoolId(STORAGE_POOL_ID);
        params.setVdsId(VDS_ID);
        params.setDoFormat(format);
        return params;
    }

    protected storage_domains expectGetStorageDomain(
            Guid domId,
            Guid poolId,
            StorageDomainType type,
            StorageType storageType) {
        StorageDomainDAO dao = mock(StorageDomainDAO.class);
        when(db.getStorageDomainDao()).thenReturn(dao);
        storage_domains dom = getStorageDomain(domId, poolId, type, storageType);
        when(dao.getForStoragePool(domId, poolId)).thenReturn(dom);
        return dom;
    }

    protected void expectGetStorageDomainStatic(Guid domId) {
        StorageDomainStaticDAO dao = mock(StorageDomainStaticDAO.class);
        when(db.getStorageDomainStaticDao()).thenReturn(dao);
        when(dao.get(domId)).thenReturn(getStorageDomainStatic(domId));
    }

    protected void expectGetStoragePool(Guid id) {
        StoragePoolDAO dao = mock(StoragePoolDAO.class);
        when(db.getStoragePoolDao()).thenReturn(dao);
        when(dao.get(id)).thenReturn(getStoragePool(id));
    }

    protected void expectGetIsoMap(Guid id) {
        ArrayList<storage_pool_iso_map> ret = new ArrayList<storage_pool_iso_map>();
        StoragePoolIsoMapDAO dao = mock(StoragePoolIsoMapDAO.class);
        when(db.getStoragePoolIsoMapDao()).thenReturn(dao);
        when(dao.getAllForStorage(id)).thenReturn(ret);
    }

    protected void expectGetVds(Guid id) {
        VdsDAO dao = mock(VdsDAO.class);
        when(db.getVdsDao()).thenReturn(dao);
        when(dao.get(id)).thenReturn(getVds(id));
    }

    protected void expectGetIsAttached(Guid id, Guid poolId) {
        StoragePoolIsoMapDAO dao = mock(StoragePoolIsoMapDAO.class);
        when(db.getStoragePoolIsoMapDao()).thenReturn(dao);
        when(dao.get(new StoragePoolIsoMapId(id, poolId))).thenReturn(new storage_pool_iso_map());
    }

    protected void expectDetach() {
        VdcReturnValueBase ret = new VdcReturnValueBase();
        ret.setSucceeded(true);
        when(backend.runInternalAction(eq(VdcActionType.DetachStorageDomainFromPool),
                any(DetachStorageDomainFromPoolParameters.class))).thenReturn(ret);
    }

    protected VDSBrokerFrontend expectFormat(VDSBrokerFrontend vdsBroker, boolean failure) {
        VDSReturnValue ret = new VDSReturnValue();
        ret.setSucceeded(!failure);
        when(vdsBroker.RunVdsCommand(eq(VDSCommandType.FormatStorageDomain),
                any(FormatStorageDomainVDSCommandParameters.class))).thenReturn(ret);
        return vdsBroker;
    }

    protected void expectBusinessEntitySnapshotDAO() {
        BusinessEntitySnapshotDAO dao = mock(BusinessEntitySnapshotDAO.class);
        when(db.getBusinessEntitySnapshotDao()).thenReturn(dao);
    }

    protected void expectRemoveStaticFromDb() {
        StorageDomainStaticDAO dao = mock(StorageDomainStaticDAO.class);
        when(db.getStorageDomainStaticDao()).thenReturn(dao);
    }

    protected void expectRemoveDynamicFromDb() {
        StorageDomainDynamicDAO dao = mock(StorageDomainDynamicDAO.class);
        when(db.getStorageDomainDynamicDao()).thenReturn(dao);
    }

    protected void expectRemoveFromDb() {
        expectRemoveDynamicFromDb();
        expectRemoveStaticFromDb();
    }

    protected storage_domains getStorageDomain(Guid id,
            Guid poolId,
            StorageDomainType type,
            StorageType storageType) {
        storage_domains dom = new storage_domains();
        dom.setId(id);
        dom.setstorage_pool_id(poolId);
        dom.setstorage_domain_type(type);
        dom.setstorage_type(storageType);
        dom.setstorage_domain_shared_status(StorageDomainSharedStatus.Unattached);
        return dom;
    }

    protected storage_domain_static getStorageDomainStatic(Guid id) {
        storage_domain_static dom = new storage_domain_static();
        dom.setId(id);
        return dom;
    }

    protected storage_pool getStoragePool(Guid id) {
        storage_pool pool = new storage_pool();
        pool.setId(id);
        return pool;
    }

    protected VDS getVds(Guid id) {
        VDS vds = new VDS();
        vds.setId(id);
        vds.setvds_group_compatibility_version(VDS_COMPATIBILITY_VERSION);
        return vds;
    }
}
