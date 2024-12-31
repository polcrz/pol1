package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.Model.LogEntry;
import com.example.myapplication.R;

import java.util.List;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {

    private List<LogEntry> logList;

    public LogAdapter(List<LogEntry> logList) {
        this.logList = logList;
    }

    @Override
    public LogViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(LogViewHolder holder, int position) {
        LogEntry log = logList.get(position);
        holder.actionType.setText(log.getActionType());
        holder.productName.setText(log.getProductName());
        holder.user.setText(log.getUser());
        holder.timestamp.setText(String.valueOf(log.getTimestamp()));
    }

    @Override
    public int getItemCount() {
        return logList.size();
    }

    public static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView actionType, productName, user, timestamp;

        public LogViewHolder(View itemView) {
            super(itemView);
            actionType = itemView.findViewById(R.id.actionType);
            productName = itemView.findViewById(R.id.productName);
            user = itemView.findViewById(R.id.user);
            timestamp = itemView.findViewById(R.id.timestamp);
        }
    }
}
