package org.ovirt.engine.core.bll.lsm;

import java.util.List;

import org.ovirt.engine.core.bll.Backend;
import org.ovirt.engine.core.bll.ImagesHandler;
import org.ovirt.engine.core.bll.job.ExecutionHandler;
import org.ovirt.engine.core.bll.tasks.TaskHandlerCommand;
import org.ovirt.engine.core.bll.tasks.SPMAsyncTaskHandler;
import org.ovirt.engine.core.common.action.CreateAllSnapshotsFromVmParameters;
import org.ovirt.engine.core.common.action.LiveMigrateDiskParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.asynctasks.AsyncTaskType;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.Image;
import org.ovirt.engine.core.common.businessentities.ImageStatus;
import org.ovirt.engine.core.common.businessentities.Snapshot.SnapshotType;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;

public class LiveSnapshotTaskHandler implements SPMAsyncTaskHandler {

    private final TaskHandlerCommand<? extends LiveMigrateDiskParameters> enclosingCommand;

    public LiveSnapshotTaskHandler(TaskHandlerCommand<? extends LiveMigrateDiskParameters> enclosingCommand) {
        this.enclosingCommand = enclosingCommand;
    }

    @Override
    public void execute() {
        VdcReturnValueBase vdcReturnValue =
                Backend.getInstance().runInternalAction(VdcActionType.CreateAllSnapshotsFromVm,
                        getCreateSnapshotParameters(),
                        ExecutionHandler.createDefaultContexForTasks(enclosingCommand.getExecutionContext()));
        enclosingCommand.getReturnValue().getTaskIdList().addAll(vdcReturnValue.getInternalTaskIdList());
        enclosingCommand.getReturnValue().setSucceeded(true);
    }

    @Override
    public void endSuccessfully() {
        updateDestinitationImageId();
        endCreateAllSnapshots();
        ImagesHandler.updateImageStatus(enclosingCommand.getParameters().getDestinationImageId(), ImageStatus.LOCKED);
    }

    private void updateDestinitationImageId() {
        Image oldLeaf =
                DbFacade.getInstance().getImageDao().get(enclosingCommand.getParameters().getImageId());
        List<DiskImage> allImages =
                DbFacade.getInstance().getDiskImageDao().getAllSnapshotsForImageGroup(oldLeaf.getDiskId());

        for (DiskImage image : allImages) {
            if (image.getImage().isActive()) {
                enclosingCommand.getParameters().setDestinationImageId(image.getImageId());
                break;
            }
        }
    }

    private void endCreateAllSnapshots() {
        Backend.getInstance().endAction(VdcActionType.CreateAllSnapshotsFromVm,
                getCreateSnapshotParameters(),
                ExecutionHandler.createDefaultContexForTasks(enclosingCommand.getExecutionContext()));
    }

    @Override
    public void endWithFailure() {
        updateDestinitationImageId();
        endCreateAllSnapshots();
    }

    @Override
    public void compensate() {
        // Unlock the image we left locked
        ImagesHandler.updateImageStatus(enclosingCommand.getParameters().getImageId(), ImageStatus.OK);
    }

    @Override
    public AsyncTaskType getTaskType() {
        // No implementation - handled by the command
        return null;
    }

    @Override
    public AsyncTaskType getRevertTaskType() {
        // No implementation - there is no live-merge
        return null;
    }

    protected CreateAllSnapshotsFromVmParameters getCreateSnapshotParameters() {
        CreateAllSnapshotsFromVmParameters params = new CreateAllSnapshotsFromVmParameters
                (enclosingCommand.getParameters().getVmId(),
                        "Auto-generated for Live Storage Migration of "
                                + enclosingCommand.getParameters().getDiskAlias());

        params.setParentCommand(VdcActionType.LiveMigrateDisk);
        params.setSnapshotType(SnapshotType.REGULAR);
        params.setParentParameters(enclosingCommand.getParameters());
        params.setImagesParameters(enclosingCommand.getParameters().getImagesParameters());
        params.setNeedsLocking(false);

        return params;
    }
}
