package com.yu.mobilestudio.v2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.Locale;

public class ModeActivity extends Activity {

    private static final int REQUEST_CAPTURE_PERMISSION = 2602;
    private static final int DEFAULT_PORT = 56791;
    private static final int PACKET_MAGIC = 0x4D535639; // MSV9
    private static final int PACKET_CONFIG = 1;
    private static final int PACKET_KEY_FRAME = 2;
    private static final int PACKET_DELTA_FRAME = 3;
    private static final int TARGET_SHORT_SIDE = 720;
    private static final int TARGET_FPS = 30;
    private static final int TARGET_BITRATE = 4_000_000;
    private static final int I_FRAME_INTERVAL_SECONDS = 2;
    private static final String AVC_MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private String mode;
    private TextView statusView;
    private TextView metricsView;
    private TextView requestButton;
    private TextView startButton;
    private TextView stopButton;
    private EditText ipInput;
    private EditText portInput;
    private TextureView studioPreviewView;
    private SurfaceTexture studioPreviewTexture;
    private Surface decoderOutputSurface;

    private Intent projectionPermissionData;
    private int projectionPermissionResultCode = Activity.RESULT_CANCELED;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private MediaCodec videoEncoder;
    private Surface encoderInputSurface;
    private Thread encoderDrainThread;
    private Thread senderAcceptThread;
    private Thread studioReadThread;
    private ServerSocket senderServerSocket;
    private Socket senderClientSocket;
    private DataOutputStream senderClientOut;
    private Socket studioSocket;
    private DataInputStream studioInput;

    private volatile boolean senderActive = false;
    private volatile boolean drainRunning = false;
    private volatile boolean studioClientRunning = false;
    private boolean releasingSession = false;

    private int sourceWidth = 0;
    private int sourceHeight = 0;
    private int sourceDensity = 0;
    private int encoderWidth = 0;
    private int encoderHeight = 0;
    private long startedAtMs = 0L;
    private long encodedBytes = 0L;
    private long encodedOutputCount = 0L;
    private long keyFrameCount = 0L;
    private long codecConfigCount = 0L;
    private long packetsSent = 0L;
    private long bytesSent = 0L;
    private long sendErrorCount = 0L;
    private long packetSequence = 0L;
    private int acceptedClients = 0;
    private String outputFormatSummary = "waiting";
    private byte[] latestConfigPacket = null;

    private long studioConnectedAtMs = 0L;
    private long studioPacketsReceived = 0L;
    private long studioBytesReceived = 0L;
    private long studioConfigReceived = 0L;
    private long studioKeyFramesReceived = 0L;
    private long studioDeltaFramesReceived = 0L;
    private long studioLastPacketAtMs = 0L;
    private String studioLastPacketSummary = "none";
    private MediaCodec videoDecoder;
    private int decoderWidth = 0;
    private int decoderHeight = 0;
    private long decodedFrameCount = 0L;
    private long decoderInputDropCount = 0L;
    private long decoderErrorCount = 0L;
    private long decoderFormatChangeCount = 0L;
    private long lastDecodedFrameAtMs = 0L;
    private String decoderFormatSummary = "waiting";

    private final Runnable metricsTicker = new Runnable() {
        @Override
        public void run() {
            updateMetrics();
            mainHandler.postDelayed(this, 1000L);
        }
    };

    private final MediaProjection.Callback projectionCallback = new MediaProjection.Callback() {
        @Override
        public void onStop() {
            mainHandler.post(() -> releaseSenderSession(false, "H.264 LAN preview stopped by Android. Request permission again."));
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
        mainHandler.post(metricsTicker);

        if ("Studio".equalsIgnoreCase(mode)) {
            buildStudioScreen();
        } else {
            buildSenderScreen();
        }
    }

    @Override
    protected void onDestroy() {
        mainHandler.removeCallbacks(metricsTicker);
        releaseSenderSession(true, null);
        stopStudioClient("Disconnected");
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        releaseSenderSession(true, null);
        stopStudioClient("Disconnected");
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != REQUEST_CAPTURE_PERMISSION) {
            return;
        }

        if (resultCode == Activity.RESULT_OK && data != null) {
            projectionPermissionResultCode = resultCode;
            projectionPermissionData = data;
            setStatus("Permission granted. Ready to start H.264 LAN preview", true);
        } else {
            projectionPermissionResultCode = Activity.RESULT_CANCELED;
            projectionPermissionData = null;
            setStatus("Screen capture permission denied", false);
        }

        updateMetrics();
        updateButtons();
    }

