package com.simplemobiletools.dialer.Activities;

import android.os.Bundle;
import android.os.PersistableBundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.simplemobiletools.dialer.utilities.Constants;
import com.simplemobiletools.dialer.utilities.PreferenceManager;

import java.util.Date;

public class BaseActivity extends AppCompatActivity {
    private DocumentReference documentReference;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager preferenceManager = new PreferenceManager(getApplicationContext());
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        documentReference = database.collection(Constants.KEY_COLLECTION_USERS)
            .document(preferenceManager.getString(Constants.KEY_USER_ID));
    }

    @Override
    protected void onPause() {
        super.onPause();
        documentReference.update(Constants.KEY_AVAILABILITY, 0);
        documentReference.update(Constants.KEY_LAST_SEEN, new Date());
    }

    @Override
    protected void onStart() {
        super.onStart();
        documentReference.update(Constants.KEY_AVAILABILITY, 1);

    }
}


