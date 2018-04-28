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

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.database.PwEncryptionAlgorithm;
import com.kunzisoft.keepass.database.edit.OnFinish;

import java.util.ArrayList;
import java.util.List;

public class DatabaseAlgorithmPreferenceDialogFragmentCompat extends DatabaseSavePreferenceDialogFragmentCompat {

    private AlgorithmAdapter algorithmAdapter;
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

        RecyclerView recyclerView = view.findViewById(R.id.pref_dialog_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        algorithmAdapter = new AlgorithmAdapter();
        recyclerView.setAdapter(algorithmAdapter);

        setAlgorithm(database.getAvailableEncryptionAlgorithm(), database.getEncryptionAlgorithm());
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

        AlgorithmAdapter() {
            this.inflater = LayoutInflater.from(getContext());
            this.algorithms = new ArrayList<>();
            this.algorithmUsed = null;
        }

        @NonNull
        @Override
        public AlgorithmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = inflater.inflate(R.layout.pref_dialog_list_radio_item, parent, false);
            return new AlgorithmViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull AlgorithmViewHolder holder, int position) {
            PwEncryptionAlgorithm algorithm = this.algorithms.get(position);
            holder.radioButton.setText(algorithm.getName(getResources()));
            if (algorithmUsed != null && algorithmUsed.equals(algorithm))
                holder.radioButton.setChecked(true);
            else
                holder.radioButton.setChecked(false);
            holder.radioButton.setOnClickListener(new OnAlgorithmClickListener(algorithm));
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

        void setAlgorithmUsed(PwEncryptionAlgorithm algorithmUsed) {
            this.algorithmUsed = algorithmUsed;
        }
    }

    private class AlgorithmViewHolder extends RecyclerView.ViewHolder {

        RadioButton radioButton;

        public AlgorithmViewHolder(View itemView) {
            super(itemView);

            radioButton = itemView.findViewById(R.id.pref_dialog_list_radio);
        }
    }

    private class OnAlgorithmClickListener implements View.OnClickListener {

        private PwEncryptionAlgorithm algorithmClicked;

        public OnAlgorithmClickListener(PwEncryptionAlgorithm algorithm) {
            this.algorithmClicked = algorithm;
        }

        @Override
        public void onClick(View view) {
            algorithmSelected = algorithmClicked;
            algorithmAdapter.setAlgorithmUsed(algorithmSelected);
            algorithmAdapter.notifyDataSetChanged();
        }
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
                database.assignEncryptionAlgorithm(newAlgorithm);

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
