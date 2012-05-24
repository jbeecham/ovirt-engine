package org.ovirt.engine.core.bll.gluster;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.ovirt.engine.core.common.action.gluster.GlusterVolumeReplaceBrickActionParameters;
import org.ovirt.engine.core.common.businessentities.VDSType;
import org.ovirt.engine.core.common.businessentities.VdsStatic;
import org.ovirt.engine.core.common.businessentities.gluster.AccessProtocol;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterBrickEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterBrickStatus;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterTaskOperation;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeStatus;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeType;
import org.ovirt.engine.core.common.businessentities.gluster.TransportType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dao.gluster.GlusterVolumeDao;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ DbFacade.class, GlusterVolumeDao.class, ReplaceGlusterVolumeBrickCommand.class })
public class ReplaceGlusterVolumeBrickCommandTest {
    @Mock
    DbFacade db;

    @Mock
    GlusterVolumeDao volumeDao;

    private Guid volumeId1 = new Guid("8bc6f108-c0ef-43ab-ba20-ec41107220f5");
    private Guid volumeId2 = new Guid("b2cb2f73-fab3-4a42-93f0-d5e4c069a43e");
    private Guid volumeId3 = Guid.createGuidFromString("000000000000-0000-0000-0000-00000003");
    private Guid volumeId4 = Guid.createGuidFromString("000000000000-0000-0000-0000-00000004");

    private ReplaceGlusterVolumeBrickCommand cmd;

    @Before
    public void mockDbFacadeAndDao() {
        MockitoAnnotations.initMocks(this);
        mockStatic(DbFacade.class);
        mockStatic(GlusterVolumeDao.class);
        when(db.getGlusterVolumeDao()).thenReturn(volumeDao);
        when(DbFacade.getInstance()).thenReturn(db);
        when(volumeDao.getById(volumeId1)).thenReturn(getDistributedVolume(volumeId1));
        when(volumeDao.getById(volumeId2)).thenReturn(getDistributedVolume(volumeId2));
        when(volumeDao.getById(volumeId3)).thenReturn(getReplicatedVolume(volumeId3, 2));
        when(volumeDao.getById(volumeId4)).thenReturn(getReplicatedVolume(volumeId4, 4));
        when(volumeDao.getById(null)).thenReturn(null);
    }

    private GlusterVolumeEntity getDistributedVolume(Guid volumeId) {
        GlusterVolumeEntity volume = getVolume(volumeId);
        volume.setStatus((volumeId == volumeId1) ? GlusterVolumeStatus.UP : GlusterVolumeStatus.DOWN);
        volume.setBricks(getBricks(volumeId, "distrib", 2));
        volume.setVolumeType(GlusterVolumeType.DISTRIBUTE);
        return volume;
    }

    private GlusterVolumeEntity getReplicatedVolume(Guid volumeId, int brickCount) {
        GlusterVolumeEntity volume = getVolume(volumeId);
        volume.setStatus(GlusterVolumeStatus.UP);
        volume.setBricks(getBricks(volumeId, "repl", brickCount));
        volume.setVolumeType(GlusterVolumeType.REPLICATE);
        volume.setReplicaCount(brickCount);
        return volume;
    }

    private GlusterVolumeEntity getVolume(Guid id) {
        GlusterVolumeEntity volumeEntity = new GlusterVolumeEntity();
        volumeEntity.setId(id);
        volumeEntity.setName("test-vol");
        volumeEntity.addAccessProtocol(AccessProtocol.GLUSTER);
        volumeEntity.addTransportType(TransportType.TCP);
        return volumeEntity;
    }

    private List<GlusterBrickEntity> getBricks(Guid volumeId, String dirPrefix, int n) {
        List<GlusterBrickEntity> bricks = new ArrayList<GlusterBrickEntity>();
        GlusterBrickEntity brick;
        for (Integer i = 0; i < n; i++) {
            brick =
                    new GlusterBrickEntity(volumeId,
                            new VdsStatic("myhost", "127.0.0.1", "0934390834", 20, new Guid(), new Guid(), "myhost", true, VDSType.oVirtNode),
                            "/tmp/" + dirPrefix + i.toString(),
                            GlusterBrickStatus.UP);
            bricks.add(brick);
        }
        return bricks;
    }

    private ReplaceGlusterVolumeBrickCommand createTestCommand1(Guid volumeId) {
        return new ReplaceGlusterVolumeBrickCommand(new GlusterVolumeReplaceBrickActionParameters(volumeId,
                GlusterTaskOperation.START,
                getBricks(volumeId, "distrib", 1).get(0),
                getBricks(volumeId, "new", 1).get(0), false));
    }

    private ReplaceGlusterVolumeBrickCommand createTestCommand2(Guid volumeId) {
        return new ReplaceGlusterVolumeBrickCommand(new GlusterVolumeReplaceBrickActionParameters(volumeId,
                GlusterTaskOperation.START,
                null,
                getBricks(volumeId, "", 1).get(0), false));
    }

    @Test
    public void canDoActionSucceedsOnUpVolume() {
        cmd = createTestCommand1(volumeId1);
        assertTrue(cmd.canDoAction());
    }

    @Test
    public void canDoActionFailesOnDownVolume() {
        cmd = createTestCommand1(volumeId2);
        assertFalse(cmd.canDoAction());
    }

    @Test
    public void canDoActionFailsOnInvalidBrick() {
        cmd = createTestCommand1(volumeId3);
        assertFalse(cmd.canDoAction());
    }

    @Test
    public void canDoActionFailsOnNull() {
        cmd = createTestCommand1(null);
        assertFalse(cmd.canDoAction());
    }

    @Test
    public void canDoActionFailsOnNoBrick() {
        cmd = createTestCommand2(volumeId4);
        assertFalse(cmd.canDoAction());
    }

}