    private void buildSenderScreen() {
        LinearLayout root = baseContent();
        addBadge(root);
        addTitle(root, "Sender Mode Ready");
        addDescription(root, "Encode screen buffers and send H.264 packet chunks to Studio over LAN for decode preview testing.");

        root.addView(infoBox("Sender H.264 LAN endpoint\nIP: " + getLocalIpv4Address() + "\nPort: " + DEFAULT_PORT + "\nProtocol: TCP framed packet preview"), fullWidthWrapWithBottom(dp(14)));

        statusView = statusBox("Status: Not requested", false);
        root.addView(statusView, fullWidthWrapWithBottom(dp(14)));

        metricsView = metricsBox("H.264 LAN metrics\nServer: inactive\nClient: none\nPackets sent: 0 | Bytes sent: 0\nEncoded: inactive");
        root.addView(metricsView, fullWidthWrapWithBottom(dp(16)));

        requestButton = actionButton("Request Screen Capture Permission", Color.rgb(124, 58, 237));
        requestButton.setOnClickListener(v -> requestCapturePermission());
        root.addView(requestButton, fullWidthWrapWithBottom(dp(10)));

        startButton = actionButton("Start H.264 LAN Preview", Color.rgb(22, 101, 52));
        startButton.setOnClickListener(v -> startSenderLanDryRun());
        root.addView(startButton, fullWidthWrapWithBottom(dp(10)));

        stopButton = actionButton("Stop H.264 LAN Preview", Color.rgb(185, 28, 28));
        stopButton.setOnClickListener(v -> releaseSenderSession(false, "H.264 LAN preview stopped. Request permission again."));
        root.addView(stopButton, fullWidthWrapWithBottom(dp(18)));

        TextView backButton = actionButton("Back", Color.rgb(39, 39, 42));
        backButton.setOnClickListener(v -> finish());
        root.addView(backButton, wrapCentered());

        updateButtons();
        setContentView(wrapInScroll(root));
    }

    private void buildStudioScreen() {
        LinearLayout root = baseContent();
        addBadge(root);
        addTitle(root, "Studio Mode Ready");
        addDescription(root, "Connect to Sender over Wi-Fi/LAN, receive H.264 packets, and render a decode preview.");

        studioPreviewView = new TextureView(this);
        studioPreviewView.setBackgroundColor(Color.BLACK);
        studioPreviewView.setOpaque(true);
        root.addView(studioPreviewView, fullWidthHeightWithBottom(dp(220), dp(14)));

        ipInput = new EditText(this);
        ipInput.setText(getBestDefaultStudioIp());
        ipInput.setHint("Sender IP, e.g. 192.168.1.50");
        ipInput.setSingleLine(true);
        ipInput.setTextSize(16);
        ipInput.setTextColor(Color.rgb(39, 39, 42));
        ipInput.setPadding(dp(18), dp(12), dp(18), dp(12));
        ipInput.setBackground(makeRoundedBackground(Color.WHITE, dp(18), Color.rgb(221, 214, 254)));
        root.addView(ipInput, fullWidthWrapWithBottom(dp(10)));

        portInput = new EditText(this);
        portInput.setText(String.valueOf(DEFAULT_PORT));
        portInput.setHint("Port");
        portInput.setSingleLine(true);
        portInput.setTextSize(16);
        portInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        portInput.setTextColor(Color.rgb(39, 39, 42));
        portInput.setPadding(dp(18), dp(12), dp(18), dp(12));
        portInput.setBackground(makeRoundedBackground(Color.WHITE, dp(18), Color.rgb(221, 214, 254)));
        root.addView(portInput, fullWidthWrapWithBottom(dp(16)));

        statusView = statusBox("Status: Preview surface starting", false);
        root.addView(statusView, fullWidthWrapWithBottom(dp(14)));

        metricsView = metricsBox("Decode preview metrics\nClient: inactive\nPackets received: 0 | Bytes received: 0\nDecoded frames: 0 | Drops: 0 | Errors: 0\nDecoder: waiting");
        root.addView(metricsView, fullWidthWrapWithBottom(dp(16)));

        startButton = actionButton("Connect + Decode Preview", Color.rgb(22, 101, 52));
        startButton.setOnClickListener(v -> startStudioClient());
        root.addView(startButton, fullWidthWrapWithBottom(dp(10)));

        stopButton = actionButton("Disconnect", Color.rgb(185, 28, 28));
        stopButton.setOnClickListener(v -> stopStudioClient("Disconnected"));
        root.addView(stopButton, fullWidthWrapWithBottom(dp(18)));

        TextView backButton = actionButton("Back", Color.rgb(39, 39, 42));
        backButton.setOnClickListener(v -> finish());
        root.addView(backButton, wrapCentered());

        updateButtons();
        setContentView(wrapInScroll(root));

        studioPreviewView.post(this::installStudioPreviewTextureListener);
    }

