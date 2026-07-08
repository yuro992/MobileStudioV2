package com.yu.mobilestudio.v2;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class ModeActivity extends Activity {

    private static final int DEFAULT_PORT = 56789;
    private static final String HEARTBEAT_PREFIX = "MOBILESTUDIOV2_HEARTBEAT";

    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final List<Socket> senderClientSockets = Collections.synchronizedList(new ArrayList<>());

    private String mode;
    private TextView statusView;
    private TextView metricsView;
    private TextView primaryButton;
    private TextView secondaryButton;
    private EditText ipInput;
    private EditText portInput;

    private volatile boolean senderServerRunning = false;
    private volatile boolean studioClientRunning = false;
    private ServerSocket serverSocket;
    private Socket studioSocket;
    private Thread senderServerThread;
    private Thread studioClientThread;

    private int senderAcceptedConnections = 0;
    private int senderHeartbeatCounter = 0;
    private int studioHeartbeatCounter = 0;
    private long senderStartedAtMs = 0L;
    private long studioConnectedAtMs = 0L;
    private long lastHeartbeatAtMs = 0L;
    private long lastLatencyMs = -1L;
    private String lastMessage = "none";

    private final Runnable metricsTicker = new Runnable() {
        @Override
        public void run() {
            refreshMetrics();
            uiHandler.postDelayed(this, 1000L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mode = getIntent().getStringExtra(MainActivity.EXTRA_MODE);
        if (mode == null || mode.trim().isEmpty()) {
            mode = "Sender";
        }

        setTitle(mode + " Mode");
        uiHandler.post(metricsTicker);

        if ("Studio".equalsIgnoreCase(mode)) {
            buildStudioUi();
        } else {
            buildSenderUi();
        }
    }

    private void buildSenderUi() {
        LinearLayout content = baseContent();
        addBadge(content);
        addTitle(content, "Sender Mode Ready");
        addDescription(content, "Start a LAN test server on the game phone. The Studio phone will connect and receive heartbeat messages only.");

        TextView networkBox = infoBox(
                "Sender LAN endpoint\n" +
                        "IP: " + getLocalIpv4Address() + "\n" +
                        "Port: " + DEFAULT_PORT + "\n" +
                        "Protocol: TCP heartbeat test"
        );
        content.addView(networkBox, fullWidthWrapWithBottom(dp(18)));

        statusView = statusBox("Status: LAN server not started", false);
        content.addView(statusView, fullWidthWrapWithBottom(dp(14)));

        metricsView = metricsBox("LAN metrics\nServer: inactive\nClients: 0\nHeartbeats sent: 0\nLocal IP: " + getLocalIpv4Address());
        content.addView(metricsView, fullWidthWrapWithBottom(dp(18)));

        primaryButton = actionButton("Start LAN Test Server", Color.rgb(22, 101, 52));
        primaryButton.setOnClickListener(v -> startSenderServer());
        content.addView(primaryButton, fullWidthWrapWithBottom(dp(12)));

        secondaryButton = actionButton("Stop LAN Test Server", Color.rgb(185, 28, 28));
        secondaryButton.setOnClickListener(v -> stopSenderServer("LAN server stopped"));
        content.addView(secondaryButton, fullWidthWrapWithBottom(dp(18)));

        TextView backButton = actionButton("Back", Color.rgb(39, 39, 42));
        backButton.setOnClickListener(v -> finish());
        content.addView(backButton, wrapCentered());

        updateSenderButtons();
        setContentView(wrapInScroll(content));
    }

    private void buildStudioUi() {
        LinearLayout content = baseContent();
        addBadge(content);
        addTitle(content, "Studio Mode Ready");
        addDescription(content, "Connect to the Sender phone over the same Wi-Fi/LAN. This phase receives heartbeat messages only.");

        ipInput = new EditText(this);
        ipInput.setText(getBestDefaultStudioIp());
        ipInput.setHint("Sender IP, e.g. 192.168.1.50");
        ipInput.setSingleLine(true);
        ipInput.setTextSize(16);
        ipInput.setTextColor(Color.rgb(39, 39, 42));
        ipInput.setPadding(dp(18), dp(12), dp(18), dp(12));
        ipInput.setBackground(makeRoundedBackground(Color.WHITE, dp(18), Color.rgb(221, 214, 254)));
        content.addView(ipInput, fullWidthWrapWithBottom(dp(10)));

        portInput = new EditText(this);
        portInput.setText(String.valueOf(DEFAULT_PORT));
        portInput.setHint("Port");
        portInput.setSingleLine(true);
        portInput.setTextSize(16);
        portInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        portInput.setTextColor(Color.rgb(39, 39, 42));
        portInput.setPadding(dp(18), dp(12), dp(18), dp(12));
        portInput.setBackground(makeRoundedBackground(Color.WHITE, dp(18), Color.rgb(221, 214, 254)));
        content.addView(portInput, fullWidthWrapWithBottom(dp(16)));

        statusView = statusBox("Status: Not connected", false);
        content.addView(statusView, fullWidthWrapWithBottom(dp(14)));

        metricsView = metricsBox("Connection metrics\nClient: inactive\nHeartbeats received: 0\nLast latency: n/a\nLast message: none");
        content.addView(metricsView, fullWidthWrapWithBottom(dp(18)));

        primaryButton = actionButton("Connect to Sender", Color.rgb(22, 101, 52));
        primaryButton.setOnClickListener(v -> startStudioClient());
        content.addView(primaryButton, fullWidthWrapWithBottom(dp(12)));

        secondaryButton = actionButton("Disconnect", Color.rgb(185, 28, 28));
        secondaryButton.setOnClickListener(v -> stopStudioClient("Disconnected"));
        content.addView(secondaryButton, fullWidthWrapWithBottom(dp(18)));

        TextView backButton = actionButton("Back", Color.rgb(39, 39, 42));
        backButton.setOnClickListener(v -> finish());
        content.addView(backButton, wrapCentered());

        updateStudioButtons();
        setContentView(wrapInScroll(content));
    }

    private void startSenderServer() {
        if (senderServerRunning) {
            return;
        }

        senderServerRunning = true;
        senderAcceptedConnections = 0;
        senderHeartbeatCounter = 0;
        senderStartedAtMs = System.currentTimeMillis();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        updateStatus("Status: Starting LAN server...", true);
        updateSenderButtons();

        senderServerThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(DEFAULT_PORT);
                runOnUiThread(() -> updateStatus("Status: LAN server active. Waiting for Studio connection", true));

                while (senderServerRunning && !serverSocket.isClosed()) {
                    Socket client = serverSocket.accept();
                    senderClientSockets.add(client);
                    senderAcceptedConnections++;
                    runOnUiThread(() -> updateStatus("Status: Studio connected. Sending heartbeat", true));
                    startSenderHeartbeatThread(client);
                }
            } catch (IOException e) {
                if (senderServerRunning) {
                    runOnUiThread(() -> updateStatus("Status: LAN server error: " + safeMessage(e), false));
                }
            } finally {
                closeServerSocketQuietly();
                closeSenderClientSocketsQuietly();
                senderServerRunning = false;
                runOnUiThread(() -> {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    updateSenderButtons();
                    refreshMetrics();
                });
            }
        }, "MobileStudioV2-SenderServer");

        senderServerThread.start();
    }

    private void startSenderHeartbeatThread(Socket client) {
        Thread heartbeatThread = new Thread(() -> {
            try {
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
                while (senderServerRunning && !client.isClosed()) {
                    int heartbeatNumber = ++senderHeartbeatCounter;
                    long now = System.currentTimeMillis();
                    String message = HEARTBEAT_PREFIX + " " + heartbeatNumber + " " + now + "\n";
                    writer.write(message);
                    writer.flush();
                    Thread.sleep(1000L);
                }
            } catch (Exception ignored) {
                // Client disconnected or server stopped.
            } finally {
                try {
                    client.close();
                } catch (IOException ignored) {
                }
                senderClientSockets.remove(client);
                runOnUiThread(() -> {
                    if (senderServerRunning && senderClientSockets.isEmpty()) {
                        updateStatus("Status: LAN server active. Waiting for Studio connection", true);
                    }
                    refreshMetrics();
                });
            }
        }, "MobileStudioV2-SenderHeartbeat");
        heartbeatThread.start();
    }

    private void stopSenderServer(String message) {
        senderServerRunning = false;
        closeServerSocketQuietly();
        closeSenderClientSocketsQuietly();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        updateStatus("Status: " + message, false);
        updateSenderButtons();
        refreshMetrics();
    }

    private void startStudioClient() {
        if (studioClientRunning) {
            return;
        }

        String host = ipInput.getText().toString().trim();
        int port = parsePort(portInput.getText().toString().trim(), DEFAULT_PORT);

        if (host.isEmpty()) {
            updateStatus("Status: Enter Sender IP first", false);
            return;
        }

        studioClientRunning = true;
        studioHeartbeatCounter = 0;
        lastLatencyMs = -1L;
        lastMessage = "none";
        lastHeartbeatAtMs = 0L;
        studioConnectedAtMs = 0L;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        updateStatus("Status: Connecting to " + host + ":" + port + "...", true);
        updateStudioButtons();

        studioClientThread = new Thread(() -> {
            try {
                Socket socket = new Socket();
                studioSocket = socket;
                socket.connect(new InetSocketAddress(host, port), 3500);
                studioConnectedAtMs = System.currentTimeMillis();
                runOnUiThread(() -> updateStatus("Status: Connected to Sender. Waiting for heartbeat", true));

                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String line;
                while (studioClientRunning && (line = reader.readLine()) != null) {
                    handleStudioHeartbeat(line);
                }

                if (studioClientRunning) {
                    runOnUiThread(() -> updateStatus("Status: Connection lost", false));
                }
            } catch (IOException e) {
                if (studioClientRunning) {
                    runOnUiThread(() -> updateStatus("Status: Connection error: " + safeMessage(e), false));
                }
            } finally {
                closeStudioSocketQuietly();
                studioClientRunning = false;
                runOnUiThread(() -> {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    updateStudioButtons();
                    refreshMetrics();
                });
            }
        }, "MobileStudioV2-StudioClient");

        studioClientThread.start();
    }

    private void handleStudioHeartbeat(String line) {
        long now = System.currentTimeMillis();
        lastHeartbeatAtMs = now;
        lastMessage = line;
        studioHeartbeatCounter++;

        String[] parts = line.split(" ");
        if (parts.length >= 3 && HEARTBEAT_PREFIX.equals(parts[0])) {
            try {
                long serverTimeMs = Long.parseLong(parts[2]);
                long latency = now - serverTimeMs;
                if (latency >= 0 && latency < 60000) {
                    lastLatencyMs = latency;
                }
            } catch (NumberFormatException ignored) {
                lastLatencyMs = -1L;
            }
        }

        runOnUiThread(() -> updateStatus("Status: Connected. Heartbeat received", true));
    }

    private void stopStudioClient(String message) {
        studioClientRunning = false;
        closeStudioSocketQuietly();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        updateStatus("Status: " + message, false);
        updateStudioButtons();
        refreshMetrics();
    }

    private void refreshMetrics() {
        if (metricsView == null) {
            return;
        }

        if ("Studio".equalsIgnoreCase(mode)) {
            long uptimeSec = studioConnectedAtMs > 0L && studioClientRunning
                    ? Math.max(0L, (System.currentTimeMillis() - studioConnectedAtMs) / 1000L)
                    : 0L;
            String latency = lastLatencyMs >= 0L ? lastLatencyMs + " ms" : "n/a";
            String since = lastHeartbeatAtMs > 0L
                    ? Math.max(0L, (System.currentTimeMillis() - lastHeartbeatAtMs) / 1000L) + "s ago"
                    : "n/a";
            metricsView.setText(
                    "Connection metrics\n" +
                            "Client: " + (studioClientRunning ? "connected" : "inactive") + "\n" +
                            "Heartbeats received: " + studioHeartbeatCounter + "\n" +
                            "Last latency: " + latency + " | Last heartbeat: " + since + "\n" +
                            "Uptime: " + uptimeSec + "s\n" +
                            "Last message: " + shorten(lastMessage, 52)
            );
        } else {
            long uptimeSec = senderStartedAtMs > 0L && senderServerRunning
                    ? Math.max(0L, (System.currentTimeMillis() - senderStartedAtMs) / 1000L)
                    : 0L;
            metricsView.setText(
                    "LAN metrics\n" +
                            "Server: " + (senderServerRunning ? "active" : "inactive") + "\n" +
                            "Local IP: " + getLocalIpv4Address() + " | Port: " + DEFAULT_PORT + "\n" +
                            "Connected clients: " + senderClientSockets.size() + " | Accepted: " + senderAcceptedConnections + "\n" +
                            "Heartbeats sent: " + senderHeartbeatCounter + "\n" +
                            "Uptime: " + uptimeSec + "s"
            );
        }
    }

    private void updateSenderButtons() {
        setButtonEnabled(primaryButton, !senderServerRunning);
        setButtonEnabled(secondaryButton, senderServerRunning);
    }

    private void updateStudioButtons() {
        setButtonEnabled(primaryButton, !studioClientRunning);
        setButtonEnabled(secondaryButton, studioClientRunning);
        if (ipInput != null) {
            ipInput.setEnabled(!studioClientRunning);
        }
        if (portInput != null) {
            portInput.setEnabled(!studioClientRunning);
        }
    }

    private void updateStatus(String text, boolean good) {
        if (statusView == null) {
            return;
        }
        statusView.setText(text);
        statusView.setTextColor(good ? Color.rgb(21, 128, 61) : Color.rgb(68, 64, 60));
        refreshMetrics();
    }

    private void closeServerSocketQuietly() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
        serverSocket = null;
    }

    private void closeSenderClientSocketsQuietly() {
        synchronized (senderClientSockets) {
            for (Socket socket : new ArrayList<>(senderClientSockets)) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
            senderClientSockets.clear();
        }
    }

    private void closeStudioSocketQuietly() {
        try {
            if (studioSocket != null) {
                studioSocket.close();
            }
        } catch (IOException ignored) {
        }
        studioSocket = null;
    }

    @Override
    protected void onDestroy() {
        uiHandler.removeCallbacks(metricsTicker);
        stopSenderServer("LAN server stopped");
        stopStudioClient("Disconnected");
        super.onDestroy();
    }

    private LinearLayout baseContent() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(dp(24), dp(28), dp(24), dp(28));
        content.setBackgroundColor(Color.rgb(250, 250, 255));
        return content;
    }

    private ScrollView wrapInScroll(LinearLayout content) {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(Color.rgb(250, 250, 255));
        scrollView.addView(content, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));
        return scrollView;
    }

    private void addBadge(LinearLayout content) {
        TextView badge = new TextView(this);
        badge.setText("Phase 7");
        badge.setTextSize(14);
        badge.setTypeface(Typeface.DEFAULT_BOLD);
        badge.setTextColor(Color.rgb(124, 58, 237));
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(14), dp(8), dp(14), dp(8));
        badge.setBackground(makeRoundedBackground(Color.rgb(237, 233, 254), dp(18)));
        content.addView(badge, wrapCenteredWithBottom(dp(22)));
    }

    private void addTitle(LinearLayout content, String text) {
        TextView title = new TextView(this);
        title.setText(text);
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.rgb(28, 25, 23));
        title.setGravity(Gravity.CENTER);
        content.addView(title, fullWidthWrapWithBottom(dp(12)));
    }

    private void addDescription(LinearLayout content, String text) {
        TextView description = new TextView(this);
        description.setText(text);
        description.setTextSize(15);
        description.setTextColor(Color.rgb(87, 83, 78));
        description.setGravity(Gravity.CENTER);
        content.addView(description, fullWidthWrapWithBottom(dp(20)));
    }

    private TextView infoBox(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(15);
        view.setTextColor(Color.rgb(68, 64, 60));
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(16), dp(16), dp(16), dp(16));
        view.setBackground(makeRoundedBackground(Color.WHITE, dp(20), Color.rgb(221, 214, 254)));
        return view;
    }

    private TextView statusBox(String text, boolean good) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(16);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setTextColor(good ? Color.rgb(21, 128, 61) : Color.rgb(68, 64, 60));
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(14), dp(14), dp(14), dp(14));
        view.setBackground(makeRoundedBackground(Color.WHITE, dp(22)));
        return view;
    }

    private TextView metricsBox(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(14);
        view.setTextColor(Color.rgb(87, 83, 78));
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(14), dp(14), dp(14), dp(14));
        view.setBackground(makeRoundedBackground(Color.rgb(245, 245, 244), dp(20)));
        return view;
    }

    private TextView actionButton(String text, int color) {
        TextView button = new TextView(this);
        button.setText(text);
        button.setTextSize(16);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setTextColor(Color.WHITE);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(16), dp(14), dp(16), dp(14));
        button.setBackground(makeRoundedBackground(color, dp(18)));
        return button;
    }

    private void setButtonEnabled(TextView button, boolean enabled) {
        if (button == null) {
            return;
        }
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1.0f : 0.45f);
    }

    private GradientDrawable makeRoundedBackground(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private GradientDrawable makeRoundedBackground(int color, int radius, int strokeColor) {
        GradientDrawable drawable = makeRoundedBackground(color, radius);
        drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private LinearLayout.LayoutParams fullWidthWrapWithBottom(int bottomMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, bottomMargin);
        return params;
    }

    private LinearLayout.LayoutParams wrapCentered() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.CENTER_HORIZONTAL;
        return params;
    }

    private LinearLayout.LayoutParams wrapCenteredWithBottom(int bottomMargin) {
        LinearLayout.LayoutParams params = wrapCentered();
        params.setMargins(0, 0, 0, bottomMargin);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int parsePort(String value, int fallback) {
        try {
            int port = Integer.parseInt(value);
            if (port > 0 && port < 65536) {
                return port;
            }
        } catch (NumberFormatException ignored) {
        }
        return fallback;
    }

    private String getBestDefaultStudioIp() {
        String local = getLocalIpv4Address();
        if (local.startsWith("192.168.")) {
            int lastDot = local.lastIndexOf('.');
            if (lastDot > 0) {
                return local.substring(0, lastDot + 1);
            }
        }
        return "";
    }

    private String getLocalIpv4Address() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "unknown";
    }

    private String safeMessage(Exception e) {
        String message = e.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return e.getClass().getSimpleName();
        }
        return message;
    }

    private String shorten(String value, int maxLength) {
        if (value == null) {
            return "none";
        }
        String cleaned = value.trim();
        if (cleaned.length() <= maxLength) {
            return cleaned;
        }
        return cleaned.substring(0, Math.max(0, maxLength - 1)) + "…";
    }
}
