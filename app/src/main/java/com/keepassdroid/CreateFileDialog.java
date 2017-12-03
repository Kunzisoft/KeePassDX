package com.keepassdroid;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.kunzisoft.keepass.R;

public class CreateFileDialog extends DialogFragment implements AdapterView.OnItemSelectedListener{

    private View rootView;
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

        // TODO Add default path
        // TODO Add intent for path selection

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
                .appendPath(filename.getText().toString()).build();
        return path;
    }

}
