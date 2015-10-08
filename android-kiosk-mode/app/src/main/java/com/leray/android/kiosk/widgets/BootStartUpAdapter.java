package com.leray.android.kiosk.widgets;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.leray.android.kiosk.R;
import com.leray.android.kiosk.utils.ShellUtils;

import java.util.List;

/**
 * Created by John on 2015/10/8.
 */
public class BootStartUpAdapter extends RecyclerView.Adapter<BootStartUpAdapter.AppInfoViewHolder> {
    private final LayoutInflater mLayoutInflater;
    private final Context mContext;
    private List<AppModel> list;

    public BootStartUpAdapter(Context context, List<AppModel> list) {
        this.list = list;
        mContext = context;
        mLayoutInflater = LayoutInflater.from(context);
    }

    @Override
    public AppInfoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new AppInfoViewHolder(mLayoutInflater.inflate(R.layout.app_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(AppInfoViewHolder holder, int position) {
        AppModel app = list.get(position);
        String title = app.getLabel();
        holder.mTextView.setText(title);
        holder.imageView.setImageDrawable(app.getIcon());
        holder.itemView.setTag(app.getStartUpReceiver());
        holder.mSwitch.setChecked(app.isBootStartEnabled());
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    public static class AppInfoViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView mTextView;
        Switch mSwitch;

        AppInfoViewHolder(View view) {
            super(view);
            imageView = (ImageView) view.findViewById(R.id.icon);
            mTextView = (TextView) view.findViewById(R.id.title);
            mSwitch = (Switch) view.findViewById(R.id.switch1);
            mSwitch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d("AppInfoViewHolder", "onClick--> position = " + getPosition());
                    boolean isOn = mSwitch.isChecked();
                    String receiver = (String) itemView.getTag();
                    if (TextUtils.isEmpty(receiver)) {
                        return;
                    }
                    if (isOn) {
                        enableApp(receiver);
                    } else {
                        disableApp(receiver);
                    }
                }
            });
        }

        private void enableApp(final String component) {
            new AsyncTask<Void, Void, ShellUtils.CommandResult>() {
                @Override
                protected ShellUtils.CommandResult doInBackground(Void... params) {
                    ShellUtils.CommandResult ret = null;

                    String cmd = "pm enable " + component;
                    Log.d("hello", cmd);
                    String[] cmds = {"su", cmd};
                    ret = ShellUtils.execCommand(cmds, true);
                    return ret;
                }

                @Override
                protected void onPostExecute(ShellUtils.CommandResult commandResult) {
                    if (commandResult == null) {
                        return;
                    }
                    Log.d("hello", " error msg:" + commandResult.errorMsg
                            + "\n success msg: " + commandResult.successMsg);
                }
            }.execute();
        }

        private void disableApp(final String component) {
            new AsyncTask<Void, Void, ShellUtils.CommandResult>() {
                @Override
                protected ShellUtils.CommandResult doInBackground(Void... params) {
                    ShellUtils.CommandResult ret = null;

                    String cmd = "pm disable " + component;
                    Log.d("hello", cmd);
                    String[] cmds = {"su", cmd};
                    ret = ShellUtils.execCommand(cmds, true);
                    return ret;
                }

                @Override
                protected void onPostExecute(ShellUtils.CommandResult commandResult) {
                    if (commandResult == null) {
                        return;
                    }
                    Log.d("hello", " error msg:" + commandResult.errorMsg
                            + "\n success msg: " + commandResult.successMsg);
                }
            }.execute();
        }
    }

}
