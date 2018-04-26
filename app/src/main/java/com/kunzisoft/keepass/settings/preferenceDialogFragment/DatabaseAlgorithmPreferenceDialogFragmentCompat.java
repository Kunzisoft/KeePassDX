package com.kunzisoft.keepass.settings.preferenceDialogFragment;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.database.PwEncryptionAlgorithm;
import com.kunzisoft.keepass.database.edit.OnFinish;

import java.util.ArrayList;
import java.util.List;

public class DatabaseAlgorithmPreferenceDialogFragmentCompat extends DatabaseSavePreferenceDialogFragmentCompat {

    private RecyclerView recyclerView;
    private AlgorithmAdapter algorithmAdapter;
    private OnAlgorithmClickCallback onAlgorithmClickCallback;
    private PwEncryptionAlgorithm algorithmSelected;

    public static DatabaseAlgorithmPreferenceDialogFragmentCompat newInstance(
            String key) {
        final DatabaseAlgorithmPreferenceDialogFragmentCompat
                fragment = new DatabaseAlgorithmPreferenceDialogFragmentCompat();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);

        return fragment;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        recyclerView = view.findViewById(R.id.pref_dialog_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        algorithmAdapter = new AlgorithmAdapter();
        recyclerView.setAdapter(algorithmAdapter);
    }

    public void setAlgorithm(List<PwEncryptionAlgorithm> algorithmList, PwEncryptionAlgorithm algorithm) {
        if (algorithmAdapter != null)
            algorithmAdapter.setAlgorithms(algorithmList, algorithm);
        algorithmSelected = algorithm;
    }

    private class AlgorithmAdapter extends RecyclerView.Adapter<AlgorithmViewHolder> {

        private LayoutInflater inflater;

        private List<PwEncryptionAlgorithm> algorithms;
        private PwEncryptionAlgorithm algorithmUsed;
        private OnAlgorithmClickListener onAlgorithmClickListener;

        public AlgorithmAdapter() {
            this.inflater = LayoutInflater.from(getContext());
            this.algorithms = new ArrayList<>();
            this.algorithmUsed = null;
            this.onAlgorithmClickListener = new OnAlgorithmClickListener();
        }

        @NonNull
        @Override
        public AlgorithmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = inflater.inflate(R.layout.list_nodes_group, parent, false);
            return new AlgorithmViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull AlgorithmViewHolder holder, int position) {
            PwEncryptionAlgorithm algorithm = this.algorithms.get(position);
            holder.nameView.setText(algorithm.getName(getResources()));
            if (algorithmUsed != null && algorithmUsed.equals(algorithm))
                holder.radioButton.setChecked(true);
            else
                holder.radioButton.setChecked(false);
            onAlgorithmClickListener.setAlgorithm(algorithm);
            holder.container.setOnClickListener(onAlgorithmClickListener);
        }

        @Override
        public int getItemCount() {
            return algorithms.size();
        }

        public void setAlgorithms(List<PwEncryptionAlgorithm> algorithms, PwEncryptionAlgorithm algorithmUsed) {
            this.algorithms.clear();
            this.algorithms.addAll(algorithms);
            this.algorithmUsed = algorithmUsed;
        }
    }

    private class AlgorithmViewHolder extends RecyclerView.ViewHolder {

        View container;
        RadioButton radioButton;
        TextView nameView;

        public AlgorithmViewHolder(View itemView) {
            super(itemView);

            container = itemView.findViewById(R.id.pref_dialog_list_container);
            radioButton = itemView.findViewById(R.id.pref_dialog_list_radio);
            nameView = itemView.findViewById(R.id.pref_dialog_list_name);
        }
    }

    private interface OnAlgorithmClickCallback {
        void onAlgorithmClick(PwEncryptionAlgorithm algorithm);
    }

    private class OnAlgorithmClickListener implements View.OnClickListener {

        private PwEncryptionAlgorithm algorithmClicked;

        public void setAlgorithm(PwEncryptionAlgorithm algorithm) {
            this.algorithmClicked = algorithm;
        }

        @Override
        public void onClick(View view) {
            if (onAlgorithmClickCallback != null)
                onAlgorithmClickCallback.onAlgorithmClick(algorithmClicked);
            algorithmSelected = algorithmClicked;

            // Close the dialog when an element is clicked
            onDialogClosed(true);
            dismiss();
        }
    }

    public void setOnAlgorithmClickCallback(OnAlgorithmClickCallback onAlgorithmClickCallback) {
        this.onAlgorithmClickCallback = onAlgorithmClickCallback;
    }

    public PwEncryptionAlgorithm getAlgorithmSelected() {
        return algorithmSelected;
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if ( positiveResult ) {
            assert getContext() != null;

            if (getAlgorithmSelected() != null) {
                PwEncryptionAlgorithm newAlgorithm = getAlgorithmSelected();
                PwEncryptionAlgorithm oldAlgorithm = database.getEncryptionAlgorithm();
                database.assignDescription(newAlgorithm.getName(getResources()));

                Handler handler = new Handler();
                setAfterSaveDatabase(new AfterDescriptionSave(getContext(), handler, newAlgorithm, oldAlgorithm));
            }
        }

        super.onDialogClosed(positiveResult);
    }

    private class AfterDescriptionSave extends OnFinish {

        private PwEncryptionAlgorithm mNewAlgorithm;
        private PwEncryptionAlgorithm mOldAlgorithm;
        private Context mCtx;

        AfterDescriptionSave(Context ctx, Handler handler, PwEncryptionAlgorithm newAlgorithm, PwEncryptionAlgorithm oldAlgorithm) {
            super(handler);

            mCtx = ctx;
            mNewAlgorithm = newAlgorithm;
            mOldAlgorithm = oldAlgorithm;
        }

        @Override
        public void run() {
            PwEncryptionAlgorithm algorithmToShow = mNewAlgorithm;

            if (!mSuccess) {
                displayMessage(mCtx);
                database.assignEncryptionAlgorithm(mOldAlgorithm);
            }

            getPreference().setSummary(algorithmToShow.getName(mCtx.getResources()));

            super.run();
        }
    }
}
