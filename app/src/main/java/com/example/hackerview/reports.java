package com.example.hackerview;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.airbnb.lottie.LottieAnimationView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class reports extends AppCompatActivity {

    private TextView resultText;
    private Button deleteButton;
    private ProgressBar loadingBar;
    private LottieAnimationView scanAnimation, btnAnimation;
    private List<File> tempFilesList = new ArrayList<>();
    private Handler handler = new Handler(Looper.getMainLooper()); // Ensures UI updates on main thread

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reports);

        // Fix layout for immersive mode
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI elements
        resultText = findViewById(R.id.resultText);
        deleteButton = findViewById(R.id.deleteButton);
        loadingBar = findViewById(R.id.loadingBar);
        scanAnimation = findViewById(R.id.scanAnimation);
        btnAnimation = findViewById(R.id.btnAnimation);

        // Hide ProgressBar and delete button initially
        loadingBar.setVisibility(View.GONE);
        deleteButton.setVisibility(View.GONE);
        scanAnimation.setVisibility(View.GONE);

        // Make btnAnimation clickable and start scanning on click
        btnAnimation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startScanning();
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteTempFiles();
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void startScanning() {
        // Hide button animation, show scan animation and progress bar
        btnAnimation.setVisibility(View.GONE);
        scanAnimation.setVisibility(View.VISIBLE);
        scanAnimation.playAnimation();
        loadingBar.setVisibility(View.VISIBLE);
        resultText.setText("Scanning for temporary files...");

        // Simulate scanning delay (2 seconds)
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                scanTempFiles();
            }
        }, 2000);
    }

    private void scanTempFiles() {
        StringBuilder result = new StringBuilder();
        tempFilesList.clear();

        // Scan for temp files in internal and external cache
        searchTempFiles(getCacheDir(), result);
        searchTempFiles(getExternalCacheDir(), result);

        // Prepare result message
        boolean filesFound = !tempFilesList.isEmpty();
        if (filesFound) {
            result.append("\nTotal temp files found: ").append(tempFilesList.size());
        } else {
            result.append("\nNo temporary files found.");
        }

        // Hide scanning animation and show button animation again
        handler.post(new Runnable() {
            @Override
            public void run() {
                loadingBar.setVisibility(View.GONE);
                scanAnimation.setVisibility(View.GONE);
                scanAnimation.pauseAnimation();
                btnAnimation.setVisibility(View.VISIBLE);
                deleteButton.setVisibility(filesFound ? View.VISIBLE : View.GONE);
                resultText.setText(result.toString());
            }
        });
    }

    private void searchTempFiles(File dir, StringBuilder result) {
        if (dir != null && dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().endsWith(".tmp") || file.getName().contains("cache") || file.getName().endsWith(".log")) {
                        tempFilesList.add(file);
                        result.append("\n").append(file.getAbsolutePath());
                    }
                }
            }
        }
    }

    private void deleteTempFiles() {
        if (tempFilesList.isEmpty()) {
            Toast.makeText(this, "No temporary files to delete!", Toast.LENGTH_SHORT).show();
            return;
        }

        int deletedCount = 0;
        for (File file : tempFilesList) {
            if (file.exists() && file.delete()) {
                deletedCount++;
            }
        }

        tempFilesList.clear();
        deleteButton.setVisibility(View.GONE);
        resultText.setText("Deleted " + deletedCount + " temporary files!");
    }
}
