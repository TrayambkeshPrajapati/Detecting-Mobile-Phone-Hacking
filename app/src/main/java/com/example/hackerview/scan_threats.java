package com.example.hackerview;

import android.annotation.SuppressLint;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.airbnb.lottie.LottieAnimationView;

import java.util.List;

public class scan_threats extends AppCompatActivity {

    private TextView resultText, resultTitle, scanningText;
    private ProgressBar progressBar;
    private CardView resultCard;
    private PackageManager packageManager;
    private LottieAnimationView lottieAnimationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_threats);

        // Initialize UI components
        resultText = findViewById(R.id.result_text);
        resultTitle = findViewById(R.id.result_title);
        scanningText = findViewById(R.id.scanning_text);
        progressBar = findViewById(R.id.progressBar);
        resultCard = findViewById(R.id.result_card);
        lottieAnimationView = findViewById(R.id.scan_animation);
        packageManager = getPackageManager();

        // Start scanning when activity opens
        startScanning();
    }

    private void startScanning() {
        // Update UI for scanning state
        resultTitle.setText("Scanning in progress...");
        resultText.setText("");  // Clear previous results
        progressBar.setVisibility(View.VISIBLE);  // Show progress bar
        resultCard.setVisibility(View.GONE);  // Hide results initially
        scanningText.setVisibility(View.VISIBLE); // Show scanning text
        scanningText.setText("🔍 Scanning your apps...");
        lottieAnimationView.setVisibility(View.VISIBLE); // Ensure animation is visible at start

        // Hide scroll view initially
        resultCard.setVisibility(View.GONE);

        // Simulate a delay for scanning (4 seconds)
        new Handler().postDelayed(this::scanForThreats, 4000);
    }

    @SuppressLint("SetTextI18n")
    private void scanForThreats() {
        List<PackageInfo> installedApps = packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS);
        LinearLayout resultContainer = findViewById(R.id.result_container);
        resultContainer.removeAllViews(); // Clear previous results

        // Define dangerous permissions
        String[] dangerousPermissions = {
                "android.permission.READ_SMS",
                "android.permission.RECEIVE_SMS",
                "android.permission.READ_CONTACTS",
                "android.permission.RECORD_AUDIO",
                "android.permission.CALL_PHONE",
                "android.permission.READ_CALL_LOG"
        };

        int threatCount = 0;

        for (PackageInfo packageInfo : installedApps) {
            if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0 ||
                    packageInfo.packageName.contains("google") ||
                    packageInfo.packageName.contains("android")) {
                continue; // Skip system apps
            }

            if (packageInfo.requestedPermissions != null) {
                int permissionCount = 0;
                for (String permission : packageInfo.requestedPermissions) {
                    for (String dangerousPermission : dangerousPermissions) {
                        if (permission.equals(dangerousPermission)) {
                            permissionCount++;
                        }
                    }
                }

                if (permissionCount >= 3) { // Show apps with many dangerous permissions
                    threatCount++;

                    // Get app name and icon
                    String appName = packageManager.getApplicationLabel(packageInfo.applicationInfo).toString();
                    Drawable appIcon = packageManager.getApplicationIcon(packageInfo.applicationInfo);

                    // Create horizontal layout for icon + app name
                    LinearLayout appItemLayout = new LinearLayout(this);
                    appItemLayout.setOrientation(LinearLayout.HORIZONTAL);
                    appItemLayout.setPadding(8, 8, 8, 8);

                    // App Icon
                    ImageView iconView = new ImageView(this);
                    iconView.setImageDrawable(appIcon);
                    LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(100, 100);
                    iconParams.setMargins(0, 0, 16, 0);
                    iconView.setLayoutParams(iconParams);

                    // App Name Text
                    TextView appTextView = new TextView(this);
                    appTextView.setText("⚠️ " + appName + "\nPermissions: " + permissionCount);
                    appTextView.setTextSize(16f);
                    appTextView.setTextColor(getResources().getColor(android.R.color.black));

                    // Add icon and name to layout
                    appItemLayout.addView(iconView);
                    appItemLayout.addView(appTextView);

                    // Add this app layout to result container
                    resultContainer.addView(appItemLayout);
                }
            }
        }

        // Hide progress, show results
        progressBar.setVisibility(View.GONE);
        scanningText.setVisibility(View.GONE);
        lottieAnimationView.setVisibility(View.GONE);
        resultCard.setVisibility(View.VISIBLE);

        if (threatCount == 0) {
            resultTitle.setText("✅ No Threats Found!");
            resultText.setText("Your device is safe.");
        } else {
            resultTitle.setText("⚠️ Potential Threats Found:");
            resultText.setText(""); // Clear old text since we are using container now
        }
    }
}