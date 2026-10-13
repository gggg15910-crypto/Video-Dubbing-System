package com.mutarjim.ai;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int PICK_VIDEO = 1001;
    private Uri selectedVideo;
    private TextView videoStatus, result;
    private EditText serverUrl;
    private Button start;
    private Spinner source, target;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xFF111827);
        getWindow().setNavigationBarColor(0xFF111827);
        ScrollView scroll = new ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(22), dp(28), dp(22), dp(24));
        page.setBackgroundColor(0xFFF5F7FB);
        scroll.addView(page);

        TextView title = new TextView(this);
        title.setText("🎬 مترجم AI");
        title.setTextSize(29); title.setTextColor(0xFF111827);
        title.setGravity(Gravity.CENTER); title.setTypeface(null, Typeface.BOLD);
        page.addView(title, params());
        TextView sub = new TextView(this);
        sub.setText("ترجمة الفيديوهات ودبلجتها إلى العربية");
        sub.setTextSize(16); sub.setTextColor(0xFF5B6473);
        sub.setGravity(Gravity.CENTER); sub.setPadding(0, dp(8), 0, dp(20));
        page.addView(sub, params());

        label(page, "1. اختر الفيديو");
        Button pick = button("اختيار فيديو من الهاتف"); page.addView(pick, params());
        videoStatus = new TextView(this); videoStatus.setText("لم يتم اختيار فيديو بعد");
        videoStatus.setTextColor(0xFF64748B); videoStatus.setPadding(0, dp(8), 0, dp(12));
        page.addView(videoStatus, params());
        pick.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.setType("video/*"); i.addCategory(Intent.CATEGORY_OPENABLE);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(i, PICK_VIDEO);
        });

        label(page, "2. لغة الفيديو الأصلية");
        source = spinner(new String[]{"اكتشاف تلقائي","الإنجليزية","العربية","الفرنسية","الإسبانية","الإيطالية","الهندية","الصينية","اليابانية"});
        page.addView(source, params());
        label(page, "3. لغة الدبلجة");
        target = spinner(new String[]{"العربية الفصحى","العربية المصرية","الإنجليزية","الفرنسية","الإسبانية","الإيطالية","الهندية","الصينية","اليابانية"});
        page.addView(target, params());

        label(page, "4. عنوان خادم Dubby");
        serverUrl = new EditText(this);
        serverUrl.setSingleLine(true); serverUrl.setHint("http://عنوان-الخادم:8000");
        serverUrl.setTextSize(14); serverUrl.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        serverUrl.setText(getPreferences(MODE_PRIVATE).getString("server_url", ""));
        serverUrl.setPadding(dp(12), dp(10), dp(12), dp(10)); serverUrl.setBackgroundColor(0xFFFFFFFF);
        page.addView(serverUrl, params());
        Button check = button("اختبار الاتصال بالخادم"); page.addView(check, params());
        check.setOnClickListener(v -> checkServer(check));

        TextView note = new TextView(this);
        note.setText("يتطلب التطبيق تشغيل خادم Dubby على جهاز أو خادم متاح عبر الشبكة. يرفع الفيديو إلى الخادم لإنشاء مشروع؛ لا تضع مفاتيح API السرية في التطبيق.");
        note.setTextSize(13); note.setTextColor(0xFF92400E);
        note.setPadding(dp(12), dp(12), dp(12), dp(12)); note.setBackgroundColor(0xFFFFF7ED);
        LinearLayout.LayoutParams np = params(); np.setMargins(0, dp(16), 0, dp(16)); page.addView(note, np);

        start = button("رفع الفيديو وإنشاء مشروع"); page.addView(start, params());
        Button open = button("فتح استوديو Dubby في المتصفح"); page.addView(open, params());
        result = new TextView(this); result.setTextColor(0xFF334155);
        result.setTextSize(14); result.setPadding(0, dp(12), 0, 0); page.addView(result, params());

        start.setOnClickListener(v -> uploadAndCreateProject());
        open.setOnClickListener(v -> {
            String base = normalizedBase();
            if (base == null) { result.setText("أدخل عنوان الخادم أولاً."); return; }
            getPreferences(MODE_PRIVATE).edit().putString("server_url", base).apply();
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(base))); }
            catch (Exception e) { result.setText("تعذر فتح المتصفح: " + e.getMessage()); }
        });
        setContentView(scroll);
    }

    private void uploadAndCreateProject() {
        if (selectedVideo == null) { result.setText("يرجى اختيار فيديو أولاً."); return; }
        final String base = normalizedBase();
        if (base == null || !(base.startsWith("http://") || base.startsWith("https://"))) {
            result.setText("أدخل عنوان خادم صحيحاً يبدأ بـ http:// أو https://"); return;
        }
        getPreferences(MODE_PRIVATE).edit().putString("server_url", base).apply();
        final String src = new String[]{"auto","en","ar","fr","es","it","hi","zh","ja"}[source.getSelectedItemPosition()];
        final String dst = new String[]{"arb","arz","en","fr","es","it","hi","zh","ja"}[target.getSelectedItemPosition()];
        final Uri video = selectedVideo;
        start.setEnabled(false); start.setText("جارٍ رفع الفيديو...");
        result.setText("جارٍ الاتصال بالخادم ورفع الفيديو. قد يستغرق ذلك وقتاً حسب حجم الملف والإنترنت.");
        worker.execute(() -> {
            HttpURLConnection conn = null;
            try {
                String boundary = "----MutarjimAI" + UUID.randomUUID().toString().replace("-", "");
                conn = (HttpURLConnection) new URL(base + "/api/projects/upload").openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(20000); conn.setReadTimeout(120000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                try (OutputStream out = new BufferedOutputStream(conn.getOutputStream())) {
                    writeTextPart(out, boundary, "source_language", src);
                    writeTextPart(out, boundary, "target", dst);
                    String name = fileName(video);
                    String mime = getContentResolver().getType(video);
                    if (mime == null || !mime.startsWith("video/")) mime = "video/mp4";
                    writeAscii(out, "--" + boundary + "\r\n");
                    writeAscii(out, "Content-Disposition: form-data; name=\"file\"; filename=\"" + safeFilename(name) + "\"\r\n");
                    writeAscii(out, "Content-Type: " + mime + "\r\n\r\n");
                    try (InputStream in = new BufferedInputStream(getContentResolver().openInputStream(video))) {
                        if (in == null) throw new IOException("تعذر قراءة ملف الفيديو");
                        byte[] buffer = new byte[64 * 1024]; int n;
                        while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
                    }
                    writeAscii(out, "\r\n--" + boundary + "--\r\n");
                }
                int code = conn.getResponseCode();
                InputStream response = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
                String body = readAll(response);
                if (code < 200 || code >= 300) throw new IOException("الخادم أعاد HTTP " + code + ": " + body);
                JSONObject project = new JSONObject(body);
                String id = project.optString("id", "");
                String projectName = project.optString("name", "مشروع جديد");
                main.post(() -> {
                    result.setText("تم رفع الفيديو وإنشاء المشروع بنجاح.\n" + projectName + (id.isEmpty() ? "" : "\nرقم المشروع: " + id) +
                        "\nالخطوة التالية: افتح استوديو Dubby من الزر أعلاه لإكمال مراحل التفريغ والترجمة والصوت والتصدير.");
                    start.setText("تم إنشاء المشروع");
                });
            } catch (Exception e) {
                String msg = e.getMessage() == null ? e.toString() : e.getMessage();
                main.post(() -> {
                    result.setText("تعذر إنشاء المشروع. تحقق من عنوان الخادم، واتصال الشبكة، وتشغيل Dubby.\nالتفاصيل: " + msg);
                    start.setText("إعادة المحاولة");
                    start.setEnabled(true);
                });
            } finally { if (conn != null) conn.disconnect(); }
        });
    }

    private void checkServer(Button check) {
        final String base = normalizedBase();
        if (base == null || !(base.startsWith("http://") || base.startsWith("https://"))) {
            result.setText("أدخل عنوان خادم صحيحاً يبدأ بـ http:// أو https://");
            return;
        }
        getPreferences(MODE_PRIVATE).edit().putString("server_url", base).apply();
        check.setEnabled(false);
        check.setText("جارٍ اختبار الاتصال...");
        result.setText("جارٍ التحقق من خادم Dubby...");
        worker.execute(() -> {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(base + "/api/system").openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);
                int code = conn.getResponseCode();
                InputStream response = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
                String body = readAll(response);
                if (code < 200 || code >= 300) throw new IOException("HTTP " + code + ": " + body);
                main.post(() -> result.setText("نجح الاتصال بخادم Dubby. يمكنك الآن اختيار الفيديو ورفعه."));
            } catch (Exception e) {
                String msg = e.getMessage() == null ? e.toString() : e.getMessage();
                main.post(() -> result.setText("فشل الاتصال. تأكد أن الخادم يعمل وأن العنوان متاح من الهاتف. التفاصيل: " + msg));
            } finally {
                if (conn != null) conn.disconnect();
                main.post(() -> { check.setEnabled(true); check.setText("اختبار الاتصال بالخادم"); });
            }
        });
    }

    private String normalizedBase() {
        if (serverUrl == null) return null;
        String s = serverUrl.getText().toString().trim();
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s.isEmpty() ? null : s;
    }
    private void writeTextPart(OutputStream out, String boundary, String key, String value) throws IOException {
        writeAscii(out, "--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + key + "\"\r\n\r\n" + value + "\r\n");
    }
    private void writeAscii(OutputStream out, String s) throws IOException { out.write(s.getBytes("UTF-8")); }
    private String readAll(InputStream in) throws IOException {
        if (in == null) return "";
        try (InputStream input = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] b = new byte[8192]; int n;
            while ((n = input.read(b)) != -1) out.write(b, 0, n);
            return new String(out.toByteArray(), "UTF-8");
        }
    }
    private String fileName(Uri uri) {
        String last = uri.getLastPathSegment();
        if (last == null || last.trim().isEmpty()) return "video.mp4";
        int slash = last.lastIndexOf('/');
        return slash >= 0 ? last.substring(slash + 1) : last;
    }
    private String safeFilename(String s) { return s.replace("\"", "_").replace("\r", "_").replace("\n", "_"); }

    private void label(LinearLayout p, String t) {
        TextView v = new TextView(this); v.setText(t); v.setTextSize(16);
        v.setTextColor(0xFF1E293B); v.setTypeface(null, Typeface.BOLD);
        v.setPadding(0, dp(12), 0, dp(8)); p.addView(v, params());
    }
    private Button button(String t) {
        Button b = new Button(this); b.setText(t); b.setAllCaps(false); b.setTextSize(16);
        b.setTextColor(0xFFFFFFFF);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF4F46E5));
        LinearLayout.LayoutParams p = params(); p.setMargins(0, dp(4), 0, dp(6));
        b.setLayoutParams(p); return b;
    }
    private Spinner spinner(String[] items) {
        Spinner s = new Spinner(this);
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); s.setAdapter(a); return s;
    }
    private LinearLayout.LayoutParams params() { return new LinearLayout.LayoutParams(-1, -2); }
    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_VIDEO && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedVideo = data.getData();
            try { getContentResolver().takePersistableUriPermission(selectedVideo, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
            catch (SecurityException ignored) {}
            videoStatus.setText("تم اختيار الفيديو: " + fileName(selectedVideo));
            result.setText("");
            start.setText("رفع الفيديو وإنشاء مشروع");
            start.setEnabled(true);
        }
    }
    @Override protected void onDestroy() { worker.shutdownNow(); super.onDestroy(); }
}
