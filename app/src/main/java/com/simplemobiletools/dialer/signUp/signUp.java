package com.simplemobiletools.dialer.signUp;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.simplemobiletools.dialer.R;
import com.simplemobiletools.dialer.activities.MainActivity;
import com.simplemobiletools.dialer.utilities.Constants;
import com.simplemobiletools.dialer.utilities.PreferenceManager;


import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;

public class signUp extends AppCompatActivity {
    private EditText inputFirstName, inputLastName, inputEmail, inputMobile;
    private MaterialButton buttonSignUp;
    private ProgressBar  signUpProgressBar;
    private PreferenceManager preferenceManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        preferenceManager = new PreferenceManager(getApplicationContext());
        inputFirstName = findViewById(R.id.inputFirstName);
        inputLastName = findViewById(R.id.inputLastName);
        inputEmail = findViewById(R.id.inputEmail);
        inputMobile = findViewById(R.id.inputMobile);
        buttonSignUp = findViewById(R.id.buttonSignUp);
        signUpProgressBar = findViewById(R.id.signUpProgressBar);

        buttonSignUp.setOnClickListener(view -> {
            if(inputFirstName.getText().toString().trim().isEmpty()){
                Toast.makeText(signUp.this, "First Name Required", Toast.LENGTH_SHORT).show();
            }else if(inputLastName.getText().toString().trim().isEmpty()){
                Toast.makeText(signUp.this, "Last Name Required", Toast.LENGTH_SHORT).show();
            }else if(!Patterns.EMAIL_ADDRESS.matcher(inputEmail.getText().toString()).matches()){
                Toast.makeText(signUp.this, " Valid Email Required", Toast.LENGTH_SHORT).show();
            }else if(!Patterns.PHONE.matcher(inputMobile.getText().toString()).matches()){
                Toast.makeText(signUp.this, " Valid Mobile Number Required", Toast.LENGTH_SHORT).show();
            }else{
                lookupAndDelete();
            }
        });

    }

    private void signUpUser(){
        buttonSignUp.setVisibility(View.INVISIBLE);
        signUpProgressBar.setVisibility(View.VISIBLE);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        HashMap<String, Object> user  = new HashMap<>();
        user.put(Constants.KEY_FIRST_NAME, inputFirstName.getText().toString());
        user.put(Constants.KEY_LAST_NAME, inputLastName.getText().toString());
        user.put(Constants.KEY_EMAIL, inputEmail.getText().toString());
        user.put(Constants.KEY_MOBILE, inputMobile.getText().toString());
        database.collection(Constants.KEY_COLLECTION_USERS)
            .add(user)
            .addOnSuccessListener(documentReference -> {
                  preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                  preferenceManager.putBoolean(Constants.KEY_PROFILE_PICTURE_SELECTED, false);
                  preferenceManager.putString(Constants.KEY_USER_ID, documentReference.getId());
                  preferenceManager.putString(Constants.KEY_FIRST_NAME, inputFirstName.getText().toString());
                  preferenceManager.putString(Constants.KEY_LAST_NAME, inputLastName.getText().toString());
                  preferenceManager.putString(Constants.KEY_EMAIL, inputEmail.getText().toString());
                  preferenceManager.putString(Constants.KEY_MOBILE, inputMobile.getText().toString());
                  Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                  intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                  startActivity(intent);
                  finish();
            })
            .addOnFailureListener(e -> {
                 signUpProgressBar.setVisibility(View.INVISIBLE);
                 buttonSignUp.setVisibility(View.VISIBLE);
                 Toast.makeText(signUp.this, "Error: Network Connectivity Failure", Toast.LENGTH_SHORT ).show();
            });
    }

    private void lookupAndDelete() {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
            .whereEqualTo(Constants.KEY_MOBILE, inputMobile.getText().toString())
            .get()
            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull @NotNull Task<QuerySnapshot> task) {
                    if(task.isSuccessful() && task.getResult()!=null && task.getResult().getDocuments().size()>0) {
                        List<DocumentSnapshot> snapshots = task.getResult().getDocuments();
                         for(int i=0;i<task.getResult().getDocuments().size();i++){
                             DocumentReference reference = task.getResult().getDocuments().get(i).getReference();
                             reference.delete();
                         }
                         signUpUser();
                    }else {
                        Log.d(Constants.TAG, "No documents to delete");
                        signUpUser();
                    }
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull @NotNull Exception e) {

                }
            });

    }

}
