package com.raynanapk.builder;
import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.view.*;
import android.graphics.Color;
import android.content.Intent;
import android.net.Uri;
import java.io.*;
import java.util.zip.*;
import android.database.Cursor;
import android.provider.OpenableColumns;
import android.webkit.*;
import android.content.res.AssetManager;
import java.security.*;
import java.security.cert.X509Certificate;
import java.math.BigInteger;
import java.util.Date;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import com.android.apksig.ApkSigner;
import java.util.Collections;
import android.os.Handler;
import android.graphics.BitmapFactory;

public class MainActivity extends Activity {
    private static final int PICK_FILE = 1001;
    private static final int PICK_ICON = 1002;
    private File selectedFile=null, selectedIcon=null;
    private TextView logView;
    private EditText appNameInput, packageInput;
    private CheckBox splashCheck, fileUploadCheck;
    private ValueCallback<Uri[]> filePathCallback;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        if(hasWebContent()) showWebViewerV6(); else showBuilderV6();
    }
    boolean hasWebContent(){ try{ String[] l=getAssets().list("www"); return l!=null&&l.length>0; }catch(Exception e){ return false; } }

    void showWebViewerV6(){
        // SPLASH SCREEN V6
        LinearLayout splash = new LinearLayout(this);
        splash.setOrientation(LinearLayout.VERTICAL);
        splash.setBackgroundColor(Color.parseColor("#0F111A"));
        splash.setGravity(Gravity.CENTER);

        ImageView logo = new ImageView(this);
        try{ logo.setImageBitmap(BitmapFactory.decodeStream(getAssets().open("www/icon.png"))); }catch(Exception e){ try{ logo.setImageResource(getResources().getIdentifier("ic_launcher","mipmap",getPackageName())); }catch(Exception ex){} }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(250,250); lp.setMargins(0,0,0,40); logo.setLayoutParams(lp);

        TextView appName = new TextView(this);
        appName.setText(getAppNameFromConfig());
        appName.setTextColor(Color.WHITE); appName.setTextSize(24); appName.setGravity(Gravity.CENTER);

        ProgressBar pb = new ProgressBar(this);

        TextView loading = new TextView(this);
        loading.setText("Loading..."); loading.setTextColor(Color.parseColor("#8B8FA8")); loading.setGravity(Gravity.CENTER); loading.setPadding(0,20,0,0);

        splash.addView(logo); splash.addView(appName); splash.addView(pb); splash.addView(loading);

        // Check config for splash
        boolean needSplash = true;
        try{ String cfg = readAsset("www/__builder_config.json"); needSplash = cfg.contains("\"splash\":true"); }catch(Exception e){}

        if(!needSplash){
            showWebViewDirect();
            return;
        }

        setContentView(splash);
        new Handler().postDelayed(() -> showWebViewDirect(), 2000);
    }

    String getAppNameFromConfig(){
        try{
            String cfg = readAsset("www/__builder_config.json");
            // simple parse "appName":"xxx"
            int i=cfg.indexOf("\"appName\"");
            if(i>-1){ int s=cfg.indexOf("\"",i+10)+1; int e=cfg.indexOf("\"",s); return cfg.substring(s,e); }
        }catch(Exception e){}
        return "MyApp";
    }
    String readAsset(String path) throws Exception {
        InputStream is=getAssets().open(path); ByteArrayOutputStream baos=new ByteArrayOutputStream(); byte[] b=new byte[8192]; int l; while((l=is.read(b))>0) baos.write(b,0,l); is.close(); return baos.toString("UTF-8");
    }

    void showWebViewDirect(){
        WebView wv=new WebView(this);
        wv.getSettings().setJavaScriptEnabled(true);
        wv.getSettings().setDomStorageEnabled(true);
        wv.getSettings().setAllowFileAccess(true);
        wv.getSettings().setAllowFileAccessFromFileURLs(true);
        wv.getSettings().setAllowUniversalAccessFromFileURLs(true);
        wv.getSettings().setMediaPlaybackRequiresUserGesture(false);
        wv.getSettings().setSupportZoom(true);
        wv.getSettings().setBuiltInZoomControls(true);
        wv.getSettings().setDisplayZoomControls(false);
        wv.setWebChromeClient(new WebChromeClient(){
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams){
                MainActivity.this.filePathCallback=filePathCallback;
                Intent intent=new Intent(Intent.ACTION_GET_CONTENT); intent.addCategory(Intent.CATEGORY_OPENABLE); intent.setType("*/*");
                startActivityForResult(Intent.createChooser(intent,"Choose File"), 1003);
                return true;
            }
            public void onShowCustomView(View view, CustomViewCallback callback){ // fullscreen video
                FrameLayout decor=(FrameLayout)getWindow().getDecorView();
                decor.addView(view, new FrameLayout.LayoutParams(-1,-1));
            }
        });
        wv.setWebViewClient(new WebViewClient());
        try{
            String[] files=getAssets().list("www");
            String target="www/index.html"; if(files!=null){ boolean has=false; for(String f:files) if(f.equals("index.html")) has=true; if(!has&&files.length>0) target="www/"+files[0]; }
            wv.loadUrl("file:///android_asset/"+target);
        }catch(Exception e){ wv.loadData("<h1>"+e.getMessage()+"</h1>","text/html","utf-8"); }
        setContentView(wv);
    }

    @Override protected void onActivityResult(int req,int res,Intent data){
        super.onActivityResult(req,res,data);
        if(req==1003){ // file chooser
            if(filePathCallback!=null){
                Uri[] results=null; if(res==RESULT_OK&&data!=null){ String dataString=data.getDataString(); if(dataString!=null) results=new Uri[]{Uri.parse(dataString)}; } filePathCallback.onReceiveValue(results); filePathCallback=null;
            } return;
        }
        if(res!=RESULT_OK||data==null) return;
        try{ Uri uri=data.getData(); String name=getFileName(uri); File tmp=new File(getCacheDir(),name);
            InputStream in=getContentResolver().openInputStream(uri); FileOutputStream out=new FileOutputStream(tmp);
            byte[] buf=new byte[8192]; int len; while((len=in.read(buf))>0) out.write(buf,0,len); out.close(); in.close();
            if(req==PICK_FILE){ selectedFile=tmp; log("✅ File: "+name); } else if(req==PICK_ICON){ selectedIcon=tmp; log("🎨 Icon: "+name); }
        }catch(Exception e){ log("❌ "+e.getMessage()); }
    }

    void showBuilderV6(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(25,30,25,25); root.setBackgroundColor(Color.parseColor("#0F111A"));
        TextView title=new TextView(this); title.setText("⚡ Builder B V6\nPLAY STORE READY"); title.setTextSize(18); title.setTextColor(Color.WHITE); title.setGravity(Gravity.CENTER); title.setPadding(0,0,0,10);
        appNameInput=new EditText(this); appNameInput.setHint("App Name"); appNameInput.setText("MyApp"); appNameInput.setTextColor(Color.WHITE); appNameInput.setHintTextColor(Color.GRAY); appNameInput.setBackgroundColor(Color.parseColor("#1A1D2E")); appNameInput.setPadding(20,20,20,20);
        packageInput=new EditText(this); packageInput.setHint("Package: com.khozenk.app"); packageInput.setText("com.khozenk.myapp"); packageInput.setTextColor(Color.WHITE); packageInput.setHintTextColor(Color.GRAY); packageInput.setBackgroundColor(Color.parseColor("#1A1D2E")); packageInput.setPadding(20,20,20,20);
        LinearLayout.LayoutParams pm=new LinearLayout.LayoutParams(-1,-2); pm.setMargins(0,8,0,8); appNameInput.setLayoutParams(pm); packageInput.setLayoutParams(pm);
        splashCheck=new CheckBox(this); splashCheck.setText("🌊 Splash Screen 2 detik"); splashCheck.setChecked(true); splashCheck.setTextColor(Color.WHITE);
        fileUploadCheck=new CheckBox(this); fileUploadCheck.setText("📁 Enable File Upload"); fileUploadCheck.setChecked(true); fileUploadCheck.setTextColor(Color.WHITE);
        Button pickBtn=makeBtn("📤 1. PILIH HTML/ZIP"); pickBtn.setOnClickListener(v->{ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("*/*"); i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"text/html","application/zip","*/*"}); startActivityForResult(i,PICK_FILE); });
        Button iconBtn=makeBtn("🎨 2. PILIH ICON"); iconBtn.setBackgroundColor(Color.parseColor("#FF6B6B")); iconBtn.setOnClickListener(v->{ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("image/*"); startActivityForResult(i,PICK_ICON); });
        Button buildBtn=makeBtn("🔨 3. BUILD V6 PLAY STORE"); buildBtn.setBackgroundColor(Color.parseColor("#00D084")); buildBtn.setOnClickListener(v->{ if(selectedFile==null){ toast("Pilih file dulu!"); return; } buildV6(); });
        logView=new TextView(this); logView.setText("📝 V6 LOG:\n> Splash Screen\n> File Upload + Fullscreen Video\n> Play Store Ready - Auto Sign V2+V3\n> Build #23 incoming!\n"); logView.setTextColor(Color.parseColor("#8B8FA8")); logView.setTextSize(11); logView.setPadding(15,15,15,15); logView.setBackgroundColor(Color.parseColor("#1A1D2E"));
        root.addView(title); root.addView(appNameInput); root.addView(packageInput); root.addView(splashCheck); root.addView(fileUploadCheck); root.addView(pickBtn); root.addView(iconBtn); root.addView(buildBtn); root.addView(logView);
        ScrollView sv=new ScrollView(this); sv.addView(root); setContentView(sv);
    }

    void buildV6(){
        new Thread(() -> {
            try{
                runOnUiThread(() -> log("\n🔨 BUILDING V6..."));
                File outDir=new File(getExternalFilesDir(null),"BuilderB_Output"); outDir.mkdirs();
                String appName=appNameInput.getText().toString(); if(appName.isEmpty()) appName="MyApp";
                File unsigned=new File(outDir,appName+"_unsigned.apk"); File signed=new File(outDir,appName+"_V6.apk");
                String srcApk=getPackageCodePath(); ZipFile srcZip=new ZipFile(srcApk);
                ZipOutputStream zos=new ZipOutputStream(new FileOutputStream(unsigned));
                var en=srcZip.entries(); while(en.hasMoreElements()){ ZipEntry e=en.nextElement(); String n=e.getName(); if(n.startsWith("assets/www/")) continue; if(n.startsWith("META-INF/")) continue; if(selectedIcon!=null && n.contains("ic_launcher") && n.endsWith(".png")) continue; zos.putNextEntry(new ZipEntry(n)); InputStream is=srcZip.getInputStream(e); byte[] b=new byte[8192]; int l; while((l=is.read(b))>0) zos.write(b,0,l); zos.closeEntry(); is.close(); } srcZip.close();
                zos.putNextEntry(new ZipEntry("assets/www/")); zos.closeEntry();
                if(selectedFile.getName().endsWith(".zip")){ ZipFile uz=new ZipFile(selectedFile); var ue=uz.entries(); while(ue.hasMoreElements()){ ZipEntry ze=ue.nextElement(); if(ze.isDirectory()) continue; zos.putNextEntry(new ZipEntry("assets/www/"+ze.getName())); InputStream iis=uz.getInputStream(ze); byte[] bb=new byte[8192]; int ll; while((ll=iis.read(bb))>0) zos.write(bb,0,ll); zos.closeEntry(); iis.close(); } uz.close(); }else{ zos.putNextEntry(new ZipEntry("assets/www/index.html")); FileInputStream fis=new FileInputStream(selectedFile); byte[] bb=new byte[8192]; int ll; while((ll=fis.read(bb))>0) zos.write(bb,0,ll); zos.closeEntry(); fis.close(); }
                if(selectedIcon!=null){ String[] paths={"res/mipmap-hdpi-v4/ic_launcher.png","res/mipmap-mdpi-v4/ic_launcher.png","res/mipmap-xhdpi-v4/ic_launcher.png","res/mipmap-xxhdpi-v4/ic_launcher.png","res/mipmap-xxxhdpi-v4/ic_launcher.png","res/mipmap-hdpi-v4/ic_launcher_round.png","res/mipmap-xxhdpi-v4/ic_launcher_round.png"}; for(String p:paths){ try{ zos.putNextEntry(new ZipEntry(p)); FileInputStream fis=new FileInputStream(selectedIcon); byte[] b=new byte[8192]; int l; while((l=fis.read(b))>0) zos.write(b,0,l); zos.closeEntry(); fis.close(); }catch(Exception ex){} } zos.putNextEntry(new ZipEntry("assets/www/icon.png")); FileInputStream fis=new FileInputStream(selectedIcon); byte[] b=new byte[8192]; int l; while((l=fis.read(b))>0) zos.write(b,0,l); zos.closeEntry(); fis.close(); }
                String config="{\"appName\":\""+appName+"\",\"package\":\""+packageInput.getText()+"\",\"splash\":"+splashCheck.isChecked()+",\"fileUpload\":"+fileUploadCheck.isChecked()+",\"version\":\"V6\",\"built\":\""+new Date()+"\"}";
                zos.putNextEntry(new ZipEntry("assets/www/__builder_config.json")); zos.write(config.getBytes()); zos.closeEntry();
                zos.close();
                runOnUiThread(() -> log("> Signing V2+V3..."));
                Security.addProvider(new BouncyCastleProvider());
                KeyPairGenerator kpg=KeyPairGenerator.getInstance("RSA"); kpg.initialize(2048); KeyPair kp=kpg.generateKeyPair();
                X500Principal issuer=new X500Principal("CN=BuilderB V6"); JcaX509v3CertificateBuilder certBuilder=new JcaX509v3CertificateBuilder(issuer, BigInteger.valueOf(System.currentTimeMillis()), new Date(System.currentTimeMillis()-100000), new Date(System.currentTimeMillis()+365L*24*3600*1000*3), issuer, kp.getPublic());
                X509Certificate cert=new JcaX509CertificateConverter().setProvider("BC").getCertificate(certBuilder.build(new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(kp.getPrivate())));
                ApkSigner.SignerConfig sc=new ApkSigner.SignerConfig.Builder("B6",kp.getPrivate(),Collections.singletonList(cert)).build();
                new ApkSigner.Builder(Collections.singletonList(sc)).setInputApk(unsigned).setOutputApk(signed).setV1SigningEnabled(true).setV2SigningEnabled(true).setV3SigningEnabled(true).build().sign();
                runOnUiThread(() -> { log("✅ V6 JADI! PLAY STORE READY!\n📁 "+signed.getPath()+"\n> Splash: "+splashCheck.isChecked()+"\n> Icon: "+(selectedIcon!=null?"Custom":"Default")); toast("V6 Sukses!"); Intent intent=new Intent(Intent.ACTION_VIEW); intent.setDataAndType(Uri.fromFile(signed),"application/vnd.android.package-archive"); intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_GRANT_READ_URI_PERMISSION); try{ startActivity(intent); }catch(Exception e){} });
            }catch(Exception e){ e.printStackTrace(); runOnUiThread(() -> log("❌ "+e.toString())); }
        }).start();
    }
    void log(String s){ runOnUiThread(() -> logView.append("\n"+s)); }
    void toast(String s){ runOnUiThread(() -> Toast.makeText(this,s,Toast.LENGTH_SHORT).show()); }
    String getFileName(Uri uri){ String r=null; if(uri.getScheme().equals("content")){ Cursor c=getContentResolver().query(uri,null,null,null,null); try{ if(c!=null&&c.moveToFirst()){ int idx=c.getColumnIndex(OpenableColumns.DISPLAY_NAME); if(idx>=0) r=c.getString(idx); } }finally{ if(c!=null) c.close(); } } if(r==null) r=uri.getPath(); int cut=r.lastIndexOf('/'); if(cut!=-1) r=r.substring(cut+1); return r; }
    Button makeBtn(String t){ Button b=new Button(this); b.setText(t); b.setTextColor(Color.WHITE); b.setBackgroundColor(Color.parseColor("#6C5CE7")); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,8,0,8); b.setLayoutParams(p); b.setPadding(0,24,0,24); return b; }
}
