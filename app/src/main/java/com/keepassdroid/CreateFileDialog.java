package com.keepassdroid;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.keepassdroid.utils.UriUtil;
import com.kunzisoft.keepass.R;
import com.nononsenseapps.filepicker.FilePickerActivity;

public class CreateFileDialog extends DialogFragment implements AdapterView.OnItemSelectedListener{

    private final int FILE_CODE = 3853;

    private View rootView;
    private EditText folderPathView;
    private DefinePathDialogListener mListener;

    public interface DefinePathDialogListener {
        void onDefinePathDialogPositiveClick(Uri pathFile);
        void onDefinePathDialogNegativeClick(Uri pathFile);
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        try {
            mListener = (DefinePathDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement " + DefinePathDialogListener.class.getName());
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        rootView = inflater.inflate(R.layout.file_creation, null);
        builder.setView(rootView)
                .setTitle(R.string.create_keepass_file)
                // Add action buttons
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onDefinePathDialogPositiveClick(buildPath());
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onDefinePathDialogNegativeClick(buildPath());
                    }
                });

        // Folder selection
        View browseView = rootView.findViewById(R.id.browse_button);
        folderPathView = (EditText) rootView.findViewById(R.id.folder_path);
        folderPathView.setText(Environment.getExternalStorageDirectory().getPath());
        browseView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getContext(), FilePickerActivity.class);
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true);
                i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_DIR);
                i.putExtra(FilePickerActivity.EXTRA_START_PATH,
                        Environment.getExternalStorageDirectory().getPath());

                startActivityForResult(i, FILE_CODE);
            }
        });


        // Extension
        Spinner spinner = (Spinner) rootView.findViewById(R.id.file_types);
        spinner.setOnItemSelectedListener(this);

        // Spinner Drop down elements
        String[] fileTypes = getResources().getStringArray(R.array.file_types);
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, fileTypes);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(dataAdapter);

        return builder.create();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CODE && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            if (uri != null)
                folderPathView.setText(uri.toString());
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
        String item = adapterView.getItemAtPosition(position).toString();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    private Uri buildPath() {
        // TODO Correct path
        TextView folderPath = (TextView) rootView.findViewById(R.id.folder_path);
        TextView filename = (TextView) rootView.findViewById(R.id.filename);

        Uri path = new Uri.Builder().path(folderPath.getText().toString())
                .appendPath(filename.getText().toString()+".kdbx")
                .build();
        path = UriUtil.translate(getContext(), path);
        return path;
    }
}
