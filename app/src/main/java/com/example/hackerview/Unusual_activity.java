package com.example.hackerview;

import android.annotation.SuppressLint;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.List;

public class Unusual_activity extends AppCompatActivity {

    private TextView statusText;
    private ProgressBar progressBar;
    private LinearLayout resultsContainer;
    private ScrollView scrollViewResults;
    private LottieAnimationView scanningAnimation;
    private final Handler handler = new Handler();

    private static final String API_KEY = "8eb62612dadf5a9e891bc47b1109e30740eb9aae8af0e4037e09af821a023249";

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unusual);

        statusText = findViewById(R.id.statusText);
        progressBar = findViewById(R.id.progressBar);
        resultsContainer = findViewById(R.id.resultsContainer);
        scanningAnimation = findViewById(R.id.scanningAnimation);
        scrollViewResults = findViewById(R.id.scrollViewResults);

        startScanning();
    }

    private void startScanning() {
        scrollViewResults.setVisibility(View.GONE);
        resultsContainer.removeAllViews();

        scanningAnimation.setVisibility(View.VISIBLE);
        scanningAnimation.playAnimation();
        progressBar.setVisibility(View.VISIBLE);
        statusText.setText("Scanning...");

        handler.postDelayed(() -> {
            scanInstalledApps();
            scanningAnimation.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            statusText.setText("Scan Complete");
            scrollViewResults.setVisibility(View.VISIBLE);
        }, 2000);
    }

    private void scanInstalledApps() {
        PackageManager packageManager = getPackageManager();
        List<PackageInfo> installedApps = packageManager.getInstalledPackages(PackageManager.GET_META_DATA);

        for (PackageInfo packageInfo : installedApps) {
            ApplicationInfo appInfo = packageInfo.applicationInfo;

            // Filter only user-installed apps (exclude system/pre-installed apps)
            if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0 ||
                    (appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {

                String appName = appInfo.loadLabel(packageManager).toString();
                Drawable appIcon = appInfo.loadIcon(packageManager);
                String apkHash = getApkSha256(packageInfo);

                if (apkHash != null) {
                    checkVirusTotal(apkHash, appName, appIcon);
                } else {
                    addResultToUI(appName, appIcon, "⚠️ Hashing failed");
                }
            }
        }
    }


    private String getApkSha256(PackageInfo packageInfo) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = packageInfo.applicationInfo.sourceDir.getBytes();
            byte[] hash = digest.digest(buffer);

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            Log.e("Hashing Error", "Unable to hash APK", e);
            return null;
        }
    }

    private void checkVirusTotal(String apkHash, String appName, Drawable appIcon) {
        new Thread(() -> {
            try {
                URL url = new URL("https://www.virustotal.com/api/v3/files/" + apkHash);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("x-apikey", API_KEY);

                int responseCode = conn.getResponseCode();

                if (responseCode == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();

                    boolean isMalicious = response.toString().contains("\"malicious\":1");

                    if (isMalicious) {
                        runOnUiThread(() -> addResultToUI(appName, appIcon, "❌ Malicious"));
                    } else {
                        runOnUiThread(() -> addResultToUI(appName, appIcon, "✅ Safe"));
                    }
                } else {
                    runOnUiThread(() -> addResultToUI(appName, appIcon, "✅ Safe"));
                }
            } catch (Exception e) {
                Log.e("VirusTotal Error", "API Request Failed", e);
                runOnUiThread(() -> addResultToUI(appName, appIcon, "⚠️ Unknown"));
            }
        }).start();
    }

    private void addResultToUI(String appName, Drawable appIcon, String result) {
        LinearLayout appLayout = new LinearLayout(this);
        appLayout.setOrientation(LinearLayout.HORIZONTAL);
        appLayout.setPadding(20, 20, 20, 20);

        ImageView imageView = new ImageView(this);
        imageView.setImageDrawable(appIcon);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(100, 100));

        TextView textView = new TextView(this);
        textView.setText(appName + ": " + result);
        textView.setTextSize(16);
        textView.setPadding(20, 0, 0, 0);

        appLayout.addView(imageView);
        appLayout.addView(textView);

        resultsContainer.addView(appLayout);
    }
}
