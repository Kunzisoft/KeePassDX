package com.keepassdroid.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import tech.jgross.keepass.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link WelcomeFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link WelcomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WelcomeFragment extends Fragment {
    public static final String TAG = WelcomeFragment.class.getSimpleName();
    private OnFragmentInteractionListener mListener;
    private LocalBroadcastManager mLocalBroadcastManager;
    private View.OnTouchListener mDelayHideTouchListener;

    public WelcomeFragment() {
        // Required empty public constructor
    }

    public void setDelayHideTouchListener(View.OnTouchListener delayHideTouchListener) {
        mDelayHideTouchListener = delayHideTouchListener;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment WelcomeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static WelcomeFragment newInstance() {
        Log.d(TAG, "New Instance!");
        WelcomeFragment fragment = new WelcomeFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "CREATED");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_welcome, container, false);
        // Send clicks from fragment to welcome activity
        view.findViewById(R.id.welcome_new)
                .setOnClickListener(welcomeButton -> {
                    mListener.onNewDatabaseClick();
                    mDelayHideTouchListener.onTouch(welcomeButton, null);
                });
        view.findViewById(R.id.welcome_existing)
                .setOnClickListener(welcomeButton -> {
                    mListener.onExistingDatabaseClick();
                    mDelayHideTouchListener.onTouch(welcomeButton, null);
                });
        return view;
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
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
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
        void onNewDatabaseClick();
        void onExistingDatabaseClick();
    }
}
