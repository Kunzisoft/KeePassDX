/*
 * Copyright 2018 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.adapters;

import android.content.Context;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.util.SortedListAdapterCallback;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.app.App;
import com.kunzisoft.keepass.database.PwGroup;
import com.kunzisoft.keepass.database.PwNode;
import com.kunzisoft.keepass.database.SortNodeEnum;
import com.kunzisoft.keepass.settings.PreferencesUtil;

public class NodeAdapter extends RecyclerView.Adapter<BasicViewHolder> {

    private SortedList<PwNode> nodeSortedList;

    private Context context;
    private LayoutInflater inflater;
    private float textSize;
    private SortNodeEnum listSort;
    private boolean groupsBeforeSort;
    private boolean ascendingSort;

    private OnNodeClickCallback onNodeClickCallback;
    private int nodePositionToUpdate;
    private NodeMenuListener nodeMenuListener;
    private boolean activateContextMenu;

    /**
     * Create node list adapter with contextMenu or not
     * @param context Context to use
     */
    public NodeAdapter(final Context context) {
        this.inflater = LayoutInflater.from(context);
        this.context = context;
        this.textSize = PreferencesUtil.getListTextSize(context);
        this.listSort = PreferencesUtil.getListSort(context);
        this.groupsBeforeSort = PreferencesUtil.getGroupsBeforeSort(context);
        this.ascendingSort = PreferencesUtil.getAscendingSort(context);
        this.activateContextMenu = false;
        this.nodePositionToUpdate = -1;

        this.nodeSortedList = new SortedList<>(PwNode.class, new SortedListAdapterCallback<PwNode>(this) {
            @Override public int compare(PwNode item1, PwNode item2) {
                return listSort.getNodeComparator(ascendingSort, groupsBeforeSort).compare(item1, item2);
            }

            @Override public boolean areContentsTheSame(PwNode oldItem, PwNode newItem) {
                return oldItem.isContentVisuallyTheSame(newItem);
            }

            @Override public boolean areItemsTheSame(PwNode item1, PwNode item2) {
                return item1.equals(item2);
            }
        });
    }

    public void setActivateContextMenu(boolean activate) {
        this.activateContextMenu = activate;
    }

    /**
     * Rebuild the list by clear and build children from the group
     */
    public void rebuildList(PwGroup group) {
        this.nodeSortedList.clear();
        if (group != null) {
            this.nodeSortedList.addAll(group.getDirectChildren());
        }
    }

    /**
     * Add a node to the list
     * @param node Node to add
     */
    public void addNode(PwNode node) {
        nodeSortedList.add(node);
    }

    /**
     * Register a node to update before an action
     * Call updateLastNodeRegister() after the action to update the node
     * @param node Node to register
     */
    public void registerANodeToUpdate(PwNode node) {
        nodePositionToUpdate = nodeSortedList.indexOf(node);
    }

    /**
     * Update the last Node register in the list
     * Work if only registerANodeToUpdate(PwNode node) is called before
     */
    public void updateLastNodeRegister(PwNode node) {
        // Don't really update here, sorted list knows each original ref, so we just notify a change
        try {
            if (nodePositionToUpdate != -1) {
                // Don't know why but there is a bug to remove a node after this update
                nodeSortedList.updateItemAt(nodePositionToUpdate, node);
                nodeSortedList.recalculatePositionOfItemAt(nodePositionToUpdate);
                nodePositionToUpdate = -1;
            }
            else {
                Log.e(NodeAdapter.class.getName(), "registerANodeToUpdate must be called before updateLastNodeRegister");
            }
        } catch (IndexOutOfBoundsException e) {
            Log.e(NodeAdapter.class.getName(), e.getMessage());
        }
    }

    /**
     * Remove node in the list
     * @param node Node to delete
     */
    public void removeNode(PwNode node) {
        nodeSortedList.remove(node);
    }

    /**
     * Notify a change sort of the list
     */
    public void notifyChangeSort(SortNodeEnum sortNodeEnum, boolean ascending, boolean groupsBefore) {
        this.listSort = sortNodeEnum;
        this.ascendingSort = ascending;
        this.groupsBeforeSort = groupsBefore;
    }

    @Override
    public int getItemViewType(int position) {
        return nodeSortedList.get(position).getType().ordinal();
    }

    @Override
    public BasicViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        BasicViewHolder basicViewHolder;
        View view;
        if (viewType == PwNode.Type.GROUP.ordinal()) {
            view = inflater.inflate(R.layout.list_nodes_group, parent, false);
            basicViewHolder = new GroupViewHolder(view);
        } else {
            view = inflater.inflate(R.layout.list_nodes_entry, parent, false);
            basicViewHolder = new EntryViewHolder(view);
        }
        return basicViewHolder;
    }

    @Override
    public void onBindViewHolder(BasicViewHolder holder, int position) {
        PwNode subNode = nodeSortedList.get(position);
        // Assign image
        App.getDB().getDrawFactory().assignDrawableTo(holder.icon,
                context.getResources(), subNode.getIcon());
        // Assign text
        holder.text.setText(subNode.getDisplayTitle());
        // Assign click
        holder.container.setOnClickListener(
                new OnNodeClickListener(subNode));
        // Context menu
        if (activateContextMenu) {
            holder.container.setOnCreateContextMenuListener(
                    new ContextMenuBuilder(subNode, nodeMenuListener));
        }
        // Assign text size
        holder.text.setTextSize(textSize);
    }

    @Override
    public int getItemCount() {
        return nodeSortedList.size();
    }

    /**
     * Assign a listener when a node is clicked
     */
    public void setOnNodeClickListener(OnNodeClickCallback onNodeClickCallback) {
        this.onNodeClickCallback = onNodeClickCallback;
    }

    /**
     * Assign a listener when an element of menu is clicked
     */
    public void setNodeMenuListener(NodeMenuListener nodeMenuListener) {
        this.nodeMenuListener = nodeMenuListener;
    }

    /**
     * Callback listener to redefine to do an action when a node is click
     */
    public interface OnNodeClickCallback {
        void onNodeClick(PwNode node);
    }

    /**
     * Menu listener to redefine to do an action in menu
     */
    public interface NodeMenuListener {
        boolean onOpenMenuClick(PwNode node);
        boolean onEditMenuClick(PwNode node);
        boolean onDeleteMenuClick(PwNode node);
    }

    /**
     * Utility class for node listener
     */
    private class OnNodeClickListener implements View.OnClickListener {
        private PwNode node;

        OnNodeClickListener(PwNode node) {
            this.node = node;
        }

        @Override
        public void onClick(View v) {
            if (onNodeClickCallback != null)
                onNodeClickCallback.onNodeClick(node);
        }
    }

    /**
     * Utility class for menu listener
     */
    private class ContextMenuBuilder implements View.OnCreateContextMenuListener {

        private static final int MENU_OPEN = Menu.FIRST;
        private static final int MENU_EDIT = MENU_OPEN + 1;
        private static final int MENU_DELETE = MENU_EDIT + 1;

        private PwNode node;
        private NodeMenuListener menuListener;

        ContextMenuBuilder(PwNode node, NodeMenuListener menuListener) {
            this.menuListener = menuListener;
            this.node = node;
        }

        @Override
        public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
            MenuItem clearMenu = contextMenu.add(Menu.NONE, MENU_OPEN, Menu.NONE, R.string.menu_open);
            clearMenu.setOnMenuItemClickListener(mOnMyActionClickListener);
            if (!App.getDB().isReadOnly() && !node.equals(App.getDB().getPwDatabase().getRecycleBin())) {
                // TODO make edit for group
                // clearMenu = contextMenu.add(Menu.NONE, MENU_EDIT, Menu.NONE, R.string.menu_edit);
                // clearMenu.setOnMenuItemClickListener(mOnMyActionClickListener);
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
                        return menuListener.onOpenMenuClick(node);
                    case MENU_EDIT:
                        return menuListener.onEditMenuClick(node);
                    case MENU_DELETE:
                        return menuListener.onDeleteMenuClick(node);
                    default:
                        return false;
                }
            }
        };
    }
}
