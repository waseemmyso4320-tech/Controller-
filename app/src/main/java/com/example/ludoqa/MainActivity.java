package com.example.ludoqa;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private LinearLayout root;
    private TextView statusText;
    private Button connectionButton;
    private Socket client;
    private OutputStream output;
    private final Handler handler = new Handler();

    private String host = "";
    private int port = 5000;

    private static final int BG = Color.rgb(18, 20, 25);
    private static final int CARD = Color.rgb(29, 32, 39);
    private static final int CARD2 = Color.rgb(39, 42, 50);
    private static final int TEXT = Color.WHITE;
    private static final int MUTED = Color.rgb(165, 170, 180);

    private static final int BLUE = Color.rgb(45, 190, 245);
    private static final int RED = Color.rgb(245, 70, 125);
    private static final int GREEN = Color.rgb(40, 220, 115);
    private static final int YELLOW = Color.rgb(245, 215, 45);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildInterface();
    }

    private void buildInterface() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(10), dp(12), dp(22));
        root.setBackgroundColor(BG);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        scroll.addView(root);
        setContentView(scroll);

        TextView title = text("Ludo QA Controller", 24, TEXT, true);
        title.setPadding(dp(4), dp(4), dp(4), dp(12));
        root.addView(title);

        addConnectionCard();

        TextView section = text("Player Dice Overrides", 19, TEXT, true);
        section.setPadding(dp(4), dp(18), dp(4), dp(4));
        root.addView(section);

        TextView hint = text("Tap a number to set the next test dice value", 12, MUTED, false);
        hint.setPadding(dp(4), 0, dp(4), dp(8));
        root.addView(hint);

        addPlayer("Player 1", "BLUE", BLUE);
        addPlayer("Player 2", "RED", RED);
        addPlayer("Player 3", "GREEN", GREEN);
        addPlayer("Player 4", "YELLOW", YELLOW);

        LinearLayout normalCard = card();
        Button normal = button("🎲  Normal / Random Dice", Color.rgb(55, 59, 68));
        normal.setOnClickListener(v -> {
            int dice = new Random().nextInt(6) + 1;
            sendCommand("NORMAL," + dice);
            Toast.makeText(this, "Random test dice: " + dice, Toast.LENGTH_SHORT).show();
        });
        normalCard.addView(normal);
        root.addView(normalCard);

        LinearLayout pingCard = card();
        Button ping = button("Ping Receiver", Color.rgb(55, 59, 68));
        ping.setOnClickListener(v -> sendCommand("PING"));
        pingCard.addView(ping);
        root.addView(pingCard);

        TextView footer = text("QA / TEST ENVIRONMENT", 10, Color.rgb(100, 105, 115), true);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(14), 0, 0);
        root.addView(footer);
    }

    private void addConnectionCard() {
        LinearLayout c = card();

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout statusColumn = new LinearLayout(this);
        statusColumn.setOrientation(LinearLayout.VERTICAL);
        statusColumn.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));

        statusText = text("●  Disconnected", 16, Color.rgb(255, 90, 90), true);
        statusColumn.addView(statusText);

        TextView receiver = text("QA Test Receiver", 12, MUTED, false);
        receiver.setPadding(0, dp(4), 0, 0);
        statusColumn.addView(receiver);

        row.addView(statusColumn);

        connectionButton = button("CONNECT", Color.rgb(45, 145, 235));
        connectionButton.setOnClickListener(v -> showConnectionDialog());
        row.addView(connectionButton, new LinearLayout.LayoutParams(dp(112), dp(46)));

        c.addView(row);
        root.addView(c);
    }

    private void addPlayer(String name, String player, int color) {
        LinearLayout playerCard = card();

        LinearLayout heading = new LinearLayout(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);

        TextView dot = text("●", 20, color, true);
        heading.addView(dot, new LinearLayout.LayoutParams(dp(30), dp(40)));

        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);

        TextView playerName = text(name + "  (" + player + ")", 17, TEXT, true);
        names.addView(playerName);

        TextView active = text("Active: RND", 12, MUTED, false);
        names.addView(active);

        heading.addView(names, new LinearLayout.LayoutParams(0, -2, 1));
        playerCard.addView(heading);

        LinearLayout numbers = new LinearLayout(this);
        numbers.setGravity(Gravity.CENTER_VERTICAL);
        numbers.setPadding(0, dp(6), 0, dp(2));

        Button[] diceButtons = new Button[6];

        for (int d = 1; d <= 6; d++) {
            final int dice = d;
            Button b = smallButton(String.valueOf(d), color);
            diceButtons[d - 1] = b;

            b.setOnClickListener(v -> {
                for (Button other : diceButtons) {
                    setButtonBackground(other, CARD2);
                }
                setButtonBackground(b, color);
                active.setText("Active: " + dice);
                active.setTextColor(color);
                sendCommand("PLAYER=" + player + ";DICE=" + dice);
            });

            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(44), 1);
            p.setMargins(dp(2), 0, dp(2), 0);
            numbers.addView(b, p);
        }

        Button rnd = smallButton("RND", Color.rgb(95, 100, 110));
        rnd.setOnClickListener(v -> {
            for (Button other : diceButtons) {
                setButtonBackground(other, CARD2);
            }
            setButtonBackground(rnd, Color.rgb(75, 80, 90));
            active.setText("Active: RND");
            active.setTextColor(MUTED);
            sendCommand("PLAYER=" + player + ";DICE=RND");
        });

        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(dp(62), dp(44));
        rp.setMargins(dp(4), 0, 0, 0);
        numbers.addView(rnd, rp);

        playerCard.addView(numbers);
        root.addView(playerCard);
    }

    private void showConnectionDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(2), dp(20), dp(2));

        EditText ip = new EditText(this);
        ip.setHint("Receiver IP address");
        ip.setSingleLine(true);
        ip.setText(host);
        layout.addView(ip);

        EditText portBox = new EditText(this);
        portBox.setHint("Port");
        portBox.setSingleLine(true);
        portBox.setInputType(2);
        portBox.setText(String.valueOf(port));
        layout.addView(portBox);

        new AlertDialog.Builder(this)
                .setTitle("Connect to QA Receiver")
                .setView(layout)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Connect", (dialog, which) -> {
                    host = ip.getText().toString().trim();
                    try {
                        port = Integer.parseInt(portBox.getText().toString().trim());
                    } catch (Exception ignored) {
                        port = 5000;
                    }
                    connect();
                })
                .show();
    }

    private void connect() {
        if (host.isEmpty()) {
            Toast.makeText(this, "Enter receiver IP address", Toast.LENGTH_SHORT).show();
            return;
        }

        statusText.setText("●  Connecting...");
        statusText.setTextColor(Color.rgb(255, 190, 60));
        connectionButton.setText("CONNECTING");

        new Thread(() -> {
            try {
                Socket socket = new Socket(host, port);
                OutputStream stream = socket.getOutputStream();
                client = socket;
                output = stream;

                handler.post(() -> {
                    statusText.setText("●  Connected to QA Receiver");
                    statusText.setTextColor(GREEN);
                    connectionButton.setText("DISCONNECT");
                    connectionButton.setOnClickListener(v -> disconnect());
                });
            } catch (Exception e) {
                handler.post(() -> {
                    statusText.setText("●  Connection failed");
                    statusText.setTextColor(Color.rgb(255, 90, 90));
                    connectionButton.setText("CONNECT");
                    connectionButton.setOnClickListener(v -> showConnectionDialog());
                    Toast.makeText(this, "Unable to connect", Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void disconnect() {
        try {
            if (client != null) client.close();
        } catch (Exception ignored) {
        }
        client = null;
        output = null;
        statusText.setText("●  Disconnected");
        statusText.setTextColor(Color.rgb(255, 90, 90));
        connectionButton.setText("CONNECT");
        connectionButton.setOnClickListener(v -> showConnectionDialog());
    }

    private void sendCommand(String command) {
        if (output == null || client == null || client.isClosed()) {
            Toast.makeText(this, "Not connected to QA receiver", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                output.write((command + "\n").getBytes(StandardCharsets.UTF_8));
                output.flush();
            } catch (Exception ignored) {
            }
        }).start();
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(12), dp(10), dp(12), dp(10));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD);
        bg.setCornerRadius(dp(17));
        bg.setStroke(dp(1), Color.rgb(48, 52, 62));
        c.setBackground(bg);

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, dp(4), 0, dp(4));
        c.setLayoutParams(p);
        return c;
    }

    private Button button(String label, int color) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(TEXT);
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        setButtonBackground(b, color);
        return b;
    }

    private Button smallButton(String label, int color) {
        Button b = button(label, CARD2);
        b.setTextSize(14);
        return b;
    }

    private void setButtonBackground(View view, int color) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(11));
        view.setBackground(bg);
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        return t;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (client != null) client.close();
        } catch (Exception ignored) {
        }
    }
}
