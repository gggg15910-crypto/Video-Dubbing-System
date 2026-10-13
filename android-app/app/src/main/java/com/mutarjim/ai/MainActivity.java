package com.mutarjim.ai;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.view.Gravity;
import android.widget.*;
import android.graphics.Typeface;

public class MainActivity extends Activity {
    private static final int PICK_VIDEO = 1001;
    private Uri selectedVideo;
    private TextView videoStatus, result;
    private EditText serverUrl;

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
            startActivityForResult(i, PICK_VIDEO);
        });

        label(page, "2. لغة الفيديو الأصلية");
        Spinner source = spinner(new String[]{"اكتشاف تلقائي","الإنجليزية","الفرنسية","الإسبانية","التركية","الألمانية","اليابانية"});
        page.addView(source, params());
        label(page, "3. لغة الدبلجة");
        Spinner target = spinner(new String[]{"العربية الفصحى","العربية المصرية","الإنجليزية","الفرنسية","الإسبانية","التركية"});
        page.addView(target, params());

        label(page, "4. عنوان خادم الدبلجة (API)");
        serverUrl = new EditText(this);
        serverUrl.setSingleLine(true); serverUrl.setHint("https://your-server.example");
        serverUrl.setTextSize(14); serverUrl.setInputType( android.text.InputType.TYPE_TEXT_VARIATION_URI );
        serverUrl.setPadding(dp(12), dp(10), dp(12), dp(10)); serverUrl.setBackgroundColor(0xFFFFFFFF);
        page.addView(serverUrl, params());

        TextView note = new TextView(this);
        note.setText("هذا إصدار أولي للواجهة. الترجمة والدبلجة الفعلية تتطلبان ربط خادم يعالج الصوت والترجمة وتوليد الكلام ثم يعيد ملف MP4.");
        note.setTextSize(13); note.setTextColor(0xFF92400E);
        note.setPadding(dp(12), dp(12), dp(12), dp(12)); note.setBackgroundColor(0xFFFFF7ED);
        LinearLayout.LayoutParams np = params(); np.setMargins(0, dp(16), 0, dp(16)); page.addView(note, np);

        Button start = button("بدء الترجمة والدبلجة"); page.addView(start, params());
        result = new TextView(this); result.setTextColor(0xFF334155);
        result.setPadding(0, dp(12), 0, 0); page.addView(result, params());
        start.setOnClickListener(v -> {
            if (selectedVideo == null) result.setText("يرجى اختيار فيديو أولاً.");
            else if (serverUrl.getText().toString().trim().isEmpty())
                result.setText("تم اختيار الفيديو. يلزم إعداد خادم الدبلجة وربطه قبل بدء المعالجة.");
            else result.setText("تم اختيار الفيديو. لم تُفعّل المعالجة بعد؛ يلزم إكمال تكامل API مع الخادم.");
        });
        setContentView(scroll);
    }

    private void label(LinearLayout p, String t) {
        TextView v = new TextView(this); v.setText(t); v.setTextSize(16);
        v.setTextColor(0xFF1E293B); v.setTypeface(null, Typeface.BOLD);
        v.setPadding(0, dp(12), 0, dp(8)); p.addView(v, params());
    }
    private Button button(String t) {
        Button b = new Button(this); b.setText(t); b.setAllCaps(false); b.setTextSize(16);
        b.setTextColor(0xFFFFFFFF);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF4F46E5));
        return b;
    }
    private Spinner spinner(String[] items) {
        Spinner s = new Spinner(this);
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); s.setAdapter(a); return s;
    }
    private LinearLayout.LayoutParams params() {
        return new LinearLayout.LayoutParams(-1, -2);
    }
    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_VIDEO && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedVideo = data.getData();
            try { getContentResolver().takePersistableUriPermission(selectedVideo, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
            catch (SecurityException ignored) {}
            videoStatus.setText("تم اختيار الفيديو بنجاح");
            result.setText("");
        }
    }
}
