package com.raynanapk.builder;
import java.io.*;
import java.util.zip.*;
import android.content.Context;

public class BuilderEngine {
    // Ini engine AppMint - repack APK
    public static File buildApk(Context ctx, File inputFile, String appName) throws Exception {
        File outDir = new File(ctx.getExternalFilesDir(null), "BuilderB_Output");
        outDir.mkdirs();
        File outApk = new File(outDir, appName.replaceAll("[^a-zA-Z0-9]","_") + "_by_BuilderB.apk");

        // Ambil APK template (APK kita sendiri)
        String srcApkPath = ctx.getPackageCodePath();

        ZipFile srcZip = new ZipFile(srcApkPath);
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outApk));

        // Copy semua isi APK lama kecuali assets/www lama
        var entries = srcZip.entries();
        while(entries.hasMoreElements()){
            ZipEntry e = entries.nextElement();
            if(e.getName().startsWith("assets/www/")) continue; // skip old web
            if(e.getName().contains("META-INF/")) continue; // skip old signature
            zos.putNextEntry(new ZipEntry(e.getName()));
            InputStream is = srcZip.getInputStream(e);
            byte[] buf = new byte[8192];
            int len;
            while((len=is.read(buf))>0) zos.write(buf,0,len);
            zos.closeEntry();
            is.close();
        }
        // Inject file user ke assets/www/
        zos.putNextEntry(new ZipEntry("assets/www/"));
        zos.closeEntry();

        if(inputFile.isDirectory()){
            addFolderToZip(zos, inputFile, "assets/www/");
        } else if(inputFile.getName().endsWith(".zip")){
            // extract zip ke assets/www
            ZipFile userZip = new ZipFile(inputFile);
            var ue = userZip.entries();
            while(ue.hasMoreElements()){
                ZipEntry ze = ue.nextElement();
                if(ze.isDirectory()) continue;
                zos.putNextEntry(new ZipEntry("assets/www/" + ze.getName()));
                InputStream iis = userZip.getInputStream(ze);
                byte[] b = new byte[8192]; int l;
                while((l=iis.read(b))>0) zos.write(b,0,l);
                zos.closeEntry(); iis.close();
            }
            userZip.close();
        } else {
            // single html file
            zos.putNextEntry(new ZipEntry("assets/www/index.html"));
            FileInputStream fis = new FileInputStream(inputFile);
            byte[] b = new byte[8192]; int l;
            while((l=fis.read(b))>0) zos.write(b,0,l);
            zos.closeEntry(); fis.close();
        }

        zos.close();
        srcZip.close();

        // TODO: Sign APK (V3.1 kita tambahin auto sign)
        // Untuk V3 ini hasil masih unsigned, bisa di-sign pakai MT Manager / apksigner

        return outApk;
    }

    static void addFolderToZip(ZipOutputStream zos, File folder, String base) throws Exception {
        for(File f : folder.listFiles()){
            if(f.isDirectory()){
                addFolderToZip(zos, f, base + f.getName() + "/");
            } else {
                zos.putNextEntry(new ZipEntry(base + f.getName()));
                FileInputStream fis = new FileInputStream(f);
                byte[] b = new byte[8192]; int l;
                while((l=fis.read(b))>0) zos.write(b,0,l);
                zos.closeEntry(); fis.close();
            }
        }
    }
}
