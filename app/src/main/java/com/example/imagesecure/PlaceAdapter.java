package com.example.imagesecure;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;



import java.util.List;

public class PlaceAdapter extends RecyclerView.Adapter<PlaceAdapter.MyViewHolder> {
    Context context;
    List<PlaceModel> list;

    public PlaceAdapter(Context context, List<PlaceModel> list) {
        this.context = context;
        this.list = list;
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtType;

        public MyViewHolder(View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtPlaceName);
            txtType = itemView.findViewById(R.id.txtType);
        }
    }

    @NonNull
    @Override
    public PlaceAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_place, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaceAdapter.MyViewHolder holder, int position) {
        PlaceModel p = list.get(position);
        holder.txtName.setText(p.name);
        holder.txtType.setText(p.type.substring(0, 1).toUpperCase() + p.type.substring(1));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}
