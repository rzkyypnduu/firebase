package com.example.firebase;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class InsertNoteActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView tvEmail;
    private TextView tvUid;
    private TextView tvCurrentData; // TextView untuk menampilkan data catatan yang ada
    private Button btnKeluar;
    private Button btnUpdate;
    private Button btnDelete;
    private Button btnEdit;
    private Button btnPrevious;
    private Button btnNext;
    private FirebaseAuth mAuth;
    private EditText etTitle;
    private EditText etDesc;
    private Button btnSubmit;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private String noteId; // Untuk menyimpan ID catatan yang dipilih
    private Note currentNote;

    // List untuk menyimpan semua catatan dan index saat ini
    private List<Note> noteList = new ArrayList<>();
    private int currentNoteIndex = 0; // Menyimpan index catatan yang sedang ditampilkan

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_insert_note);

        // Initialize Views
        tvEmail = findViewById(R.id.tv_email);
        tvUid = findViewById(R.id.tv_uid);
        tvCurrentData = findViewById(R.id.tv_current_data); // Inisialisasi TextView untuk data catatan
        btnKeluar = findViewById(R.id.btn_keluar);
        etTitle = findViewById(R.id.et_title);
        etDesc = findViewById(R.id.et_description);
        btnSubmit = findViewById(R.id.btn_submit);
        btnUpdate = findViewById(R.id.btn_update);
        btnDelete = findViewById(R.id.btn_delete);
        btnEdit = findViewById(R.id.btn_edit);
        btnPrevious = findViewById(R.id.btn_previous);
        btnNext = findViewById(R.id.btn_next);

        // Initialize Firebase Auth and Database
        mAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance("https://pamfirebase-5670c-default-rtdb.asia-southeast1.firebasedatabase.app/");
        databaseReference = firebaseDatabase.getReference();

        // Set listeners for buttons
        btnKeluar.setOnClickListener(this);
        btnSubmit.setOnClickListener(this);
        btnUpdate.setOnClickListener(this);
        btnDelete.setOnClickListener(this);
        btnEdit.setOnClickListener(this);
        btnPrevious.setOnClickListener(this);
        btnNext.setOnClickListener(this);

        // Initialize EditText to be empty
        etTitle.setText(""); // Ensure title is empty by default
        etDesc.setText("");  // Ensure description is empty by default

        // Get data from Firebase
        readData();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            tvEmail.setText(currentUser.getEmail());
            tvUid.setText(currentUser.getUid());
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_keluar) {
            logOut();
        } else if (view.getId() == R.id.btn_submit) {
            submitData();
        } else if (view.getId() == R.id.btn_update) {
            if (currentNote != null) {
                updateData(noteId, etTitle.getText().toString(), etDesc.getText().toString());
            } else {
                Toast.makeText(this, "No note selected", Toast.LENGTH_SHORT).show();
            }
        } else if (view.getId() == R.id.btn_delete) {
            if (currentNote != null) {
                deleteData(noteId);
            } else {
                Toast.makeText(this, "No note selected", Toast.LENGTH_SHORT).show();
            }
        } else if (view.getId() == R.id.btn_edit) {
            editData();
        } else if (view.getId() == R.id.btn_previous) {
            // Previous Note
            if (currentNoteIndex > 0) {
                currentNoteIndex--;
                currentNote = noteList.get(currentNoteIndex);
                displayCurrentNote();
            } else {
                Toast.makeText(this, "No previous note", Toast.LENGTH_SHORT).show();
            }
        } else if (view.getId() == R.id.btn_next) {
            // Next Note
            if (currentNoteIndex < noteList.size() - 1) {
                currentNoteIndex++;
                currentNote = noteList.get(currentNoteIndex);
                displayCurrentNote();
            } else {
                Toast.makeText(this, "No next note", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Method to log out and return to MainActivity
    public void logOut() {
        mAuth.signOut();
        Intent intent = new Intent(InsertNoteActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    // Method to submit data to Firebase Database (Create)
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
                        Toast.makeText(InsertNoteActivity.this, "Data Added", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(InsertNoteActivity.this, "Failed to Add Data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Method to read data from Firebase
    private void readData() {
        DatabaseReference ref = firebaseDatabase.getReference().child("notes").child(mAuth.getUid());
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                noteList.clear();  // Clear the previous data
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Note note = snapshot.getValue(Note.class);
                    if (note != null) {
                        noteList.add(note);
                    }
                }

                // If data is available, set the first note as the current note
                if (!noteList.isEmpty()) {
                    currentNoteIndex = 0;
                    currentNote = noteList.get(currentNoteIndex);
                    noteId = dataSnapshot.getChildren().iterator().next().getKey(); // Save the note ID
                    displayCurrentNote();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("Firebase", "Error reading data: " + databaseError.getMessage());
            }
        });
    }

    // Method to update the note (Update)
    public void updateData(String noteId, String newTitle, String newDescription) {
        if (noteId == null) {
            Toast.makeText(this, "No note selected", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference ref = firebaseDatabase.getReference().child("notes").child(mAuth.getUid()).child(noteId);
        ref.child("title").setValue(newTitle);
        ref.child("description").setValue(newDescription);
        Toast.makeText(this, "Data Updated", Toast.LENGTH_SHORT).show();
    }

    // Method to delete data (Delete)
    public void deleteData(String noteId) {
        if (noteId == null) {
            Toast.makeText(this, "No note selected", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference ref = firebaseDatabase.getReference().child("notes").child(mAuth.getUid()).child(noteId);
        ref.removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(InsertNoteActivity.this, "Data Deleted", Toast.LENGTH_SHORT).show();
                    tvCurrentData.setText("Current Note: Not Set"); // Reset the current data
                    etTitle.setText("");  // Reset EditText
                    etDesc.setText("");   // Reset EditText
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(InsertNoteActivity.this, "Failed to delete data", Toast.LENGTH_SHORT).show();
                });
    }

    // Method to allow editing of the current data
    public void editData() {
        if (currentNote != null) {
            etTitle.setText(currentNote.getTitle());
            etDesc.setText(currentNote.getDescription());
        }
    }

    // Display current note in the UI
    private void displayCurrentNote() {
        if (currentNote != null) {
            tvCurrentData.setText("Note: (Judul) " + currentNote.getTitle() + " - (Deskripsi) " + currentNote.getDescription());
            etTitle.setText(currentNote.getTitle());
            etDesc.setText(currentNote.getDescription());
        } else {
            tvCurrentData.setText("Current Note: Not Set");
            etTitle.setText("");  // Clear the title field
            etDesc.setText("");   // Clear the description field
        }
    }

    // Validate form fields
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
