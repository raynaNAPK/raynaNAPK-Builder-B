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
    private static final int PICK_FILE=1001,PICK_ICON=1002;
    private File selectedFile=null,selectedIcon=null;
    private TextView logView;
    private EditText appNameInput,packageInput;
    private CheckBox splashCheck,fileCheck;
    private ValueCallback<Uri[]> filePathCallback;

    @Override protected void onCreate(Bundle b){ super.onCreate(b); if(hasWebContent()) showViewerV7(); else showBuilderV7(); }
    boolean hasWebContent(){ try{ String[] l=getAssets().list("www"); return l!=null&&l.length>0; }catch(Exception e){ return false; } }
    String getAppName(){ try{ String cfg=readAsset("www/__builder_config.json"); int i=cfg.indexOf("\"appName\""); if(i>-1){ int s=cfg.indexOf("\"",i+10)+1; int e=cfg.indexOf("\"",s); return cfg.substring(s,e);} }catch(Exception e){} return "MyApp"; }
    String readAsset(String p) throws Exception { InputStream is=getAssets().open(p); ByteArrayOutputStream baos=new ByteArrayOutputStream(); byte[] buf=new byte[8192]; int l; while((l=is.read(buf))>0) baos.write(buf,0,l); is.close(); return baos.toString("UTF-8"); }

    void showViewerV7(){
        boolean needSplash=true;
        try{ String cfg=readAsset("www/__builder_config.json"); needSplash=cfg.contains("\"splash\":true"); }catch(Exception e){}
        if(needSplash){
            LinearLayout splash=new LinearLayout(this); splash.setOrientation(LinearLayout.VERTICAL); splash.setBackgroundColor(Color.parseColor("#0F111A")); splash.setGravity(Gravity.CENTER);
            ImageView logo=new ImageView(this); try{ logo.setImageBitmap(BitmapFactory.decodeStream(getAssets().open("www/icon.png"))); }catch(Exception ex){ try{ logo.setImageResource(getResources().getIdentifier("ic_launcher","mipmap",getPackageName())); }catch(Exception ex2){} }
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(250,250); lp.setMargins(0,0,0,40); logo.setLayoutParams(lp);
            TextView appName=new TextView(this); appName.setText(getAppName()); appName.setTextColor(Color.WHITE); appName.setTextSize(24); appName.setGravity(Gravity.CENTER);
            ProgressBar pb=new ProgressBar(this); splash.addView(logo); splash.addView(appName); splash.addView(pb);
            setContentView(splash); new Handler().postDelayed(() -> showWebV7(), 2000);
        } else showWebV7();
    }
    void showWebV7(){
        FrameLayout root=new FrameLayout(this);
        WebView wv=new WebView(this);
        wv.getSettings().setJavaScriptEnabled(true); wv.getSettings().setDomStorageEnabled(true); wv.getSettings().setAllowFileAccess(true); wv.getSettings().setAllowFileAccessFromFileURLs(true); wv.getSettings().setAllowUniversalAccessFromFileURLs(true); wv.getSettings().setMediaPlaybackRequiresUserGesture(false);
        wv.setWebChromeClient(new WebChromeClient(){ public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams){ MainActivity.this.filePathCallback=filePathCallback; Intent intent=new Intent(Intent.ACTION_GET_CONTENT); intent.addCategory(Intent.CATEGORY_OPENABLE); intent.setType("*/*"); startActivityForResult(Intent.createChooser(intent,"Choose File"), 1003); return true; } });
        wv.setWebViewClient(new WebViewClient());
        try{ String[] files=getAssets().list("www"); String target="www/index.html"; if(files!=null){ boolean has=false; for(String f:files) if(f.equals("index.html")) has=true; if(!has&&files.length>0) target="www/"+files[0]; } wv.loadUrl("file:///android_asset/"+target); }catch(Exception e){ wv.loadData("<h1>"+e.getMessage()+"</h1>","text/html","utf-8"); }
        root.addView(wv, new FrameLayout.LayoutParams(-1,-1));
        setContentView(root);
    }
    @Override protected void onActivityResult(int req,int res,Intent data){
        super.onActivityResult(req,res,data);
        if(req==1003){ if(filePathCallback!=null){ Uri[] results=null; if(res==RESULT_OK&&data!=null){ String ds=data.getDataString(); if(ds!=null) results=new Uri[]{Uri.parse(ds)}; } filePathCallback.onReceiveValue(results); filePathCallback=null; } return; }
        if(res!=RESULT_OK||data==null) return;
        try{ Uri uri=data.getData(); String name=getFileName(uri); File tmp=new File(getCacheDir(),name); InputStream in=getContentResolver().openInputStream(uri); FileOutputStream out=new FileOutputStream(tmp); byte[] buf=new byte[8192]; int len; while((len=in.read(buf))>0) out.write(buf,0,len); out.close(); in.close(); if(req==PICK_FILE){ selectedFile=tmp; log("✅ File: "+name); } else if(req==PICK_ICON){ selectedIcon=tmp; log("🎨 Icon: "+name); } }catch(Exception e){ log("❌ "+e.getMessage()); }
    }
    void showBuilderV7(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(20,25,20,20); root.setBackgroundColor(Color.parseColor("#0F111A"));
        TextView title=new TextView(this); title.setText("⚡ Builder B V7.1\nFIXED - STABLE"); title.setTextSize(18); title.setTextColor(Color.WHITE); title.setGravity(Gravity.CENTER); title.setPadding(0,0,0,8);
        appNameInput=new EditText(this); appNameInput.setHint("App Name"); appNameInput.setText("MyApp"); appNameInput.setTextColor(Color.WHITE); appNameInput.setHintTextColor(Color.GRAY); appNameInput.setBackgroundColor(Color.parseColor("#1A1D2E")); appNameInput.setPadding(15,15,15,15);
        packageInput=new EditText(this); packageInput.setHint("Package"); packageInput.setText("com.khozenk.myapp"); packageInput.setTextColor(Color.WHITE); packageInput.setHintTextColor(Color.GRAY); packageInput.setBackgroundColor(Color.parseColor("#1A1D2E")); packageInput.setPadding(15,15,15,15);
        LinearLayout.LayoutParams pm=new LinearLayout.LayoutParams(-1,-2); pm.setMargins(0,6,0,6); appNameInput.setLayoutParams(pm); packageInput.setLayoutParams(pm);
        splashCheck=new CheckBox(this); splashCheck.setText("🌊 Splash 2 detik"); splashCheck.setChecked(true); splashCheck.setTextColor(Color.WHITE);
        fileCheck=new CheckBox(this); fileCheck.setText("📁 File Upload"); fileCheck.setChecked(true); fileCheck.setTextColor(Color.WHITE);
        Button pickBtn=makeBtn("📤 1. PILIH HTML/ZIP"); pickBtn.setOnClickListener(v->{ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("*/*"); i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"text/html","application/zip","*/*"}); startActivityForResult(i,PICK_FILE); });
        Button iconBtn=makeBtn("🎨 2. PILIH ICON"); iconBtn.setBackgroundColor(Color.parseColor("#FF6B6B")); iconBtn.setOnClickListener(v->{ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("image/*"); startActivityForResult(i,PICK_ICON); });
        Button buildBtn=makeBtn("🔨 3. BUILD V7.1 STABLE"); buildBtn.setBackgroundColor(Color.parseColor("#00D084")); buildBtn.setOnClickListener(v->{ if(selectedFile==null){ toast("Pilih file!"); return; } buildV7(); });
        logView=new TextView(this); logView.setText("📝 V7.1 LOG:\n> Fix mipmap error\n> Stable build\n> Splash + File Upload\n> AdMob V8 next!\n"); logView.setTextColor(Color.parseColor("#8B8FA8")); logView.setTextSize(10); logView.setPadding(12,12,12,12); logView.setBackgroundColor(Color.parseColor("#1A1D2E"));
        root.addView(title); root.addView(appNameInput); root.addView(packageInput); root.addView(splashCheck); root.addView(fileCheck); root.addView(pickBtn); root.addView(iconBtn); root.addView(buildBtn); root.addView(logView);
        ScrollView sv=new ScrollView(this); sv.addView(root); setContentView(sv);
    }
    void buildV7(){
        new Thread(() -> {
            try{
                runOnUiThread(() -> log("\n🔨 BUILDING V7.1..."));
                File outDir=new File(getExternalFilesDir(null),"BuilderB_Output"); outDir.mkdirs();
                String appName=appNameInput.getText().toString(); if(appName.isEmpty()) appName="MyApp";
                File unsigned=new File(outDir,appName+"_unsigned.apk"); File signed=new File(outDir,appName+"_V7.apk");
                String srcApk=getPackageCodePath(); ZipFile srcZip=new ZipFile(srcApk);
                ZipOutputStream zos=new ZipOutputStream(new FileOutputStream(unsigned));
                var en=srcZip.entries(); while(en.hasMoreElements()){ ZipEntry e=en.nextElement(); String n=e.getName(); if(n.startsWith("assets/www/")) continue; if(n.startsWith("META-INF/")) continue; if(selectedIcon!=null && n.contains("ic_launcher") && n.endsWith(".png")) continue; zos.putNextEntry(new ZipEntry(n)); InputStream is=srcZip.getInputStream(e); byte[] b=new byte[8192]; int l; while((l=is.read(b))>0) zos.write(b,0,l); zos.closeEntry(); is.close(); } srcZip.close();
                zos.putNextEntry(new ZipEntry("assets/www/")); zos.closeEntry();
                if(selectedFile.getName().endsWith(".zip")){ ZipFile uz=new ZipFile(selectedFile); var ue=uz.entries(); while(ue.hasMoreElements()){ ZipEntry ze=ue.nextElement(); if(ze.isDirectory()) continue; zos.putNextEntry(new ZipEntry("assets/www/"+ze.getName())); InputStream iis=uz.getInputStream(ze); byte[] bb=new byte[8192]; int ll; while((ll=iis.read(bb))>0) zos.write(bb,0,ll); zos.closeEntry(); iis.close(); } uz.close(); }else{ zos.putNextEntry(new ZipEntry("assets/www/index.html")); FileInputStream fis=new FileInputStream(selectedFile); byte[] bb=new byte[8192]; int ll; while((ll=fis.read(bb))>0) zos.write(bb,0,ll); zos.closeEntry(); fis.close(); }
                if(selectedIcon!=null){ String[] paths={"res/mipmap-hdpi-v4/ic_launcher.png","res/mipmap-mdpi-v4/ic_launcher.png","res/mipmap-xhdpi-v4/ic_launcher.png","res/mipmap-xxhdpi-v4/ic_launcher.png","res/mipmap-xxxhdpi-v4/ic_launcher.png","res/mipmap-hdpi-v4/ic_launcher_round.png"}; for(String p:paths){ try{ zos.putNextEntry(new ZipEntry(p)); FileInputStream fis=new FileInputStream(selectedIcon); byte[] b=new byte[8192]; int l; while((l=fis.read(b))>0) zos.write(b,0,l); zos.closeEntry(); fis.close(); }catch(Exception ex){} } zos.putNextEntry(new ZipEntry("assets/www/icon.png")); FileInputStream fis=new FileInputStream(selectedIcon); byte[] b=new byte[8192]; int l; while((l=fis.read(b))>0) zos.write(b,0,l); zos.closeEntry(); fis.close(); }
                String config="{\"appName\":\""+appName+"\",\"package\":\""+packageInput.getText()+"\",\"splash\":"+splashCheck.isChecked()+",\"version\":\"V7.1\"}";
                zos.putNextEntry(new ZipEntry("assets/www/__builder_config.json")); zos.write(config.getBytes()); zos.closeEntry(); zos.close();
                runOnUiThread(() -> log("> Signing..."));
                Security.addProvider(new BouncyCastleProvider()); KeyPairGenerator kpg=KeyPairGenerator.getInstance("RSA"); kpg.initialize(2048); KeyPair kp=kpg.generateKeyPair(); X500Principal issuer=new X500Principal("CN=BuilderB V7.1"); JcaX509v3CertificateBuilder certBuilder=new JcaX509v3CertificateBuilder(issuer, BigInteger.valueOf(System.currentTimeMillis()), new Date(System.currentTimeMillis()-100000), new Date(System.currentTimeMillis()+365L*24*3600*1000*3), issuer, kp.getPublic()); X509Certificate cert=new JcaX509CertificateConverter().setProvider("BC").getCertificate(certBuilder.build(new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(kp.getPrivate()))); ApkSigner.SignerConfig sc=new ApkSigner.SignerConfig.Builder("B7",kp.getPrivate(),Collections.singletonList(cert)).build(); new ApkSigner.Builder(Collections.singletonList(sc)).setInputApk(unsigned).setOutputApk(signed).setV1SigningEnabled(true).setV2SigningEnabled(true).setV3SigningEnabled(true).build().sign();
                runOnUiThread(() -> { log("✅ V7.1 JADI!\n📁 "+signed.getPath()); toast("Sukses!"); Intent intent=new Intent(Intent.ACTION_VIEW); intent.setDataAndType(Uri.fromFile(signed),"application/vnd.android.package-archive"); intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_GRANT_READ_URI_PERMISSION); try{ startActivity(intent); }catch(Exception e){} });
            }catch(Exception e){ e.printStackTrace(); runOnUiThread(() -> log("❌ "+e.toString())); }
        }).start();
    }
    void log(String s){ runOnUiThread(() -> logView.append("\n"+s)); }
    void toast(String s){ runOnUiThread(() -> Toast.makeText(this,s,Toast.LENGTH_SHORT).show()); }
    String getFileName(Uri uri){ String r=null; if(uri.getScheme().equals("content")){ Cursor c=getContentResolver().query(uri,null,null,null,null); try{ if(c!=null&&c.moveToFirst()){ int idx=c.getColumnIndex(OpenableColumns.DISPLAY_NAME); if(idx>=0) r=c.getString(idx); } }finally{ if(c!=null) c.close(); } } if(r==null) r=uri.getPath(); int cut=r.lastIndexOf('/'); if(cut!=-1) r=r.substring(cut+1); return r; }
    Button makeBtn(String t){ Button b=new Button(this); b.setText(t); b.setTextColor(Color.WHITE); b.setBackgroundColor(Color.parseColor("#6C5CE7")); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,6,0,6); b.setLayoutParams(p); b.setPadding(0,20,0,20); return b; }
}
