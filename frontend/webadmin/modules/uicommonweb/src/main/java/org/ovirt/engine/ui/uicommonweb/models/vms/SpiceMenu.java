package org.ovirt.engine.ui.uicommonweb.models.vms;

import org.ovirt.engine.core.compat.StringFormat;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class SpiceMenu
{
    private List<SpiceMenuItem> items;

    public List<SpiceMenuItem> getItems()
    {
        if (items == null)
        {
            items = new ArrayList<SpiceMenuItem>();
        }

        return items;
    }

    public List<SpiceMenuItem> Descendants()
    {
        ArrayList<SpiceMenuItem> list = new ArrayList<SpiceMenuItem>();
        for (SpiceMenuItem item : items)
        {
            DescendantsInternal(list, item);
        }

        return list;
    }

    private void DescendantsInternal(List<SpiceMenuItem> list, SpiceMenuItem root)
    {
        list.add(root);
        if (root instanceof SpiceMenuContainerItem)
        {
            for (SpiceMenuItem item : ((SpiceMenuContainerItem) root).getItems())
            {
                DescendantsInternal(list, item);
            }
        }
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        for (SpiceMenuItem item : getItems())
        {
            builder.append(ItemToString(item, null));
        }

        return builder.toString();
    }

    private String ItemToString(SpiceMenuItem item, SpiceMenuItem parent)
    {
        StringBuilder builder = new StringBuilder();
        int parentID = parent != null ? parent.getId() : 0;

        if (item instanceof SpiceMenuCommandItem)
        {
            SpiceMenuCommandItem commandItem = (SpiceMenuCommandItem) item;
            builder.append(StringFormat.format("%1$s\r%2$s\r%3$s\r%4$s\n", //$NON-NLS-1$
                    parentID,
                    commandItem.getId(),
                    commandItem.getText(),
                    (commandItem.getIsEnabled()) ? 0 : 2));
        }

        if (item instanceof SpiceMenuContainerItem)
        {
            SpiceMenuContainerItem containerItem = (SpiceMenuContainerItem) item;
            builder.append(StringFormat.format("%1$s\r%2$s\r%3$s\r4\n", //$NON-NLS-1$
                    parentID,
                    containerItem.getId(),
                    containerItem.getText()));

            if (containerItem.getItems().size() > 0)
            {
                for (SpiceMenuItem localItem : containerItem.getItems())
                {
                    builder.append(ItemToString(localItem, containerItem));
                }
            }
        }

        if (item instanceof SpiceMenuSeparatorItem)
        {
            builder.append(StringFormat.format("%1$s\r%2$s\r%3$s\r1\n", parentID, item.getId(), "-")); //$NON-NLS-1$ //$NON-NLS-2$
        }

        return builder.toString();
    }
}
