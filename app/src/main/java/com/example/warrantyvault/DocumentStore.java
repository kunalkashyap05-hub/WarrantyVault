package com.example.warrantyvault;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

/**
 * Everything about files: copying picked files into private app storage, opening, sharing and
 * downloading (saving a copy into the phone's Downloads folder).
 */
public class DocumentStore {

    public static final String DIR_BILLS = "product_bills";
    public static final String DIR_DOCS = "product_docs";

    /** A file that has been copied into app storage. */
    public static class Saved {
        public String path;
        public String name;
        public String mime;
        public long size;
    }

    // ------------------------------------------------------------------
    // Import
    // ------------------------------------------------------------------
    public static Saved copyIn(Context ctx, Uri source, String subDir) {
        try {
            ContentResolver cr = ctx.getContentResolver();
            String name = queryName(cr, source);
            String mime = cr.getType(source);
            String ext = extensionOf(name);
            if (ext.isEmpty() && mime != null) {
                String guess = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime);
                ext = guess != null ? guess : "";
            }
            if (name == null || name.isEmpty()) {
                name = "document" + (ext.isEmpty() ? "" : "." + ext);
            }
            if (mime == null) mime = mimeForName(name);

            File dir = new File(ctx.getFilesDir(), subDir);
            if (!dir.exists() && !dir.mkdirs()) return null;
            File dest = new File(dir, System.currentTimeMillis() + "_" + (int) (Math.random() * 10000)
                    + (ext.isEmpty() ? "" : "." + ext));

            try (InputStream in = cr.openInputStream(source);
                 OutputStream out = new java.io.FileOutputStream(dest)) {
                if (in == null) return null;
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            }

            Saved s = new Saved();
            s.path = dest.getAbsolutePath();
            s.name = name;
            s.mime = mime;
            s.size = dest.length();
            return s;
        } catch (IOException | SecurityException e) {
            return null;
        }
    }

    private static String queryName(ContentResolver cr, Uri uri) {
        try (Cursor c = cr.query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (i != -1) return c.getString(i);
            }
        } catch (Exception ignored) {
        }
        String last = uri.getLastPathSegment();
        return last != null ? last : "";
    }

    public static String extensionOf(String name) {
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) return "";
        String ext = name.substring(dot + 1).toLowerCase(Locale.ROOT);
        return ext.length() > 8 ? "" : ext;
    }

    public static String mimeForName(String name) {
        String ext = extensionOf(name);
        String m = ext.isEmpty() ? null : MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        return m != null ? m : "application/octet-stream";
    }

    public static boolean isImage(String mime, String name) {
        if (mime != null && mime.startsWith("image/")) return true;
        return mimeForName(name).startsWith("image/");
    }

    public static boolean isPdf(String mime, String name) {
        return "application/pdf".equals(mime) || "pdf".equals(extensionOf(name));
    }

    public static String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.getDefault(), "%.0f KB", bytes / 1024f);
        return String.format(Locale.getDefault(), "%.1f MB", bytes / (1024f * 1024f));
    }

    public static void deleteFile(String path) {
        if (path == null || path.isEmpty()) return;
        try {
            File f = new File(path);
            if (f.exists()) //noinspection ResultOfMethodCallIgnored
                f.delete();
        } catch (Exception ignored) {
        }
    }

    // ------------------------------------------------------------------
    // View / share
    // ------------------------------------------------------------------
    private static Uri uriFor(Context ctx, File f) {
        return FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".fileprovider", f);
    }

    public static void open(Context ctx, String path, String mime) {
        File f = new File(path);
        if (!f.exists()) {
            Toast.makeText(ctx, "File not found", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(uriFor(ctx, f), mime != null ? mime : mimeForName(path));
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            ctx.startActivity(Intent.createChooser(i, "Open with"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(ctx, "No app found to open this file", Toast.LENGTH_SHORT).show();
        }
    }

    public static void share(Context ctx, String path, String mime) {
        File f = new File(path);
        if (!f.exists()) {
            Toast.makeText(ctx, "File not found", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType(mime != null ? mime : mimeForName(path));
        i.putExtra(Intent.EXTRA_STREAM, uriFor(ctx, f));
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        ctx.startActivity(Intent.createChooser(i, "Share"));
    }

    // ------------------------------------------------------------------
    // Download
    // ------------------------------------------------------------------

    /** Safe file name for Downloads, e.g. "Samsung TV - Bill.jpg". */
    public static String downloadName(String baseName, String sourcePath, String originalName) {
        String ext = extensionOf(originalName);
        if (ext.isEmpty()) ext = extensionOf(sourcePath);
        String clean = baseName.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (clean.isEmpty()) clean = "document";
        if (!ext.isEmpty() && !clean.toLowerCase(Locale.ROOT).endsWith("." + ext)) clean += "." + ext;
        return clean;
    }

    /**
     * Saves a copy into the public Downloads/WarrantyVault folder (Android 10+).
     * Returns false on older Android versions: the caller should then use the system "Save as" picker
     * and {@link #copyTo(Context, String, Uri)}.
     */
    public static boolean saveToDownloads(Context ctx, String path, String fileName, String mime) throws IOException {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false;

        ContentResolver cr = ctx.getContentResolver();
        ContentValues v = new ContentValues();
        v.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        v.put(MediaStore.MediaColumns.MIME_TYPE, mime != null ? mime : mimeForName(fileName));
        v.put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/WarrantyVault");
        Uri uri = cr.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
        if (uri == null) throw new IOException("Could not create file in Downloads");

        try (InputStream in = new FileInputStream(path); OutputStream out = cr.openOutputStream(uri)) {
            if (out == null) throw new IOException("Could not open Downloads file");
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        } catch (IOException e) {
            cr.delete(uri, null, null);
            throw e;
        }
        return true;
    }

    /** Copies a stored file into a user-chosen destination (system "Save as" picker result). */
    public static void copyTo(Context ctx, String path, Uri dest) throws IOException {
        try (InputStream in = new FileInputStream(path);
             OutputStream out = ctx.getContentResolver().openOutputStream(dest)) {
            if (out == null) throw new IOException("Could not open destination");
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
    }
}
