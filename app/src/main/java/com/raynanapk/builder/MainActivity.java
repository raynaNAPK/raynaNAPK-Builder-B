package com.raynanapk.builder;
import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.view.Gravity;
import android.graphics.Color;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import java.io.File;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40,80,40,40);
        root.setBackgroundColor(Color.parseColor("#0F111A"));
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        
        TextView title = new TextView(this);
        title.setText("⚡ Builder B");
        title.setTextSize(28);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0,0,0,20);
        
        TextView subtitle = new TextView(this);
        subtitle.setText("raynaNAPK APK Builder - v5.0\nBuilt with Termux + GitHub Actions\n17 Builds -> Finally GREEN ✅");
        subtitle.setTextColor(Color.parseColor("#8B8FA8"));
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0,0,0,40);
        
        TextView status = new TextView(this);
        status.setText("📁 Project: raynaNAPK-Builder-B\n📦 Status: READY TO BUILD\n🟢 Last Build: #17 GREEN - 01.13 WIB");
        status.setTextColor(Color.parseColor("#00FF88"));
        status.setTextSize(14);
        status.setPadding(30,30,30,30);
        status.setBackgroundColor(Color.parseColor("#1A1D2E"));
        
        LinearLayout btnLayout = new LinearLayout(this);
        btnLayout.setOrientation(LinearLayout.VERTICAL);
        btnLayout.setPadding(0,50,0,0);
        
        Button btn1 = makeBtn("📂 Open Project Folder");
        Button btn2 = makeBtn("🔨 Build APK (GitHub)");
        Button btn3 = makeBtn("🌐 Open GitHub Repo");
        Button btn4 = makeBtn("💬 MultiChat Khozenk");
        
        btn1.setOnClickListener(v -> {
            Toast.makeText(this, "Project di: /storage/emulated/0/raynaNAPK-Builder-B", Toast.LENGTH_LONG).show();
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(Uri.parse(Environment.getExternalStorageDirectory().getPath()), "resource/folder");
            try { startActivity(i); } catch(Exception e){ Toast.makeText(this,"Install MT Manager / ZArchiver", Toast.LENGTH_SHORT).show(); }
        });
        
        btn2.setOnClickListener(v -> {
            Toast.makeText(this, "Push ke GitHub untuk build! git push origin main", Toast.LENGTH_LONG).show();
        });
        
        btn3.setOnClickListener(v -> {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/raynaNAPK/raynaNAPK-Builder-B/actions")));
        });
        
        btn4.setOnClickListener(v -> {
            Toast.makeText(this, "MultiChat Khozenk - Ready!", Toast.LENGTH_SHORT).show();
        });
        
        btnLayout.addView(btn1);
        btnLayout.addView(btn2);
        btnLayout.addView(btn3);
        btnLayout.addView(btn4);
        
        TextView footer = new TextView(this);
        footer.setText("\n\nBuilt on Android - No PC Needed\nTermux + Godot + MT Manager Stack");
        footer.setTextColor(Color.parseColor("#555770"));
        footer.setGravity(Gravity.CENTER);
        footer.setTextSize(12);
        
        root.addView(title);
        root.addView(subtitle);
        root.addView(status);
        root.addView(btnLayout);
        root.addView(footer);
        
        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        setContentView(sv);
    }
    
    Button makeBtn(String t){
        Button b = new Button(this);
        b.setText(t);
        b.setTextColor(Color.WHITE);
        b.setBackgroundColor(Color.parseColor("#6C5CE7"));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0,15,0,15);
        b.setLayoutParams(p);
        b.setPadding(0,30,0,30);
        return b;
    }
}
