package com.keepassdroid.fragments;

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
 *
 * Use the {@link NewDatabasePasswordFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NewDatabasePasswordFragment extends Fragment {
    public static final String TAG = NewDatabasePasswordFragment.class.getSimpleName();
    public static final String PASSWORD = "PASSWORD";
    public static final String KEYFILE_PATH = "KEYFILE_PATH";

    private View.OnTouchListener mDelayHideTouchListener;
    private OnFragmentInteractionListener mListener;
    private EditText mPasswordEdit;
    private EditText mPasswordConfirmEdit;

    public NewDatabasePasswordFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment NewDatabasePasswordFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static NewDatabasePasswordFragment newInstance() {
        NewDatabasePasswordFragment fragment = new NewDatabasePasswordFragment();
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
        // Inflate the layout for this fragment
        View view = inflater
                .inflate(R.layout.fragment_new_database_password, container, false);
        view.setOnTouchListener(mDelayHideTouchListener);
        mPasswordEdit = view.findViewById(R.id.new_db_password);
        mPasswordConfirmEdit = view.findViewById(R.id.new_db_password_confirm);
        mPasswordEdit.setOnTouchListener(mDelayHideTouchListener);
        mPasswordConfirmEdit.setOnTouchListener(mDelayHideTouchListener);
        return view;
    }

    public String getPassword() {
        return mPasswordEdit.getText().toString();
    }

    public String getPasswordConfirm() {
        return mPasswordConfirmEdit.getText().toString();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof NewDatabaseFragment.OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
//        mLocalBroadcastManager = LocalBroadcastManager.getInstance(context);
//        mFileChoiceReciever = new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                if (intent.getExtras() != null) {
//                    String keyfilePath = intent
//                            .getExtras().getString(IntentExtras.EXTRA_KEYFILE_PATH, "");
//                    if (!keyfilePath.isEmpty()) {
//                        mKeyfilePathEdit.setText(keyfilePath);
//                    }
//                }
//            }
//        };
//        mLocalBroadcastManager.registerReceiver(mFileChoiceReciever,
//                new IntentFilter(IntentExtras.ACTION_NEW_DB_KEYFILE_CHOSEN));
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
//        if (mFileChoiceReciever != null && mLocalBroadcastManager != null) {
//            mLocalBroadcastManager.unregisterReceiver(mFileChoiceReciever);
//        }
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
        void onChooseKeyfile(EditText keyfileEditText);
    }

    public class IntentExtras {
        public static final String ACTION_NEW_DB_KEYFILE_CHOSEN =
                "com.keepassdroid.intent.actions.ACTION_NEW_DB_KEYFILE_CHOSEN";
        public static final String EXTRA_KEYFILE_PATH = "KEYFILE_PATH";
    }
}
