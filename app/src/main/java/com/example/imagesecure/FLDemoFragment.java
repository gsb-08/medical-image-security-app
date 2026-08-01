package com.example.imagesecure;

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

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FLDemoFragment extends Fragment {

    private TextView tvDoctorUid, tvLocalCount, tvRound, tvResult;
    private MaterialButton btnSubmitLocal, btnAggregate;
    private BarChart barChartWeights;
    private LineChart lineChartMetrics;

    private String currentUid;
    private String roundId;
    private DatabaseReference recordsRef;
    private DatabaseReference flRef;
    private FederatedLearningManager flManager;

    public FLDemoFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_f_l_demo, container, false);

        tvDoctorUid = v.findViewById(R.id.tvDoctorUid);
        tvLocalCount = v.findViewById(R.id.tvLocalCount);
        tvRound = v.findViewById(R.id.tvRound);
        tvResult = v.findViewById(R.id.tvResult);
        btnSubmitLocal = v.findViewById(R.id.btnSubmitLocal);
        btnAggregate = v.findViewById(R.id.btnAggregate);
        barChartWeights = v.findViewById(R.id.barChartWeights);
        lineChartMetrics = v.findViewById(R.id.lineChartMetrics);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return v;
        }

        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        roundId = "round_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());

        recordsRef = FirebaseDatabase.getInstance().getReference("medicalBlocks").child(currentUid);
        flRef = FirebaseDatabase.getInstance().getReference("federatedLearning");
        flManager = new FederatedLearningManager();

        tvDoctorUid.setText("Doctor UID: " + currentUid);
        tvRound.setText("Round ID: " + roundId);

        setupBarChart();
        setupLineChart();
        loadRecordCount();

        btnSubmitLocal.setOnClickListener(view -> loadAndSubmitLocalUpdate());
        btnAggregate.setOnClickListener(view -> aggregateRound());

        return v;
    }

    private void loadRecordCount() {
        recordsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int count = (int) snapshot.getChildrenCount();
                tvLocalCount.setText("Local Records: " + count);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAndSubmitLocalUpdate() {
        recordsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int count = (int) snapshot.getChildrenCount();

                flManager.submitLocalUpdate(roundId, currentUid, count, new FederatedLearningManager.FLCallback() {
                    @Override
                    public void onSuccess(String msg) {
                        tvResult.setText("Local client update submitted.\nDoctor UID: " + currentUid + "\nRecords used: " + count);
                        loadClientMetricsGraph();
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(String err) {
                        Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void aggregateRound() {
        flManager.aggregateGlobalModel(roundId, new FederatedLearningManager.FLCallback() {
            @Override
            public void onSuccess(String msg) {
                loadGlobalModel();
                loadClientMetricsGraph();
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String err) {
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadGlobalModel() {
        flRef.child(roundId).child("globalModel").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                float w1 = getFloat(snapshot.child("w1").getValue());
                float w2 = getFloat(snapshot.child("w2").getValue());
                float w3 = getFloat(snapshot.child("w3").getValue());
                String clientCount = String.valueOf(snapshot.child("clientCount").getValue());

                tvResult.setText(
                        "Federated Learning Demo Completed ✅\n\n" +
                                "Global Model:\n" +
                                "w1 = " + w1 + "\n" +
                                "w2 = " + w2 + "\n" +
                                "w3 = " + w3 + "\n\n" +
                                "Aggregated Clients: " + safe(clientCount)
                );

                updateBarChart(w1, w2, w3);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadClientMetricsGraph() {
        flRef.child(roundId).child("clients").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Entry> accuracyEntries = new ArrayList<>();
                List<Entry> lossEntries = new ArrayList<>();
                List<String> labels = new ArrayList<>();

                int index = 0;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Double acc = ds.child("localAccuracy").getValue(Double.class);
                    Double loss = ds.child("localLoss").getValue(Double.class);
                    String uid = ds.getKey();

                    labels.add(uid != null && uid.length() > 6 ? uid.substring(0, 6) : "C" + (index + 1));

                    accuracyEntries.add(new Entry(index, acc != null ? acc.floatValue() : 0f));
                    lossEntries.add(new Entry(index, loss != null ? loss.floatValue() : 0f));
                    index++;
                }

                updateLineChart(accuracyEntries, lossEntries, labels);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupBarChart() {
        Description description = new Description();
        description.setText("Global Weights");
        barChartWeights.setDescription(description);
        barChartWeights.setFitBars(true);
        barChartWeights.animateY(800);

        XAxis xAxis = barChartWeights.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);

        YAxis leftAxis = barChartWeights.getAxisLeft();
        leftAxis.setAxisMinimum(0f);

        barChartWeights.getAxisRight().setEnabled(false);

        Legend legend = barChartWeights.getLegend();
        legend.setEnabled(true);
    }

    private void updateBarChart(float w1, float w2, float w3) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, w1));
        entries.add(new BarEntry(1, w2));
        entries.add(new BarEntry(2, w3));

        BarDataSet dataSet = new BarDataSet(entries, "Global Model Weights");
        dataSet.setValueTextSize(12f);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.5f);

        barChartWeights.setData(data);
        barChartWeights.getXAxis().setValueFormatter(
                new IndexAxisValueFormatter(new String[]{"w1", "w2", "w3"})
        );
        barChartWeights.invalidate();
    }

    private void setupLineChart() {
        Description description = new Description();
        description.setText("Client Metrics");
        lineChartMetrics.setDescription(description);
        lineChartMetrics.animateX(800);

        XAxis xAxis = lineChartMetrics.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);

        YAxis leftAxis = lineChartMetrics.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(1f);

        lineChartMetrics.getAxisRight().setEnabled(false);

        Legend legend = lineChartMetrics.getLegend();
        legend.setEnabled(true);
    }

    private void updateLineChart(List<Entry> accuracyEntries, List<Entry> lossEntries, List<String> labels) {
        LineDataSet accSet = new LineDataSet(accuracyEntries, "Local Accuracy");
        accSet.setCircleRadius(4f);
        accSet.setLineWidth(2f);
        accSet.setValueTextSize(10f);

        LineDataSet lossSet = new LineDataSet(lossEntries, "Local Loss");
        lossSet.setCircleRadius(4f);
        lossSet.setLineWidth(2f);
        lossSet.setValueTextSize(10f);

        LineData lineData = new LineData(accSet, lossSet);
        lineChartMetrics.setData(lineData);

        XAxis xAxis = lineChartMetrics.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelCount(labels.size(), false);

        lineChartMetrics.invalidate();
    }

    private float getFloat(Object value) {
        if (value == null) return 0f;
        try {
            return Float.parseFloat(String.valueOf(value));
        } catch (Exception e) {
            return 0f;
        }
    }

    private String safe(String s) {
        return TextUtils.isEmpty(s) || "null".equalsIgnoreCase(s) ? "-" : s;
    }
}