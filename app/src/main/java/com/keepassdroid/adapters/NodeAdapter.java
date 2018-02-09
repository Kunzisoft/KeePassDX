package com.keepassdroid.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.keepassdroid.app.App;
import com.keepassdroid.database.PwEntry;
import com.keepassdroid.database.PwGroup;
import com.keepassdroid.database.PwNode;
import com.keepassdroid.settings.PrefsUtil;
import com.kunzisoft.keepass.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NodeAdapter extends RecyclerView.Adapter<BasicViewHolder> {

    private static final int MENU_OPEN = Menu.FIRST;
    private static final int MENU_DELETE = MENU_OPEN + 1;

    private Context context;
    private LayoutInflater inflater;
    private PwGroup pwGroup;
    private float textSize;

    private OnNodeClickCallback onNodeClickCallback;
    private NodeMenuListener nodeMenuListener;

    private List<PwGroup> groupsForViewing;
    private List<PwEntry> entriesForViewing;
    private Comparator<PwEntry> entryComp = new PwEntry.EntryNameComparator();
    private Comparator<PwGroup> groupComp = new PwGroup.GroupNameComparator();
    private boolean isListSort;

    public NodeAdapter(Context context, PwGroup pwGroup) {
        this.inflater = LayoutInflater.from(context);
        this.context = context;
        this.pwGroup = pwGroup;
        this.isListSort = PrefsUtil.isListSort(context);
        this.textSize = PrefsUtil.getListTextSize(context);

        filterAndSort();
    }

    public void notifyDataSetChangedAndSort() {
        super.notifyDataSetChanged();
        filterAndSort();
    }

    @Override
    public int getItemViewType(int position) {
        if ( position < groupsForViewing.size() ) {
            return PwNode.Type.GROUP.ordinal();
        } else {
            return PwNode.Type.ENTRY.ordinal();
        }
    }

    @Override
    public BasicViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        BasicViewHolder basicViewHolder;
        View view;
        if (viewType == PwNode.Type.GROUP.ordinal()) {
            view = inflater.inflate(R.layout.list_entries_group, parent, false);
            basicViewHolder = new GroupViewHolder(view);
        } else {
            view = inflater.inflate(R.layout.list_entries_entry, parent, false);
            basicViewHolder = new EntryViewHolder(view);
        }
        return basicViewHolder;
    }

    // TODO change location
    private void filterAndSort() {
        entriesForViewing = new ArrayList<>();

        if (pwGroup != null) {
            for (int i = 0; i < pwGroup.childEntries.size(); i++) {
                PwEntry entry = pwGroup.childEntries.get(i);
                if (!entry.isMetaStream()) {
                    entriesForViewing.add(entry);
                }
            }

            if (isListSort) {
                groupsForViewing = new ArrayList<>(pwGroup.childGroups);

                Collections.sort(entriesForViewing, entryComp);
                Collections.sort(groupsForViewing, groupComp);
            } else {
                groupsForViewing = pwGroup.childGroups;
            }
        }
    }

    @Override
    public void onBindViewHolder(BasicViewHolder holder, int position) {
        int listGroupsSize = groupsForViewing.size();
        if ( position < listGroupsSize ) {
            PwGroup group = groupsForViewing.get(position);
            // Assign image
            App.getDB().drawFactory.assignDrawableTo(holder.icon,
                    context.getResources(), group.getIcon());
            // Assign text
            holder.text.setText(group.getName());
            // Assign click
            holder.container.setOnClickListener(
                    new OnNodeClickListener(group, position));
            holder.container.setOnCreateContextMenuListener(
                    new ContextMenuBuilder(group, position, nodeMenuListener));
        } else {
            int entryPosition = position - listGroupsSize;
            PwEntry entry = entriesForViewing.get(entryPosition);
            App.getDB().drawFactory.assignDrawableTo(holder.icon,
                    context.getResources(), entry.getIcon());
            // Assign text
            holder.text.setText(entry.getDisplayTitle());
            // Assign click
            holder.container.setOnClickListener(
                    new OnNodeClickListener(entry, position));
            holder.container.setOnCreateContextMenuListener(
                    new ContextMenuBuilder(entry, entryPosition, nodeMenuListener));
        }
        // Assign text size
        holder.text.setTextSize(textSize);
    }

    @Override
    public int getItemCount() {
        return groupsForViewing.size() + entriesForViewing.size();
    }

    public void setOnNodeClickListener(OnNodeClickCallback onNodeClickCallback) {
        this.onNodeClickCallback = onNodeClickCallback;
    }

    public void setNodeMenuListener(NodeMenuListener nodeMenuListener) {
        this.nodeMenuListener = nodeMenuListener;
    }

    public interface OnNodeClickCallback {
        void onNodeClick(PwNode node, int position);
    }

    public class OnNodeClickListener implements View.OnClickListener {
        private PwNode node;
        private int position;

        OnNodeClickListener(PwNode node, int position) {
            this.node = node;
            this.position = position;
        }

        @Override
        public void onClick(View v) {
            if (onNodeClickCallback != null)
                onNodeClickCallback.onNodeClick(node, position);
        }
    }

    public interface NodeMenuListener {
        boolean onOpenMenuClick(PwNode node, int position);
        boolean onDeleteMenuClick(PwNode node, int position);
    }

    private class ContextMenuBuilder implements View.OnCreateContextMenuListener {

        private PwNode node;
        private int position;
        private NodeMenuListener menuListener;

        ContextMenuBuilder(PwNode node, int position, NodeMenuListener menuListener) {
            this.menuListener = menuListener;
            this.node = node;
            this.position = position;
        }

        @Override
        public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
            MenuItem clearMenu = contextMenu.add(Menu.NONE, MENU_OPEN, Menu.NONE, R.string.menu_open);
            clearMenu.setOnMenuItemClickListener(mOnMyActionClickListener);
            if (!App.getDB().readOnly) {
                clearMenu = contextMenu.add(Menu.NONE, MENU_DELETE, Menu.NONE, R.string.menu_delete);
                clearMenu.setOnMenuItemClickListener(mOnMyActionClickListener);
            }
        }

        private MenuItem.OnMenuItemClickListener mOnMyActionClickListener = new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (menuListener == null)
                    return false;
                switch ( item.getItemId() ) {
                    case MENU_OPEN:
                        return menuListener.onOpenMenuClick(node, position);
                    case MENU_DELETE:
                        return menuListener.onDeleteMenuClick(node, position);
                    default:
                        return false;
                }
            }
        };
    }
}
