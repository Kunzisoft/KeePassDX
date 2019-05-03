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
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.util.SortedListAdapterCallback;
import android.util.Log;
import android.view.*;
import android.widget.Toast;
import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.app.App;
import com.kunzisoft.keepass.database.SortNodeEnum;
import com.kunzisoft.keepass.database.element.*;
import com.kunzisoft.keepass.settings.PreferencesUtil;
import com.kunzisoft.keepass.utils.Util;

public class NodeAdapter extends RecyclerView.Adapter<BasicViewHolder> {
    private static final String TAG = NodeAdapter.class.getName();

    private SortedList<NodeVersioned> nodeSortedList;

    private Context context;
    private LayoutInflater inflater;
    private MenuInflater menuInflater;
    private float textSize;
    private float subtextSize;
    private float iconSize;
    private SortNodeEnum listSort;
    private boolean groupsBeforeSort;
    private boolean ascendingSort;
    private boolean showUsernames;

    private NodeClickCallback nodeClickCallback;
    private NodeMenuListener nodeMenuListener;
    private boolean activateContextMenu;
    private boolean readOnly;
    private boolean isASearchResult;

    private Database database;

    private int iconGroupColor;
    private int iconEntryColor;

    /**
     * Create node list adapter with contextMenu or not
     * @param context Context to use
     */
    public NodeAdapter(final Context context, MenuInflater menuInflater) {
        this.inflater = LayoutInflater.from(context);
        this.menuInflater = menuInflater;
        this.context = context;
        assignPreferences();
        this.activateContextMenu = false;
        this.readOnly = false;
        this.isASearchResult = false;

        this.nodeSortedList = new SortedList<>(NodeVersioned.class, new SortedListAdapterCallback<NodeVersioned>(this) {
            @Override public int compare(NodeVersioned item1, NodeVersioned item2) {
                return listSort.getNodeComparator(ascendingSort, groupsBeforeSort).compare(item1, item2);
            }

            @Override public boolean areContentsTheSame(NodeVersioned oldItem, NodeVersioned newItem) {
				return oldItem.getTitle().equals(newItem.getTitle())
						&& oldItem.getIcon().equals(newItem.getIcon());
            }

            @Override public boolean areItemsTheSame(NodeVersioned item1, NodeVersioned item2) {
                return item1.equals(item2);
            }
        });

        // Database
        this.database = App.getDB();

        // Retrieve the color to tint the icon
        int[] attrTextColorPrimary = {android.R.attr.textColorPrimary};
        TypedArray taTextColorPrimary = context.getTheme().obtainStyledAttributes(attrTextColorPrimary);
        this.iconGroupColor = taTextColorPrimary.getColor(0, Color.BLACK);
        taTextColorPrimary.recycle();
        int[] attrTextColor = {android.R.attr.textColor}; // In two times to fix bug compilation
        TypedArray taTextColor = context.getTheme().obtainStyledAttributes(attrTextColor);
        this.iconEntryColor = taTextColor.getColor(0, Color.BLACK);
        taTextColor.recycle();
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public void setIsASearchResult(boolean isASearchResult) {
        this.isASearchResult = isASearchResult;
    }

    public void setActivateContextMenu(boolean activate) {
        this.activateContextMenu = activate;
    }

    private void assignPreferences() {
        float textSizeDefault = Util.getListTextDefaultSize(context);
        this.textSize = PreferencesUtil.getListTextSize(context);
        this.subtextSize = context.getResources().getInteger(R.integer.list_small_size_default)
                * textSize / textSizeDefault;
        // Retrieve the icon size
        float iconDefaultSize = context.getResources().getDimension(R.dimen.list_icon_size_default);
        this.iconSize = iconDefaultSize * textSize / textSizeDefault;
        this.listSort = PreferencesUtil.getListSort(context);
        this.groupsBeforeSort = PreferencesUtil.getGroupsBeforeSort(context);
        this.ascendingSort = PreferencesUtil.getAscendingSort(context);
        this.showUsernames = PreferencesUtil.showUsernamesListEntries(context);
    }

    /**
     * Rebuild the list by clear and build children from the group
     */
    public void rebuildList(GroupVersioned group) {
        this.nodeSortedList.clear();
        assignPreferences();
        // TODO verify sort
        try {
            this.nodeSortedList.addAll(group.getChildEntriesWithoutMetaStream());
        } catch (Exception e) {
            Log.e(TAG, "Can't add node elements to the list", e);
            Toast.makeText(context, "Can't add node elements to the list : " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Determine if the adapter contains or not any element
     * @return true if the list is empty
     */
    public boolean isEmpty() {
        return nodeSortedList.size() <= 0;
    }

    /**
     * Add a node to the list
     * @param node Node to add
     */
    public void addNode(NodeVersioned node) {
        nodeSortedList.add(node);
    }

    /**
     * Remove a node in the list
     * @param node Node to delete
     */
    public void removeNode(NodeVersioned node) {
        nodeSortedList.remove(node);
    }

    /**
     * Update a node in the list
     * @param oldNode Node before the update
     * @param newNode Node after the update
     */
    public void updateNode(NodeVersioned oldNode, NodeVersioned newNode) {
        nodeSortedList.beginBatchedUpdates();
        nodeSortedList.remove(oldNode);
        nodeSortedList.add(newNode);
        nodeSortedList.endBatchedUpdates();
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

    @NonNull
    @Override
    public BasicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        BasicViewHolder basicViewHolder;
        View view;
        if (viewType == Type.GROUP.ordinal()) {
            view = inflater.inflate(R.layout.list_nodes_group, parent, false);
            basicViewHolder = new GroupViewHolder(view);
        } else {
            view = inflater.inflate(R.layout.list_nodes_entry, parent, false);
            basicViewHolder = new EntryViewHolder(view);
        }
        return basicViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull BasicViewHolder holder, int position) {
        NodeVersioned subNode = nodeSortedList.get(position);
        // Assign image
        int iconColor = Color.BLACK;
        switch (subNode.getType()) {
            case GROUP:
                iconColor = iconGroupColor;
                break;
            case ENTRY:
                iconColor = iconEntryColor;
                break;
        }
        database.getDrawFactory().assignDatabaseIconTo(context, holder.icon, subNode.getIcon(), iconColor);
        // Assign text
        holder.text.setText(subNode.getTitle());
        // Assign click
        holder.container.setOnClickListener(
                new OnNodeClickListener(subNode));
        // Context menu
        if (activateContextMenu) {
            holder.container.setOnCreateContextMenuListener(
                    new ContextMenuBuilder(subNode, nodeMenuListener, readOnly));
        }

        // Add username
        holder.subText.setText("");
        holder.subText.setVisibility(View.GONE);
        if (subNode.getType().equals(Type.ENTRY)) {
            EntryVersioned entry = (EntryVersioned) subNode;

            database.startManageEntry(entry);

            holder.text.setText(entry.getVisualTitle());

            String username = entry.getUsername();
            if (showUsernames && !username.isEmpty()) {
                holder.subText.setVisibility(View.VISIBLE);
                holder.subText.setText(username);
            }

            database.stopManageEntry(entry);
        }

        // Assign image and text size
        // Relative size of the icon
        holder.icon.getLayoutParams().height = ((int) iconSize);
        holder.icon.getLayoutParams().width = ((int) iconSize);
        holder.text.setTextSize(textSize);
        holder.subText.setTextSize(subtextSize);
    }

    @Override
    public int getItemCount() {
        return nodeSortedList.size();
    }

    /**
     * Assign a listener when a node is clicked
     */
    public void setOnNodeClickListener(NodeClickCallback nodeClickCallback) {
        this.nodeClickCallback = nodeClickCallback;
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
    public interface NodeClickCallback {
        void onNodeClick(NodeVersioned node);
    }

    /**
     * Menu listener to redefine to do an action in menu
     */
    public interface NodeMenuListener {
        boolean onOpenMenuClick(NodeVersioned node);
        boolean onEditMenuClick(NodeVersioned node);
        boolean onCopyMenuClick(NodeVersioned node);
        boolean onMoveMenuClick(NodeVersioned node);
        boolean onDeleteMenuClick(NodeVersioned node);
    }

    /**
     * Utility class for node listener
     */
    private class OnNodeClickListener implements View.OnClickListener {
        private NodeVersioned node;

        OnNodeClickListener(NodeVersioned node) {
            this.node = node;
        }

        @Override
        public void onClick(View v) {
            if (nodeClickCallback != null)
                nodeClickCallback.onNodeClick(node);
        }
    }

    /**
     * Utility class for menu listener
     */
    private class ContextMenuBuilder implements View.OnCreateContextMenuListener {

        private NodeVersioned node;
        private NodeMenuListener menuListener;
        private boolean readOnly;

        ContextMenuBuilder(NodeVersioned node, NodeMenuListener menuListener, boolean readOnly) {
            this.menuListener = menuListener;
            this.node = node;
            this.readOnly = readOnly;
        }

        @Override
        public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
            menuInflater.inflate(R.menu.node_menu, contextMenu);

            // Opening
            MenuItem menuItem = contextMenu.findItem(R.id.menu_open);
            menuItem.setOnMenuItemClickListener(mOnMyActionClickListener);

			Database database = App.getDB();

            // Edition
            if (readOnly || node.equals(database.getRecycleBin())) {
                contextMenu.removeItem(R.id.menu_edit);
            } else {
                menuItem = contextMenu.findItem(R.id.menu_edit);
                menuItem.setOnMenuItemClickListener(mOnMyActionClickListener);
            }

            // Copy (not for group)
            if (readOnly
                    || isASearchResult
                    || node.equals(database.getRecycleBin())
                    || node.getType().equals(Type.GROUP)) {
                // TODO COPY For Group
                contextMenu.removeItem(R.id.menu_copy);
            } else {
                menuItem = contextMenu.findItem(R.id.menu_copy);
                menuItem.setOnMenuItemClickListener(mOnMyActionClickListener);
            }

            // Move
            if (readOnly
                    || isASearchResult
                    || node.equals(database.getRecycleBin())) {
                contextMenu.removeItem(R.id.menu_move);
            } else {
                menuItem = contextMenu.findItem(R.id.menu_move);
                menuItem.setOnMenuItemClickListener(mOnMyActionClickListener);
            }

            // Deletion
            if (readOnly || node.equals(database.getRecycleBin())) {
                contextMenu.removeItem(R.id.menu_delete);
            } else {
                menuItem = contextMenu.findItem(R.id.menu_delete);
                menuItem.setOnMenuItemClickListener(mOnMyActionClickListener);
            }
        }

        private MenuItem.OnMenuItemClickListener mOnMyActionClickListener = new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (menuListener == null)
                    return false;
                switch ( item.getItemId() ) {
                    case R.id.menu_open:
                        return menuListener.onOpenMenuClick(node);
                    case R.id.menu_edit:
                        return menuListener.onEditMenuClick(node);
                    case R.id.menu_copy:
                        return menuListener.onCopyMenuClick(node);
                    case R.id.menu_move:
                        return menuListener.onMoveMenuClick(node);
                    case R.id.menu_delete:
                        return menuListener.onDeleteMenuClick(node);
                    default:
                        return false;
                }
            }
        };
    }
}
