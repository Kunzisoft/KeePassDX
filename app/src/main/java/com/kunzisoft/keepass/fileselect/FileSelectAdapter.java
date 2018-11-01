/*
 * Copyright 2018 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.fileselect;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.settings.PreferencesUtil;

import java.util.List;

public class FileSelectAdapter extends RecyclerView.Adapter<FileSelectViewHolder> {

    private static final int MENU_CLEAR = 1;

    private Context context;
    private LayoutInflater inflater;
    private List<String> listFiles;
    private FileItemOpenListener fileItemOpenListener;
    private FileSelectClearListener fileSelectClearListener;
    private FileInformationShowListener fileInformationShowListener;

    private @ColorInt
    int defaultColor;
    private @ColorInt
    int warningColor;

    FileSelectAdapter(Context context, List<String> listFiles) {
        this.inflater = LayoutInflater.from(context);
        this.context = context;
        this.listFiles = listFiles;

        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(R.attr.colorAccentCompat, typedValue, true);
        warningColor = typedValue.data;
        theme.resolveAttribute(android.R.attr.textColorHintInverse, typedValue, true);
        defaultColor = typedValue.data;
    }

    @NonNull
    @Override
    public FileSelectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.file_row, parent, false);
        return new FileSelectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileSelectViewHolder holder, int position) {
        FileSelectBean fileSelectBean = new FileSelectBean(context, listFiles.get(position));
        // Context menu creation
        holder.fileContainer.setOnCreateContextMenuListener(new ContextMenuBuilder(fileSelectBean));
        // Click item to open file
        if (fileItemOpenListener != null)
            holder.fileContainer.setOnClickListener(new FileItemClickListener(position));
        // Assign file name
        if (PreferencesUtil.isFullFilePathEnable(context))
            holder.fileName.setText(Uri.decode(fileSelectBean.getFileUri().toString()));
        else
            holder.fileName.setText(fileSelectBean.getFileName());
        holder.fileName.setTextSize(PreferencesUtil.getListTextSize(context));
        // Click on information
        if (fileInformationShowListener != null)
            holder.fileInformation.setOnClickListener(new FileInformationClickListener(fileSelectBean));
    }

    @Override
    public int getItemCount() {
        return listFiles.size();
    }

    void setOnItemClickListener(FileItemOpenListener fileItemOpenListener) {
        this.fileItemOpenListener = fileItemOpenListener;
    }

    void setFileSelectClearListener(FileSelectClearListener fileSelectClearListener) {
        this.fileSelectClearListener = fileSelectClearListener;
    }

    void setFileInformationShowListener(FileInformationShowListener fileInformationShowListener) {
        this.fileInformationShowListener = fileInformationShowListener;
    }

    public interface FileItemOpenListener {
        void onFileItemOpenListener(int itemPosition);
    }

    public interface FileSelectClearListener {
        boolean onFileSelectClearListener(FileSelectBean fileSelectBean);
    }

    public interface FileInformationShowListener {
        void onClickFileInformation(FileSelectBean fileSelectBean);
    }

    private class FileItemClickListener implements View.OnClickListener {

        private int position;

        FileItemClickListener(int position) {
            this.position = position;
        }

        @Override
        public void onClick(View v) {
            fileItemOpenListener.onFileItemOpenListener(position);
        }
    }

    private class FileInformationClickListener implements View.OnClickListener {

        private FileSelectBean fileSelectBean;

        FileInformationClickListener(FileSelectBean fileSelectBean) {
            this.fileSelectBean = fileSelectBean;
        }

        @Override
        public void onClick(View view) {
            fileInformationShowListener.onClickFileInformation(fileSelectBean);
        }
    }

    private class ContextMenuBuilder implements View.OnCreateContextMenuListener {

        private FileSelectBean fileSelectBean;

        ContextMenuBuilder(FileSelectBean fileSelectBean) {
            this.fileSelectBean = fileSelectBean;
        }

        @Override
        public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
            MenuItem clearMenu = contextMenu.add(Menu.NONE, MENU_CLEAR, Menu.NONE, R.string.remove_from_filelist);
            clearMenu.setOnMenuItemClickListener(mOnMyActionClickListener);
        }

        private MenuItem.OnMenuItemClickListener mOnMyActionClickListener = new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (fileSelectClearListener == null)
                    return false;
                switch ( item.getItemId() ) {
                    case MENU_CLEAR:
                        return fileSelectClearListener.onFileSelectClearListener(fileSelectBean);
                    default:
                        return false;
                }
            }
        };
    }
}
