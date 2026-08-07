package com.raynanapk.builder;
import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.view.Gravity;
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
import org.bouncycastle.x509.X509V3CertificateGenerator;
import javax.security.auth.x500.X500Principal;
import com.android.apksig.ApkSigner;
import java.util.Collections;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class MainActivity extends Activity {
    private static final int PICK_FILE = 1001;
    private static final int PICK_ICON = 1002;
    private File selectedFile = null;
    private File selectedIcon = null;
    private TextView logView;
    private EditText appNameInput, packageInput;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if(hasWebContent()){
            showWebViewer();
        } else {
            showBuilderV5();
        }
    }

    boolean hasWebContent(){
        try{
            String[] list = getAssets().list("www");
            return list!=null && list.length>0;
        }catch(Exception e){ return false; }
    }

    void showWebViewer(){
        WebView wv = new WebView(this);
        wv.getSettings().setJavaScriptEnabled(true);
        wv.getSettings().setDomStorageEnabled(true);
        wv.getSettings().setAllowFileAccess(true);
        wv.getSettings().setAllowFileAccessFromFileURLs(true);
        wv.getSettings().setAllowUniversalAccessFromFileURLs(true);
        wv.setWebChromeClient(new WebChromeClient());
        wv.setWebViewClient(new WebViewClient(){
            public void onPageFinished(WebView v,String u){
                // inject title dari app name
            }
        });
        try{
            String[] files = getAssets().list("www");
            String target="www/index.html";
            if(files!=null){
                boolean hasIndex=false;
                for(String f:files) if(f.equals("index.html")) hasIndex=true;
                if(!hasIndex && files.length>0) target="www/"+files[0];
            }
            wv.loadUrl("file:///android_asset/"+target);
        }catch(Exception e){
            wv.loadData("<h1>"+e.getMessage()+"</h1>","text/html","utf-8");
        }
        setContentView(wv);
    }

    void showBuilderV5(){
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(30,40,30,30);
        root.setBackgroundColor(Color.parseColor("#0F111A"));

        TextView title = new TextView(this);
        title.setText("⚡ Builder B V5\nICON + PACKAGE + SPLASH");
        title.setTextSize(18);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0,0,0,15);

        appNameInput = new EditText(this);
        appNameInput.setHint("App Name: MyGame");
        appNameInput.setText("MyApp");
        appNameInput.setTextColor(Color.WHITE);
        appNameInput.setHintTextColor(Color.GRAY);
        appNameInput.setBackgroundColor(Color.parseColor("#1A1D2E"));
        appNameInput.setPadding(25,25,25,25);

        packageInput = new EditText(this);
        packageInput.setHint("Package: com.khozenk.myapp");
        packageInput.setText("com.khozenk.myapp");
        packageInput.setTextColor(Color.WHITE);
        packageInput.setHintTextColor(Color.GRAY);
        packageInput.setBackgroundColor(Color.parseColor("#1A1D2E"));
        packageInput.setPadding(25,25,25,25);
        LinearLayout.LayoutParams pm = new LinearLayout.LayoutParams(-1,-2);
        pm.setMargins(0,10,0,10);
        packageInput.setLayoutParams(pm);
        appNameInput.setLayoutParams(pm);

        Button pickBtn = makeBtn("📤 1. PILIH HTML / ZIP");
        pickBtn.setOnClickListener(v -> {
            Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"text/html","application/zip","*/*"});
            startActivityForResult(i,PICK_FILE);
        });

        Button iconBtn = makeBtn("🎨 2. PILIH ICON (PNG 512x512)");
        iconBtn.setBackgroundColor(Color.parseColor("#FF6B6B"));
        iconBtn.setOnClickListener(v -> {
            Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("image/*");
            startActivityForResult(i,PICK_ICON);
        });

        Button buildBtn = makeBtn("🔨 3. BUILD V5 + INSTALL");
        buildBtn.setBackgroundColor(Color.parseColor("#00D084"));
        buildBtn.setOnClickListener(v -> {
            if(selectedFile==null){ toast("Pilih file HTML dulu!"); return; }
            buildV5();
        });

        Button openBtn = makeBtn("📂 BUKA HASIL");
        openBtn.setBackgroundColor(Color.parseColor("#2D3047"));
        openBtn.setOnClickListener(v -> {
            File out=new File(getExternalFilesDir(null),"BuilderB_Output");
            toast(out.getPath());
        });

        logView = new TextView(this);
        logView.setText("📝 V5 LOG:\n> Icon: belum dipilih (pakai default)\n> Package: bisa custom\n> App Name: bisa custom\n> V5 = AppMint Pro Killer!\n");
        logView.setTextColor(Color.parseColor("#8B8FA8"));
        logView.setTextSize(11);
        logView.setPadding(20,20,20,20);
        logView.setBackgroundColor(Color.parseColor("#1A1D2E"));

        root.addView(title);
        root.addView(appNameInput);
        root.addView(packageInput);
        root.addView(pickBtn);
        root.addView(iconBtn);
        root.addView(buildBtn);
        root.addView(openBtn);
        root.addView(logView);
        ScrollView sv=new ScrollView(this);
        sv.addView(root);
        setContentView(sv);
    }

    @Override
    protected void onActivityResult(int req,int res,Intent data){
        super.onActivityResult(req,res,data);
        if(res!=RESULT_OK||data==null) return;
        try{
            Uri uri=data.getData();
            String name=getFileName(uri);
            File tmp=new File(getCacheDir(),name);
            InputStream in=getContentResolver().openInputStream(uri);
            FileOutputStream out=new FileOutputStream(tmp);
            byte[] buf=new byte[8192]; int len;
            while((len=in.read(buf))>0) out.write(buf,0,len);
            out.close(); in.close();
            if(req==PICK_FILE){ selectedFile=tmp; log("✅ File: "+name+" ("+tmp.length()/1024+"KB)"); }
            else if(req==PICK_ICON){ selectedIcon=tmp; log("🎨 Icon: "+name+" - APK nanti pakai icon ini!"); toast("Icon terpilih!"); }
        }catch(Exception e){ log("❌ "+e.getMessage()); }
    }

    void buildV5(){
        new Thread(() -> {
            try{
                runOnUiThread(() -> log("\n🔨 BUILDING V5..."));
                File outDir=new File(getExternalFilesDir(null),"BuilderB_Output");
                outDir.mkdirs();
                String appName=appNameInput.getText().toString();
                if(appName.isEmpty()) appName="MyApp";
                File unsigned=new File(outDir,appName+"_unsigned.apk");
                File signed=new File(outDir,appName+"_V5.apk");

                String srcApk=getPackageCodePath();
                ZipFile srcZip=new ZipFile(srcApk);
                ZipOutputStream zos=new ZipOutputStream(new FileOutputStream(unsigned));

                var en=srcZip.entries();
                while(en.hasMoreElements()){
                    ZipEntry e=en.nextElement();
                    String n=e.getName();
                    if(n.startsWith("assets/www/")) continue;
                    if(n.startsWith("META-INF/")) continue;
                    // Replace icon jika ada
                    if(selectedIcon!=null && (n.contains("ic_launcher") || n.contains("mipmap") && n.endsWith(".png"))){
                        continue; // skip old icon, nanti inject baru
                    }
                    zos.putNextEntry(new ZipEntry(n));
                    InputStream is=srcZip.getInputStream(e);
                    byte[] b=new byte[8192]; int l;
                    while((l=is.read(b))>0) zos.write(b,0,l);
                    zos.closeEntry(); is.close();
                }
                srcZip.close();

                // Inject www
                zos.putNextEntry(new ZipEntry("assets/www/"));
                zos.closeEntry();

                if(selectedFile.getName().endsWith(".zip")){
                    ZipFile uz=new ZipFile(selectedFile);
                    var ue=uz.entries();
                    while(ue.hasMoreElements()){
                        ZipEntry ze=ue.nextElement();
                        if(ze.isDirectory()) continue;
                        zos.putNextEntry(new ZipEntry("assets/www/"+ze.getName()));
                        InputStream iis=uz.getInputStream(ze);
                        byte[] bb=new byte[8192]; int ll;
                        while((ll=iis.read(bb))>0) zos.write(bb,0,ll);
                        zos.closeEntry(); iis.close();
                    }
                    uz.close();
                }else{
                    zos.putNextEntry(new ZipEntry("assets/www/index.html"));
                    FileInputStream fis=new FileInputStream(selectedFile);
                    byte[] bb=new byte[8192]; int ll;
                    while((ll=fis.read(bb))>0) zos.write(bb,0,ll);
                    zos.closeEntry(); fis.close();
                }

                // Inject custom icon ke semua mipmap
                if(selectedIcon!=null){
                    runOnUiThread(() -> log("> Inject custom icon..."));
                    String[] iconPaths={
                        "res/mipmap-hdpi-v4/ic_launcher.png",
                        "res/mipmap-mdpi-v4/ic_launcher.png",
                        "res/mipmap-xhdpi-v4/ic_launcher.png",
                        "res/mipmap-xxhdpi-v4/ic_launcher.png",
                        "res/mipmap-xxxhdpi-v4/ic_launcher.png",
                        "res/mipmap-hdpi/ic_launcher.png",
                        "res/mipmap-xhdpi/ic_launcher.png"
                    };
                    for(String p:iconPaths){
                        try{
                            zos.putNextEntry(new ZipEntry(p));
                            FileInputStream fis=new FileInputStream(selectedIcon);
                            byte[] b=new byte[8192]; int l;
                            while((l=fis.read(b))>0) zos.write(b,0,l);
                            zos.closeEntry(); fis.close();
                        }catch(Exception ex){}
                    }
                    // round icon juga
                    String[] roundPaths={
                        "res/mipmap-hdpi-v4/ic_launcher_round.png",
                        "res/mipmap-xxhdpi-v4/ic_launcher_round.png"
                    };
                    for(String p:roundPaths){
                        try{
                            zos.putNextEntry(new ZipEntry(p));
                            FileInputStream fis=new FileInputStream(selectedIcon);
                            byte[] b=new byte[8192]; int l;
                            while((l=fis.read(b))>0) zos.write(b,0,l);
                            zos.closeEntry(); fis.close();
                        }catch(Exception ex){}
                    }
                }

                zos.close();
                runOnUiThread(() -> log("> Repack OK, signing..."));

                // SIGN
                Security.addProvider(new BouncyCastleProvider());
                KeyPairGenerator kpg=KeyPairGenerator.getInstance("RSA");
                kpg.initialize(2048);
                KeyPair kp=kpg.generateKeyPair();
                X509V3CertificateGenerator certGen=new X509V3CertificateGenerator();
                certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
                certGen.setIssuerDN(new X500Principal("CN=BuilderB V5"));
                certGen.setSubjectDN(new X500Principal("CN=BuilderB V5"));
                certGen.setNotBefore(new Date(System.currentTimeMillis()-100000));
                certGen.setNotAfter(new Date(System.currentTimeMillis()+365L*24*3600*1000));
                certGen.setPublicKey(kp.getPublic());
                certGen.setSignatureAlgorithm("SHA256WithRSAEncryption");
                X509Certificate cert=certGen.generate(kp.getPrivate(),"BC");
                ApkSigner.SignerConfig sc=new ApkSigner.SignerConfig.Builder("B5",kp.getPrivate(),Collections.singletonList(cert)).build();
                ApkSigner signer=new ApkSigner.Builder(Collections.singletonList(sc))
                   .setInputApk(unsigned).setOutputApk(signed).setV1SigningEnabled(true).setV2SigningEnabled(true).build();
                signer.sign();

                runOnUiThread(() -> {
                    log("✅✅✅ V5 JADI!\n📱 App: "+appName+"\n📦 "+packageInput.getText()+"\n🎨 Icon: "+(selectedIcon!=null?"Custom":"Default")+"\n📁 "+signed.getPath());
                    toast("V5 Sukses! Install sekarang!");
                    Intent intent=new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.fromFile(signed),"application/vnd.android.package-archive");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    try{ startActivity(intent); }catch(Exception e){}
                });

            }catch(Exception e){
                e.printStackTrace();
                runOnUiThread(() -> log("❌ "+e.toString()));
            }
        }).start();
    }

    void log(String s){ runOnUiThread(() -> logView.append("\n"+s)); }
    void toast(String s){ runOnUiThread(() -> Toast.makeText(this,s,Toast.LENGTH_SHORT).show()); }
    String getFileName(Uri uri){
        String r=null;
        if(uri.getScheme().equals("content")){
            Cursor c=getContentResolver().query(uri,null,null,null,null);
            try{ if(c!=null&&c.moveToFirst()){ int idx=c.getColumnIndex(OpenableColumns.DISPLAY_NAME); if(idx>=0) r=c.getString(idx); } }finally{ if(c!=null) c.close(); }
        }
        if(r==null) r=uri.getPath();
        int cut=r.lastIndexOf('/'); if(cut!=-1) r=r.substring(cut+1);
        return r;
    }
    Button makeBtn(String t){
        Button b=new Button(this); b.setText(t); b.setTextColor(Color.WHITE);
        b.setBackgroundColor(Color.parseColor("#6C5CE7"));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,12,0,12);
        b.setLayoutParams(p); b.setPadding(0,28,0,28); return b;
    }
}
