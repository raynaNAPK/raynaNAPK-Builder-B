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
import android.os.Environment;

public class MainActivity extends Activity {
    private static final int PICK_FILE=1001,PICK_ICON=1002;
    private File selectedFile=null,selectedIcon=null;
    private TextView logView;
    private EditText appNameInput;
    @Override protected void onCreate(Bundle b){ super.onCreate(b); if(hasWebContent()) showViewer(); else showBuilder(); }
    boolean hasWebContent(){ try{ String[] l=getAssets().list("www"); return l!=null&&l.length>0; }catch(Exception e){ return false; } }
    String readAsset(String p) throws Exception { InputStream is=getAssets().open(p); ByteArrayOutputStream baos=new ByteArrayOutputStream(); byte[] buf=new byte[8192]; int l; while((l=is.read(buf))>0) baos.write(buf,0,l); is.close(); return baos.toString("UTF-8"); }
    String getCfg(String k,String d){ try{ String cfg=readAsset("www/__builder_config.json"); if(cfg.contains(k)){ int i=cfg.indexOf(k); int s=cfg.indexOf("\"",i+k.length()+2)+1; int e=cfg.indexOf("\"",s); if(s>0&&e>s) return cfg.substring(s,e); } }catch(Exception e){} return d; }
    void showViewer(){ String appName=getCfg("appName","Raynan Style"); FrameLayout root=new FrameLayout(this); WebView wv=new WebView(this); wv.getSettings().setJavaScriptEnabled(true); wv.getSettings().setDomStorageEnabled(true); wv.getSettings().setAllowFileAccess(true); wv.getSettings().setAllowFileAccessFromFileURLs(true); wv.getSettings().setAllowUniversalAccessFromFileURLs(true); try{ String[] files=getAssets().list("www"); String target="www/index.html"; if(files!=null){ boolean has=false; for(String f:files) if(f.equals("index.html")) has=true; if(!has&&files.length>0) target="www/"+files[0]; } wv.loadUrl("file:///android_asset/"+target); }catch(Exception e){ wv.loadData("<h1>"+e.getMessage()+"</h1>","text/html","utf-8"); } root.addView(wv, new FrameLayout.LayoutParams(-1,-1)); setContentView(root); }
    @Override protected void onActivityResult(int req,int res,Intent data){ super.onActivityResult(req,res,data); if(res!=RESULT_OK||data==null) return; try{ Uri uri=data.getData(); String name=getFileName(uri); File tmp=new File(getCacheDir(),name); InputStream in=getContentResolver().openInputStream(uri); FileOutputStream out=new FileOutputStream(tmp); byte[] buf=new byte[8192]; int len; while((len=in.read(buf))>0) out.write(buf,0,len); out.close(); in.close(); if(req==PICK_FILE){ selectedFile=tmp; log("File: "+name); } else if(req==PICK_ICON){ selectedIcon=tmp; log("Icon: "+name); } }catch(Exception e){ log("ERR "+e.getMessage()); } }
    void showBuilder(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(18,22,18,18); root.setBackgroundColor(Color.parseColor("#0F111A"));
        TextView title=new TextView(this); title.setText("Builder B V15\ncom.raynanteam.style\nLANDING PRICE READY!"); title.setTextSize(12); title.setTextColor(Color.parseColor("#00D084")); title.setGravity(Gravity.CENTER);
        appNameInput=new EditText(this); appNameInput.setHint("Landing Price / Raynan Style"); appNameInput.setText("Raynan Style"); appNameInput.setTextColor(Color.WHITE); appNameInput.setBackgroundColor(Color.parseColor("#1A1D2E")); appNameInput.setPadding(10,10,10,10);
        TextView hint=new TextView(this); hint.setText("Package: com.raynanteam.style (20 char)\nSama kayak com.multichat.khozenk yang work!\n100% INSTALL, NO PATCH!"); hint.setTextColor(Color.YELLOW); hint.setTextSize(9);
        Button pickBtn=makeBtn("1. PILIH HTML/ZIP Landing Price"); pickBtn.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("*/*"); startActivityForResult(i,PICK_FILE); }});
        Button iconBtn=makeBtn("2. PILIH ICON"); iconBtn.setBackgroundColor(Color.parseColor("#FF6B6B")); iconBtn.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("image/*"); startActivityForResult(i,PICK_ICON); }});
        Button buildBtn=makeBtn("3. BUILD APK com.raynanteam.style"); buildBtn.setBackgroundColor(Color.parseColor("#00D084")); buildBtn.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ if(selectedFile==null){ toast("Pilih file dulu!"); return; } buildV15(); }});
        logView=new TextView(this); logView.setText("V15 LOG:\n- Package: com.raynanteam.style (20)\n- Clean manifest like khozenk\n- Landing Price\n"); logView.setTextColor(Color.parseColor("#8B8FA8")); logView.setTextSize(9); logView.setPadding(8,8,8,8); logView.setBackgroundColor(Color.parseColor("#1A1D2E"));
        root.addView(title); root.addView(hint); root.addView(appNameInput); root.addView(pickBtn); root.addView(iconBtn); root.addView(buildBtn); root.addView(logView);
        ScrollView sv=new ScrollView(this); sv.addView(root); setContentView(sv);
    }
    void buildV15(){
        new Thread(new Runnable(){
            public void run(){
            try{
                runOnUiThread(new Runnable(){ public void run(){ log("\nBUILDING V15 com.raynanteam.style..."); } });
                File outDir=new File(getExternalFilesDir(null),"BuilderB_Output"); outDir.mkdirs();
                String appName=appNameInput.getText().toString(); if(appName.isEmpty()) appName="RaynanStyle";
                String newPkg="com.raynanteam.style";
                File unsigned=new File(outDir,appName+"_unsigned.apk"); File signed=new File(outDir,appName+"_V15.apk");
                String srcApk=getPackageCodePath(); ZipFile srcZip=new ZipFile(srcApk); ZipOutputStream zos=new ZipOutputStream(new FileOutputStream(unsigned));
                Enumeration en=srcZip.entries(); while(en.hasMoreElements()){
                    ZipEntry e=(ZipEntry)en.nextElement(); String n=e.getName();
                    if(n.startsWith("assets/www/")) continue;
                    if(n.startsWith("META-INF/")) continue;
                    if(selectedIcon!=null && n.contains("ic_launcher") && n.endsWith(".png")) continue;
                    zos.putNextEntry(new ZipEntry(n)); InputStream is=srcZip.getInputStream(e); byte[] b=new byte[8192]; int l; while((l=is.read(b))>0) zos.write(b,0,l); zos.closeEntry(); is.close();
                } srcZip.close();
                zos.putNextEntry(new ZipEntry("assets/www/")); zos.closeEntry();
                if(selectedFile.getName().endsWith(".zip")){ ZipFile uz=new ZipFile(selectedFile); Enumeration ue=uz.entries(); while(ue.hasMoreElements()){ ZipEntry ze=(ZipEntry)ue.nextElement(); if(ze.isDirectory()) continue; zos.putNextEntry(new ZipEntry("assets/www/"+ze.getName())); InputStream iis=uz.getInputStream(ze); byte[] bb=new byte[8192]; int ll; while((ll=iis.read(bb))>0) zos.write(bb,0,ll); zos.closeEntry(); iis.close(); } uz.close(); }else{ zos.putNextEntry(new ZipEntry("assets/www/index.html")); FileInputStream fis=new FileInputStream(selectedFile); byte[] bb=new byte[8192]; int ll; while((ll=fis.read(bb))>0) zos.write(bb,0,ll); zos.closeEntry(); fis.close(); }
                if(selectedIcon!=null){ String[] paths={"res/mipmap-hdpi-v4/ic_launcher.png","res/mipmap-mdpi-v4/ic_launcher.png","res/mipmap-xhdpi-v4/ic_launcher.png","res/mipmap-xxhdpi-v4/ic_launcher.png","res/mipmap-xxxhdpi-v4/ic_launcher.png"}; for(String p:paths){ try{ zos.putNextEntry(new ZipEntry(p)); FileInputStream fis=new FileInputStream(selectedIcon); byte[] b=new byte[8192]; int l; while((l=fis.read(b))>0) zos.write(b,0,l); zos.closeEntry(); fis.close(); }catch(Exception ex){} } }
                String config="{\"appName\":\""+appName+"\",\"package\":\""+newPkg+"\",\"version\":\"V15\"}";
                zos.putNextEntry(new ZipEntry("assets/www/__builder_config.json")); zos.write(config.getBytes()); zos.closeEntry(); zos.close();
                try{ Security.removeProvider("BC"); }catch(Exception e){}
                BouncyCastleProvider bcProvider = new BouncyCastleProvider(); Security.addProvider(bcProvider);
                KeyPairGenerator kpg=KeyPairGenerator.getInstance("RSA",bcProvider); kpg.initialize(2048); KeyPair kp=kpg.generateKeyPair();
                X500Principal issuer=new X500Principal("CN=BuilderB V15");
                ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").setProvider(bcProvider).build(kp.getPrivate());
                JcaX509v3CertificateBuilder certBuilder=new JcaX509v3CertificateBuilder(issuer, BigInteger.valueOf(System.currentTimeMillis()), new Date(System.currentTimeMillis()-100000), new Date(System.currentTimeMillis()+365L*24*3600*1000*3), issuer, kp.getPublic());
                X509Certificate cert=new JcaX509CertificateConverter().setProvider(bcProvider).getCertificate(certBuilder.build(signer));
                ApkSigner.SignerConfig sc=new ApkSigner.SignerConfig.Builder("B15",kp.getPrivate(),Collections.singletonList(cert)).build();
                new ApkSigner.Builder(Collections.singletonList(sc)).setInputApk(unsigned).setOutputApk(signed).setV1SigningEnabled(true).setV2SigningEnabled(true).setV3SigningEnabled(true).build().sign();
                File downloadFile=null;
                try{ File dlDir=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS); dlDir.mkdirs(); downloadFile=new File(dlDir, appName+"_"+newPkg.replace(".","_")+"-V15.apk"); FileInputStream fis=new FileInputStream(signed); FileOutputStream fos=new FileOutputStream(downloadFile); byte[] b=new byte[8192]; int l; while((l=fis.read(b))>0) fos.write(b,0,l); fis.close(); fos.close(); final String dp=downloadFile.getAbsolutePath(); runOnUiThread(new Runnable(){ public void run(){ log("COPY: "+dp); } }); }catch(Exception ex){ downloadFile=signed; }
                final File finalFile=downloadFile;
                runOnUiThread(new Runnable(){ public void run(){ log("JADI V15! "+newPkg+"\n100% INSTALL!"); toast("SUKSES "+newPkg); try{ Intent intent=new Intent(Intent.ACTION_VIEW); intent.setDataAndType(Uri.fromFile(finalFile),"application/vnd.android.package-archive"); intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_GRANT_READ_URI_PERMISSION); startActivity(intent); }catch(Exception ex){ log("Manual: "+finalFile.getAbsolutePath()); } } });
            }catch(final Exception e){ e.printStackTrace(); runOnUiThread(new Runnable(){ public void run(){ log("ERR "+e.toString()); } }); }
            }
        }).start();
    }
    void log(String s){ final String ss=s; runOnUiThread(new Runnable(){ public void run(){ logView.append("\n"+ss); } }); }
    void toast(String s){ final String ss=s; runOnUiThread(new Runnable(){ public void run(){ Toast.makeText(MainActivity.this,ss,Toast.LENGTH_LONG).show(); } }); }
    String getFileName(Uri uri){ String r=null; if(uri.getScheme().equals("content")){ Cursor c=getContentResolver().query(uri,null,null,null,null); try{ if(c!=null&&c.moveToFirst()){ int idx=c.getColumnIndex(OpenableColumns.DISPLAY_NAME); if(idx>=0) r=c.getString(idx); } }finally{ if(c!=null) c.close(); } } if(r==null) r=uri.getPath(); int cut=r.lastIndexOf('/'); if(cut!=-1) r=r.substring(cut+1); return r; }
    Button makeBtn(String t){ Button b=new Button(this); b.setText(t); b.setTextColor(Color.WHITE); b.setBackgroundColor(Color.parseColor("#6C5CE7")); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,4,0,4); b.setLayoutParams(p); b.setPadding(0,16,0,16); b.setTextSize(12); return b; }
}
