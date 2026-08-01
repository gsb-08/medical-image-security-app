package com.example.imagesecure;

import android.app.Application;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import java.util.List;

public class MyApplication extends Application {

    public static final String SECONDARY_APP_NAME = "secondaryStorageApp";

    @Override
    public void onCreate() {
        super.onCreate();

        List<FirebaseApp> apps = FirebaseApp.getApps(this);
        boolean alreadyExists = false;

        for (FirebaseApp app : apps) {
            if (SECONDARY_APP_NAME.equals(app.getName())) {
                alreadyExists = true;
                break;
            }
        }

        if (!alreadyExists) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApplicationId(getString(R.string.secondary_app_id))
                    .setApiKey(getString(R.string.secondary_api_key))
                    .setDatabaseUrl(getString(R.string.secondary_db_url))
                    .setProjectId(getString(R.string.secondary_project_id))
                    .setStorageBucket(getString(R.string.secondary_storage_bucket))
                    .build();

            FirebaseApp.initializeApp(this, options, SECONDARY_APP_NAME);
        }
    }
}