    private void installStudioPreviewTextureListener() {
        if (studioPreviewView == null) {
            return;
        }
        studioPreviewView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                attachStudioDecoderSurface(surface);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                attachStudioDecoderSurface(surface);
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                studioPreviewTexture = null;
                if (decoderOutputSurface != null) {
                    try {
                        decoderOutputSurface.release();
                    } catch (Exception ignored) {
                    }
                    decoderOutputSurface = null;
                }
                releaseStudioDecoder();
                if (studioClientRunning) {
                    setStatus("Preview surface lost. Disconnect and reconnect", false);
                }
                updateMetrics();
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });

        if (studioPreviewView.isAvailable()) {
            SurfaceTexture surface = studioPreviewView.getSurfaceTexture();
            if (surface != null) {
                attachStudioDecoderSurface(surface);
            }
        }
    }

    private void attachStudioDecoderSurface(SurfaceTexture surface) {
        if (surface == null) {
            return;
        }
        studioPreviewTexture = surface;
        if (decoderOutputSurface != null) {
            try {
                decoderOutputSurface.release();
            } catch (Exception ignored) {
            }
        }
        decoderOutputSurface = new Surface(surface);
        if (!studioClientRunning) {
            setStatus("Preview surface ready. Connect to Sender", true);
        }
        updateMetrics();
    }

    private void requestCapturePermission() {
        MediaProjectionManager manager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (manager == null) {
            setStatus("MediaProjectionManager unavailable", false);
            return;
        }
        startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_CAPTURE_PERMISSION);
    }

    private void startSenderLanDryRun() {
        if (senderActive) {
            return;
        }
        if (projectionPermissionData == null || projectionPermissionResultCode != Activity.RESULT_OK) {
            setStatus("Request screen capture permission first", false);
            return;
        }

        resetSenderCounters();
        startKeepAliveService();
        if (!MediaProjectionKeepAliveService.isRunning()) {
            setStatus("Starting media projection foreground service...", true);
            mainHandler.postDelayed(() -> startSenderLanDryRun(), 150L);
            return;
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setStatus("Starting H.264 LAN preview server and encoder...", true);
        senderActive = true;
        updateButtons();

        try {
            MediaProjectionManager manager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            if (manager == null) {
                throw new IllegalStateException("MediaProjectionManager unavailable");
            }

            mediaProjection = manager.getMediaProjection(projectionPermissionResultCode, projectionPermissionData);
            if (mediaProjection == null) {
                throw new IllegalStateException("MediaProjection unavailable");
            }
            mediaProjection.registerCallback(projectionCallback, mainHandler);

            prepareEncoder();
            startSenderAcceptThread();
            videoEncoder.start();
            createVirtualDisplay();
            startEncoderDrainThread();

            setStatus("H.264 LAN preview active. Waiting for Studio packet client", true);
        } catch (Exception e) {
            releaseSenderSession(false, "Failed to start H.264 LAN preview: " + safeMessage(e));
        }
    }

    private void resetSenderCounters() {
        sourceWidth = 0;
        sourceHeight = 0;
        sourceDensity = 0;
        encoderWidth = 0;
        encoderHeight = 0;
        startedAtMs = System.currentTimeMillis();
        encodedBytes = 0L;
        encodedOutputCount = 0L;
        keyFrameCount = 0L;
        codecConfigCount = 0L;
        packetsSent = 0L;
        bytesSent = 0L;
        sendErrorCount = 0L;
        packetSequence = 0L;
        acceptedClients = 0;
        outputFormatSummary = "waiting";
        latestConfigPacket = null;
    }

    private void prepareEncoder() throws IOException {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
        sourceWidth = metrics.widthPixels;
        sourceHeight = metrics.heightPixels;
        sourceDensity = metrics.densityDpi;

        int[] size = calculateEncoderSize(sourceWidth, sourceHeight);
        encoderWidth = size[0];
        encoderHeight = size[1];

        MediaFormat format = MediaFormat.createVideoFormat(AVC_MIME_TYPE, encoderWidth, encoderHeight);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, TARGET_BITRATE);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, TARGET_FPS);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL_SECONDS);

        videoEncoder = MediaCodec.createEncoderByType(AVC_MIME_TYPE);
        videoEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        encoderInputSurface = videoEncoder.createInputSurface();
    }

    private void createVirtualDisplay() {
        virtualDisplay = mediaProjection.createVirtualDisplay(
                "MobileStudioV2-Phase9-LanDecodePreview",
                encoderWidth,
                encoderHeight,
                sourceDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                encoderInputSurface,
                null,
                mainHandler
        );
    }

    private void startSenderAcceptThread() throws IOException {
        senderServerSocket = new ServerSocket(DEFAULT_PORT);
        senderAcceptThread = new Thread(() -> {
            while (senderActive && senderServerSocket != null && !senderServerSocket.isClosed()) {
                try {
                    Socket client = senderServerSocket.accept();
                    client.setTcpNoDelay(true);
                    setSenderClient(client);
                    acceptedClients++;
                    mainHandler.post(() -> setStatus("Studio connected. Sending H.264 packets", true));
                } catch (IOException e) {
                    if (senderActive) {
                        mainHandler.post(() -> setStatus("LAN accept error: " + safeMessage(e), false));
                    }
                }
            }
        }, "MobileStudioV2-Phase9-SenderAccept");
        senderAcceptThread.start();
    }

    private synchronized void setSenderClient(Socket client) {
        closeSenderClientQuietly();
        senderClientSocket = client;
        try {
            senderClientOut = new DataOutputStream(new BufferedOutputStream(client.getOutputStream()));
            if (latestConfigPacket != null) {
                senderClientOut.write(latestConfigPacket);
                senderClientOut.flush();
                packetsSent++;
                bytesSent += latestConfigPacket.length;
            }
        } catch (IOException e) {
            sendErrorCount++;
            closeSenderClientQuietly();
        }
    }

    private void startEncoderDrainThread() {
        drainRunning = true;
        encoderDrainThread = new Thread(() -> {
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

            while (drainRunning && videoEncoder != null) {
                try {
                    int outputIndex = videoEncoder.dequeueOutputBuffer(bufferInfo, 10_000);
                    if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        continue;
                    }
                    if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        MediaFormat newFormat = videoEncoder.getOutputFormat();
                        outputFormatSummary = newFormat.getString(MediaFormat.KEY_MIME) + " " + encoderWidth + "x" + encoderHeight;
                        continue;
                    }
                    if (outputIndex < 0) {
                        continue;
                    }

                    ByteBuffer encodedBuffer = videoEncoder.getOutputBuffer(outputIndex);
                    if (encodedBuffer != null && bufferInfo.size > 0) {
                        byte[] data = new byte[bufferInfo.size];
                        encodedBuffer.position(bufferInfo.offset);
                        encodedBuffer.limit(bufferInfo.offset + bufferInfo.size);
                        encodedBuffer.get(data);

                        encodedBytes += data.length;
                        encodedOutputCount++;

                        int packetType = PACKET_DELTA_FRAME;
                        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            codecConfigCount++;
                            packetType = PACKET_CONFIG;
                        } else if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
                            keyFrameCount++;
                            packetType = PACKET_KEY_FRAME;
                        }

                        byte[] packet = buildVideoPacket(packetType, data, bufferInfo.flags, bufferInfo.presentationTimeUs);
                        if (packetType == PACKET_CONFIG) {
                            latestConfigPacket = packet;
                        }
                        sendPacketToStudio(packet);
                    }

                    videoEncoder.releaseOutputBuffer(outputIndex, false);
                } catch (Exception e) {
                    if (senderActive) {
                        sendErrorCount++;
                    }
                }
            }
        }, "MobileStudioV2-Phase9-EncoderDrain");
        encoderDrainThread.start();
    }

    private byte[] buildVideoPacket(int type, byte[] data, int flags, long ptsUs) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream(40 + data.length);
        DataOutputStream out = new DataOutputStream(baos);
        out.writeInt(PACKET_MAGIC);
        out.writeInt(type);
        out.writeLong(++packetSequence);
        out.writeLong(System.currentTimeMillis());
        out.writeLong(ptsUs);
        out.writeInt(flags);
        out.writeInt(encoderWidth);
        out.writeInt(encoderHeight);
        out.writeInt(data.length);
        out.write(data);
        out.flush();
        return baos.toByteArray();
    }

    private synchronized void sendPacketToStudio(byte[] packet) {
        if (senderClientOut == null) {
            return;
        }
        try {
            senderClientOut.write(packet);
            senderClientOut.flush();
            packetsSent++;
            bytesSent += packet.length;
        } catch (IOException e) {
            sendErrorCount++;
            closeSenderClientQuietly();
            mainHandler.post(() -> setStatus("Studio disconnected. Encoder still active", true));
        }
    }

    private void releaseSenderSession(boolean finishing, String message) {
        if (releasingSession) {
            return;
        }
        releasingSession = true;

        senderActive = false;
        drainRunning = false;
        closeSenderServerQuietly();
        closeSenderClientQuietly();

        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        if (videoEncoder != null) {
            try {
                videoEncoder.stop();
            } catch (Exception ignored) {
            }
            try {
                videoEncoder.release();
            } catch (Exception ignored) {
            }
            videoEncoder = null;
        }
        if (encoderInputSurface != null) {
            try {
                encoderInputSurface.release();
            } catch (Exception ignored) {
            }
            encoderInputSurface = null;
        }
        if (mediaProjection != null) {
            try {
                mediaProjection.unregisterCallback(projectionCallback);
            } catch (Exception ignored) {
            }
            try {
                mediaProjection.stop();
            } catch (Exception ignored) {
            }
            mediaProjection = null;
        }

        projectionPermissionData = null;
        projectionPermissionResultCode = Activity.RESULT_CANCELED;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        stopKeepAliveService();

        if (!finishing && message != null) {
            setStatus(message, false);
        }
        updateButtons();
        updateMetrics();
        releasingSession = false;
    }

    private void startStudioClient() {
        if (studioClientRunning) {
            return;
        }

        String host = ipInput.getText().toString().trim();
        int port = parsePort(portInput.getText().toString().trim(), DEFAULT_PORT);
        if (host.isEmpty()) {
            setStatus("Enter Sender IP first", false);
            return;
        }

        studioClientRunning = true;
        studioConnectedAtMs = 0L;
        studioPacketsReceived = 0L;
        studioBytesReceived = 0L;
        studioConfigReceived = 0L;
        studioKeyFramesReceived = 0L;
        studioDeltaFramesReceived = 0L;
        studioLastPacketAtMs = 0L;
        studioLastPacketSummary = "none";
        decodedFrameCount = 0L;
        decoderInputDropCount = 0L;
        decoderErrorCount = 0L;
        decoderFormatChangeCount = 0L;
        lastDecodedFrameAtMs = 0L;
        decoderFormatSummary = "waiting";
        releaseStudioDecoder();
        if (decoderOutputSurface == null || !decoderOutputSurface.isValid()) {
            studioClientRunning = false;
            setStatus("Preview surface not ready yet", false);
            updateButtons();
            return;
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setStatus("Connecting to " + host + ":" + port + "...", true);
        updateButtons();

        studioReadThread = new Thread(() -> {
            try {
                Socket socket = new Socket();
                studioSocket = socket;
                socket.setTcpNoDelay(true);
                socket.connect(new InetSocketAddress(host, port), 3500);
                studioInput = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                studioConnectedAtMs = System.currentTimeMillis();
                mainHandler.post(() -> setStatus("Connected. Waiting for H.264 packets", true));

                while (studioClientRunning) {
                    readOneVideoPacket(studioInput);
                }
            } catch (IOException e) {
                if (studioClientRunning) {
                    mainHandler.post(() -> setStatus("Connection error/lost: " + safeMessage(e), false));
                }
            } finally {
                closeStudioSocketQuietly();
                releaseStudioDecoder();
                studioClientRunning = false;
                mainHandler.post(() -> {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    updateButtons();
                    updateMetrics();
                });
            }
        }, "MobileStudioV2-Phase9-StudioRead");
        studioReadThread.start();
    }

    private void readOneVideoPacket(DataInputStream input) throws IOException {
        int magic = input.readInt();
        if (magic != PACKET_MAGIC) {
            throw new IOException("Bad packet magic");
        }
        int type = input.readInt();
        long sequence = input.readLong();
        long sentAtMs = input.readLong();
        long ptsUs = input.readLong();
        int flags = input.readInt();
        int packetWidth = input.readInt();
        int packetHeight = input.readInt();
        int size = input.readInt();
        if (size < 0 || size > 2_000_000) {
            throw new IOException("Bad packet size: " + size);
        }
        byte[] data = new byte[size];
        input.readFully(data);

        studioPacketsReceived++;
        studioBytesReceived += size;
        studioLastPacketAtMs = System.currentTimeMillis();

        String label;
        if (type == PACKET_CONFIG) {
            studioConfigReceived++;
            label = "config";
        } else if (type == PACKET_KEY_FRAME) {
            studioKeyFramesReceived++;
            label = "key";
        } else {
            studioDeltaFramesReceived++;
            label = "delta";
        }
        studioLastPacketSummary = label + " seq=" + sequence + " size=" + readableBytes(size) + " latency=" + Math.max(0L, studioLastPacketAtMs - sentAtMs) + "ms pts=" + ptsUs + " flags=" + flags;

        feedStudioDecoder(type, data, flags, ptsUs, packetWidth, packetHeight);
        mainHandler.post(() -> setStatus("Connected. Decode preview active", true));
    }

    private synchronized void feedStudioDecoder(int type, byte[] data, int flags, long ptsUs, int packetWidth, int packetHeight) {
        if (decoderOutputSurface == null || !decoderOutputSurface.isValid()) {
            decoderErrorCount++;
            return;
        }

        try {
            ensureStudioDecoder(packetWidth, packetHeight);
            int inputIndex = videoDecoder.dequeueInputBuffer(2_000);
            if (inputIndex < 0) {
                decoderInputDropCount++;
                return;
            }

            ByteBuffer inputBuffer = videoDecoder.getInputBuffer(inputIndex);
            if (inputBuffer == null || data.length > inputBuffer.capacity()) {
                decoderInputDropCount++;
                return;
            }

            inputBuffer.clear();
            inputBuffer.put(data);
            int queueFlags = flags;
            if (type == PACKET_CONFIG) {
                queueFlags |= MediaCodec.BUFFER_FLAG_CODEC_CONFIG;
            }
            videoDecoder.queueInputBuffer(inputIndex, 0, data.length, Math.max(0L, ptsUs), queueFlags);
            drainStudioDecoderOutputs();
        } catch (Exception e) {
            decoderErrorCount++;
            decoderFormatSummary = "decoder error: " + shorten(safeMessage(e), 42);
        }
    }

    private synchronized void ensureStudioDecoder(int packetWidth, int packetHeight) throws IOException {
        int width = packetWidth > 0 ? packetWidth : TARGET_SHORT_SIDE;
        int height = packetHeight > 0 ? packetHeight : TARGET_SHORT_SIDE * 2;

        if (videoDecoder != null && decoderWidth == width && decoderHeight == height) {
            return;
        }

        releaseStudioDecoder();
        MediaFormat format = MediaFormat.createVideoFormat(AVC_MIME_TYPE, width, height);
        videoDecoder = MediaCodec.createDecoderByType(AVC_MIME_TYPE);
        videoDecoder.configure(format, decoderOutputSurface, null, 0);
        videoDecoder.start();
        decoderWidth = width;
        decoderHeight = height;
        decoderFormatSummary = "video/avc " + width + "x" + height + " -> TextureView";
    }

    private synchronized void drainStudioDecoderOutputs() {
        if (videoDecoder == null) {
            return;
        }

        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        while (true) {
            int outputIndex = videoDecoder.dequeueOutputBuffer(info, 0);
            if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                return;
            }
            if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat format = videoDecoder.getOutputFormat();
                decoderFormatChangeCount++;
                decoderFormatSummary = format.toString();
                continue;
            }
            if (outputIndex < 0) {
                return;
            }

            videoDecoder.releaseOutputBuffer(outputIndex, true);
            if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                decodedFrameCount++;
                lastDecodedFrameAtMs = System.currentTimeMillis();
            }
        }
    }

    private synchronized void releaseStudioDecoder() {
        if (videoDecoder != null) {
            try {
                videoDecoder.stop();
            } catch (Exception ignored) {
            }
            try {
                videoDecoder.release();
            } catch (Exception ignored) {
            }
            videoDecoder = null;
        }
        decoderWidth = 0;
        decoderHeight = 0;
    }

    private void stopStudioClient(String message) {
        studioClientRunning = false;
        closeStudioSocketQuietly();
        releaseStudioDecoder();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (statusView != null && "Studio".equalsIgnoreCase(mode)) {
            setStatus(message, false);
        }
        updateButtons();
        updateMetrics();
    }

    private void updateMetrics() {
        if (metricsView == null) {
            return;
        }
 if (metricsView == null) {
            return;
        }

        if ("Studio".equalsIgnoreCase(mode)) {
            long uptimeSec = studioConnectedAtMs > 0L && studioClientRunning ? Math.max(0L, (System.currentTimeMillis() - studioConnectedAtMs) / 1000L) : 0L;
            String packetAge = studioLastPacketAtMs > 0L ? Math.max(0L, (System.currentTimeMillis() - studioLastPacketAtMs) / 1000L) + "s ago" : "n/a";
            String frameAge = lastDecodedFrameAtMs > 0L ? Math.max(0L, (System.currentTimeMillis() - lastDecodedFrameAtMs) / 1000L) + "s ago" : "n/a";
            metricsView.setText(
                    "Decode preview metrics\n" +
                            "Client: " + (studioClientRunning ? "connected" : "inactive") + " | Surface: " + (decoderOutputSurface != null && decoderOutputSurface.isValid() ? "ready" : "not ready") + "\n" +
                            "Packets received: " + studioPacketsReceived + " | Bytes received: " + readableBytes(studioBytesReceived) + "\n" +
                            "Config: " + studioConfigReceived + " | Key frames: " + studioKeyFramesReceived + " | Delta: " + studioDeltaFramesReceived + "\n" +
                            "Decoded frames: " + decodedFrameCount + " | Drops: " + decoderInputDropCount + " | Errors: " + decoderErrorCount + "\n" +
                            "Last packet: " + packetAge + " | Last frame: " + frameAge + " | Uptime: " + uptimeSec + "s\n" +
                            "Decoder: " + shorten(decoderFormatSummary, 76)
            );
            return;
        }

        long uptimeSec = startedAtMs > 0L && senderActive ? Math.max(0L, (System.currentTimeMillis() - startedAtMs) / 1000L) : 0L;
        metricsView.setText(
                "H.264 LAN metrics\n" +
                        "Endpoint: " + getLocalIpv4Address() + ":" + DEFAULT_PORT + "\n" +
                        "Server: " + (senderServerSocket != null && !senderServerSocket.isClosed() ? "active" : "inactive") + " | Client: " + (senderClientOut != null ? "connected" : "none") + " | Accepted: " + acceptedClients + "\n" +
                        "Source: " + (sourceWidth > 0 ? sourceWidth + "x" + sourceHeight + " @ " + sourceDensity + " dpi" : "not active") + "\n" +
                        "Encoder: H.264 " + (encoderWidth > 0 ? encoderWidth + "x" + encoderHeight : "target pending") + " @ " + TARGET_FPS + " fps, " + String.format(Locale.US, "%.1f Mbps", TARGET_BITRATE / 1_000_000f) + "\n" +
                        "Encoded: " + readableBytes(encodedBytes) + " | Outputs: " + encodedOutputCount + " | Key: " + keyFrameCount + " | Config: " + codecConfigCount + "\n" +
                        "Sent: " + packetsSent + " packets | " + readableBytes(bytesSent) + " | Errors: " + sendErrorCount + "\n" +
                        "Format: " + outputFormatSummary + " | Uptime: " + uptimeSec + "s\n" +
                        "Network: LAN packet preview | File: off | Sound: off"
        );
    }

    private void updateButtons() {
        if ("Studio".equalsIgnoreCase(mode)) {
            setButtonEnabled(startButton, !studioClientRunning);
            setButtonEnabled(stopButton, studioClientRunning);
            if (ipInput != null) ipInput.setEnabled(!studioClientRunning);
            if (portInput != null) portInput.setEnabled(!studioClientRunning);
            return;
        }
        setButtonEnabled(requestButton, !senderActive);
        setButtonEnabled(startButton, !senderActive && projectionPermissionData != null);
        setButtonEnabled(stopButton, senderActive);
    }

    private void setStatus(String message, boolean good) {
        if (statusView == null) {
            return;
        }
 if (statusView != null) {
            statusView.setText("Status: " + message);
            statusView.setTextColor(good ? Color.rgb(21, 128, 61) : Color.rgb(68, 64, 60));
        }
        updateMetrics();
    }

    private int[] calculateEncoderSize(int width, int height) {
        int shortSide = Math.min(width, height);
        float scale = TARGET_SHORT_SIDE / (float) shortSide;
        int scaledWidth = makeEven(Math.round(width * scale));
        int scaledHeight = makeEven(Math.round(height * scale));
        return new int[]{scaledWidth, scaledHeight};
    }

    private int makeEven(int value) {
        int even = value % 2 == 0 ? value : value + 1;
        return Math.max(2, even);
    }

    private void startKeepAliveService() {
        Intent serviceIntent = MediaProjectionKeepAliveService.startIntent(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void stopKeepAliveService() {
        try {
            startService(MediaProjectionKeepAliveService.stopIntent(this));
        } catch (Exception ignored) {
            stopService(new Intent(this, MediaProjectionKeepAliveService.class));
        }
    }

    private synchronized void closeSenderClientQuietly() {
        try {
            if (senderClientOut != null) senderClientOut.close();
        } catch (IOException ignored) {
        }
        senderClientOut = null;
        try {
            if (senderClientSocket != null) senderClientSocket.close();
        } catch (IOException ignored) {
        }
        senderClientSocket = null;
    }

    private void closeSenderServerQuietly() {
        try {
            if (senderServerSocket != null) senderServerSocket.close();
        } catch (IOException ignored) {
        }
        senderServerSocket = null;
    }

    private void closeStudioSocketQuietly() {
        try {
            if (studioInput != null) studioInput.close();
        } catch (IOException ignored) {
        }
        studioInput = null;
        try {
            if (studioSocket != null) studioSocket.close();
        } catch (IOException ignored) {
        }
        studioSocket = null;
    }

    private int parsePort(String value, int fallback) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed > 0 && parsed <= 65535) return parsed;
        } catch (NumberFormatException ignored) {
        }
        return fallback;
    }

    private String getBestDefaultStudioIp() {
        String local = getLocalIpv4Address();
        if (local.startsWith("192.168.")) {
            int lastDot = local.lastIndexOf('.');
            if (lastDot > 0) return local.substring(0, lastDot + 1) + "51";
        }
        return "192.168.1.51";
    }

    private String getLocalIpv4Address() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback()) continue;
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
        return "0.0.0.0";
    }

    private String safeMessage(Throwable throwable) {
        String message = throwable == null ? null : throwable.getMessage();
        return message == null || message.trim().isEmpty() ? throwable.getClass().getSimpleName() : message;
    }

    private String readableBytes(long bytes) {
        if (bytes < 1024L) return bytes + " B";
        if (bytes < 1024L * 1024L) return String.format(Locale.US, "%.1f KB", bytes / 1024f);
        return String.format(Locale.US, "%.2f MB", bytes / (1024f * 1024f));
    }

    private String shorten(String value, int max) {
        if (value == null) return "none";
        if (value.length() <= max) return value;
        return value.substring(0, Math.max(0, max - 3)) + "...";
    }

    private LinearLayout baseContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24), dp(28), dp(24), dp(28));
        root.setBackgroundColor(Color.rgb(250, 250, 255));
        return root;
    }

    private ScrollView wrapInScroll(LinearLayout root) {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(Color.rgb(250, 250, 255));
        scrollView.addView(root, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        return scrollView;
    }

    private void addBadge(LinearLayout root) {
        TextView badge = new TextView(this);
        badge.setText("Phase 9");
        badge.setTextSize(14);
        badge.setTypeface(Typeface.DEFAULT_BOLD);
        badge.setTextColor(Color.rgb(124, 58, 237));
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(14), dp(8), dp(14), dp(8));
        badge.setBackground(makeRoundedBackground(Color.rgb(237, 233, 254), dp(18)));
        root.addView(badge, wrapCenteredWithBottom(dp(22)));
    }

    private void addTitle(LinearLayout root, String text) {
        TextView title = new TextView(this);
        title.setText(text);
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.rgb(28, 25, 23));
        title.setGravity(Gravity.CENTER);
        root.addView(title, fullWidthWrapWithBottom(dp(12)));
    }

    private void addDescription(LinearLayout root, String text) {
        TextView description = new TextView(this);
        description.setText(text);
        description.setTextSize(15);
        description.setTextColor(Color.rgb(87, 83, 78));
        description.setGravity(Gravity.CENTER);
        root.addView(description, fullWidthWrapWithBottom(dp(18)));
    }

    private TextView infoBox(String text) {
        TextView box = new TextView(this);
        box.setText(text);
        box.setTextSize(15);
        box.setTextColor(Color.rgb(68, 64, 60));
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(14), dp(14), dp(14), dp(14));
        box.setBackground(makeRoundedBackground(Color.WHITE, dp(18), Color.rgb(221, 214, 254)));
        return box;
    }

    private TextView statusBox(String text, boolean good) {
        TextView box = new TextView(this);
        box.setText(text);
        box.setTextSize(16);
        box.setTypeface(Typeface.DEFAULT_BOLD);
        box.setTextColor(good ? Color.rgb(21, 128, 61) : Color.rgb(68, 64, 60));
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(14), dp(12), dp(14), dp(12));
        box.setBackground(makeRoundedBackground(Color.WHITE, dp(18), null));
        return box;
    }

    private TextView metricsBox(String text) {
        TextView box = new TextView(this);
        box.setText(text);
        box.setTextSize(14);
        box.setTextColor(Color.rgb(87, 83, 78));
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(12), dp(14), dp(12), dp(14));
        box.setBackground(makeRoundedBackground(Color.rgb(245, 245, 244), dp(18), null));
        return box;
    }

    private TextView actionButton(String text, int color) {
        TextView button = new TextView(this);
        button.setText(text);
        button.setTextSize(16);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setTextColor(Color.WHITE);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(18), dp(14), dp(18), dp(14));
        button.setBackground(makeRoundedBackground(color, dp(18), null));
        return button;
    }

    private void setButtonEnabled(TextView button, boolean enabled) {
        if (button == null) return;
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1f : 0.45f);
    }

    private GradientDrawable makeRoundedBackground(int color, int radius) {
        return makeRoundedBackground(color, radius, null);
    }

    private GradientDrawable makeRoundedBackground(int color, int radius, Integer strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (strokeColor != null) {
            drawable.setStroke(dp(1), strokeColor);
        }
        return drawable;
    }

    private LinearLayout.LayoutParams fullWidthWrapWithBottom(int bottomMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, bottomMargin);
        return params;
    }

    private LinearLayout.LayoutParams fullWidthHeightWithBottom(int height, int bottomMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height);
        params.setMargins(0, 0, 0, bottomMargin);
        return params;
    }

    private LinearLayout.LayoutParams wrapCentered() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
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
}
