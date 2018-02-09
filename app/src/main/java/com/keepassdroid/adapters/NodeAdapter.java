package com.keepassdroid.adapters;

import android.content.Context;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.util.SortedListAdapterCallback;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.keepassdroid.app.App;
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

    private OnNodeClickCallback onNodeClickCallback;
    private NodeMenuListener nodeMenuListener;

    public NodeAdapter(final Context context, PwNode mainNode) {
        this.inflater = LayoutInflater.from(context);
        this.context = context;
        this.textSize = PrefsUtil.getListTextSize(context);

        this.nodeSortedList = new SortedList<>(PwNode.class, new SortedListAdapterCallback<PwNode>(this) {
            @Override public int compare(PwNode item1, PwNode item2) {
                if(PrefsUtil.isListSortByName(context))
                    return item1.compareTo(item2);
                else
                    return item1.compareTo(item2); // TODO Different sort
            }

            @Override public boolean areContentsTheSame(PwNode oldItem, PwNode newItem) {
                return oldItem.equals(newItem);
            }

            @Override public boolean areItemsTheSame(PwNode item1, PwNode item2) {
                return item1.equals(item2);
            }
        });
        this.nodeSortedList.addAll(mainNode.getDirectChildren());
    }

    public void addNode(PwNode node) {
        nodeSortedList.add(node);
    }

    public void removeNode(PwNode node) {
        nodeSortedList.remove(node);
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

    public void setOnNodeClickListener(OnNodeClickCallback onNodeClickCallback) {
        this.onNodeClickCallback = onNodeClickCallback;
    }

    public void setNodeMenuListener(NodeMenuListener nodeMenuListener) {
        this.nodeMenuListener = nodeMenuListener;
    }

    public interface OnNodeClickCallback {
        void onNodeClick(PwNode node);
    }

    public class OnNodeClickListener implements View.OnClickListener {
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

    public interface NodeMenuListener {
        boolean onOpenMenuClick(PwNode node);
        boolean onDeleteMenuClick(PwNode node);
    }

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
