package com.kunzisoft.keepass.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.adapters.NodeAdapter;
import com.kunzisoft.keepass.app.App;
import com.kunzisoft.keepass.database.PwDatabase;
import com.kunzisoft.keepass.database.PwGroup;
import com.kunzisoft.keepass.database.PwNode;
import com.kunzisoft.keepass.database.SortNodeEnum;
import com.kunzisoft.keepass.dialogs.SortDialogFragment;
import com.kunzisoft.keepass.settings.PreferencesUtil;
import com.kunzisoft.keepass.stylish.StylishFragment;

public class ListNodesFragment extends StylishFragment implements
        SortDialogFragment.SortSelectionListener {

    private static final String TAG = ListNodesFragment.class.getName();

    private static final String GROUP_KEY = "GROUP_KEY";
    private static final String IS_SEARCH = "IS_SEARCH";

    private NodeAdapter.NodeClickCallback nodeClickCallback;
    private NodeAdapter.NodeMenuListener nodeMenuListener;
    private OnScrollListener onScrollListener;

    private RecyclerView listView;
    private PwGroup currentGroup;
    private NodeAdapter mAdapter;

    private View notFoundView;
    private boolean isASearchResult;

    // Preferences for sorting
    private SharedPreferences prefs;

    private boolean readOnly;

    public static ListNodesFragment newInstance(PwGroup group, boolean readOnly, boolean isASearch) {
        Bundle bundle = new Bundle();
        if (group != null) {
            bundle.putParcelable(GROUP_KEY, group);
        }
        bundle.putBoolean(IS_SEARCH, isASearch);
        ReadOnlyHelper.putReadOnlyInBundle(bundle, readOnly);
        ListNodesFragment listNodesFragment = new ListNodesFragment();
        listNodesFragment.setArguments(bundle);
        return listNodesFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            nodeClickCallback = (NodeAdapter.NodeClickCallback) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(context.toString()
                    + " must implement " + NodeAdapter.NodeClickCallback.class.getName());
        }
        try {
            nodeMenuListener = (NodeAdapter.NodeMenuListener) context;
        } catch (ClassCastException e) {
            nodeMenuListener = null;
            // Context menu can be omit
            Log.w(TAG, context.toString()
                    + " must implement " + NodeAdapter.NodeMenuListener.class.getName());
        }
        try {
            onScrollListener = (OnScrollListener) context;
        } catch (ClassCastException e) {
            onScrollListener = null;
            // Context menu can be omit
            Log.w(TAG, context.toString()
                    + " must implement " + RecyclerView.OnScrollListener.class.getName());
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if ( getActivity() != null ) {
            setHasOptionsMenu(true);

            readOnly = ReadOnlyHelper.retrieveReadOnlyFromInstanceStateOrArguments(savedInstanceState, getArguments());

            if (getArguments() != null) {
                // Contains all the group in element
                if (getArguments().containsKey(GROUP_KEY)) {
                    currentGroup = getArguments().getParcelable(GROUP_KEY);
                }

                if (getArguments().containsKey(IS_SEARCH)) {
                    isASearchResult = getArguments().getBoolean(IS_SEARCH);
                }
            }

            mAdapter = new NodeAdapter(getContextThemed(), getActivity().getMenuInflater());
            mAdapter.setReadOnly(readOnly);
            mAdapter.setIsASearchResult(isASearchResult);
            mAdapter.setOnNodeClickListener(nodeClickCallback);

            if (nodeMenuListener != null) {
                mAdapter.setActivateContextMenu(true);
                mAdapter.setNodeMenuListener(nodeMenuListener);
            }
            prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        ReadOnlyHelper.onSaveInstanceState(outState, readOnly);
        super.onSaveInstanceState(outState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // To apply theme
        View rootView = inflater.cloneInContext(getContextThemed())
                .inflate(R.layout.list_nodes_fragment, container, false);
        listView = rootView.findViewById(R.id.nodes_list);
        notFoundView = rootView.findViewById(R.id.not_found_container);

        if (onScrollListener != null) {
            listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    onScrollListener.onScrolled(dy);
                }
            });
        }

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        rebuildList();

        if (isASearchResult && mAdapter.isEmpty()) {
            // To show the " no search entry found "
            listView.setVisibility(View.GONE);
            notFoundView.setVisibility(View.VISIBLE);
        } else {
            listView.setVisibility(View.VISIBLE);
            notFoundView.setVisibility(View.GONE);
        }
    }

    public void rebuildList() {
        // Add elements to the list
        if (currentGroup != null)
            mAdapter.rebuildList(currentGroup);
        assignListToNodeAdapter(listView);
    }

    protected void assignListToNodeAdapter(RecyclerView recyclerView) {
        recyclerView.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onSortSelected(SortNodeEnum sortNodeEnum, boolean ascending, boolean groupsBefore, boolean recycleBinBottom) {
        // Toggle setting
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(getString(R.string.sort_node_key), sortNodeEnum.name());
        editor.putBoolean(getString(R.string.sort_ascending_key), ascending);
        editor.putBoolean(getString(R.string.sort_group_before_key), groupsBefore);
        editor.putBoolean(getString(R.string.sort_recycle_bin_bottom_key), recycleBinBottom);
        editor.apply();

        // Tell the adapter to refresh it's list
        mAdapter.notifyChangeSort(sortNodeEnum, ascending, groupsBefore);
        mAdapter.rebuildList(currentGroup);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.tree, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch ( item.getItemId() ) {

            case R.id.menu_sort:
                SortDialogFragment sortDialogFragment;

                PwDatabase database = App.getDB().getPwDatabase();
                /*
                // TODO Recycle bin bottom
                if (database.isRecycleBinAvailable() && database.isRecycleBinEnabled()) {
                    sortDialogFragment =
                            SortDialogFragment.getInstance(
                                    PrefsUtil.getListSort(this),
                                    PrefsUtil.getAscendingSort(this),
                                    PrefsUtil.getGroupsBeforeSort(this),
                                    PrefsUtil.getRecycleBinBottomSort(this));
                } else {
                */
                sortDialogFragment =
                        SortDialogFragment.getInstance(
                                PreferencesUtil.getListSort(getContext()),
                                PreferencesUtil.getAscendingSort(getContext()),
                                PreferencesUtil.getGroupsBeforeSort(getContext()));
                //}

                sortDialogFragment.show(getChildFragmentManager(), "sortDialog");
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case EntryEditActivity.ADD_OR_UPDATE_ENTRY_REQUEST_CODE:
                if (resultCode == EntryEditActivity.ADD_ENTRY_RESULT_CODE ||
                        resultCode == EntryEditActivity.UPDATE_ENTRY_RESULT_CODE) {
                    PwNode newNode = data.getParcelableExtra(EntryEditActivity.ADD_OR_UPDATE_ENTRY_KEY);
                    if (newNode != null) {
                        if (resultCode == EntryEditActivity.ADD_ENTRY_RESULT_CODE)
                            mAdapter.addNode(newNode);
                        if (resultCode == EntryEditActivity.UPDATE_ENTRY_RESULT_CODE) {
                            //mAdapter.updateLastNodeRegister(newNode);
                            mAdapter.rebuildList(currentGroup);
                        }
                    } else {
                        Log.e(this.getClass().getName(), "New node can be retrieve in Activity Result");
                    }
                }
                break;
        }
    }

    public boolean isEmpty() {
        return mAdapter == null || mAdapter.getItemCount() <= 0;
    }

    public void addNode(PwNode newNode) {
        mAdapter.addNode(newNode);
    }

    public void updateNode(PwNode oldNode, PwNode newNode) {
        mAdapter.updateNode(oldNode, newNode);
    }

    public void removeNode(PwNode pwNode) {
        mAdapter.removeNode(pwNode);
    }

    public PwGroup getMainGroup() {
        return currentGroup;
    }

    public interface OnScrollListener {

        /**
         * Callback method to be invoked when the RecyclerView has been scrolled. This will be
         * called after the scroll has completed.
         *
         * @param dy The amount of vertical scroll.
         */
        void onScrolled(int dy);
    }
}
