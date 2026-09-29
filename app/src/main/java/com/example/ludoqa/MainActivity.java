package com.example.ludoqa;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class MainActivity extends Activity {

    final int PORT = 47621;

    LinearLayout root, content;
    TextView status;
    ExecutorService pool = Executors.newCachedThreadPool();
    ServerSocket server;
    volatile Socket client;

    final int BG = Color.rgb(12, 17, 24);
    final int CARD = Color.rgb(25, 33, 43);
    final int CARD2 = Color.rgb(31, 41, 53);
    final int TEXT = Color.WHITE;
    final int MUTED = Color.rgb(165, 177, 190);
    final int BORDER = Color.rgb(55, 68, 83);
    final int BLUE = Color.rgb(38, 180, 235);
    final int RED = Color.rgb(244, 74, 125);
    final int GREEN = Color.rgb(45, 220, 115);
    final int YELLOW = Color.rgb(247, 214, 35);

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        showHome();
    }

    int dp(float v) {
        return (int)(v * getResources().getDisplayMetrics().density + 0.5f);
    }

    TextView text(String s, float size, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    GradientDrawable bg(int color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        return g;
    }

    GradientDrawable outline(int color, int stroke, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        g.setStroke(dp(1), stroke);
        return g;
    }

    Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(TEXT);
        b.setTextSize(15);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setGravity(Gravity.CENTER);
        b.setPadding(0, 0, 0, 0);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setBackground(outline(CARD2, BORDER, 12));
        return b;
    }

    void base(String title) {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        TextView header = text(title, 21, TEXT);
        header.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.setPadding(dp(20), 0, dp(20), 0);
        header.setBackgroundColor(BG);

        root.addView(header,
                new LinearLayout.LayoutParams(-1, dp(64)));

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(8), dp(16), dp(24));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(content);

        root.addView(scroll,
                new LinearLayout.LayoutParams(-1, 0, 1));

        setContentView(root);
    }

    void showHome() {
        base("Ludo QA Controller");

        TextView subtitle = text(
                "Research / test environment",
                17,
                TEXT
        );
        subtitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        content.addView(subtitle,
                new LinearLayout.LayoutParams(-1, dp(45)));

        TextView info = text(
                "Choose which role this phone should run.",
                14,
                MUTED
        );
        content.addView(info,
                new LinearLayout.LayoutParams(-1, dp(40)));

        Button controller = button("🎮   CONTROLLER");
        controller.setTextSize(16);
        controller.setBackground(outline(CARD, BORDER, 16));
        controller.setOnClickListener(v -> showController());

        LinearLayout.LayoutParams cp =
                new LinearLayout.LayoutParams(-1, dp(58));
        cp.setMargins(0, dp(12), 0, dp(10));
        content.addView(controller, cp);

        Button receiver = button("🧪   TEST GAME RECEIVER");
        receiver.setTextSize(16);
        receiver.setOnClickListener(v -> showReceiver());

        LinearLayout.LayoutParams rp =
                new LinearLayout.LayoutParams(-1, dp(58));
        rp.setMargins(0, 0, 0, dp(18));
        content.addView(receiver, rp);

        TextView note = text(
                "The receiver is a simple local QA endpoint. " +
                "It is not connected to the official Ludo King service.",
                13,
                MUTED
        );
        note.setPadding(dp(4), dp(8), dp(4), dp(8));
        content.addView(note);
    }

    void showController() {
        base("Ludo QA Controller");

        // Connection card
        LinearLayout connection = new LinearLayout(this);
        connection.setOrientation(LinearLayout.VERTICAL);
        connection.setPadding(dp(16), dp(14), dp(16), dp(14));
        connection.setBackground(bg(CARD, 18));

        LinearLayout.LayoutParams connParams =
                new LinearLayout.LayoutParams(-1, dp(112));
        connParams.setMargins(0, 0, 0, dp(18));
        content.addView(connection, connParams);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);

        status = text("●  Disconnected", 15, Color.rgb(255, 100, 100));
        status.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        top.addView(status,
                new LinearLayout.LayoutParams(0, dp(48), 1));

        Button disconnect = button("Disconnect");
        disconnect.setTextSize(13);
        disconnect.setTextColor(TEXT);
        disconnect.setBackground(outline(CARD2, BORDER, 12));

        top.addView(disconnect,
                new LinearLayout.LayoutParams(dp(110), dp(46)));

        connection.addView(top);

        LinearLayout ipRow = new LinearLayout(this);
        ipRow.setGravity(Gravity.CENTER_VERTICAL);

        EditText ip = new EditText(this);
        ip.setHint("Receiver IP address");
        ip.setHintTextColor(MUTED);
        ip.setTextColor(TEXT);
        ip.setSingleLine(true);
        ip.setTextSize(14);
        ip.setPadding(dp(12), 0, dp(12), 0);
        ip.setBackground(outline(CARD2, BORDER, 12));

        ipRow.addView(ip,
                new LinearLayout.LayoutParams(0, dp(48), 1));

        Button connect = button("Connect");
        connect.setTextSize(13);
        connect.setBackground(bg(Color.rgb(38, 130, 180), 12));

        LinearLayout.LayoutParams conBtn =
                new LinearLayout.LayoutParams(dp(95), dp(48));
        conBtn.setMargins(dp(8), 0, 0, 0);
        ipRow.addView(connect, conBtn);

        connection.addView(ipRow);

        connect.setOnClickListener(v ->
                connectTo(ip.getText().toString().trim()));

        disconnect.setOnClickListener(v -> disconnectClient());

        TextView heading = text(
                "Player Dice Overrides  (Tap number to set)",
                15,
                TEXT
        );
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.setPadding(dp(4), 0, 0, dp(8));
        content.addView(heading);

        addPlayer("Player 1", "BLUE", BLUE);
        addPlayer("Player 2", "RED", RED);
        addPlayer("Player 3", "GREEN", GREEN);
        addPlayer("Player 4", "YELLOW", YELLOW);
    }

    void addPlayer(String playerName, String player, int playerColor) {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp
rm -f app-debug.apk
git rm --cached app-debug.apk 2>/dev/null || true
sed -i "s/versionCode 2; versionName '1.1'/versionCode 3; versionName '1.2'/" app/build.gradle
cat > .gitignore <<'EOF'
*.apk
*.aab
.gradle/
build/
app/build/
