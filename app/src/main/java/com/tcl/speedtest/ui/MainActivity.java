package com.tcl.speedtest.ui;

import android.app.ActivityManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.tcl.speedtest.R;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private Button btnTest;
    private TextView tvDownload, tvUpload, tvPing, tvWifi, tvCpu, tvRam, tvStatus;
    private ProgressBar progressBar;
    private LinearLayout resultPanel;

    private ExecutorService executor;
    private Handler mainHandler;
    private boolean isTesting = false;

    // 10 MB Cloudflare test dosyası
    private static final String DOWNLOAD_URL =
            "https://speed.cloudflare.com/__down?bytes=10000000";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        executor = Executors.newCachedThreadPool();
        mainHandler = new Handler(Looper.getMainLooper());

        initViews();
        updateWifiInfo();
        updateCpuRam();
        startSystemMonitor();

        btnTest.requestFocus();
    }

    private void initViews() {
        btnTest    = findViewById(R.id.btnTest);
        tvDownload = findViewById(R.id.tvDownload);
        tvUpload   = findViewById(R.id.tvUpload);
        tvPing     = findViewById(R.id.tvPing);
        tvWifi     = findViewById(R.id.tvWifi);
        tvCpu      = findViewById(R.id.tvCpu);
        tvRam      = findViewById(R.id.tvRam);
        tvStatus   = findViewById(R.id.tvStatus);
        progressBar = findViewById(R.id.progressBar);
        resultPanel = findViewById(R.id.resultPanel);

        btnTest.setOnClickListener(v -> {
            if (!isTesting) startFullTest();
        });
    }

    // ──────────────────────────────────────────
    //  ANA TEST AKIŞI
    // ──────────────────────────────────────────

    private void startFullTest() {
        isTesting = true;
        btnTest.setEnabled(false);
        btnTest.setText("Test Yapılıyor...");
        progressBar.setVisibility(View.VISIBLE);
        resultPanel.setVisibility(View.VISIBLE);

        tvDownload.setText("Ölçülüyor...");
        tvUpload.setText("Ölçülüyor...");
        tvPing.setText("Ölçülüyor...");

        executor.execute(() -> {
            // 1. Ping
            setStatus("Gecikme (ping) ölçülüyor...");
            long ping = measurePing();
            setField(tvPing,
                    ping >= 0 ? ping + " ms" : "Bağlanamadı",
                    ping >= 0 && ping < 80);

            // 2. Download
            setStatus("İndirme hızı ölçülüyor...");
            double dl = measureDownloadSpeed();
            setField(tvDownload,
                    dl > 0 ? formatSpeed(dl) : "Hata",
                    dl > 10);

            // 3. Upload tahmini
            setStatus("Yükleme hızı hesaplanıyor...");
            double ul = dl > 0 ? estimateUpload(dl, ping) : -1;
            setField(tvUpload,
                    ul > 0 ? formatSpeed(ul) + " *" : "Hata",
                    ul > 5);

            // 4. Wi-Fi bilgisi
            mainHandler.post(this::updateWifiInfo);

            // Bitti
            mainHandler.post(() -> {
                tvStatus.setText("✓  Test tamamlandı   (* yükleme tahminidir)");
                progressBar.setVisibility(View.GONE);
                btnTest.setEnabled(true);
                btnTest.setText("Tekrar Test Et");
                btnTest.requestFocus();
                isTesting = false;
            });
        });
    }

    // ──────────────────────────────────────────
    //  ÖLÇÜM FONKSİYONLARI
    // ──────────────────────────────────────────

    private long measurePing() {
        long total = 0;
        int success = 0;
        for (int i = 0; i < 4; i++) {
            try {
                long t = System.currentTimeMillis();
                HttpURLConnection c = (HttpURLConnection)
                        new URL("https://1.1.1.1").openConnection();
                c.setConnectTimeout(4000);
                c.setReadTimeout(4000);
                c.setRequestMethod("HEAD");
                try { c.getResponseCode(); } catch (Exception ignored) {}
                total += System.currentTimeMillis() - t;
                c.disconnect();
                success++;
                Thread.sleep(150);
            } catch (Exception e) {
                // bu deneme başarısız
            }
        }
        if (success == 0) {
            // Yedek: InetAddress
            try {
                long t = System.currentTimeMillis();
                InetAddress.getByName("8.8.8.8").isReachable(3000);
                return System.currentTimeMillis() - t;
            } catch (IOException ex) {
                return -1;
            }
        }
        return total / success;
    }

    private double measureDownloadSpeed() {
        try {
            HttpURLConnection conn = (HttpURLConnection)
                    new URL(DOWNLOAD_URL).openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            conn.connect();

            InputStream is = conn.getInputStream();
            byte[] buf = new byte[8192];
            long bytes = 0;
            long start = System.currentTimeMillis();
            long limit = 9000; // maks 9 saniye ölç

            int n;
            while ((n = is.read(buf)) != -1) {
                bytes += n;
                if (System.currentTimeMillis() - start > limit) break;
            }
            is.close();
            conn.disconnect();

            long elapsed = System.currentTimeMillis() - start;
            if (elapsed == 0 || bytes == 0) return -1;

            // Mbps
            return (bytes * 8.0) / (elapsed / 1000.0) / (1024 * 1024);
        } catch (Exception e) {
            return -1;
        }
    }

    /** Upload için gerçek test sunucusu olmadan basit tahmin */
    private double estimateUpload(double dlMbps, long pingMs) {
        // Fiber (<20ms) → ~%45, VDSL (~50ms) → %35, ADSL/4G → %25
        double ratio = pingMs < 20 ? 0.45 : pingMs < 60 ? 0.35 : 0.25;
        return dlMbps * ratio;
    }

    // ──────────────────────────────────────────
    //  SİSTEM BİLGİSİ
    // ──────────────────────────────────────────

    private void startSystemMonitor() {
        mainHandler.postDelayed(new Runnable() {
            @Override public void run() {
                updateCpuRam();
                mainHandler.postDelayed(this, 3000);
            }
        }, 3000);
    }

    private void updateCpuRam() {
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);

        long totalMB = mi.totalMem  / (1024 * 1024);
        long availMB = mi.availMem  / (1024 * 1024);
        long usedMB  = totalMB - availMB;
        int pct = (int) ((usedMB * 100) / totalMB);
        tvRam.setText(usedMB + " MB / " + totalMB + " MB  (" + pct + "%)");

        executor.execute(() -> {
            String cpu = readCpuUsage();
            mainHandler.post(() -> tvCpu.setText(cpu));
        });
    }

    private long[] lastCpu = null;

    private String readCpuUsage() {
        try {
            java.io.BufferedReader r = new java.io.BufferedReader(
                    new java.io.FileReader("/proc/stat"));
            String[] p = r.readLine().split("\\s+");
            r.close();

            long user   = Long.parseLong(p[1]);
            long nice   = Long.parseLong(p[2]);
            long system = Long.parseLong(p[3]);
            long idle   = Long.parseLong(p[4]);
            long iowait = Long.parseLong(p[5]);

            long active = user + nice + system;
            long total  = active + idle + iowait;

            if (lastCpu == null) {
                lastCpu = new long[]{active, total};
                return "Hesaplanıyor...";
            }
            long dA = active - lastCpu[0];
            long dT = total  - lastCpu[1];
            lastCpu = new long[]{active, total};

            int pct = dT == 0 ? 0 : (int) (dA * 100 / dT);
            int cores = Runtime.getRuntime().availableProcessors();
            return pct + "%  (" + cores + " çekirdek)";
        } catch (Exception e) {
            return "Bilgi alınamadı";
        }
    }

    private void updateWifiInfo() {
        try {
            WifiManager wm = (WifiManager) getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            if (wm != null && wm.isWifiEnabled()) {
                WifiInfo wi = wm.getConnectionInfo();
                int rssi  = wi.getRssi();
                int level = WifiManager.calculateSignalLevel(rssi, 5);
                String bars = "█".repeat(level) + "░".repeat(Math.max(0, 4 - level));
                String ssid = wi.getSSID().replace("\"", "");
                int link = wi.getLinkSpeed();
                tvWifi.setText(bars + "  " + rssi + " dBm  •  " + link + " Mbps\n" + ssid);
            } else {
                // Ethernet kontrolü
                ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                boolean eth = false;
                if (cm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    NetworkCapabilities nc = cm.getNetworkCapabilities(cm.getActiveNetwork());
                    eth = nc != null && nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);
                }
                tvWifi.setText(eth ? "Ethernet bağlantısı" : "Wi-Fi kapalı veya bağlı değil");
            }
        } catch (Exception e) {
            tvWifi.setText("Bağlantı bilgisi alınamadı");
        }
    }

    // ──────────────────────────────────────────
    //  YARDIMCI
    // ──────────────────────────────────────────

    private String formatSpeed(double mbps) {
        if (mbps >= 1000) return String.format("%.2f Gbps", mbps / 1000);
        if (mbps >= 1)    return String.format("%.1f Mbps", mbps);
        return String.format("%.0f Kbps", mbps * 1024);
    }

    private void setStatus(String msg) {
        mainHandler.post(() -> tvStatus.setText(msg));
    }

    private void setField(TextView tv, String text, boolean good) {
        mainHandler.post(() -> {
            tv.setText(text);
            tv.setTextColor(getResources().getColor(
                    good ? R.color.colorGood : R.color.colorWarn, getTheme()));
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)
                && btnTest.hasFocus() && !isTesting) {
            startFullTest();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
