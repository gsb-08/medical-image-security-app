package com.example.imagesecure;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.RecordViewHolder> {

    public interface OnRecordClickListener {
        void onRecordClick(MedicalBlock block);
        void onDeleteClick(MedicalBlock block);
    }

    private final Context context;
    private final List<MedicalBlock> list;
    private final OnRecordClickListener listener;

    public RecordAdapter(Context context, List<MedicalBlock> list, OnRecordClickListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_record, parent, false);
        return new RecordViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordViewHolder h, int position) {
        MedicalBlock block = list.get(position);

        h.tvPatientName.setText(!TextUtils.isEmpty(block.patientName) ? block.patientName : "Unknown Patient");
        h.tvAge.setText("Age: " + safe(block.age));
        h.tvSymptoms.setText("Symptoms: " + safe(block.symptoms));
        h.tvTimestamp.setText(safe(block.timestamp));
        h.tvNode.setText("Node: " + safe(block.nodeAddress));

        if (block.verified) {
            h.tvStatus.setText("Verified");
            h.tvStatus.setTextColor(Color.parseColor("#15803D"));
        } else {
            h.tvStatus.setText("Not Verified");
            h.tvStatus.setTextColor(Color.parseColor("#DC2626"));
        }

        h.cardMain.setOnClickListener(v -> listener.onRecordClick(block));
        h.ivDelete.setOnClickListener(v -> listener.onDeleteClick(block));
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    static class RecordViewHolder extends RecyclerView.ViewHolder {

        CardView cardMain;
        TextView tvPatientName, tvAge, tvSymptoms, tvTimestamp, tvNode, tvStatus;
        ImageView ivDelete;

        public RecordViewHolder(@NonNull View itemView) {
            super(itemView);
            cardMain = itemView.findViewById(R.id.cardMain);
            tvPatientName = itemView.findViewById(R.id.tvPatientName);
            tvAge = itemView.findViewById(R.id.tvAge);
            tvSymptoms = itemView.findViewById(R.id.tvSymptoms);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvNode = itemView.findViewById(R.id.tvNode);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            ivDelete = itemView.findViewById(R.id.ivDelete);
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}