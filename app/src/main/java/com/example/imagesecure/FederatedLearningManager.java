package com.example.imagesecure;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class FederatedLearningManager {

    private final DatabaseReference flRef;
    private final Random random = new Random();

    public interface FLCallback {
        void onSuccess(String msg);
        void onFailure(String err);
    }

    public FederatedLearningManager() {
        flRef = FirebaseDatabase.getInstance().getReference("federatedLearning");
    }

    public void submitLocalUpdate(String roundId, String doctorUid, int localRecordCount, FLCallback callback) {
        if (doctorUid == null || doctorUid.isEmpty()) {
            callback.onFailure("Invalid doctor UID");
            return;
        }

        Map<String, Double> weights = generateLocalWeights(localRecordCount);

        double accuracy = 0.70 + (random.nextDouble() * 0.25); // 0.70 to 0.95
        double loss = 0.10 + (random.nextDouble() * 0.40);     // 0.10 to 0.50

        FLClientUpdate update = new FLClientUpdate(
                doctorUid,
                round2(accuracy),
                round2(loss),
                weights,
                System.currentTimeMillis()
        );

        flRef.child(roundId)
                .child("clients")
                .child(doctorUid)
                .setValue(update)
                .addOnSuccessListener(unused -> callback.onSuccess("Local update submitted"))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    private Map<String, Double> generateLocalWeights(int recordCount) {
        double factor = Math.max(1, recordCount) * 0.01;

        Map<String, Double> map = new HashMap<>();
        map.put("w1", round4(0.40 + random.nextDouble() * 0.20 + factor));
        map.put("w2", round4(0.30 + random.nextDouble() * 0.20 + factor));
        map.put("w3", round4(0.20 + random.nextDouble() * 0.20 + factor));
        return map;
    }

    public void aggregateGlobalModel(String roundId, FLCallback callback) {
        flRef.child(roundId).child("clients").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int count = 0;
                double sumW1 = 0, sumW2 = 0, sumW3 = 0;

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Double w1 = ds.child("weights").child("w1").getValue(Double.class);
                    Double w2 = ds.child("weights").child("w2").getValue(Double.class);
                    Double w3 = ds.child("weights").child("w3").getValue(Double.class);

                    if (w1 != null && w2 != null && w3 != null) {
                        sumW1 += w1;
                        sumW2 += w2;
                        sumW3 += w3;
                        count++;
                    }
                }

                if (count == 0) {
                    callback.onFailure("No client updates found");
                    return;
                }

                Map<String, Object> globalModel = new HashMap<>();
                globalModel.put("w1", round4(sumW1 / count));
                globalModel.put("w2", round4(sumW2 / count));
                globalModel.put("w3", round4(sumW3 / count));
                globalModel.put("clientCount", count);
                globalModel.put("aggregatedAt", System.currentTimeMillis());

                int finalCount = count;
                flRef.child(roundId).child("globalModel").setValue(globalModel)
                        .addOnSuccessListener(unused ->
                                flRef.child(roundId).child("status").setValue("completed")
                                        .addOnSuccessListener(unused2 ->
                                                callback.onSuccess("Global model aggregated from " + finalCount + " clients"))
                                        .addOnFailureListener(e -> callback.onFailure(e.getMessage()))
                        )
                        .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailure(error.getMessage());
            }
        });
    }

    private double round2(double v) {
        return Double.parseDouble(String.format(Locale.US, "%.2f", v));
    }

    private double round4(double v) {
        return Double.parseDouble(String.format(Locale.US, "%.4f", v));
    }
}