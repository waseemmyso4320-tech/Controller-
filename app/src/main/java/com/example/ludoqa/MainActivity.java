package com.example.ludoqa;

import android.app.*;
import android.os.*;
import android.graphics.Color;
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

    @Override public void onCreate(Bundle b) { super.onCreate(b); showHome(); }

    TextView tv(String s, int sp) { TextView t=new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(Color.DKGRAY); t.setPadding(16,12,16,12); return t; }
    Button btn(String s) { Button b=new Button(this); b.setText(s); b.setAllCaps(false); return b; }

    void base(String title) {
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.rgb(248,249,251));
        TextView bar=tv(title,22); bar.setTextColor(Color.WHITE); bar.setGravity(Gravity.CENTER_VERTICAL); bar.setBackgroundColor(Color.rgb(16,24,32)); bar.setPadding(20,18,20,18);
        root.addView(bar,new LinearLayout.LayoutParams(-1,64));
        content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(16,12,16,20);
        ScrollView scroll=new ScrollView(this); scroll.addView(content); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1)); setContentView(root);
    }

    void showHome(){
        base("Ludo QA Controller");
        content.addView(tv("Research / test environment",16));
        content.addView(tv("Choose which role this phone should run.",15));
        Button c=btn("🎮  Controller"); c.setOnClickListener(v->showController()); content.addView(c,new LinearLayout.LayoutParams(-1,60));
        Button r=btn("🧪  Test Game Receiver"); r.setOnClickListener(v->showReceiver()); content.addView(r,new LinearLayout.LayoutParams(-1,60));
        content.addView(tv("\nThe receiver is a simple local test endpoint. It is not connected to Ludo King.",13));
    }

    void showController(){
        base("Ludo QA Controller");
        LinearLayout conn=new LinearLayout(this); conn.setOrientation(LinearLayout.HORIZONTAL);
        EditText ip=new EditText(this); ip.setHint("Receiver IP, e.g. 192.168.1.20"); ip.setSingleLine();
        conn.addView(ip,new LinearLayout.LayoutParams(0,58,1)); Button connect=btn("Connect"); conn.addView(connect,new LinearLayout.LayoutParams(120,58)); content.addView(conn);
        status=tv("● Disconnected",15); content.addView(status);
        connect.setOnClickListener(v->connectTo(ip.getText().toString().trim()));
        String[] players={"BLUE","RED","GREEN","YELLOW"};
        for(String p:players){
            TextView h=tv(p+" PLAYER — Dice Override",18); h.setTextColor(Color.BLACK); content.addView(h);
            LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
            for(int d=1;d<=6;d++){ final int dice=d; Button b=btn(String.valueOf(d)); b.setOnClickListener(v->sendCommand(p,dice)); row.addView(b,new LinearLayout.LayoutParams(0,58,1)); }
            content.addView(row);
        }
        Button normal=btn("🎲 Normal / Random Dice"); normal.setOnClickListener(v->sendRaw("{\"type\":\"dice_mode\",\"mode\":\"normal\"}")); content.addView(normal);
        Button ping=btn("Ping Receiver"); ping.setOnClickListener(v->sendRaw("{\"type\":\"ping\"}")); content.addView(ping);
    }

    void showReceiver(){
        base("Test Game Receiver");
        status=tv("● Stopped",17); content.addView(status);
        Button start=btn("Start QA Receiver"); content.addView(start,new LinearLayout.LayoutParams(-1,60));
        Button stop=btn("Stop Receiver"); content.addView(stop,new LinearLayout.LayoutParams(-1,60));
        TextView log=tv("Waiting...",14); content.addView(log);
        start.setOnClickListener(v->{
            if(server!=null && !server.isClosed()) return;
            pool.execute(()->{ try{
                server=new ServerSocket(PORT);
                runOnUiThread(()->status.setText("● Listening on port "+PORT+"\nFind this phone's local IP in Wi‑Fi settings."));
                while(!server.isClosed()){
                    Socket s=server.accept(); client=s;
                    pool.execute(()->readClient(s,log));
                }
            }catch(Exception e){ runOnUiThread(()->status.setText("Receiver stopped: "+e.getMessage())); }});
        });
        stop.setOnClickListener(v->{ try{ if(server!=null)server.close(); }catch(Exception ignored){} });
    }

    void readClient(Socket s, TextView log){
        try(BufferedReader br=new BufferedReader(new InputStreamReader(s.getInputStream(),StandardCharsets.UTF_8))){
            String line; while((line=br.readLine())!=null){ final String x=line; runOnUiThread(()->log.setText("Last command:\n"+x)); }
        }catch(Exception ignored){} finally { try{s.close();}catch(Exception ignored){} }
    }

    void connectTo(String host){ pool.execute(()->{ try{ client=new Socket(); client.connect(new InetSocketAddress(host,PORT),2500); runOnUiThread(()->status.setText("● Connected to "+host+":"+PORT)); }catch(Exception e){ runOnUiThread(()->status.setText("● Connection failed: "+e.getMessage())); }}); }
    void sendCommand(String player,int dice){ sendRaw("{\"type\":\"dice_override\",\"player\":\""+player+"\",\"dice\":"+dice+"}"); }
    void sendRaw(String s){ pool.execute(()->{ try{ if(client==null || client.isClosed()){ runOnUiThread(()->status.setText("● Not connected")); return; } OutputStream out=client.getOutputStream(); out.write((s+"\n").getBytes(StandardCharsets.UTF_8)); out.flush(); runOnUiThread(()->status.setText("● Sent: "+s)); }catch(Exception e){ runOnUiThread(()->status.setText("● Send failed: "+e.getMessage())); }}); }

    @Override protected void onDestroy(){ super.onDestroy(); try{if(server!=null)server.close();}catch(Exception ignored){} pool.shutdownNow(); }
}
