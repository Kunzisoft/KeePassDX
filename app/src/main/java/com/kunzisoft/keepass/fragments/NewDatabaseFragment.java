package com.kunzisoft.keepass.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import tech.jgross.keepass.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link NewDatabaseFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link NewDatabaseFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NewDatabaseFragment extends Fragment {
    public static final String TAG = NewDatabaseFragment.class.getSimpleName();

    private LocalBroadcastManager mLocalBroadcastManager;
    private BroadcastReceiver mFolderChoiceReceiver;
    private OnFragmentInteractionListener mListener;
    private View.OnTouchListener mDelayHideTouchListener;
    private EditText mDatabasePathEditText;
    private EditText mDatabaseFilenameEditText;

    public static final String DATABASE_PATH = "DATABASE_PATH";
    public static final String DATABASE_FILENAME = "DATABASE_FILENAME";

    public NewDatabaseFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment NewDatabaseFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static NewDatabaseFragment newInstance() {
        NewDatabaseFragment fragment = new NewDatabaseFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_new_database, container, false);
        // Set on touch listeners so that we can return to full screen
        view.setOnTouchListener(mDelayHideTouchListener);
        mDatabasePathEditText = view.findViewById(R.id.new_db_path);
        mDatabaseFilenameEditText = view.findViewById(R.id.new_db_keyfile_name);
        mDatabasePathEditText.setOnTouchListener(mDelayHideTouchListener);
        mDatabaseFilenameEditText.setOnTouchListener(mDelayHideTouchListener);
        view.findViewById(R.id.new_db_keyfile_folder_button)
                .setOnClickListener(folderButton -> mListener.onChooseFolder(mDatabasePathEditText));
        return view;
    }

    public String getDatabasePath() {
        return mDatabasePathEditText.getText().toString();
    }

    public String getDatabaseFilename() {
        return mDatabaseFilenameEditText.getText().toString();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(context);
        mFolderChoiceReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getExtras() != null) {
                    String folderPath = intent
                            .getExtras().getString(IntentExtras.EXTRA_FOLDER_PATH, "");
                    if (!folderPath.isEmpty()) {
                        mDatabasePathEditText.setText(folderPath);
                    }
                }
            }
        };
        mLocalBroadcastManager.registerReceiver(mFolderChoiceReceiver,
                new IntentFilter(IntentExtras.ACTION_NEW_DB_FOLDER_CHOSEN));
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        if (mFolderChoiceReceiver != null && mLocalBroadcastManager != null) {
            mLocalBroadcastManager.unregisterReceiver(mFolderChoiceReceiver);
        }
    }

    public void setDelayHideTouchListener(View.OnTouchListener delayHideTouchListener) {
        mDelayHideTouchListener = delayHideTouchListener;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onChooseFolder(View folderTextEdit);
    }

    public class IntentExtras {
        public static final String ACTION_NEW_DB_FOLDER_CHOSEN =
                "com.kunzisoft.keepass.intent.actions.ACTION_NEW_DB_FOLDER_CHOSEN";
        public static final String EXTRA_FOLDER_PATH = "FOLDER_PATH";
    }
}
