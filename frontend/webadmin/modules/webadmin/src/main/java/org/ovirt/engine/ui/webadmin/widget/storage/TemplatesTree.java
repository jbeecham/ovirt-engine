package org.ovirt.engine.ui.webadmin.widget.storage;

import java.util.ArrayList;

import org.ovirt.engine.core.common.businessentities.BusinessEntity;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.Quota;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.businessentities.storage_domains;
import org.ovirt.engine.ui.common.CommonApplicationConstants;
import org.ovirt.engine.ui.common.CommonApplicationResources;
import org.ovirt.engine.ui.common.widget.label.DiskSizeLabel;
import org.ovirt.engine.ui.common.widget.label.TextBoxLabel;
import org.ovirt.engine.ui.common.widget.renderer.DiskSizeRenderer.DiskSizeUnit;
import org.ovirt.engine.ui.common.widget.tree.AbstractSubTabTree;
import org.ovirt.engine.ui.uicommonweb.models.SearchableListModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.ApplicationTemplates;
import org.ovirt.engine.ui.webadmin.widget.label.FullDateTimeLabel;

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.TreeItem;

public class TemplatesTree<M extends SearchableListModel> extends AbstractSubTabTree<M, VmTemplate, DiskImage> {

    ApplicationResources resources;
    ApplicationConstants constants;

    public TemplatesTree(CommonApplicationResources resources,
            CommonApplicationConstants constants,
            ApplicationTemplates templates) {
        super(resources, constants, templates);
        this.resources = (ApplicationResources) resources;
        this.constants = (ApplicationConstants) constants;
    }

    @Override
    protected TreeItem getRootItem(VmTemplate template) {
        HorizontalPanel panel = new HorizontalPanel();
        panel.setSpacing(1);
        panel.setWidth("100%"); //$NON-NLS-1$

        addItemToPanel(panel, new Image(resources.vmImage()), "25px"); //$NON-NLS-1$
        addTextBoxToPanel(panel, new TextBoxLabel(), template.getname(), ""); //$NON-NLS-1$
        addValueLabelToPanel(panel, new DiskSizeLabel<Integer>(), template.getDiskMap().size(), "110px"); //$NON-NLS-1$
        addValueLabelToPanel(panel, new DiskSizeLabel<Double>(), template.getActualDiskSize(), "110px"); //$NON-NLS-1$
        addValueLabelToPanel(panel, new FullDateTimeLabel(), template.getcreation_date(), "140px"); //$NON-NLS-1$

        TreeItem treeItem = new TreeItem(panel);
        treeItem.setUserObject(template.getId());
        return treeItem;
    }

    @Override
    protected TreeItem getNodeItem(DiskImage disk) {
        HorizontalPanel panel = new HorizontalPanel();
        panel.setSpacing(1);
        panel.setWidth("100%"); //$NON-NLS-1$

        addItemToPanel(panel, new Image(resources.diskImage()), "25px"); //$NON-NLS-1$
        addTextBoxToPanel(panel, new TextBoxLabel(), disk.getDiskAlias(), ""); //$NON-NLS-1$
        addTextBoxToPanel(panel, new TextBoxLabel(), "", "110px"); //$NON-NLS-1$ //$NON-NLS-2$
        addValueLabelToPanel(panel, new DiskSizeLabel<Long>(DiskSizeUnit.BYTE), disk.getsize(), "110px"); //$NON-NLS-1$
        addValueLabelToPanel(panel, new DiskSizeLabel<Double>(), disk.getActualSize(), "110px"); //$NON-NLS-1$
        addValueLabelToPanel(panel, new FullDateTimeLabel(), disk.getcreation_date(), "140px"); //$NON-NLS-1$

        TreeItem treeItem = new TreeItem(panel);
        treeItem.setUserObject(disk.getImageId());
        return treeItem;
    }

    @Override
    protected ArrayList<DiskImage> getNodeObjects(VmTemplate template) {
        return new ArrayList<DiskImage>(template.getDiskImageMap().values());
    }

    @Override
    protected boolean getIsNodeEnabled(DiskImage disk) {
        if (listModel.getEntity() == null) {
            return true;
        }
        if (listModel.getEntity() instanceof Quota) {
            return ((BusinessEntity) listModel.getEntity()).getId().equals(((DiskImage) disk).getQuotaId());
        }
        return disk.getstorage_ids().contains(((storage_domains) listModel.getEntity()).getId());
    }
}
