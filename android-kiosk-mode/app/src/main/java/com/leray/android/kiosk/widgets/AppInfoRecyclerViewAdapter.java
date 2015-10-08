

package com.leray.android.kiosk.widgets;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.leray.android.kiosk.R;

import java.util.List;

public class AppInfoRecyclerViewAdapter extends RecyclerView.Adapter<AppInfoRecyclerViewAdapter.AppInfoViewHolder> {
    private final LayoutInflater mLayoutInflater;
    private final Context mContext;
    private List<AppInfo> list;

    public AppInfoRecyclerViewAdapter(Context context, List<AppInfo> list) {
        this.list = list;
        mContext = context;
        mLayoutInflater = LayoutInflater.from(context);
    }

    @Override
    public AppInfoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new AppInfoViewHolder(mLayoutInflater.inflate(R.layout.item_view, parent, false));
    }

    @Override
    public void onBindViewHolder(AppInfoViewHolder holder, int position) {
        AppInfo app = list.get(position);
        String title = app.getTitle();
        holder.mTextView.setText(title);
        holder.imageView.setImageDrawable(app.getIcon());
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    public static class AppInfoViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView mTextView;

        AppInfoViewHolder(View view) {
            super(view);
            imageView = (ImageView) view.findViewById(R.id.imageView);
            mTextView = (TextView) view.findViewById(R.id.textView);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d("AppInfoViewHolder", "onClick--> position = " + getPosition());
                }
            });
        }
    }
}