package com.example.imagesecure;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class HomeFragment extends Fragment {

    private ImageView ivPreview;
    private MaterialButton btnPickImage, btnSubmit;
    private TextInputEditText etPatientName, etAge, etSymptoms, etMedicine;
    private TextView tvResultTitle, tvHashKey, tvSavedInfo;

    private Uri selectedImageUri = null;
    private ActivityResultLauncher<Intent> pickImageLauncher;

    private DatabaseReference blocksRef;
    private StorageReference secondaryStorageRef;
    private String currentUid;

    public HomeFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_home, container, false);

        ivPreview = v.findViewById(R.id.ivPreview);
        btnPickImage = v.findViewById(R.id.btnPickImage);
        btnSubmit = v.findViewById(R.id.btnSubmit);

        etPatientName = v.findViewById(R.id.etPatientName);
        etAge = v.findViewById(R.id.etAge);
        etSymptoms = v.findViewById(R.id.etSymptoms);
        etMedicine = v.findViewById(R.id.etMedicine);

        tvResultTitle = v.findViewById(R.id.tvResultTitle);
        tvHashKey = v.findViewById(R.id.tvHashKey);
        tvSavedInfo = v.findViewById(R.id.tvSavedInfo);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return v;
        }

        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Primary Realtime Database -> store blockchain records doctor-wise
        blocksRef = FirebaseDatabase.getInstance()
                .getReference("medicalBlocks")
                .child(currentUid);

        // Secondary Firebase Storage
        FirebaseApp secondaryApp = FirebaseApp.getInstance(MyApplication.SECONDARY_APP_NAME);
        FirebaseStorage secondaryStorage = FirebaseStorage.getInstance(secondaryApp);
        secondaryStorageRef = secondaryStorage.getReference("medical_records");

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            ivPreview.setImageURI(selectedImageUri);
                        }
                    }
                }
        );

        btnPickImage.setOnClickListener(view -> openGallery());
        btnSubmit.setOnClickListener(view -> validateAndUpload());

        return v;
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    private void validateAndUpload() {
        if (selectedImageUri == null) {
            Toast.makeText(requireContext(), "Please choose an image first.", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = safeText(etPatientName);
        String age = safeText(etAge);
        String symptoms = safeText(etSymptoms);
        String medicine = safeText(etMedicine);

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(age) || TextUtils.isEmpty(symptoms)) {
            Toast.makeText(requireContext(), "Fill Patient Name, Age, and Symptoms.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Uploading...");

        uploadImageToSecondaryStorage(name, age, symptoms, medicine);
    }

    private void uploadImageToSecondaryStorage(String name, String age, String symptoms, String medicine) {
        String fileName = currentUid + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID() + ".jpg";
        StorageReference fileRef = secondaryStorageRef.child(currentUid).child(fileName);

        fileRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot ->
                        fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();
                            saveBlockRecord(name, age, symptoms, medicine, imageUrl);
                        }).addOnFailureListener(e -> {
                            resetSubmitButton();
                            Toast.makeText(requireContext(), "URL fetch failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        })
                )
                .addOnFailureListener(e -> {
                    resetSubmitButton();
                    Toast.makeText(requireContext(), "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveBlockRecord(String name, String age, String symptoms, String medicine, String imageUrl) {
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        Query lastBlockQuery = blocksRef.orderByKey().limitToLast(1);
        lastBlockQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                String prevNodeAddress = "GENESIS";
                String prevHash = "00000000000000000000000000000000";
                String lastNodeKey = null;

                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        MedicalBlock lastBlock = ds.getValue(MedicalBlock.class);
                        if (lastBlock != null) {
                            prevNodeAddress = lastBlock.nodeAddress;
                            prevHash = lastBlock.currentHash;
                            lastNodeKey = ds.getKey();
                        }
                    }
                }

                String newNodeKey = blocksRef.push().getKey();
                if (newNodeKey == null) {
                    resetSubmitButton();
                    Toast.makeText(requireContext(), "Failed to create node.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String payload = "uid=" + currentUid +
                        "|node=" + newNodeKey +
                        "|prevNode=" + prevNodeAddress +
                        "|prevHash=" + prevHash +
                        "|name=" + name +
                        "|age=" + age +
                        "|symptoms=" + symptoms +
                        "|medicine=" + medicine +
                        "|imageUrl=" + imageUrl +
                        "|timestamp=" + time;

                String currentHash = sha256(payload);

                MedicalBlock newBlock = new MedicalBlock(
                        currentUid,
                        newNodeKey,
                        prevNodeAddress,
                        "",
                        prevHash,
                        currentHash,
                        name,
                        age,
                        symptoms,
                        medicine,
                        imageUrl,
                        time,
                        true
                );

                String finalLastNodeKey = lastNodeKey;
                String finalPrevNodeAddress = prevNodeAddress;

                blocksRef.child(newNodeKey).setValue(newBlock)
                        .addOnSuccessListener(unused -> {
                            if (finalLastNodeKey != null) {
                                blocksRef.child(finalLastNodeKey)
                                        .child("nextNodeAddress")
                                        .setValue(newNodeKey);
                            }

                            tvResultTitle.setVisibility(View.VISIBLE);
                            tvHashKey.setVisibility(View.VISIBLE);
                            tvSavedInfo.setVisibility(View.VISIBLE);

                            tvHashKey.setText("SHA-256 Hash Key:\n" + currentHash);

                            tvSavedInfo.setText(
                                    "Saved Successfully ✅\n" +
                                            "Doctor UID: " + currentUid + "\n" +
                                            "Node: " + newNodeKey + "\n" +
                                            "Prev Node: " + finalPrevNodeAddress + "\n" +
                                            "Patient: " + name + " (" + age + ")\n" +
                                            "Image: Uploaded to Secondary Storage\n" +
                                            "Time: " + time
                            );

                            clearInputs();
                            resetSubmitButton();
                            Toast.makeText(requireContext(), "Record uploaded and stored.", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            resetSubmitButton();
                            Toast.makeText(requireContext(), "Database save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                resetSubmitButton();
                Toast.makeText(requireContext(), "Firebase error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resetSubmitButton() {
        btnSubmit.setEnabled(true);
        btnSubmit.setText("Submit Secure Record");
    }

    private void clearInputs() {
        etPatientName.setText("");
        etAge.setText("");
        etSymptoms.setText("");
        etMedicine.setText("");
        selectedImageUri = null;
        ivPreview.setImageResource(android.R.drawable.ic_menu_report_image);
    }

    private String safeText(TextInputEditText et) {
        if (et == null || et.getText() == null) return "";
        return et.getText().toString().trim();
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