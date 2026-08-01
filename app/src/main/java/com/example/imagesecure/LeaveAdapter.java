package com.example.imagesecure;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class LeaveAdapter extends RecyclerView.Adapter<LeaveAdapter.LeaveViewHolder> {

    private ArrayList<LeaveModel> leaveList;

    public LeaveAdapter(ArrayList<LeaveModel> leaveList) {
        this.leaveList = leaveList;
    }

    @NonNull
    @Override
    public LeaveViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leave, parent, false);
        return new LeaveViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LeaveViewHolder holder, int position) {
        LeaveModel leave = leaveList.get(position);

        holder.txtLeaveDate.setText("Date: " + leave.getLeaveDate());
        holder.txtLeaveType.setText("Type: " + leave.getLeaveType());
        holder.txtReason.setText("Reason: " + leave.getReason());
        holder.txtStatus.setText("Status: " + leave.getStatus());

        // Convert the timestamp to a readable applied date
        if (leave.getTimestamp() != null) {
            long timestamp = Long.parseLong(leave.getTimestamp());
            String appliedDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(timestamp));
            holder.txtRequestDate.setText("Applied On: " + appliedDate);
        } else {
            holder.txtRequestDate.setText("Applied On: N/A");
        }

    }

    @Override
    public int getItemCount() {
        return leaveList.size();
    }

    public static class LeaveViewHolder extends RecyclerView.ViewHolder {
        TextView txtLeaveDate, txtLeaveType, txtReason, txtStatus, txtRequestDate;

        public LeaveViewHolder(@NonNull View itemView) {
            super(itemView);

            txtLeaveDate = itemView.findViewById(R.id.txtLeaveDate);
            txtLeaveType = itemView.findViewById(R.id.txtLeaveType);
            txtReason = itemView.findViewById(R.id.txtReason);
            txtStatus = itemView.findViewById(R.id.txtStatus);
            txtRequestDate = itemView.findViewById(R.id.tvTimestamp);
        }
    }
}
