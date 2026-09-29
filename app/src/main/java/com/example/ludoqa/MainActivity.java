package com.example.ludoqa;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int PORT = 5050;
    private static final int BG = Color.rgb(10, 15, 24);
    private static final int CARD = Color.rgb(20, 28, 41);
    private static final int CARD_2 = Color.rgb(27, 37, 54);
    private static final int TEXT = Color.rgb(245, 247, 250);
    private static final int MUTED = Color.rgb(159, 171, 190);
    private static final int ACCENT = Color.rgb(76, 175, 80);

    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final Random random = new Random();
    private Socket client;
    private OutputStream clientOut;
    private ServerSocket server;
    private volatile boolean destroyed;

    private TextView connectionText;
    private TextView receivedPlayer;
    private TextView receivedDice;
    private TextView receivedRaw;
    private EditText hostInput;
    private Button connectButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showModeChooser();
    }

    private void showModeChooser() {
        LinearLayout root = baseColumn();
        root.setPadding(dp(22), dp(26), dp(22), dp(24));

        TextView brand = text("LUDO QA", 30, TEXT, Typeface.BOLD);
        root.addView(brand);
        root.addView(text("Controller & Test Receiver", 15, MUTED, Typeface.NORMAL), lpWrap());

        LinearLayout hero = card();
        hero.setPadding(dp(20), dp(20), dp(20), dp(20));
        TextView icon = text("◉", 42, Color.rgb(76, 175, 80), Typeface.BOLD);
        hero.addView(icon, lpWrap());
        TextView title = text("One APK. Two phones.\nOne QA connection.", 25, TEXT, Typeface.BOLD);
        hero.addView(title, lp(LinearLayout.LayoutParams.MATCH_PARENT, -2, 0, 14, 0, 0));
        hero.addView(text("Use Controller Mode on one phone and Receiver Mode on the other. Both phones run this same APK.", 14, MUTED, Typeface.NORMAL), lpWrap());
        root.addView(hero, lp(LinearLayout.LayoutParams.MATCH_PARENT, -2, 0, 22, 0, 0));

        Button controller = bigButton("🎮   CONTROLLER MODE", ACCENT);
        controller.setOnClickListener(v -> showController());
        root.addView(controller, lp(LinearLayout.LayoutParams.MATCH_PARENT, dp(62), 0, 0, 0, 12));

        Button receiver = bigButton("🧪   RECEIVER MODE", Color.rgb(33, 150, 243));
        receiver.setOnClickListener(v -> showReceiver());
        root.addView(receiver, lp(LinearLayout.LayoutParams.MATCH_PARENT, dp(62), 0, 0, 0, 18));

        LinearLayout note = card();
        note.setPadding(dp(16), dp(14), dp(16), dp(14));
        note.addView(text("QA TEST ENVIRONMENT", 12, Color.rgb(255, 193, 7), Typeface.BOLD));
        note.addView(text("The receiver is a test environment controlled by this project. It does not modify or inject values into third-party game apps.", 13, MUTED, Typeface.NORMAL), lp(LinearLayout.LayoutParams.MATCH_PARENT, -2, 0, 7, 0, 0));
        root.addView(note, lpWrap());
        setContentView(scroll(root));
    }

    private void showController() {
        LinearLayout root = baseColumn();
        root.setPadding(dp(16), dp(20), dp(16), dp(22));
        root.addView(topBar("🎮  Controller", "Send QA dice commands"));

        LinearLayout connection = card();
        connection.setPadding(dp(16), dp(16), dp(16), dp(16));
        connection.addView(text("CONNECTION", 12, MUTED, Typeface.BOLD));
        connectionText = text("●  Offline", 16, Color.rgb(244, 67, 54), Typeface.BOLD);
        connection.addView(connectionText, lp(LinearLayout.LayoutParams.MATCH_PARENT, -2, 0, 8, 0, 0));
        hostInput = new EditText(this);
        hostInput.setHint("Receiver IP  e.g. 192.168.1.20");
        hostInput.setHintTextColor(Color.rgb(112, 124, 143));
        hostInput.setTextColor(TEXT);
        hostInput.setSingleLine(true);
        hostInput.setInputType(InputType.TYPE_CLASS_PHONE);
        hostInput.setPadding(dp(14), 0, dp(14), 0);
        hostInput.setBackground(round(Color.rgb(31, 42, 60), 14));
        connection.addView(hostInput, lp(LinearLayout.LayoutParams.MATCH_PARENT, dp(52), 0, 0, 0, 10));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        connectButton = smallButton("CONNECT", Color.rgb(33, 150, 243));
        connectButton.setOnClickListener(v -> connectToReceiver());
        Button ping = smallButton("PING", Color.rgb(82, 96, 112));
        ping.setOnClickListener(v -> send("PING|SYSTEM|0"));
        row.addView(connectButton, lp(0, dp(48), 1, 0, 8, 0));
        row.addView(ping, lp(0, dp(48), 1, 0, 0, 0));
        connection.addView(row, lpWrap());
        root.addView(connection, lp(LinearLayout.LayoutParams.MATCH_PARENT, -2, 0, 0, 0, 16));

        root.addView(sectionTitle("DICE OVERRIDE", "Choose a player, then send a test value 1–6."), lpWrap());
        addPlayer(root, "PLAYER 1", "BLUE", Color.rgb(41, 121, 255));
        addPlayer(root, "PLAYER 2", "RED", Color.rgb(239, 83, 80));
        addPlayer(root, "PLAYER 3", "GREEN", Color.rgb(67, 160, 71));
        addPlayer(root, "PLAYER 4", "YELLOW", Color.rgb(251, 192, 45));

        Button normal = bigButton("🎲  RANDOM / NORMAL", Color.rgb(66, 78, 96));
        normal.setOnClickListener(v -> send("RANDOM|SYSTEM|" + (random.nextInt(6) + 1)));
        root.addView(normal, lp(LinearLayout.LayoutParams.MATCH_PARENT, dp(54), 0, 14, 0, 0));

        Button back = ghostButton("←  Change Mode");
        back.setOnClickListener(v -> showModeChooser());
        root.addView(back, lp(LinearLayout.LayoutParams.MATCH_PARENT, dp(48), 0, 8, 0, 0));
        setContentView(scroll(root));
    }

    private void addPlayer(LinearLayout root, String label, String player, int accent) {
        LinearLayout card = card();
        card.setPadding(dp(14), dp(13), dp(14), dp(14));
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView dot = text("●", 17, accent, Typeface.BOLD);
        titleRow.addView(dot, lp(dp(28), dp(28), 0, 0, 8, 0));
        titleRow.addView(text(label + "  ·  " + player, 17, TEXT, Typeface.BOLD), lp(0, -2, 1, 0, 0, 0));
        card.addView(titleRow, lpWrap());

        LinearLayout dice = new LinearLayout(this);
        dice.setGravity(Gravity.CENTER);
        for (int i = 1; i <= 6; i++) {
            final int value = i;
            Button b = diceButton(String.valueOf(i), accent);
            b.setOnClickListener(v -> send(player + "|" + player + "|" + value));
            dice.addView(b, lp(0, dp(48), 1, 8, 0, 0));
        }
        card.addView(dice, lp(LinearLayout.LayoutParams.MATCH_PARENT, dp(64), 0, 8, 0, 0));
        Button rnd = diceButton("RND", Color.rgb(91, 105, 124));
        rnd.setOnClickListener(v -> send(player + "|" + player + "|" + (random.nextInt(6) + 1)));
        card.addView(rnd, lp(LinearLayout.LayoutParams.MATCH_PARENT, dp(42), 0, 6, 0, 0));
        root.addView(card, lp(LinearLayout.LayoutParams.MATCH_PARENT, -2, 0, 0, 0, 10));
    }

    private void showReceiver() {
        LinearLayout root = baseColumn();
        root.setPadding(dp(16), dp(20), dp(16), dp(22));
        root.addView(topBar("🧪  Test Receiver", "Listen for QA commands"));

        LinearLayout status = card();
        status.setPadding(dp(18), dp(18), dp(18), dp(18));
        status.addView(text("RECEIVER STATUS", 12, MUTED, Typeface.BOLD));
        connectionText = text("●  Starting server…", 17, Color.rgb(255, 193, 7), Typeface.BOLD);
        status.addView(connectionText, lp(LinearLayout.MATCH_PARENT, -2, 0, 7, 0, 0));
        String ip = getLocalIp();
        status.addView(text("IP ADDRESS", 11, MUTED, Typeface.BOLD), lp(LinearLayout.MATCH_PARENT, -2, 0, 18, 0, 0));
        TextView ipView = text(ip, 24, TEXT, Typeface.BOLD);
        status.addView(ipView, lpWrap());
        status.addView(text("Port  " + PORT + "   •   Tell the controller phone to connect to this IP.", 13, MUTED, Typeface.NORMAL), lp(LinearLayout.MATCH_PARENT, -2, 0, 5, 0, 0));
        root.addView(status, lp(LinearLayout.LayoutParams.MATCH_PARENT, -2, 0, 0, 0, 16));

        LinearLayout result = card();
        result.setPadding(dp(18), dp(18), dp(18), dp(18));
        result.addView(text("LAST RECEIVED COMMAND", 12, MUTED, Typeface.BOLD));
        receivedPlayer = text("—", 20, TEXT, Typeface.BOLD);
        result.addView(receivedPlayer, lp(LinearLayout.LayoutParams.MATCH_PARENT, -2, 0, 10, 0, 0));
        receivedDice = text("—", 64, Color.rgb(76, 175, 80), Typeface.BOLD);
        receivedDice.setGravity(Gravity.CENTER);
        result.addView(receivedDice, lp(LinearLayout.LayoutParams.MATCH_PARENT, dp(92), 0, 4, 0, 0));
        receivedRaw = text("Waiting for a command…", 13, MUTED, Typeface.NORMAL);
        receivedRaw.setGravity(Gravity.CENTER);
        result.addView(receivedRaw, lp(LinearLayout.LayoutParams.MATCH_PARENT, -2, 0, 4, 0, 0));
        root.addView(result, lp(LinearLayout.LayoutParams.MATCH_PARENT, -2, 0, 0, 0, 16));

        root.addView(sectionTitle("TEST BOARD", "A simple board display for validating the controller link."), lpWrap());
        LinearLayout board = card();
        board.setPadding(dp(14), dp(14), dp(14), dp(14));
        String[] names = {"BLUE", "RED", "GREEN", "YELLOW"};
        int[] colors = {Color.rgb(41,121,255), Color.rgb(239,83,80), Color.rgb(67,160,71), Color.rgb(251,192,45)};
        for (int i = 0; i < names.length; i++) {
            LinearLayout r = new LinearLayout(this);
            r.setGravity(Gravity.CENTER_VERTICAL);
            TextView d = text("●", 16, colors[i], Typeface.BOLD);
            r.addView(d, lp(dp(28), dp(38), 0, 0, 7, 0));
            r.addView(text(names[i], 15, TEXT, Typeface.BOLD), lp(0, dp(38), 1, 0, 0, 0));
            r.addView(text("Ready", 13, MUTED, Typeface.NORMAL), lpWrap());
            board.addView(r, lp(LinearLayout.LayoutParams.MATCH_PARENT, dp(42), 0, 2, 0, 2));
        }
        root.addView(board, lp(LinearLayout.LayoutParams.MATCH_PARENT, -2, 0, 0, 0, 10));
        Button back = ghostButton("←  Change Mode");
        back.setOnClickListener(v -> stopServerAndBack());
        root.addView(back, lp(LinearLayout.LayoutParams.MATCH_PARENT, dp(48), 0, 8, 0, 0));
        setContentView(scroll(root));
        startServer();
    }

    private void startServer() {
        pool.execute(() -> {
            try {
                if (server != null && !server.isClosed()) server.close();
                server = new ServerSocket(PORT);
                runOnUiThread(() -> setStatus("●  Waiting for Controller", Color.rgb(255, 193, 7)));
                while (!destroyed && !server.isClosed()) {
                    Socket s = server.accept();
                    runOnUiThread(() -> setStatus("●  Controller connected", ACCENT));
                    listenForCommands(s);
                }
            } catch (Exception e) {
                if (!destroyed) runOnUiThread(() -> setStatus("●  Receiver error", Color.rgb(244, 67, 54)));
            }
        });
    }

    private void listenForCommands(Socket s) {
        pool.execute(() -> {
            try (Socket socket = s; BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null && !destroyed) {
                    handleCommand(line.trim());
                }
            } catch (Exception ignored) {
            }
            if (!destroyed) runOnUiThread(() -> setStatus("●  Waiting for Controller", Color.rgb(255, 193, 7)));
        });
    }

    private void handleCommand(String line) {
        if (line.length() == 0) return;
        String[] p = line.split("\\|", -1);
        if (p.length >= 3 && !"PING".equalsIgnoreCase(p[0])) {
            String player = p[1];
            String dice = p[2];
            runOnUiThread(() -> {
                if (receivedPlayer != null) receivedPlayer.setText("●  " + player + " PLAYER");
                if (receivedDice != null) receivedDice.setText(dice);
                if (receivedRaw != null) receivedRaw.setText("Received: " + line);
            });
        }
    }

    private void connectToReceiver() {
        final String host = hostInput == null ? "" : hostInput.getText().toString().trim();
        if (host.length() == 0) {
            Toast.makeText(this, "Enter the Receiver IP address", Toast.LENGTH_SHORT).show();
            return;
        }
        setStatus("●  Connecting…", Color.rgb(255, 193, 7));
        pool.execute(() -> {
            try {
                closeClient();
                client = new Socket(host, PORT);
                clientOut = client.getOutputStream();
                runOnUiThread(() -> {
                    setStatus("●  Connected", ACCENT);
                    if (connectButton != null) connectButton.setText("CONNECTED");
                });
            } catch (Exception e) {
                runOnUiThread(() -> setStatus("●  Connection failed", Color.rgb(244, 67, 54)));
            }
        });
    }

    private void send(String command) {
        pool.execute(() -> {
            try {
                if (client == null || client.isClosed() || clientOut == null) {
                    runOnUiThread(() -> Toast.makeText(this, "Connect to Receiver first", Toast.LENGTH_SHORT).show());
                    return;
                }
                clientOut.write((command + "\n").getBytes(StandardCharsets.UTF_8));
                clientOut.flush();
                runOnUiThread(() -> setStatus("●  Sent  " + command, ACCENT));
            } catch (Exception e) {
                runOnUiThread(() -> setStatus("●  Send failed", Color.rgb(244, 67, 54)));
            }
        });
    }

    private void setStatus(String value, int color) {
        if (connectionText != null) {
            connectionText.setText(value);
            connectionText.setTextColor(color);
        }
    }

    private void stopServerAndBack() {
        try { if (server != null) server.close(); } catch (Exception ignored) {}
        showModeChooser();
    }

    private void closeClient() {
        try { if (client != null) client.close(); } catch (Exception ignored) {}
        client = null;
        clientOut = null;
    }

    private String getLocalIp() {
        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            if (wm != null) {
                WifiInfo info = wm.getConnectionInfo();
                int ip = info.getIpAddress();
                return String.format(Locale.US, "%d.%d.%d.%d", ip & 0xff, (ip >> 8) & 0xff, (ip >> 16) & 0xff, (ip >> 24) & 0xff);
            }
        } catch (Exception ignored) {}
        return "Unavailable — connect to Wi-Fi";
    }

    private LinearLayout baseColumn() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackgroundColor(BG);
        return l;
    }

    private ScrollView scroll(View child) {
        ScrollView s = new ScrollView(this);
        s.setFillViewport(true);
        s.setBackgroundColor(BG);
        s.addView(child);
        return s;
    }

    private LinearLayout topBar(String title, String subtitle) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        TextView t = text(title, 27, TEXT, Typeface.BOLD);
        row.addView(t, lpWrap());
        row.addView(text(subtitle, 13, MUTED, Typeface.NORMAL), lp(LinearLayout.LayoutParams.MATCH_PARENT, -2, 0, 3, 0, 18));
        return row;
    }

    private LinearLayout sectionTitle(String title, String subtitle) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        TextView t = text(title, 13, TEXT, Typeface.BOLD);
        wrap.addView(t, lpWrap());
        wrap.addView(text(subtitle, 12, MUTED, Typeface.NORMAL), lp(LinearLayout.LayoutParams.MATCH_PARENT, -2, 0, 4, 0, 10));
        return wrap;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(round(CARD, 18));
        return c;
    }

    private Button bigButton(String label, int color) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setTextSize(15);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setBackground(round(color, 16));
        return b;
    }

    private Button smallButton(String label, int color) {
        return bigButton(label, color);
    }

    private Button diceButton(String label, int color) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(TEXT);
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setPadding(0, 0, 0, 0);
        b.setBackground(round(Color.rgb(34, 46, 65), 12));
        return b;
    }

    private Button ghostButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(MUTED);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setBackground(round(Color.TRANSPARENT, 12));
        return b;
    }

    private TextView text(String value, float size, int color, int style) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setTypeface(Typeface.DEFAULT, style);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        return g;
    }

    private LinearLayout.LayoutParams lpWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams lp(int w, int h, float weight, int l, int t, int r) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h, weight);
        p.setMargins(dp(l), dp(t), dp(r), 0);
        return p;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        closeClient();
        try { if (server != null) server.close(); } catch (Exception ignored) {}
        pool.shutdownNow();
        super.onDestroy();
    }
}
