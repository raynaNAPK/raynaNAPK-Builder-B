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
import org.bouncycastle.operator.ContentSigner;
import com.android.apksig.ApkSigner;
import java.util.Collections;
import android.os.Handler;
import android.graphics.BitmapFactory;
import com.google.android.gms.ads.*;
import com.google.android.gms.ads.initialization.*;
import com.google.android.gms.ads.interstitial.*;

public class MainActivity extends Activity {
    private static final int PICK_FILE=1001,PICK_ICON=1002;
    private File selectedFile=null,selectedIcon=null;
    private TextView logView;
    private EditText appNameInput,packageInput,bannerInput,interInput,jsInjectInput;
    private CheckBox splashCheck,fileCheck,adsCheck,aabCheck;
    private ValueCallback<Uri[]> filePathCallback;
    private InterstitialAd interstitialAd;
    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        try{ MobileAds.initialize(this, s->{}); }catch(Exception e){}
        if(hasWebContent()) showViewerV9(); else showBuilderV9();
    }
    boolean hasWebContent(){ try{ String[] l=getAssets().list("www"); return l!=null&&l.length>0; }catch(Exception e){ return false; } }
    String readAsset(String p) throws Exception { InputStream is=getAssets().open(p); ByteArrayOutputStream baos=new ByteArrayOutputStream(); byte[] buf=new byte[8192]; int l; while((l=is.read(buf))>0) baos.write(buf,0,l); is.close(); return baos.toString("UTF-8"); }
    String getCfg(String key,String def){ try{ String cfg=readAsset("www/__builder_config.json"); if(cfg.contains(key)){ int i=cfg.indexOf(key); int s=cfg.indexOf("\"",i+key.length()+2)+1; int e=cfg.indexOf("\"",s); if(s>0&&e>s) return cfg.substring(s,e); } }catch(Exception e){} return def; }
    boolean getCfgBool(String key){ try{ String cfg=readAsset("www/__builder_config.json"); return cfg.contains("\""+key+"\":true"); }catch(Exception e){ return false; } }
    void showViewerV9(){
        boolean needSplash=getCfgBool("splash"); boolean needAds=getCfgBool("ads");
        String bannerId=getCfg("bannerId","ca-app-pub-3940256099942544/6300978111");
        String interId=getCfg("interId","ca-app-pub-3940256099942544/1033173712");
        String jsInject=getCfg("jsInject",""); String appName=getCfg("appName","MyApp");
        if(needSplash){
            LinearLayout splash=new LinearLayout(this); splash.setOrientation(LinearLayout.VERTICAL); splash.setBackgroundColor(Color.parseColor("#0F111A")); splash.setGravity(Gravity.CENTER);
            ImageView logo=new ImageView(this); try{ logo.setImageBitmap(BitmapFactory.decodeStream(getAssets().open("www/icon.png"))); }catch(Exception ex){ try{ logo.setImageResource(getResources().getIdentifier("ic_launcher","mipmap",getPackageName())); }catch(Exception ex2){} }
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(300,300); lp.setMargins(0,0,0,30); logo.setLayoutParams(lp);
            TextView tv=new TextView(this); tv.setText(appName); tv.setTextColor(Color.WHITE); tv.setTextSize(28); tv.setGravity(Gravity.CENTER);
            ProgressBar pb=new ProgressBar(this); splash.addView(logo); splash.addView(tv); splash.addView(pb); setContentView(splash);
            boolean fAds=needAds; String fb=bannerId, fi=interId; String fJs=jsInject; new Handler().postDelayed(() -> showWebV9(fAds,fb,fi,fJs), 2000);
        } else showWebV9(needAds,bannerId,interId,jsInject);
    }
    void showWebV9(boolean needAds,String bannerId,String interId,String jsInject){
        FrameLayout root=new FrameLayout(this); WebView wv=new WebView(this);
        wv.getSettings().setJavaScriptEnabled(true); wv.getSettings().setDomStorageEnabled(true); wv.getSettings().setAllowFileAccess(true); wv.getSettings().setAllowFileAccessFromFileURLs(true); wv.getSettings().setAllowUniversalAccessFromFileURLs(true); wv.getSettings().setMediaPlaybackRequiresUserGesture(false);
        wv.setWebChromeClient(new WebChromeClient(){ public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams){ MainActivity.this.filePathCallback=filePathCallback; Intent intent=new Intent(Intent.ACTION_GET_CONTENT); intent.addCategory(Intent.CATEGORY_OPENABLE); intent.setType("*/*"); startActivityForResult(Intent.createChooser(intent,"Choose File"), 1003); return true; } });
        wv.setWebViewClient(new WebViewClient(){ public void onPageFinished(WebView view, String url){ if(jsInject!=null&&!jsInject.isEmpty()&&!jsInject.equals("null")){ try{ view.evaluateJavascript(jsInject,null); }catch(Exception e){} } } });
        try{ String[] files=getAssets().list("www"); String target="www/index.html"; if(files!=null){ boolean has=false; for(String f:files) if(f.equals("index.html")) has=true; if(!has&&files.length>0) target="www/"+files[0]; } wv.loadUrl("file:///android_asset/"+target); }catch(Exception e){ wv.loadData("<h1>"+e.getMessage()+"</h1>","text/html","utf-8"); }
        root.addView(wv, new FrameLayout.LayoutParams(-1,-1));
        if(needAds){ try{ AdView adView=new AdView(this); adView.setAdSize(AdSize.BANNER); adView.setAdUnitId(bannerId); FrameLayout.LayoutParams adParams=new FrameLayout.LayoutParams(-1,-2); adParams.gravity=Gravity.BOTTOM; root.addView(adView, adParams); AdRequest adRequest=new AdRequest.Builder().build(); adView.loadAd(adRequest); InterstitialAd.load(this, interId, new AdRequest.Builder().build(), new InterstitialAdLoadCallback(){ @Override public void onAdLoaded(InterstitialAd ad){ interstitialAd=ad; new Handler().postDelayed(() -> { if(interstitialAd!=null) interstitialAd.show(MainActivity.this); }, 1500); } }); }catch(Exception e){} }
        setContentView(root);
    }
    @Override protected void onActivityResult(int req,int res,Intent data){
        super.onActivityResult(req,res,data);
        if(req==1003){ if(filePathCallback!=null){ Uri[] results=null; if(res==RESULT_OK&&data!=null){ String ds=data.getDataString(); if(ds!=null) results=new Uri[]{Uri.parse(ds)}; } filePathCallback.onReceiveValue(results); filePathCallback=null; } return; }
        if(res!=RESULT_OK||data==null) return;
        try{ Uri uri=data.getData(); String name=getFileName(uri); File tmp=new File(getCacheDir(),name); InputStream in=getContentResolver().openInputStream(uri); FileOutputStream out=new FileOutputStream(tmp); byte[] buf=new byte[8192]; int len; while((len=in.read(buf))>0) out.write(buf,0,len); out.close(); in.close(); if(req==PICK_FILE){ selectedFile=tmp; log("✅ File: "+name); } else if(req==PICK_ICON){ selectedIcon=tmp; log("🎨 Icon: "+name); } }catch(Exception e){ log("❌ "+e.getMessage()); }
    }
    void showBuilderV9(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(18,22,18,18); root.setBackgroundColor(Color.parseColor("#0F111A"));
        TextView title=new TextView(this); title.setText("⚡ Builder B V9.1\n👑 FIXED SIGNING"); title.setTextSize(17); title.setTextColor(Color.parseColor("#FFD700")); title.setGravity(Gravity.CENTER);
        appNameInput=new EditText(this); appNameInput.setHint("App Name"); appNameInput.setText("dadu"); appNameInput.setTextColor(Color.WHITE); appNameInput.setBackgroundColor(Color.parseColor("#1A1D2E")); appNameInput.setPadding(10,10,10,10);
        packageInput=new EditText(this); packageInput.setHint("Package"); packageInput.setText("com.khozenk.dadu"); packageInput.setTextColor(Color.WHITE); packageInput.setBackgroundColor(Color.parseColor("#1A1D2E")); packageInput.setPadding(10,10,10,10);
        bannerInput=new EditText(this); bannerInput.setHint("Banner ID"); bannerInput.setTextColor(Color.WHITE); bannerInput.setBackgroundColor(Color.parseColor("#1A1D2E")); bannerInput.setPadding(8,8,8,8); bannerInput.setTextSize(9);
        interInput=new EditText(this); interInput.setHint("Inter ID"); interInput.setTextColor(Color.WHITE); interInput.setBackgroundColor(Color.parseColor("#1A1D2E")); interInput.setPadding(8,8,8,8); interInput.setTextSize(9);
        jsInjectInput=new EditText(this); jsInjectInput.setHint("JS Inject"); jsInjectInput.setTextColor(Color.WHITE); jsInjectInput.setBackgroundColor(Color.parseColor("#1A1D2E")); jsInjectInput.setPadding(8,8,8,8); jsInjectInput.setTextSize(9);
        splashCheck=new CheckBox(this); splashCheck.setText("🌊 Splash"); splashCheck.setChecked(true); splashCheck.setTextColor(Color.WHITE); splashCheck.setTextSize(11);
        fileCheck=new CheckBox(this); fileCheck.setText("📁 File Upload"); fileCheck.setChecked(true); fileCheck.setTextColor(Color.WHITE); fileCheck.setTextSize(11);
        adsCheck=new CheckBox(this); adsCheck.setText("💰 AdMob"); adsCheck.setChecked(false); adsCheck.setTextColor(Color.parseColor("#00D084")); adsCheck.setTextSize(11);
        aabCheck=new CheckBox(this); aabCheck.setText("📦 Build AAB"); aabCheck.setChecked(true); aabCheck.setTextColor(Color.parseColor("#FFD700")); aabCheck.setTextSize(11);
        Button pickBtn=makeBtn("📤 1. HTML/ZIP"); pickBtn.setOnClickListener(v->{ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("*/*"); i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"text/html","application/zip","*/*"}); startActivityForResult(i,PICK_FILE); });
        Button iconBtn=makeBtn("🎨 2. ICON"); iconBtn.setBackgroundColor(Color.parseColor("#FF6B6B")); iconBtn.setOnClickListener(v->{ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("image/*"); startActivityForResult(i,PICK_ICON); });
        Button buildBtn=makeBtn("💰 3. BUILD APK"); buildBtn.setBackgroundColor(Color.parseColor("#00D084")); buildBtn.setOnClickListener(v->{ if(selectedFile==null){ toast("Pilih file!"); return; } buildV9(false); });
        Button aabBtn=makeBtn("👑 4. BUILD AAB"); aabBtn.setBackgroundColor(Color.parseColor("#FFD700")); aabBtn.setTextColor(Color.BLACK); aabBtn.setOnClickListener(v->{ if(selectedFile==null){ toast("Pilih file!"); return; } buildV9(true); });
        logView=new TextView(this); logView.setText("📝 V9.1 LOG:\n> Fix BC bug\n> Ready!\n"); logView.setTextColor(Color.parseColor("#8B8FA8")); logView.setTextSize(9); logView.setPadding(8,8,8,8); logView.setBackgroundColor(Color.parseColor("#1A1D2E"));
        root.addView(title); root.addView(appNameInput); root.addView(packageInput); root.addView(bannerInput); root.addView(interInput); root.addView(jsInjectInput); root.addView(splashCheck); root.addView(fileCheck); root.addView(adsCheck); root.addView(aabCheck); root.addView(pickBtn); root.addView(iconBtn); root.addView(buildBtn); root.addView(aabBtn); root.addView(logView);
        ScrollView sv=new ScrollView(this); sv.addView(root); setContentView(sv);
    }
    void buildV9(boolean buildAAB){
        new Thread(() -> {
            try{
                runOnUiThread(() -> log("\n👑 BUILDING V9.1..."));
                File outDir=new File(getExternalFilesDir(null),"BuilderB_Output"); outDir.mkdirs();
                String appName=appNameInput.getText().toString(); if(appName.isEmpty()) appName="MyApp";
                File unsigned=new File(outDir,appName+"_unsigned.apk"); File signed=new File(outDir,appName+(buildAAB?"_V9.aab":"_V9.apk"));
                String srcApk=getPackageCodePath(); ZipFile srcZip=new ZipFile(srcApk); ZipOutputStream zos=new ZipOutputStream(new FileOutputStream(unsigned));
                var en=srcZip.entries(); while(en.hasMoreElements()){ ZipEntry e=en.nextElement(); String n=e.getName(); if(n.startsWith("assets/www/")) continue; if(n.startsWith("META-INF/")) continue; if(selectedIcon!=null && n.contains("ic_launcher") && n.endsWith(".png")) continue; zos.putNextEntry(new ZipEntry(n)); InputStream is=srcZip.getInputStream(e); byte[] b=new byte[8192]; int l; while((l=is.read(b))>0) zos.write(b,0,l); zos.closeEntry(); is.close(); } srcZip.close();
                zos.putNextEntry(new ZipEntry("assets/www/")); zos.closeEntry();
                if(selectedFile.getName().endsWith(".zip")){ ZipFile uz=new ZipFile(selectedFile); var ue=uz.entries(); while(ue.hasMoreElements()){ ZipEntry ze=ue.nextElement(); if(ze.isDirectory()) continue; zos.putNextEntry(new ZipEntry("assets/www/"+ze.getName())); InputStream iis=uz.getInputStream(ze); byte[] bb=new byte[8192]; int ll; while((ll=iis.read(bb))>0) zos.write(bb,0,ll); zos.closeEntry(); iis.close(); } uz.close(); }else{ zos.putNextEntry(new ZipEntry("assets/www/index.html")); FileInputStream fis=new FileInputStream(selectedFile); byte[] bb=new byte[8192]; int ll; while((ll=fis.read(bb))>0) zos.write(bb,0,ll); zos.closeEntry(); fis.close(); }
                if(selectedIcon!=null){ String[] paths={"res/mipmap-hdpi-v4/ic_launcher.png","res/mipmap-mdpi-v4/ic_launcher.png","res/mipmap-xhdpi-v4/ic_launcher.png","res/mipmap-xxhdpi-v4/ic_launcher.png","res/mipmap-xxxhdpi-v4/ic_launcher.png","res/mipmap-hdpi-v4/ic_launcher_round.png"}; for(String p:paths){ try{ zos.putNextEntry(new ZipEntry(p)); FileInputStream fis=new FileInputStream(selectedIcon); byte[] b=new byte[8192]; int l; while((l=fis.read(b))>0) zos.write(b,0,l); zos.closeEntry(); fis.close(); }catch(Exception ex){} } zos.putNextEntry(new ZipEntry("assets/www/icon.png")); FileInputStream fis=new FileInputStream(selectedIcon); byte[] b=new byte[8192]; int l; while((l=fis.read(b))>0) zos.write(b,0,l); zos.closeEntry(); fis.close(); }
                String jsEsc=jsInjectInput.getText().toString().replace("\"","\\\"");
                String config="{\"appName\":\""+appName+"\",\"package\":\""+packageInput.getText()+"\",\"splash\":"+splashCheck.isChecked()+",\"ads\":"+adsCheck.isChecked()+",\"bannerId\":\""+bannerInput.getText()+"\",\"interId\":\""+interInput.getText()+"\",\"jsInject\":\""+jsEsc+"\",\"version\":\"V9.1\"}";
                zos.putNextEntry(new ZipEntry("assets/www/__builder_config.json")); zos.write(config.getBytes()); zos.closeEntry(); zos.close();
                runOnUiThread(() -> log("> Signing FIXED..."));
                try{ Security.removeProvider("BC"); }catch(Exception e){}
                BouncyCastleProvider bcProvider = new BouncyCastleProvider(); Security.addProvider(bcProvider);
                KeyPairGenerator kpg=KeyPairGenerator.getInstance("RSA",bcProvider); kpg.initialize(2048); KeyPair kp=kpg.generateKeyPair();
                X500Principal issuer=new X500Principal("CN=BuilderB V9.1");
                ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").setProvider(bcProvider).build(kp.getPrivate());
                JcaX509v3CertificateBuilder certBuilder=new JcaX509v3CertificateBuilder(issuer, BigInteger.valueOf(System.currentTimeMillis()), new Date(System.currentTimeMillis()-100000), new Date(System.currentTimeMillis()+365L*24*3600*1000*3), issuer, kp.getPublic());
                X509Certificate cert=new JcaX509CertificateConverter().setProvider(bcProvider).getCertificate(certBuilder.build(signer));
                ApkSigner.SignerConfig sc=new ApkSigner.SignerConfig.Builder("B9",kp.getPrivate(),Collections.singletonList(cert)).build();
                new ApkSigner.Builder(Collections.singletonList(sc)).setInputApk(unsigned).setOutputApk(signed).setV1SigningEnabled(true).setV2SigningEnabled(true).setV3SigningEnabled(true).build().sign();
                runOnUiThread(() -> { log("✅ V9.1 JADI!\n📁 "+signed.getPath()); toast("Sukses!"); if(!buildAAB){ Intent intent=new Intent(Intent.ACTION_VIEW); intent.setDataAndType(Uri.fromFile(signed),"application/vnd.android.package-archive"); intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_GRANT_READ_URI_PERMISSION); try{ startActivity(intent); }catch(Exception e){} } });
            }catch(Exception e){ e.printStackTrace(); runOnUiThread(() -> log("❌ "+e.toString())); }
        }).start();
    }
    void log(String s){ runOnUiThread(() -> logView.append("\n"+s)); }
    void toast(String s){ runOnUiThread(() -> Toast.makeText(this,s,Toast.LENGTH_SHORT).show()); }
    String getFileName(Uri uri){ String r=null; if(uri.getScheme().equals("content")){ Cursor c=getContentResolver().query(uri,null,null,null,null); try{ if(c!=null&&c.moveToFirst()){ int idx=c.getColumnIndex(OpenableColumns.DISPLAY_NAME); if(idx>=0) r=c.getString(idx); } }finally{ if(c!=null) c.close(); } } if(r==null) r=uri.getPath(); int cut=r.lastIndexOf('/'); if(cut!=-1) r=r.substring(cut+1); return r; }
    Button makeBtn(String t){ Button b=new Button(this); b.setText(t); b.setTextColor(Color.WHITE); b.setBackgroundColor(Color.parseColor("#6C5CE7")); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,4,0,4); b.setLayoutParams(p); b.setPadding(0,16,0,16); b.setTextSize(12); return b; }
}
