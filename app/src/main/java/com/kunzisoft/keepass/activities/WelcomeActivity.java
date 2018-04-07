package com.kunzisoft.keepass.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

import com.kunzisoft.keepass.fileselect.FilePickerStylishActivity;
import com.kunzisoft.keepass.fileselect.FileSelectActivity;
import com.kunzisoft.keepass.fileselect.KeyFileHelper;
import com.kunzisoft.keepass.fragments.NewDatabaseFragment;
import com.kunzisoft.keepass.fragments.NewDatabasePasswordFragment;
import com.kunzisoft.keepass.fragments.NewDatabaseWizardFragment;
import com.kunzisoft.keepass.fragments.WelcomeFragment;
import com.nononsenseapps.filepicker.FilePickerActivity;
import com.nononsenseapps.filepicker.Utils;

import java.io.File;

import tech.jgross.keepass.R;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class WelcomeActivity extends AppCompatActivity implements
        WelcomeFragment.OnFragmentInteractionListener,
        NewDatabaseWizardFragment.OnFragmentInteractionListener,
        NewDatabaseFragment.OnFragmentInteractionListener,
        NewDatabasePasswordFragment.OnFragmentInteractionListener {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 2000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private static final int FOLDER_CODE = 1337;
    private Uri mDatabaseFolder;
    private Uri mKeyfile;

    private LocalBroadcastManager mLocalBroadcastManager;
    private static KeyFileHelper mFileHelper;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private final Runnable mShowPart2Runnable = () -> {
        // Delayed display of UI elements
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.show();
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = this::hide;
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                WelcomeActivity.this.delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, WelcomeActivity.class);
        // only to avoid visible flickering when redirecting
        activity.startActivityForResult(intent, 0);
        activity.overridePendingTransition(R.anim.slide_in_from_right, 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_welcome);

        FragmentManager fragmentManager = getSupportFragmentManager();
        WelcomeFragment welcomeFragment = (WelcomeFragment) fragmentManager.findFragmentByTag(WelcomeFragment.TAG);
        if (welcomeFragment == null) {
            welcomeFragment = WelcomeFragment.newInstance();
        }

        fragmentManager
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_in_from_right, R.anim.slide_out_left,
                        R.anim.slide_in_from_left, R.anim.slide_out_right)
                .replace(R.id.welcome_content, welcomeFragment)
                .addToBackStack(WelcomeFragment.TAG)
                .commit();

        welcomeFragment.setDelayHideTouchListener(mDelayHideTouchListener);

        mVisible = true;
        mFileHelper = new KeyFileHelper(this);
        mContentView = findViewById(R.id.fullscreen_content);

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(view -> toggle());
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Override
    public void onBackPressed() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        for (Fragment fragment : fragmentManager.getFragments()) {
            if (fragment.isVisible()) {
                FragmentManager childFragmentManager = fragment.getChildFragmentManager();
                int backStackCount = childFragmentManager.getBackStackEntryCount();
                if (backStackCount > 1) {
                    String tagName = childFragmentManager.getBackStackEntryAt(backStackCount - 2).getName();
                    childFragmentManager.popBackStack();
                    Intent intent = new Intent();
                    intent.setAction(NewDatabaseWizardFragment.IntentExtras
                            .ACTION_STEP_FRAGMENT_CHANGED);
                    intent.putExtra(NewDatabaseWizardFragment.IntentExtras
                            .EXTRA_FRAGMENT_TAG, tagName);
                    mLocalBroadcastManager.sendBroadcast(intent);
                    return;
                } else {
                    WelcomeFragment welcomeFragment = (WelcomeFragment) fragmentManager
                            .findFragmentByTag(WelcomeFragment.TAG);
                    if (welcomeFragment == null) {
                        welcomeFragment = WelcomeFragment.newInstance();
                    }
                    welcomeFragment.setDelayHideTouchListener(mDelayHideTouchListener);

                    NewDatabaseWizardFragment wizardFragment = (NewDatabaseWizardFragment) fragmentManager
                            .findFragmentByTag(NewDatabaseWizardFragment.TAG);
                    if (wizardFragment != null) {
                        fragmentManager
                                .beginTransaction()
                                .setCustomAnimations(R.anim.slide_in_from_right,
                                        R.anim.slide_out_right)
                                .remove(wizardFragment)
                                .commit();
                    }

                    fragmentManager
                            .beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_from_left, R.anim.slide_out_left)
                            .replace(R.id.welcome_content, welcomeFragment, WelcomeFragment.TAG)
                            .commit();
                    return;
                }
            }
        }
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onNewDatabaseClick() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        NewDatabaseWizardFragment newDatabaseWizardFragment =
                (NewDatabaseWizardFragment) fragmentManager.findFragmentByTag(NewDatabaseWizardFragment.TAG);
        if (newDatabaseWizardFragment == null) {
            newDatabaseWizardFragment = NewDatabaseWizardFragment.newInstance();
        }

        fragmentManager
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_in_from_right, R.anim.slide_out_left)
                .replace(R.id.welcome_content, newDatabaseWizardFragment)
                .commit();

        newDatabaseWizardFragment.setDelayHideTouchListener(mDelayHideTouchListener);
    }

    @Override
    public void onExistingDatabaseClick() {
        FileSelectActivity.launch(this);
    }

    @Override
    public void onChooseFolder(View folderEditText) {
        {
            Intent i = new Intent(this, FilePickerStylishActivity.class);
            i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
            i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true);
            i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_DIR);
            i.putExtra(FilePickerActivity.EXTRA_START_PATH,
                    Environment.getExternalStorageDirectory().getPath());
            startActivityForResult(i, FOLDER_CODE);
        }
    }

    @Override
    public void onChooseKeyfile(EditText keyfileEditText) {
        mFileHelper.getOpenFileOnClickViewListener(
                () -> Uri.parse("file://" + keyfileEditText.getText().toString()))
                .onClick(keyfileEditText);
    }

    @Override
    public void onFinishWizard() {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FOLDER_CODE && resultCode == Activity.RESULT_OK) {
            mDatabaseFolder = data.getData();
            if (mDatabaseFolder != null) {
                File file = Utils.getFileForUri(mDatabaseFolder);
                Intent intent = new Intent();
                intent.setAction(NewDatabaseFragment
                        .IntentExtras.ACTION_NEW_DB_FOLDER_CHOSEN);
                intent.putExtra(NewDatabaseFragment.IntentExtras.EXTRA_FOLDER_PATH,
                        file.getPath());
                mLocalBroadcastManager.sendBroadcast(intent);
            }
        } else {
            mFileHelper.onActivityResultCallback(requestCode, resultCode, data,
                    uri -> {
                        if (uri != null) {
                            mKeyfile = uri;
                            Intent intent = new Intent();
                            intent.setAction(NewDatabasePasswordFragment
                                    .IntentExtras.ACTION_NEW_DB_KEYFILE_CHOSEN);
                            intent.putExtra(
                                    NewDatabasePasswordFragment.IntentExtras.EXTRA_KEYFILE_PATH,
                                    mKeyfile.toString());
                            mLocalBroadcastManager.sendBroadcast(intent);
                        }
                    });
        }
        mDelayHideTouchListener.onTouch(null, null);
    }


}
