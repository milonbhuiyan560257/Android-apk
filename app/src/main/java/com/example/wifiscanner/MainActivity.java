package com.example.wifiscanner;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private WifiManager wifiManager;
    private ListView listView;
    private Button buttonScan;
    private ArrayList<String> arrayList = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonScan = findViewById(R.id.btnScan);
        listView = findViewById(R.id.wifiList);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, arrayList);
        listView.setAdapter(adapter);

        buttonScan.setOnClickListener(v -> scanWifi());

        // ওয়াইফাই লিস্টের কোনো আইটেমে ক্লিক করলে পাসওয়ার্ড বক্স দেখানোর লিসেনার
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // লিস্ট থেকে ক্লিক করা ওয়াইফাইয়ের নাম (SSID) আলাদা করা হচ্ছে
                String fullText = parent.getItemAtPosition(position).toString();
                final String selectedWifi = fullText.split("\\(")[0].trim();

                // পাসওয়ার্ড ইনপুট নেওয়ার জন্য একটি টেক্সট বক্স তৈরি
                final EditText passwordInput = new EditText(MainActivity.this);
                passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                passwordInput.setHint("পাসওয়ার্ড লিখুন");

                // ডায়ালগ বক্স ইন্টারফেস তৈরি
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(selectedWifi + " এর পাসওয়ার্ড");
                builder.setView(passwordInput);

                // কানেক্ট বাটন
                builder.setPositiveButton("কানেক্ট", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String password = passwordInput.getText().toString();
                        if (!password.isEmpty()) {
                            connectToWifi(selectedWifi, password);
                        } else {
                            Toast.makeText(MainActivity.this, "পাসওয়ার্ড খালি রাখা যাবে না!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                // বাতিল বাটন
                builder.setNegativeButton("বাতিল", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                builder.create().show();
            }
        });
    }

    private void scanWifi() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        arrayList.clear();
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();
        Toast.makeText(this, "স্ক্যানিং শুরু হয়েছে...", Toast.LENGTH_SHORT).show();
    }

    BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                List<ScanResult> results = wifiManager.getScanResults();
                unregisterReceiver(this);

                for (ScanResult scanResult : results) {
                    if (!scanResult.SSID.isEmpty()) {
                        arrayList.add(scanResult.SSID + " (" + scanResult.level + " dBm)");
                    }
                }
                adapter.notifyDataSetChanged();
            }
        }
    };

    // পাসওয়ার্ড দিয়ে ওয়াইফাই কানেক্ট করার মূল মেথড
    private void connectToWifi(String ssid, String password) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // অ্যান্ড্রয়েড ১০ বা তার উপরের সংস্করণের জন্য আধুনিক নিয়ম
            Toast.makeText(this, ssid + " এর সাথে কানেক্ট করার রিকোয়েস্ট পাঠানো হচ্ছে...", Toast.LENGTH_SHORT).show();

            WifiNetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
                    .setSsid(ssid)
                    .setWpa2Passphrase(password)
                    .build();

            NetworkRequest request = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .setNetworkSpecifier(specifier)
                    .build();

            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                connectivityManager.requestNetwork(request, new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(Network network) {
                        super.onAvailable(network);
                        // সিস্টেমকে এই নেটওয়ার্কটি ডিফল্ট হিসেবে ব্যবহার করতে বাধ্য করা
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            connectivityManager.bindProcessToNetwork(network);
                        }
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "সফলভাবে কানেক্ট হয়েছে!", Toast.LENGTH_LONG).show());
                    }

                    @Override
                    public void onUnavailable() {
                        super.onUnavailable();
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "কানেক্ট হতে পারেনি। পাসওয়ার্ড চেক করুন।", Toast.LENGTH_LONG).show());
                    }
                });
            }
        } else {
            // অ্যান্ড্রয়েড ৯ বা তার নিচের সংস্করণের জন্য পুরাতন নিয়ম
            WifiConfiguration wifiConfig = new WifiConfiguration();
            wifiConfig.SSID = String.format("\"%s\"", ssid);
            wifiConfig.preSharedKey = String.format("\"%s\"", password);

            int netId = wifiManager.addNetwork(wifiConfig);
            wifiManager.disconnect();
            wifiManager.enableNetwork(netId, true);
            wifiManager.reconnect();
            Toast.makeText(this, ssid + " এর সাথে কানেক্ট করার চেষ্টা করা হচ্ছে...", Toast.LENGTH_SHORT).show();
        }
    }
}
