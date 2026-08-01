package com.example.imagesecure;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecordFragment extends Fragment implements RecordAdapter.OnRecordClickListener {

    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private RecordAdapter adapter;
    private final List<MedicalBlock> recordList = new ArrayList<>();

    private DatabaseReference blocksRef;
    private String currentUid;

    public RecordFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_record, container, false);

        recyclerView = v.findViewById(R.id.recyclerViewRecords);
        tvEmpty = v.findViewById(R.id.tvEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new RecordAdapter(requireContext(), recordList, this);
        recyclerView.setAdapter(adapter);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return v;
        }

        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        blocksRef = FirebaseDatabase.getInstance()
                .getReference("medicalBlocks")
                .child(currentUid);

        loadRecords();

        return v;
    }

    private void loadRecords() {
        blocksRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                recordList.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    MedicalBlock block = ds.getValue(MedicalBlock.class);
                    if (block != null) {
                        recordList.add(block);
                    }
                }

                Collections.reverse(recordList);
                adapter.notifyDataSetChanged();

                if (recordList.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRecordClick(MedicalBlock block) {
        showDetailsDialog(block);
    }

    @Override
    public void onDeleteClick(MedicalBlock block) {
        showDeleteDialog(block);
    }

    private void showDeleteDialog(MedicalBlock block) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Record")
                .setMessage("Are you sure you want to delete this record?")
                .setPositiveButton("Delete", (dialog, which) -> deleteRecord(block))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteRecord(MedicalBlock block) {
        if (block == null || TextUtils.isEmpty(block.nodeAddress)) {
            Toast.makeText(requireContext(), "Invalid record.", Toast.LENGTH_SHORT).show();
            return;
        }

        blocksRef.child(block.nodeAddress).removeValue()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(requireContext(), "Record deleted.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showDetailsDialog(MedicalBlock b) {
        View dv = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_record_details, null);

        TextView tvNode = dv.findViewById(R.id.tvNode);
        TextView tvPrevNode = dv.findViewById(R.id.tvPrevNode);
        TextView tvNextNode = dv.findViewById(R.id.tvNextNode);
        TextView tvPrevHash = dv.findViewById(R.id.tvPrevHash);
        TextView tvCurrentHash = dv.findViewById(R.id.tvCurrentHash);
        TextView tvPatient = dv.findViewById(R.id.tvPatient);
        TextView tvAge = dv.findViewById(R.id.tvAge);
        TextView tvSymptoms = dv.findViewById(R.id.tvSymptoms);
        TextView tvMedicine = dv.findViewById(R.id.tvMedicine);
        TextView tvImageUri = dv.findViewById(R.id.tvImageUri);
        TextView tvTimestamp = dv.findViewById(R.id.tvTimestamp);
        TextView tvVerified = dv.findViewById(R.id.tvVerified);
        TextView tvDoctorUid = dv.findViewById(R.id.tvDoctorUid);
        MaterialButton btnDelete = dv.findViewById(R.id.btnDeleteRecord);
        MaterialButton btnClose = dv.findViewById(R.id.btnCloseRecord);

        tvNode.setText(safe(b.nodeAddress));
        tvPrevNode.setText(safe(b.prevNodeAddress));
        tvNextNode.setText(safe(b.nextNodeAddress));
        tvPrevHash.setText(safe(b.prevHash));
        tvCurrentHash.setText(safe(b.currentHash));
        tvPatient.setText(safe(b.patientName));
        tvAge.setText(safe(b.age));
        tvSymptoms.setText(safe(b.symptoms));
        tvMedicine.setText(safe(b.medicine));
        tvImageUri.setText(safe(b.imageUrl));   // cloud storage url
        tvTimestamp.setText(safe(b.timestamp));
        tvVerified.setText(b.verified ? "Verified ✅" : "Not Verified ❌");
        tvDoctorUid.setText(safe(b.doctorUid));

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dv)
                .create();

        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnDelete.setOnClickListener(v -> {
            dialog.dismiss();
            showDeleteDialog(b);
        });

        dialog.show();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}