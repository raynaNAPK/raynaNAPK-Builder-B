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
import java.util.Enumeration;
import android.os.Handler;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;

public class MainActivity extends Activity {
    private static final int PICK_FILE=1001,PICK_ICON=1002;
    private File selectedFile=null,selectedIcon=null;
    private TextView logView;
    private EditText appNameInput,packageInput;
    private CheckBox splashCheck,fileCheck;
    private ValueCallback<Uri[]> filePathCallback;
    @Override protected void onCreate(Bundle b){ super.onCreate(b); if(hasWebContent()) showViewerV9(); else showBuilderV9(); }
    boolean hasWebContent(){ try{ String[] l=getAssets().list("www"); return l!=null&&l.length>0; }catch(Exception e){ return false; } }
    String readAsset(String p) throws Exception { InputStream is=getAssets().open(p); ByteArrayOutputStream baos=new ByteArrayOutputStream(); byte[] buf=new byte[8192]; int l; while((l=is.read(buf))>0) baos.write(buf,0,l); is.close(); return baos.toString("UTF-8"); }
    String getCfg(String key,String def){ try{ String cfg=readAsset("www/__builder_config.json"); if(cfg.contains(key)){ int i=cfg.indexOf(key); int s=cfg.indexOf("\"",i+key.length()+2)+1; int e=cfg.indexOf("\"",s); if(s>0&&e>s) return cfg.substring(s,e); } }catch(Exception e){} return def; }
    boolean getCfgBool(String key){ try{ String cfg=readAsset("www/__builder_config.json"); return cfg.contains("\""+key+"\":true"); }catch(Exception e){ return false; } }
    void showViewerV9(){ boolean needSplash=getCfgBool("splash"); String appName=getCfg("appName","MyApp"); if(needSplash){ LinearLayout splash=new LinearLayout(this); splash.setOrientation(LinearLayout.VERTICAL); splash.setBackgroundColor(Color.parseColor("#0F111A")); splash.setGravity(Gravity.CENTER); TextView tv=new TextView(this); tv.setText(appName); tv.setTextColor(Color.WHITE); tv.setTextSize(28); splash.addView(tv); setContentView(splash); new Handler().postDelayed(new Runnable(){ public void run(){ showWebV9(false,"","",getCfg("jsInject","")); } },1500);} else showWebV9(false,"","",getCfg("jsInject","")); }
    void showWebV9(boolean needAds,String bannerId,String interId,String jsInject){ FrameLayout root=new FrameLayout(this); WebView wv=new WebView(this); wv.getSettings().setJavaScriptEnabled(true); wv.getSettings().setDomStorageEnabled(true); wv.getSettings().setAllowFileAccess(true); wv.getSettings().setAllowFileAccessFromFileURLs(true); wv.getSettings().setAllowUniversalAccessFromFileURLs(true); wv.setWebChromeClient(new WebChromeClient(){ public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams){ MainActivity.this.filePathCallback=filePathCallback; Intent intent=new Intent(Intent.ACTION_GET_CONTENT); intent.addCategory(Intent.CATEGORY_OPENABLE); intent.setType("*/*"); startActivityForResult(Intent.createChooser(intent,"Choose File"), 1003); return true; } }); try{ String[] files=getAssets().list("www"); String target="www/index.html"; if(files!=null){ boolean has=false; for(String f:files) if(f.equals("index.html")) has=true; if(!has&&files.length>0) target="www/"+files[0]; } wv.loadUrl("file:///android_asset/"+target); }catch(Exception e){ wv.loadData("<h1>"+e.getMessage()+"</h1>","text/html","utf-8"); } root.addView(wv, new FrameLayout.LayoutParams(-1,-1)); setContentView(root); }
    @Override protected void onActivityResult(int req,int res,Intent data){ super.onActivityResult(req,res,data); if(req==1003){ if(filePathCallback!=null){ Uri[] results=null; if(res==RESULT_OK&&data!=null){ String ds=data.getDataString(); if(ds!=null) results=new Uri[]{Uri.parse(ds)}; } filePathCallback.onReceiveValue(results); filePathCallback=null; } return; } if(res!=RESULT_OK||data==null) return; try{ Uri uri=data.getData(); String name=getFileName(uri); File tmp=new File(getCacheDir(),name); InputStream in=getContentResolver().openInputStream(uri); FileOutputStream out=new FileOutputStream(tmp); byte[] buf=new byte[8192]; int len; while((len=in.read(buf))>0) out.write(buf,0,len); out.close(); in.close(); if(req==PICK_FILE){ selectedFile=tmp; log("File: "+name); } else if(req==PICK_ICON){ selectedIcon=tmp; log("Icon: "+name); } }catch(Exception e){ log("ERR "+e.getMessage()); } }
    void showBuilderV9(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(18,22,18,18); root.setBackgroundColor(Color.parseColor("#0F111A"));
        TextView title=new TextView(this); title.setText("Builder B V13.1 FIX BUILD\nLANDING PRICE READY!"); title.setTextSize(11); title.setTextColor(Color.parseColor("#00D084")); title.setGravity(Gravity.CENTER);
        appNameInput=new EditText(this); appNameInput.setHint("Landing Price"); appNameInput.setText("Landing Price"); appNameInput.setTextColor(Color.WHITE); appNameInput.setBackgroundColor(Color.parseColor("#1A1D2E")); appNameInput.setPadding(10,10,10,10);
        packageInput=new EditText(this); packageInput.setHint("com.andi.dadido.app12 (21)"); packageInput.setText("com.andi.dadido.app12"); packageInput.setTextColor(Color.parseColor("#00D084")); packageInput.setBackgroundColor(Color.parseColor("#1A1D2E")); packageInput.setPadding(10,10,10,10);
        TextView hint=new TextView(this); hint.setText("V13.1: Compile fix, hapus fileprovider conflict\n21 CHAR WAJIB!"); hint.setTextColor(Color.YELLOW); hint.setTextSize(9);
        splashCheck=new CheckBox(this); splashCheck.setText("Splash OFF"); splashCheck.setChecked(false); splashCheck.setTextColor(Color.WHITE);
        fileCheck=new CheckBox(this); fileCheck.setText("File Upload ON"); fileCheck.setChecked(true); fileCheck.setTextColor(Color.WHITE);
        Button pickBtn=makeBtn("1. PILIH HTML/ZIP Landing Price"); pickBtn.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("*/*"); startActivityForResult(i,PICK_FILE); }});
        Button iconBtn=makeBtn("2. PILIH ICON"); iconBtn.setBackgroundColor(Color.parseColor("#FF6B6B")); iconBtn.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("image/*"); startActivityForResult(i,PICK_ICON); }});
        Button buildBtn=makeBtn("3. BUILD APK V13.1 -> DOWNLOAD"); buildBtn.setBackgroundColor(Color.parseColor("#00D084")); buildBtn.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ if(selectedFile==null){ toast("Pilih file dulu!"); return; } if(packageInput.getText().toString().length()!=21){ toast("WAJIB 21 CHAR!"); return; } buildV13(); }});
        logView=new TextView(this); logView.setText("V13.1 LOG:\n"); logView.setTextColor(Color.parseColor("#8B8FA8")); logView.setTextSize(9); logView.setPadding(8,8,8,8); logView.setBackgroundColor(Color.parseColor("#1A1D2E"));
        root.addView(title); root.addView(hint); root.addView(appNameInput); root.addView(packageInput); root.addView(splashCheck); root.addView(fileCheck); root.addView(pickBtn); root.addView(iconBtn); root.addView(buildBtn); root.addView(logView);
        ScrollView sv=new ScrollView(this); sv.addView(root); setContentView(sv);
    }
    byte[] patchManifestOnly(byte[] data, String oldPkg, String newPkg21){
        byte[] oldUtf8=oldPkg.getBytes(); byte[] newUtf8=newPkg21.getBytes();
        for(int i=0;i<=data.length-oldUtf8.length;i++){ boolean m=true; for(int j=0;j<oldUtf8.length;j++) if(data[i+j]!=oldUtf8[j]){m=false;break;} if(m){ for(int j=0;j<oldUtf8.length;j++) data[i+j]=newUtf8[j]; } }
        byte[] oldUtf16=new byte[oldPkg.length()*2]; byte[] newUtf16=new byte[newPkg21.length()*2];
        for(int k=0;k<oldPkg.length();k++){ oldUtf16[k*2]=(byte)oldPkg.charAt(k); oldUtf16[k*2+1]=0; }
        for(int k=0;k<newPkg21.length();k++){ newUtf16[k*2]=(byte)newPkg21.charAt(k); newUtf16[k*2+1]=0; }
        for(int i=0;i<=data.length-oldUtf16.length;i++){ boolean m=true; for(int j=0;j<oldUtf16.length;j++) if(data[i+j]!=oldUtf16[j]){m=false;break;} if(m){ for(int j=0;j<newUtf16.length;j++) data[i+j]=newUtf16[j]; } }
        return data;
    }
    void buildV13(){
        new Thread(new Runnable(){
            public void run(){
            try{
                runOnUiThread(new Runnable(){ public void run(){ log("\nBUILDING V13.1..."); } });
                File outDir=new File(getExternalFilesDir(null),"BuilderB_Output"); outDir.mkdirs();
                String appName=appNameInput.getText().toString(); if(appName.isEmpty()) appName="LandingPrice";
                String newPkg=packageInput.getText().toString().trim().toLowerCase();
                final String padLog=newPkg;
                runOnUiThread(new Runnable(){ public void run(){ log("Package 21: "+padLog); } });
                File unsigned=new File(outDir,appName+"_unsigned.apk"); File signed=new File(outDir,appName+"_V13.apk");
                String srcApk=getPackageCodePath(); ZipFile srcZip=new ZipFile(srcApk); ZipOutputStream zos=new ZipOutputStream(new FileOutputStream(unsigned));
                Enumeration en=srcZip.entries(); while(en.hasMoreElements()){
                    ZipEntry e=(ZipEntry)en.nextElement(); String n=e.getName();
                    if(n.startsWith("assets/www/")) continue;
                    if(n.startsWith("META-INF/")) continue;
                    if(n.startsWith("lib/")) continue;
                    if(n.equals("res/xml/file_paths.xml")) continue;
                    if(n.contains("fileprovider")) continue;
                    if(selectedIcon!=null && n.contains("ic_launcher") && n.endsWith(".png")) continue;
                    zos.putNextEntry(new ZipEntry(n)); InputStream is=srcZip.getInputStream(e); byte[] b=new byte[8192]; int l; while((l=is.read(b))>0) zos.write(b,0,l); zos.closeEntry(); is.close();
                } srcZip.close();
                zos.putNextEntry(new ZipEntry("assets/www/")); zos.closeEntry();
                if(selectedFile.getName().endsWith(".zip")){ ZipFile uz=new ZipFile(selectedFile); Enumeration ue=uz.entries(); while(ue.hasMoreElements()){ ZipEntry ze=(ZipEntry)ue.nextElement(); if(ze.isDirectory()) continue; zos.putNextEntry(new ZipEntry("assets/www/"+ze.getName())); InputStream iis=uz.getInputStream(ze); byte[] bb=new byte[8192]; int ll; while((ll=iis.read(bb))>0) zos.write(bb,0,ll); zos.closeEntry(); iis.close(); } uz.close(); }else{ zos.putNextEntry(new ZipEntry("assets/www/index.html")); FileInputStream fis=new FileInputStream(selectedFile); byte[] bb=new byte[8192]; int ll; while((ll=fis.read(bb))>0) zos.write(bb,0,ll); zos.closeEntry(); fis.close(); }
                if(selectedIcon!=null){ String[] paths={"res/mipmap-hdpi-v4/ic_launcher.png","res/mipmap-mdpi-v4/ic_launcher.png","res/mipmap-xhdpi-v4/ic_launcher.png","res/mipmap-xxhdpi-v4/ic_launcher.png","res/mipmap-xxxhdpi-v4/ic_launcher.png"}; for(String p:paths){ try{ zos.putNextEntry(new ZipEntry(p)); FileInputStream fis=new FileInputStream(selectedIcon); byte[] b=new byte[8192]; int l; while((l=fis.read(b))>0) zos.write(b,0,l); zos.closeEntry(); fis.close(); }catch(Exception ex){} } }
                String config="{\"appName\":\""+appName+"\",\"package\":\""+padLog+"\",\"version\":\"V13\"}";
                zos.putNextEntry(new ZipEntry("assets/www/__builder_config.json")); zos.write(config.getBytes()); zos.closeEntry(); zos.close();
                File patched=new File(outDir,appName+"_patched.apk");
                ZipFile z2=new ZipFile(unsigned);
                ZipOutputStream zos2=new ZipOutputStream(new FileOutputStream(patched));
                Enumeration en2=z2.entries();
                while(en2.hasMoreElements()){
                    ZipEntry e2=(ZipEntry)en2.nextElement();
                    if(e2.getName().equals("res/xml/file_paths.xml")) continue;
                    zos2.putNextEntry(new ZipEntry(e2.getName()));
                    InputStream is2=z2.getInputStream(e2);
                    ByteArrayOutputStream baos=new ByteArrayOutputStream();
                    byte[] b2=new byte[8192]; int l2; while((l2=is2.read(b2))>0) baos.write(b2,0,l2); is2.close();
                    byte[] bytes=baos.toByteArray();
                    if(e2.getName().equals("AndroidManifest.xml")){
                        bytes=patchManifestOnly(bytes,"com.raynanapk.builder",padLog);
                        runOnUiThread(new Runnable(){ public void run(){ log("PATCHED: Manifest ONLY!"); } });
                    }
                    zos2.write(bytes);
                    zos2.closeEntry();
                }
                z2.close(); zos2.close();
                unsigned=patched;
                try{ java.security.Security.removeProvider("BC"); }catch(Exception e){}
                org.bouncycastle.jce.provider.BouncyCastleProvider bcProvider = new org.bouncycastle.jce.provider.BouncyCastleProvider(); java.security.Security.addProvider(bcProvider);
                java.security.KeyPairGenerator kpg=java.security.KeyPairGenerator.getInstance("RSA",bcProvider); kpg.initialize(2048); java.security.KeyPair kp=kpg.generateKeyPair();
                javax.security.auth.x500.X500Principal issuer=new javax.security.auth.x500.X500Principal("CN=BuilderB V13");
                org.bouncycastle.operator.ContentSigner signer = new org.bouncycastle.operator.jcajce.JcaContentSignerBuilder("SHA256withRSA").setProvider(bcProvider).build(kp.getPrivate());
                org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder certBuilder=new org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder(issuer, java.math.BigInteger.valueOf(System.currentTimeMillis()), new java.util.Date(System.currentTimeMillis()-100000), new java.util.Date(System.currentTimeMillis()+365L*24*3600*1000*3), issuer, kp.getPublic());
                java.security.cert.X509Certificate cert=new org.bouncycastle.cert.jcajce.JcaX509CertificateConverter().setProvider(bcProvider).getCertificate(certBuilder.build(signer));
                com.android.apksig.ApkSigner.SignerConfig sc=new com.android.apksig.ApkSigner.SignerConfig.Builder("B13",kp.getPrivate(),java.util.Collections.singletonList(cert)).build();
                new com.android.apksig.ApkSigner.Builder(java.util.Collections.singletonList(sc)).setInputApk(unsigned).setOutputApk(signed).setV1SigningEnabled(true).setV2SigningEnabled(true).setV3SigningEnabled(true).build().sign();
                try{
                    PackageManager pm=getPackageManager();
                    PackageInfo pi=pm.getPackageArchiveInfo(signed.getAbsolutePath(),0);
                    final String detected=pi!=null?pi.packageName:"null";
                    runOnUiThread(new Runnable(){ public void run(){ log("DETECTED PKG: "+detected); } });
                }catch(Exception ex){}
                File downloadFile=null;
                try{ File dlDir=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS); dlDir.mkdirs(); downloadFile=new File(dlDir, appName+"_"+padLog.replace(".","_")+"-V13.apk"); FileInputStream fis=new FileInputStream(signed); FileOutputStream fos=new FileOutputStream(downloadFile); byte[] b=new byte[8192]; int l; while((l=fis.read(b))>0) fos.write(b,0,l); fis.close(); fos.close(); final String dp=downloadFile.getAbsolutePath(); runOnUiThread(new Runnable(){ public void run(){ log("COPY: "+dp); } }); }catch(Exception ex){ downloadFile=signed; }
                final File finalFile=downloadFile;
                runOnUiThread(new Runnable(){ public void run(){ log("JADI V13.1! "+padLog); toast("SUKSES "+padLog); try{ Intent intent=new Intent(Intent.ACTION_VIEW); intent.setDataAndType(Uri.fromFile(finalFile),"application/vnd.android.package-archive"); intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_GRANT_READ_URI_PERMISSION); startActivity(intent); }catch(Exception ex){ log("Manual: "+finalFile.getAbsolutePath()); } } });
            }catch(final Exception e){ e.printStackTrace(); runOnUiThread(new Runnable(){ public void run(){ log("ERR "+e.toString()); } }); }
            }
        }).start();
    }
    void log(String s){ final String ss=s; runOnUiThread(new Runnable(){ public void run(){ logView.append("\n"+ss); } }); }
    void toast(String s){ final String ss=s; runOnUiThread(new Runnable(){ public void run(){ Toast.makeText(MainActivity.this,ss,Toast.LENGTH_LONG).show(); } }); }
    String getFileName(Uri uri){ String r=null; if(uri.getScheme().equals("content")){ Cursor c=getContentResolver().query(uri,null,null,null,null); try{ if(c!=null&&c.moveToFirst()){ int idx=c.getColumnIndex(OpenableColumns.DISPLAY_NAME); if(idx>=0) r=c.getString(idx); } }finally{ if(c!=null) c.close(); } } if(r==null) r=uri.getPath(); int cut=r.lastIndexOf('/'); if(cut!=-1) r=r.substring(cut+1); return r; }
    Button makeBtn(String t){ Button b=new Button(this); b.setText(t); b.setTextColor(Color.WHITE); b.setBackgroundColor(Color.parseColor("#6C5CE7")); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,4,0,4); b.setLayoutParams(p); b.setPadding(0,16,0,16); b.setTextSize(12); return b; }
}
