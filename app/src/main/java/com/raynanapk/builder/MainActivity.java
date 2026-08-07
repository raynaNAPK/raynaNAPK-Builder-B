package com.raynanapk.builder;
import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.view.Gravity;
import android.graphics.Color;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import java.io.*;
import java.util.zip.*;
import android.database.Cursor;
import android.provider.OpenableColumns;

public class MainActivity extends Activity {
    private static final int PICK_FILE = 1001;
    private File selectedFile = null;
    private TextView logView;
    private EditText appNameInput;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(30,50,30,30);
        root.setBackgroundColor(Color.parseColor("#0F111A"));

        TextView title = new TextView(this);
        title.setText("⚡ Builder B V3\nAppMint Clone - OFFLINE");
        title.setTextSize(22);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0,0,0,20);

        appNameInput = new EditText(this);
        appNameInput.setHint("Nama APK (contoh: MyGame)");
        appNameInput.setText("MyApp");
        appNameInput.setTextColor(Color.WHITE);
        appNameInput.setHintTextColor(Color.GRAY);
        appNameInput.setBackgroundColor(Color.parseColor("#1A1D2E"));
        appNameInput.setPadding(30,30,30,30);

        Button pickBtn = makeBtn("📤 1. PILIH FILE HTML / ZIP / FOLDER");
        pickBtn.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            String[] mimes = {"text/html", "application/zip", "application/x-zip", "*/*"};
            i.putExtra(Intent.EXTRA_MIME_TYPES, mimes);
            startActivityForResult(i, PICK_FILE);
        });

        Button buildBtn = makeBtn("🔨 2. BUILD APK SEKARANG (OFFLINE)");
        buildBtn.setBackgroundColor(Color.parseColor("#00D084"));
        buildBtn.setOnClickListener(v -> {
            if(selectedFile == null){
                toast("Pilih file dulu bro!");
                return;
            }
            String name = appNameInput.getText().toString();
            if(name.isEmpty()) name = "MyApp";
            buildApkOffline(name);
        });

        Button openFolderBtn = makeBtn("📂 BUKA FOLDER HASIL");
        openFolderBtn.setBackgroundColor(Color.parseColor("#2D3047"));
        openFolderBtn.setOnClickListener(v -> {
            File out = new File(getExternalFilesDir(null), "BuilderB_Output");
            toast("Hasil di: " + out.getPath());
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(out.getPath()), "resource/folder");
            try{ startActivity(intent); } catch(Exception e){
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("content://com.android.externalstorage.documents/document/primary%3AAndroid%2Fdata%2Fcom.raynanapk.builder%2Ffiles%2FBuilderB_Output"));
                try{ startActivity(i); } catch(Exception ex){ toast("Buka pakai ZArchiver / MT Manager: Android/data/com.raynanapk.builder/files/BuilderB_Output"); }
            }
        });

        logView = new TextView(this);
        logView.setText("📝 LOG:\n> Ready. Pilih file HTML/ZIP dulu.\n> Contoh: index.html atau project.zip\n> Nanti APK jadi di folder BuilderB_Output\n");
        logView.setTextColor(Color.parseColor("#8B8FA8"));
        logView.setTextSize(13);
        logView.setPadding(20,20,20,20);
        logView.setBackgroundColor(Color.parseColor("#1A1D2E"));

        root.addView(title);
        root.addView(appNameInput);
        root.addView(pickBtn);
        root.addView(buildBtn);
        root.addView(openFolderBtn);
        root.addView(logView);

        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        setContentView(sv);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data){
        super.onActivityResult(req,res,data);
        if(req==PICK_FILE && res==RESULT_OK && data!=null){
            try{
                Uri uri = data.getData();
                String name = getFileName(uri);
                File tmp = new File(getCacheDir(), name);
                InputStream in = getContentResolver().openInputStream(uri);
                FileOutputStream out = new FileOutputStream(tmp);
                byte[] buf = new byte[8192]; int len;
                while((len=in.read(buf))>0) out.write(buf,0,len);
                out.close(); in.close();
                selectedFile = tmp;
                log("✅ File dipilih: " + name + "\n📦 Size: " + (tmp.length()/1024) + " KB\n> Siap Build!");
                toast("File terpilih: " + name);
            }catch(Exception e){ log("❌ Error: " + e.getMessage()); }
        }
    }

    void buildApkOffline(String appName){
        new Thread(() -> {
            try{
                runOnUiThread(() -> log("\n🔨 BUILDING...\n> Copy template APK..."));
                File outDir = new File(getExternalFilesDir(null), "BuilderB_Output");
                outDir.mkdirs();
                File outApk = new File(outDir, appName + "_BuilderB.apk");

                String srcApk = getPackageCodePath();
                ZipFile srcZip = new ZipFile(srcApk);
                ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outApk));

                var en = srcZip.entries();
                while(en.hasMoreElements()){
                    ZipEntry e = en.nextElement();
                    String n = e.getName();
                    if(n.startsWith("assets/www/")) continue;
                    if(n.startsWith("META-INF/")) continue;
                    if(n.equals("AndroidManifest.xml")) continue; // kita pakai manifest original
                    zos.putNextEntry(new ZipEntry(n));
                    InputStream is = srcZip.getInputStream(e);
                    byte[] b = new byte[8192]; int l;
                    while((l=is.read(b))>0) zos.write(b,0,l);
                    zos.closeEntry(); is.close();
                }
                srcZip.close();

                // Inject www
                zos.putNextEntry(new ZipEntry("assets/www/"));
                zos.closeEntry();

                runOnUiThread(() -> log("> Inject file user ke assets/www/..."));

                if(selectedFile.getName().endsWith(".zip")){
                    ZipFile userZip = new ZipFile(selectedFile);
                    var ue = userZip.entries();
                    while(ue.hasMoreElements()){
                        ZipEntry ze = ue.nextElement();
                        if(ze.isDirectory()) continue;
                        String entryName = ze.getName();
                        if(entryName.startsWith("/")) entryName = entryName.substring(1);
                        zos.putNextEntry(new ZipEntry("assets/www/" + entryName));
                        InputStream iis = userZip.getInputStream(ze);
                        byte[] bb = new byte[8192]; int ll;
                        while((ll=iis.read(bb))>0) zos.write(bb,0,ll);
                        zos.closeEntry(); iis.close();
                    }
                    userZip.close();
                } else {
                    zos.putNextEntry(new ZipEntry("assets/www/index.html"));
                    FileInputStream fis = new FileInputStream(selectedFile);
                    byte[] bb = new byte[8192]; int ll;
                    while((ll=fis.read(bb))>0) zos.write(bb,0,ll);
                    zos.closeEntry(); fis.close();
                }

                // Copy manifest original dari APK template
                ZipFile srcZip2 = new ZipFile(srcApk);
                ZipEntry manifestEntry = srcZip2.getEntry("AndroidManifest.xml");
                if(manifestEntry!= null){
                    zos.putNextEntry(new ZipEntry("AndroidManifest.xml"));
                    InputStream mis = srcZip2.getInputStream(manifestEntry);
                    byte[] mb = new byte[8192]; int ml;
                    while((ml=mis.read(mb))>0) zos.write(mb,0,ml);
                    zos.closeEntry(); mis.close();
                }
                srcZip2.close();
                zos.close();

                runOnUiThread(() -> {
                    log("> Repack selesai!\n> ⚠️ APK belum signed (V3.0)\n> Buka MT Manager > Klik APK > Sign\n> Atau install pakai: \n" + outApk.getPath());
                    log("\n✅ APK JADI: " + outApk.getName() + "\n📁 " + outApk.getPath() + "\n\n> Next V3.1 bakal auto-sign biar langsung install!");
                    toast("APK Jadi! Cek folder BuilderB_Output");
                });

                // Auto buka folder
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(outApk), "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try{ Thread.sleep(1000); }catch(Exception e){}

            }catch(Exception e){
                String err = e.toString();
                runOnUiThread(() -> log("❌ GAGAL: " + err + "\n" + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    void log(String s){
        logView.append("\n" + s);
    }
    void toast(String s){ Toast.makeText(this,s,Toast.LENGTH_SHORT).show(); }

    String getFileName(Uri uri){
        String result = null;
        if(uri.getScheme().equals("content")){
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try{
                if(cursor!=null && cursor.moveToFirst()){
                    int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if(idx>=0) result = cursor.getString(idx);
                }
            }finally{ if(cursor!=null) cursor.close(); }
        }
        if(result==null) result = uri.getPath();
        int cut = result.lastIndexOf('/');
        if(cut!=-1) result = result.substring(cut+1);
        return result;
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
