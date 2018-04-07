package com.keepassdroid.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import tech.jgross.keepass.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link NewDatabaseWizardFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NewDatabaseWizardFragment extends Fragment {
    public static final String TAG = NewDatabaseWizardFragment.class.getSimpleName();
    private OnFragmentInteractionListener mListener;
    private String mCurrentStepTag;
    private LocalBroadcastManager mLocalBroadcastManager;
    private BroadcastReceiver mFragmentChangeReceiver;
    private View.OnTouchListener mDelayHideTouchListener;
    private Bundle mStepArgs;
    private TextView mNextText;

    public NewDatabaseWizardFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment NewDatabaseWizardFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static NewDatabaseWizardFragment newInstance() {
        Log.d(TAG, "New instance!");
        NewDatabaseWizardFragment fragment = new NewDatabaseWizardFragment();
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
        if (mStepArgs == null) {
            mStepArgs = new Bundle();
        }

        View view = inflater
                .inflate(R.layout.fragment_new_database_wizard, container, false);

        FragmentManager fragmentManager = getChildFragmentManager();
        NewDatabaseFragment fragment = (NewDatabaseFragment) fragmentManager
                .findFragmentByTag(NewDatabaseFragment.TAG);

        if (fragment == null) {
            fragment = NewDatabaseFragment.newInstance();
        }

        fragment.setDelayHideTouchListener(mDelayHideTouchListener);
        mCurrentStepTag = NewDatabaseFragment.TAG;

        fragmentManager
                .beginTransaction()
                .setCustomAnimations(0, R.anim.slide_out_left,
                        R.anim.slide_in_from_left, R.anim.slide_out_right)
                .replace(R.id.new_database_wizard_content, fragment,
                        NewDatabaseFragment.TAG)
                .addToBackStack(NewDatabaseFragment.TAG)
                .commit();

        mNextText = view.findViewById(R.id.new_db_next);
        mNextText.setOnClickListener(nextButton -> this.onNext());
        view.findViewById(R.id.new_db_next_arrow).setOnClickListener(nextArrow -> this.onNext());
        view.findViewById(R.id.new_db_footer).setOnTouchListener(mDelayHideTouchListener);
        view.setOnTouchListener(mDelayHideTouchListener);
        Log.d(TAG, "CREATE VIEW CALLED");
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "RESUME CALLED");
    }

    private void onNext() {
        FragmentManager fragmentManager = getChildFragmentManager();
        if (mCurrentStepTag.equals(NewDatabaseFragment.TAG)) {
            NewDatabaseFragment currentStep = (NewDatabaseFragment) fragmentManager
                    .findFragmentByTag(mCurrentStepTag);

            String databasePath = currentStep.getDatabasePath();
            String databaseFilename = currentStep.getDatabaseFilename();
            // TODO: Check if path or filename is empty and warn user
            mStepArgs.putString(NewDatabaseFragment.DATABASE_PATH, databasePath);
            mStepArgs.putString(NewDatabaseFragment.DATABASE_FILENAME, databaseFilename);

            NewDatabasePasswordFragment nextStep = (NewDatabasePasswordFragment) fragmentManager
                    .findFragmentByTag(NewDatabasePasswordFragment.TAG);

            if (nextStep == null) {
                nextStep = NewDatabasePasswordFragment.newInstance();
            }
            nextStep.setDelayHideTouchListener(mDelayHideTouchListener);

            fragmentManager
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_from_right, R.anim.slide_out_left,
                            R.anim.slide_in_from_left, R.anim.slide_out_right)
                    .replace(R.id.new_database_wizard_content, nextStep,
                            NewDatabasePasswordFragment.TAG)
                    .addToBackStack(NewDatabasePasswordFragment.TAG)
                    .commit();

            mCurrentStepTag = NewDatabasePasswordFragment.TAG;
            mNextText.setText("SKIP");
        } else if (mCurrentStepTag.equals(NewDatabasePasswordFragment.TAG)) {
            NewDatabasePasswordFragment currentStep =
                    (NewDatabasePasswordFragment) fragmentManager
                            .findFragmentByTag(mCurrentStepTag);

            String password = currentStep.getPassword();
            String passwordConfirm = currentStep.getPasswordConfirm();
            // TODO: Check if password matches and provide feedback if they don't
            mStepArgs.putString(NewDatabasePasswordFragment.PASSWORD, password);


            // TODO: Launch next fragment
        }
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            String dbPath = savedInstanceState.getString(NewDatabaseFragment.DATABASE_PATH);
            String dbFilename = savedInstanceState.getString(NewDatabaseFragment.DATABASE_FILENAME);
            if (dbPath != null) {
                mStepArgs.putString(NewDatabaseFragment.DATABASE_PATH, dbPath);
            }
            if (dbFilename != null) {
                mStepArgs.putString(NewDatabaseFragment.DATABASE_FILENAME, dbFilename);
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putAll(mStepArgs);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof WelcomeFragment.OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
        Log.d(TAG, "ON_ATTACH!");
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(context);
        if (mFragmentChangeReceiver == null) {
            mFragmentChangeReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getExtras() != null) {
                        mCurrentStepTag = intent.getExtras()
                                .getString(IntentExtras.EXTRA_FRAGMENT_TAG, mCurrentStepTag);
                    }
                }
            };
        }
        mLocalBroadcastManager.registerReceiver(mFragmentChangeReceiver,
                new IntentFilter(IntentExtras.ACTION_STEP_FRAGMENT_CHANGED));
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        if (mFragmentChangeReceiver != null && mLocalBroadcastManager != null) {
            mLocalBroadcastManager.unregisterReceiver(mFragmentChangeReceiver);
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
        void onFinishWizard();
    }

    public class IntentExtras {
        public static final String ACTION_RESET_NEXT_TEXT =
                "com.keepassdroid.intent.actions.ACTION_RESET_NEXT_TEXT";
        public static final String ACTION_STEP_FRAGMENT_CHANGED =
                "com.keepassdroid.intent.actions.ACTION_STEP_FRAGMENT_CHANGED";
        public static final String EXTRA_FRAGMENT_TAG = "FRAGMENT_TAG";
    }
}
