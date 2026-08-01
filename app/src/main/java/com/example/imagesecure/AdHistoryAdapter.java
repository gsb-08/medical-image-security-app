package com.example.imagesecure;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.imagesecure.R;

import java.util.List;

public class AdHistoryAdapter extends RecyclerView.Adapter<AdHistoryAdapter.VH> {

    public interface Listener {
        void onCopy(AdRecord r);
        void onDelete(AdRecord r);
    }

    private final Context ctx;
    private final List<AdRecord> list;
    private final Listener listener;

    public AdHistoryAdapter(Context ctx, List<AdRecord> list, Listener listener) {
        this.ctx = ctx;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_ad_history, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        AdRecord r = list.get(position);

        h.txtDate.setText(!TextUtils.isEmpty(r.createdAtText) ? r.createdAtText : "—");
        h.txtHeadline.setText(!TextUtils.isEmpty(r.headline) ? r.headline : "(No headline)");
        h.txtSnippet.setText(!TextUtils.isEmpty(r.fullText) ? r.fullText : "");

        h.btnCopy.setOnClickListener(v -> listener.onCopy(r));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(r));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView txtDate, txtHeadline, txtSnippet;
        Button btnCopy, btnDelete;

        VH(@NonNull View itemView) {
            super(itemView);
            txtDate = itemView.findViewById(R.id.txtHistoryDate);
            txtHeadline = itemView.findViewById(R.id.txtHistoryHeadline);
            txtSnippet = itemView.findViewById(R.id.txtHistorySnippet);
            btnCopy = itemView.findViewById(R.id.btnHistoryCopy);
            btnDelete = itemView.findViewById(R.id.btnHistoryDelete);
        }
    }
}
