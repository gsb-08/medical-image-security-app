package com.example.imagesecure;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class VerificationFragment extends Fragment {

    private LinearLayout llContainer;
    private MaterialButton btnVerifyAll;
    private TextView tvSummary;

    private DatabaseReference blocksRef;
    private final List<MedicalBlock> blockList = new ArrayList<>();
    private String currentUid;

    public VerificationFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_verification, container, false);

        llContainer = v.findViewById(R.id.llContainer);
        btnVerifyAll = v.findViewById(R.id.btnVerifyAll);
        tvSummary = v.findViewById(R.id.tvSummary);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return v;
        }

        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        blocksRef = FirebaseDatabase.getInstance()
                .getReference("medicalBlocks")
                .child(currentUid);

        btnVerifyAll.setOnClickListener(view -> verifyEntireChain());

        loadAllBlocks();

        return v;
    }

    private void loadAllBlocks() {
        blocksRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                blockList.clear();
                llContainer.removeAllViews();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    MedicalBlock block = ds.getValue(MedicalBlock.class);
                    if (block != null) blockList.add(block);
                }

                renderBlocks();
                tvSummary.setText("Doctor UID: " + currentUid + "\nTotal Nodes: " + blockList.size());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderBlocks() {
        if (getContext() == null) return;

        for (MedicalBlock b : blockList) {
            CardView card = new CardView(requireContext());
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            cardParams.bottomMargin = 24;
            card.setLayoutParams(cardParams);
            card.setRadius(24f);
            card.setCardElevation(6f);
            card.setUseCompatPadding(true);

            LinearLayout inner = new LinearLayout(requireContext());
            inner.setOrientation(LinearLayout.VERTICAL);
            inner.setPadding(32, 32, 32, 32);

            TextView tv = new TextView(requireContext());
            tv.setTextSize(14f);
            tv.setText(
                    "Doctor UID: " + safe(b.doctorUid) + "\n\n" +
                            "Node: " + safe(b.nodeAddress) + "\n\n" +
                            "Prev Node: " + safe(b.prevNodeAddress) + "\n" +
                            "Next Node: " + safe(b.nextNodeAddress) + "\n\n" +
                            "Prev Hash: " + safe(b.prevHash) + "\n\n" +
                            "Current Hash: " + safe(b.currentHash) + "\n\n" +
                            "Patient: " + safe(b.patientName) + " (" + safe(b.age) + ")\n" +
                            "Symptoms: " + safe(b.symptoms) + "\n" +
                            "Medicine: " + safe(b.medicine) + "\n" +
                            "Image URL: " + safe(b.imageUrl) + "\n" +
                            "Time: " + safe(b.timestamp) + "\n\n" +
                            "Verified: " + (b.verified ? "YES ✅" : "NO ❌")
            );

            inner.addView(tv);
            card.addView(inner);
            llContainer.addView(card);
        }
    }

    private void verifyEntireChain() {
        if (blockList.isEmpty()) {
            Toast.makeText(requireContext(), "No blocks found.", Toast.LENGTH_SHORT).show();
            return;
        }

        int validCount = 0;

        for (int i = 0; i < blockList.size(); i++) {
            MedicalBlock current = blockList.get(i);
            boolean valid = true;

            if (i == 0) {
                if (!"GENESIS".equals(current.prevNodeAddress)) valid = false;
                if (!"00000000000000000000000000000000".equals(current.prevHash)) valid = false;
            } else {
                MedicalBlock prev = blockList.get(i - 1);

                if (!safe(current.prevNodeAddress).equals(safe(prev.nodeAddress))) {
                    valid = false;
                }

                if (!safe(current.prevHash).equals(safe(prev.currentHash))) {
                    valid = false;
                }

                if (!TextUtils.isEmpty(prev.nextNodeAddress) &&
                        !safe(prev.nextNodeAddress).equals(safe(current.nodeAddress))) {
                    valid = false;
                }
            }

            String payload = "uid=" + safe(current.doctorUid) +
                    "|node=" + safe(current.nodeAddress) +
                    "|prevNode=" + safe(current.prevNodeAddress) +
                    "|prevHash=" + safe(current.prevHash) +
                    "|name=" + safe(current.patientName) +
                    "|age=" + safe(current.age) +
                    "|symptoms=" + safe(current.symptoms) +
                    "|medicine=" + safe(current.medicine) +
                    "|imageUrl=" + safe(current.imageUrl) +
                    "|timestamp=" + safe(current.timestamp);

            String recalculatedHash = sha256(payload);

            if (!recalculatedHash.equals(safe(current.currentHash))) {
                valid = false;
            }

            current.verified = valid;
            blocksRef.child(current.nodeAddress).child("verified").setValue(valid);

            if (valid) validCount++;
        }

        tvSummary.setText(
                "Doctor UID: " + currentUid +
                        "\nVerification Complete: " + validCount + "/" + blockList.size() + " valid"
        );

        Toast.makeText(requireContext(), "Chain verification completed.", Toast.LENGTH_SHORT).show();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "hash_error";
        }
    }
}