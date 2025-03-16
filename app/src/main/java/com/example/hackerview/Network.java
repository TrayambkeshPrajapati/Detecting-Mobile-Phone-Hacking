package com.example.hackerview;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;

public class Network extends AppCompatActivity {

    private TextView networkStatusTextView;
    private WifiManager wifiManager;
    private static final int LOCATION_PERMISSION_REQUEST = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_network);

        networkStatusTextView = findViewById(R.id.networkStatus);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
            } else {
                checkInternetStatus();
            }
        } else {
            checkInternetStatus();
        }
    }

    private void checkInternetStatus() {
        String connectionType = getConnectionType();
        networkStatusTextView.setText("📡 Connection Type: " + connectionType + "\n");

        if (isConnectedToInternet()) {
            new InternetSecurityCheckTask().execute();
        } else {
            networkStatusTextView.append("\n❌ No Internet Connection.");
        }
    }

    private String getConnectionType() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return "Unknown";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (cm.getActiveNetwork() == null) return "No Internet";

            NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
            if (capabilities != null) {
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return "Wi-Fi";
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return "Mobile Data";
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return "Ethernet";
            }
        } else {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if (activeNetwork != null) {
                if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) return "Wi-Fi";
                if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) return "Mobile Data";
                if (activeNetwork.getType() == ConnectivityManager.TYPE_ETHERNET) return "Ethernet";
            }
        }
        return "Unknown";
    }

    private boolean isConnectedToInternet() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (cm.getActiveNetwork() == null) return false;
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return capabilities != null && (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET));
        } else {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
    }

    private class InternetSecurityCheckTask extends AsyncTask<Void, String, String> {
        @Override
        protected String doInBackground(Void... voids) {
            StringBuilder securityReport = new StringBuilder("\n🔹 Checking Internet Security...");

            if (wifiManager != null) {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                String ssid = wifiInfo.getSSID();
                securityReport.append("\n📶 Connected to: ").append(ssid);
            }

            securityReport.append("\n🔹 Checking gateway security...");
            if (isGatewayReachable()) {
                securityReport.append("\n✅ Gateway is reachable.");
            } else {
                securityReport.append("\n❌ Gateway unreachable. Possible security risk.");
            }

            securityReport.append("\n🔹 Scanning common ports (21, 22, 23, 80, 443, 8080)...");

            int[] commonPorts = {21, 22, 23, 80, 443, 8080};
            for (int port : commonPorts) {
                if (isPortOpen(port)) {
                    securityReport.append("\n⚠️ Port ").append(port).append(" is open. Possible vulnerability.");
                } else {
                    securityReport.append("\n✅ Port ").append(port).append(" is closed.");
                }
            }

            securityReport.append("\n🔹 Checking HTTPS Security...");
            if (isHttpsSecure()) {
                securityReport.append("\n✅ Internet uses secure HTTPS.");
            } else {
                securityReport.append("\n⚠️ Internet may not be secure (HTTPS check failed).");
            }

            return securityReport.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            networkStatusTextView.append(result);
        }

        private boolean isGatewayReachable() {
            try {
                InetAddress gateway = InetAddress.getByName(getGatewayAddress());
                return gateway.isReachable(3000);
            } catch (IOException e) {
                Log.e("Network", "Error checking gateway reachability", e);
                return false;
            }
        }

        private String getGatewayAddress() {
            int ip = wifiManager.getDhcpInfo().gateway;
            return ((ip & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." + ((ip >> 16) & 0xFF) + "." + ((ip >>> 24) & 0xFF));
        }

        private boolean isPortOpen(int port) {
            try (Socket socket = new Socket(getGatewayAddress(), port)) {
                return true;
            } catch (IOException e) {
                return false;
            }
        }

        private boolean isHttpsSecure() {
            try {
                URL url = new URL("https://www.google.com");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(3000);
                connection.connect();

                int responseCode = connection.getResponseCode();
                return (responseCode >= 200 && responseCode < 400);
            } catch (IOException e) {
                Log.e("Network", "Error checking HTTPS security", e);
                return false;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkInternetStatus();
            } else {
                networkStatusTextView.setText("Permission denied. Cannot access Wi-Fi info.");
            }
        }
    }
}