/*
 * Copyright 2018 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass Libre.
 *
 *  KeePass Libre is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass Libre is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass Libre.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioGroup;

import com.kunzisoft.keepass.database.SortNodeEnum;
import tech.jgross.keepass.R;

public class SortDialogFragment extends DialogFragment {

    private static final String SORT_NODE_ENUM_BUNDLE_KEY = "SORT_NODE_ENUM_BUNDLE_KEY";
    private static final String SORT_ASCENDING_BUNDLE_KEY = "SORT_ASCENDING_BUNDLE_KEY";
    private static final String SORT_GROUPS_BEFORE_BUNDLE_KEY = "SORT_GROUPS_BEFORE_BUNDLE_KEY";
    private static final String SORT_RECYCLE_BIN_BOTTOM_BUNDLE_KEY = "SORT_RECYCLE_BIN_BOTTOM_BUNDLE_KEY";

    private SortSelectionListener mListener;

    private SortNodeEnum sortNodeEnum;
    private @IdRes
    int mCheckedId;
    private boolean mGroupsBefore;
    private boolean mAscending;
    private boolean mRecycleBinBottom;

    private static Bundle buildBundle(SortNodeEnum sortNodeEnum,
                               boolean ascending,
                               boolean groupsBefore) {
        Bundle bundle = new Bundle();
        bundle.putString(SORT_NODE_ENUM_BUNDLE_KEY, sortNodeEnum.name());
        bundle.putBoolean(SORT_ASCENDING_BUNDLE_KEY, ascending);
        bundle.putBoolean(SORT_GROUPS_BEFORE_BUNDLE_KEY, groupsBefore);
        return bundle;
    }

    public static SortDialogFragment getInstance(SortNodeEnum sortNodeEnum,
                                                 boolean ascending,
                                                 boolean groupsBefore) {
        Bundle bundle = buildBundle(sortNodeEnum, ascending, groupsBefore);
        SortDialogFragment fragment = new SortDialogFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    public static SortDialogFragment getInstance(SortNodeEnum sortNodeEnum,
                                                 boolean ascending,
                                                 boolean groupsBefore,
                                                 boolean recycleBinBottom) {
        Bundle bundle = buildBundle(sortNodeEnum, ascending, groupsBefore);
        bundle.putBoolean(SORT_RECYCLE_BIN_BOTTOM_BUNDLE_KEY, recycleBinBottom);
        SortDialogFragment fragment = new SortDialogFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (SortSelectionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement " + SortSelectionListener.class.getName());
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        sortNodeEnum = SortNodeEnum.TITLE;
        mAscending = true;
        mGroupsBefore = true;
        boolean recycleBinAllowed = false;
        mRecycleBinBottom = true;

        if (getArguments() != null) {
            if (getArguments().containsKey(SORT_NODE_ENUM_BUNDLE_KEY))
                sortNodeEnum = SortNodeEnum.valueOf(getArguments().getString(SORT_NODE_ENUM_BUNDLE_KEY));
            if (getArguments().containsKey(SORT_ASCENDING_BUNDLE_KEY))
                mAscending = getArguments().getBoolean(SORT_ASCENDING_BUNDLE_KEY);
            if (getArguments().containsKey(SORT_GROUPS_BEFORE_BUNDLE_KEY))
                mGroupsBefore = getArguments().getBoolean(SORT_GROUPS_BEFORE_BUNDLE_KEY);
            if (getArguments().containsKey(SORT_RECYCLE_BIN_BOTTOM_BUNDLE_KEY)) {
                recycleBinAllowed = true;
                mRecycleBinBottom = getArguments().getBoolean(SORT_RECYCLE_BIN_BOTTOM_BUNDLE_KEY);
            }
        }

        mCheckedId = retrieveViewFromEnum(sortNodeEnum);

        View rootView = inflater.inflate(R.layout.sort_selection, null);
        builder.setTitle(R.string.sort_menu);
        builder.setView(rootView)
                // Add action buttons
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onSortSelected(sortNodeEnum, mAscending, mGroupsBefore, mRecycleBinBottom);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {}
                });

        CompoundButton ascendingView = (CompoundButton) rootView.findViewById(R.id.sort_selection_ascending);
        // Check if is ascending or descending
        ascendingView.setChecked(mAscending);
        ascendingView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mAscending = isChecked;
            }
        });

        CompoundButton groupsBeforeView = (CompoundButton) rootView.findViewById(R.id.sort_selection_groups_before);
        // Check if groups before
        groupsBeforeView.setChecked(mGroupsBefore);
        groupsBeforeView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mGroupsBefore = isChecked;
            }
        });

        CompoundButton recycleBinBottomView = (CompoundButton) rootView.findViewById(R.id.sort_selection_recycle_bin_bottom);
        if (!recycleBinAllowed) {
            recycleBinBottomView.setVisibility(View.GONE);
        } else {
            // Check if recycle bin at the bottom
            recycleBinBottomView.setChecked(mRecycleBinBottom);
            recycleBinBottomView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    mRecycleBinBottom = isChecked;
                }
            });
        }

        RadioGroup sortSelectionRadioGroupView = (RadioGroup) rootView.findViewById(R.id.sort_selection_radio_group);
        // Check value by default
        sortSelectionRadioGroupView.check(mCheckedId);
        sortSelectionRadioGroupView.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                sortNodeEnum = retrieveSortEnumFromViewId(checkedId);
            }
        });

        return builder.create();
    }

    private @IdRes
    int retrieveViewFromEnum(SortNodeEnum sortNodeEnum) {
        switch (sortNodeEnum) {
            case DB:
                return R.id.sort_selection_db;
            default:
            case TITLE:
                return R.id.sort_selection_title;
            case USERNAME:
                return R.id.sort_selection_username;
            case CREATION_TIME:
                return R.id.sort_selection_creation_time;
            case LAST_MODIFY_TIME:
                return R.id.sort_selection_last_modify_time;
            case LAST_ACCESS_TIME:
                return R.id.sort_selection_last_access_time;
        }
    }

    private SortNodeEnum retrieveSortEnumFromViewId(@IdRes int checkedId) {
        SortNodeEnum sortNodeEnum;
        // Change enum
        switch (checkedId) {
            case R.id.sort_selection_db:
                sortNodeEnum = SortNodeEnum.DB;
                break;
            default:
            case R.id.sort_selection_title:
                sortNodeEnum = SortNodeEnum.TITLE;
                break;
            case R.id.sort_selection_username:
                sortNodeEnum = SortNodeEnum.USERNAME;
                break;
            case R.id.sort_selection_creation_time:
                sortNodeEnum = SortNodeEnum.CREATION_TIME;
                break;
            case R.id.sort_selection_last_modify_time:
                sortNodeEnum = SortNodeEnum.LAST_MODIFY_TIME;
                break;
            case R.id.sort_selection_last_access_time:
                sortNodeEnum = SortNodeEnum.LAST_ACCESS_TIME;
                break;
        }
        return sortNodeEnum;
    }

    public interface SortSelectionListener {
        void onSortSelected(SortNodeEnum sortNodeEnum,
                            boolean ascending,
                            boolean groupsBefore,
                            boolean recycleBinBottom);
    }
}
