/*
 * Copyright 2018 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
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
 *
 */
package com.keepassdroid.adapters;

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

import com.keepassdroid.app.App;
import com.keepassdroid.database.PwGroup;
import com.keepassdroid.database.PwNode;
import com.keepassdroid.settings.PrefsUtil;
import com.kunzisoft.keepass.R;

public class NodeAdapter extends RecyclerView.Adapter<BasicViewHolder> {

    private static final int MENU_OPEN = Menu.FIRST;
    private static final int MENU_DELETE = MENU_OPEN + 1;

    private SortedList<PwNode> nodeSortedList;

    private Context context;
    private LayoutInflater inflater;
    private float textSize;
    private boolean sortByName;

    private OnNodeClickCallback onNodeClickCallback;
    private int nodePositionToUpdate;
    private NodeMenuListener nodeMenuListener;

    public NodeAdapter(final Context context, PwGroup mainNode) {
        this.inflater = LayoutInflater.from(context);
        this.context = context;
        this.textSize = PrefsUtil.getListTextSize(context);
        this.sortByName = PrefsUtil.isListSortByName(context);
        this.nodePositionToUpdate = -1;

        this.nodeSortedList = new SortedList<>(PwNode.class, new SortedListAdapterCallback<PwNode>(this) {
            @Override public int compare(PwNode item1, PwNode item2) {
                // Choose sort depend of preferences
                if(sortByName)
                    return new PwNode.NodeNameComparator().compare(item1, item2);
                else
                    return new PwNode.NodeCreationComparator().compare(item1, item2);
            }

            @Override public boolean areContentsTheSame(PwNode oldItem, PwNode newItem) {
                return oldItem.isContentVisuallyTheSame(newItem);
            }

            @Override public boolean areItemsTheSame(PwNode item1, PwNode item2) {
                return item1.equals(item2);
            }
        });
        this.nodeSortedList.addAll(mainNode.getDirectChildren());
    }

    /**
     * Rebuild the list by clear and build again for the group
     */
    public void rebuildList(PwGroup group) {
        this.nodeSortedList.clear();
        this.nodeSortedList.addAll(group.getDirectChildren());
    }

    /**
     * Add a node to the list
     * @param node Node to add
     */
    public void addNode(PwNode node) {
        nodeSortedList.add(node);
    }

    /**
     * Update the last Node clicked in the list
     */
    public void updateLastNodeClicked() {
        // Don't really update here, sorted list knows each original ref, so we just notify a change
        try {
            notifyItemChanged(nodePositionToUpdate);
            nodeSortedList.recalculatePositionOfItemAt(nodePositionToUpdate);
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
    public void notifyChangeSort() {
        this.sortByName = PrefsUtil.isListSortByName(context);
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
            view = inflater.inflate(R.layout.list_entries_group, parent, false);
            basicViewHolder = new GroupViewHolder(view);
        } else {
            view = inflater.inflate(R.layout.list_entries_entry, parent, false);
            basicViewHolder = new EntryViewHolder(view);
        }
        return basicViewHolder;
    }

    @Override
    public void onBindViewHolder(BasicViewHolder holder, int position) {
        PwNode subNode = nodeSortedList.get(position);
        // Assign image
        App.getDB().drawFactory.assignDrawableTo(holder.icon,
                context.getResources(), subNode.getIcon());
        // Assign text
        holder.text.setText(subNode.getDisplayTitle());
        // Assign click
        holder.container.setOnClickListener(
                new OnNodeClickListener(subNode));
        holder.container.setOnCreateContextMenuListener(
                new ContextMenuBuilder(subNode, nodeMenuListener));
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
            nodePositionToUpdate = nodeSortedList.indexOf(node);
            if (onNodeClickCallback != null)
                onNodeClickCallback.onNodeClick(node);
        }
    }

    /**
     * Utility class for menu listener
     */
    private class ContextMenuBuilder implements View.OnCreateContextMenuListener {

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
                        return menuListener.onOpenMenuClick(node);
                    case MENU_DELETE:
                        return menuListener.onDeleteMenuClick(node);
                    default:
                        return false;
                }
            }
        };
    }
}
