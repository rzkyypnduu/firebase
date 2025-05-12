package com.example.firebase;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class InsertNoteActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView tvEmail;
    private TextView tvUid;
    private Button btnKeluar;
    private FirebaseAuth mAuth;
    private EditText etTitle;
    private EditText etDesc;
    private Button btnSubmit;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    Note note;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_insert_note);

        // Initialize Views
        tvEmail = findViewById(R.id.tv_email);
        tvUid = findViewById(R.id.tv_uid);
        btnKeluar = findViewById(R.id.btn_keluar);
        etTitle = findViewById(R.id.et_title);
        etDesc = findViewById(R.id.et_description);
        btnSubmit = findViewById(R.id.btn_submit);

        // Initialize Firebase Auth and Database
        mAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance("https://pamfirebase-5670c-default-rtdb.asia-southeast1.firebasedatabase.app/");
        databaseReference = firebaseDatabase.getReference();

        // Initialize Note
        note = new Note();

        // Set listeners for buttons
        btnKeluar.setOnClickListener(this);
        btnSubmit.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Check if user is signed in (non-null) and update UI accordingly
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            tvEmail.setText(currentUser.getEmail());
            tvUid.setText(currentUser.getUid());
        }
    }

    @Override
    public void onClick(View view) {
        // Replacing switch with if-else for button actions
        if (view.getId() == R.id.btn_keluar) {
            logOut();
        } else if (view.getId() == R.id.btn_submit) {
            submitData();
        }
    }

    // Method to log out and return to MainActivity
    public void logOut() {
        mAuth.signOut();
        Intent intent = new Intent(InsertNoteActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Ensure user can't go back
        startActivity(intent);
    }

    // Method to submit data to Firebase Database
    public void submitData() {
        if (!validateForm()) {
            return;
        }

        String title = etTitle.getText().toString();
        String desc = etDesc.getText().toString();
        Note baru = new Note(title, desc);

        databaseReference.child("notes").child(mAuth.getUid()).push().setValue(baru)
                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(InsertNoteActivity.this, "Add data", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(InsertNoteActivity.this, "Failed to Add data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Method to validate form data
    private boolean validateForm() {
        boolean result = true;
        if (TextUtils.isEmpty(etTitle.getText().toString())) {
            etTitle.setError("Required");
            result = false;
        } else {
            etTitle.setError(null);
        }
        if (TextUtils.isEmpty(etDesc.getText().toString())) {
            etDesc.setError("Required");
            result = false;
        } else {
            etDesc.setError(null);
        }
        return result;
    }
}
