package com.agile.punchhelp;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.agile.punchhelp.PluginWebView.MY_PRE_NAME;

public class MainActivity extends AppCompatActivity {

    private Button mSubmitForm;
    private Button mWordStart;
    private Button mWordEnd;
    private PluginWebView mWebView;
    private Button mShareImage;
    public static final String SUBMIT_FORM_URL = "https://gitlab.hydsoft.net:8845/hydsoft/daily-research/index.html";//表单提交链接

    // 截屏图片存放路径
    public static final String SCREEN_SHOTS_DIR = Environment.getExternalStorageDirectory().getAbsolutePath() + "/PunchHelper/";
    public static final String SCREEN_SHOTS_FILE_NAME = "screenshots";//截图名称
    private SharedPreferences mSharedPref;
    private final static String MY_PRE_WORK_START_KEY = "work_start";//上班
    private final static String MY_PRE_WORK_START_FULL_KEY = "work_start_full";//上班
    private final static String MY_PRE_WORK_END_KEY = "work_end";//下班
    private final static String MY_PRE_WORK_NOTICE_KEY = "work_notice";//带包带卡
    private final static String MY_PRE_WEEKEND_KEY = "weekend";//周末疫情
    private final static String MY_PRE_WEEKEND_FULL_KEY = "weekend_full";//周末疫情
    private static final SimpleDateFormat mDateFormat = new SimpleDateFormat("MM月dd日");
    private static final SimpleDateFormat mTimeFormat = new SimpleDateFormat("HH:mm");
    private Button mWorkNotice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            android.webkit.WebView.enableSlowWholeDocumentDraw();
        }
        mSharedPref = getSharedPreferences(MY_PRE_NAME, Context.MODE_PRIVATE);
        setContentView(R.layout.activity_main);
        mWebView = findViewById(R.id.mWebView);
        mSubmitForm = findViewById(R.id.mSubmitForm);
        mWordStart = findViewById(R.id.mWordStart);
        mWordEnd = findViewById(R.id.mWordEnd);
        mShareImage = findViewById(R.id.mShareImage);
        mWorkNotice = findViewById(R.id.mWorkNoticeButton);

        mWebView.loadUrl(SUBMIT_FORM_URL);

        mWorkNotice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String notice = mSharedPref.getString(MY_PRE_WORK_NOTICE_KEY, "");
                if (TextUtils.isEmpty(notice)) {
                    notice = "无包，带卡，手机1台，无其它电子设备";
                }
                final EditText inputServer = new EditText(MainActivity.this);
                inputServer.setText(notice);
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("带包带卡").setView(inputServer)
                        .setNegativeButton("取消", null);
                builder.setPositiveButton("分享", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String message = inputServer.getText().toString();
                        mSharedPref.edit().putString(MY_PRE_WORK_NOTICE_KEY, message).apply();
                        shareBySys(message, "带包带卡");
                    }
                });
                builder.show();
            }
        });

        mShareImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //分享截图
                Bitmap bitmap = getViewBp(mWebView);
                Uri uri = saveBitmap(bitmap);
                if (uri != null) {
                    /** * 分享图片 */
                    Intent share_intent = new Intent();
                    share_intent.setAction(Intent.ACTION_SEND);//设置分享行为
                    share_intent.setType("image/*");  //设置分享内容的类型
                    share_intent.putExtra(Intent.EXTRA_STREAM, uri);
                    //创建分享的Dialog
                    share_intent = Intent.createChooser(share_intent, "分享截图");
                    MainActivity.this.startActivity(share_intent);
                }
            }
        });

        mSubmitForm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWebView.submit();//提交表单
            }
        });
        mWordStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //上班打卡
                Date dateTime = new Date();
                final String date = mDateFormat.format(dateTime);
                final String time = mTimeFormat.format(dateTime);
                String userName = mWebView.getUserName();
                if (TextUtils.isEmpty(userName)) {
                    userName = "{名字}";
                }
                String workStart = userName + "  " + date + "  正常上班\n" +
                        "\n" +
                        "上班  " + time + "  {交通方式}\n" +
                        "\n" +
                        "身体状况： 正常～已填表";
                String workStartCache = mSharedPref.getString(MY_PRE_WORK_START_KEY, "");
                if (!TextUtils.isEmpty(workStartCache)) {
                    workStartCache = workStartCache.replace("{date}", date).replace("{time}", time);
                    workStart = workStartCache;
                }
                final EditText inputServer = new EditText(MainActivity.this);
                inputServer.setText(workStart);
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("上班打卡").setView(inputServer)
                        .setNegativeButton("取消", null);
                builder.setPositiveButton("分享", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String message = inputServer.getText().toString();
                        String messageCopy = message;
                        int timeIndex = messageCopy.indexOf("上班  ");
                        String subString = messageCopy.substring(timeIndex, timeIndex + 9);
                        messageCopy = messageCopy.replace(subString, "上班  " + time);
                        String cacheMessage = messageCopy.replace(date, "{date}").replace(time, "{time}");
                        mSharedPref.edit().putString(MY_PRE_WORK_START_KEY, cacheMessage).apply();
                        mSharedPref.edit().putString(MY_PRE_WORK_START_FULL_KEY, message).apply();
                        shareBySys(message, "上班打卡");
                    }
                });
                builder.show();

            }
        });
        mWordEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //下班打卡
                Date dateTime = new Date();
                final String date = mDateFormat.format(dateTime);
                final String time = mTimeFormat.format(dateTime);
                String userName = mWebView.getUserName();
                if (TextUtils.isEmpty(userName)) {
                    userName = "{名字}";
                }
                String workEnd = userName + "  " + date + "  正常上班\n" +
                        "\n" +
                        "上班 xx:xx  {交通方式}\n" +
                        "\n";
                String workStartCache = mSharedPref.getString(MY_PRE_WORK_START_FULL_KEY, "");
                String workEndCache = mSharedPref.getString(MY_PRE_WORK_END_KEY, "");
                if (TextUtils.isEmpty(workStartCache)) {
                    Toast.makeText(MainActivity.this, "您还没有上班打卡", Toast.LENGTH_LONG).show();
                } else {
                    int startIndex = workStartCache.indexOf("身体状况");
                    String head = "";
                    if (startIndex > 0) {
                        head = workStartCache.substring(0, startIndex);
                    }

                    workEnd = head;
                }
                if (!TextUtils.isEmpty(workEndCache)) {
                    int endIndex = workEndCache.indexOf("下班");
                    String end = "";
                    if (endIndex > 0) {
                        end = workEndCache.substring(endIndex, workEndCache.length());
                    }
                    workEnd += end;

                } else {
                    workEnd += ("下班  " + time + "  {交通方式}\n" +
                            "\n" +
                            "身体状况： 正常～已填表");
                }
                workEnd = workEnd.replace("{date}", date).replace("{time}", time);

                final EditText inputServer = new EditText(MainActivity.this);
                inputServer.setText(workEnd);
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("下班打卡").setView(inputServer)
                        .setNegativeButton("取消", null);
                builder.setPositiveButton("分享", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String message = inputServer.getText().toString();
                        String cacheMessage = message.replace(date, "{date}").replace(time, "{time}");
                        mSharedPref.edit().putString(MY_PRE_WORK_END_KEY, cacheMessage).apply();
                        shareBySys(message, "下班打卡");
                    }
                });
                builder.show();
            }
        });

        findViewById(R.id.mWeekend).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //休息疫情
                Date dateTime = new Date();
                final String date = mDateFormat.format(dateTime);
                final String time = mTimeFormat.format(dateTime);
                String userName = mWebView.getUserName();
                if (TextUtils.isEmpty(userName)) {
                    userName = "{名字}";
                }
                String weekend = userName + "  " + date + "  正常上班\n" +
                        "\n" +
                        "身体状况： 正常～已填表";
                String weekendCache = mSharedPref.getString(MY_PRE_WEEKEND_KEY, "");
                if (!TextUtils.isEmpty(weekendCache)) {
                    weekendCache = weekendCache.replace("{date}", date);
                    weekend = weekendCache;
                }
                final EditText inputServer = new EditText(MainActivity.this);
                inputServer.setText(weekend);
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("周末打卡").setView(inputServer)
                        .setNegativeButton("取消", null);
                builder.setPositiveButton("分享", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String message = inputServer.getText().toString();
                        String cacheMessage = message.replace(date, "{date}");
                        mSharedPref.edit().putString(MY_PRE_WEEKEND_KEY, cacheMessage).apply();
                        mSharedPref.edit().putString(MY_PRE_WEEKEND_FULL_KEY, message).apply();
                        shareBySys(message, "周末打卡");
                    }
                });
                builder.show();
            }
        });

//        mWebView.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                int bottom = mWebView.computeVerticalScrollRange();
//                mWebView.scrollTo(0, bottom);
//            }
//        }, 2000);
        requestPermission();
    }

    //系统分享文本
    public void shareBySys(String content, String title) {
        Intent share_intent = new Intent();
        share_intent.setAction(Intent.ACTION_SEND);//设置分享行为
        share_intent.setType("text/plain");//设置分享内容的类型
        share_intent.putExtra(Intent.EXTRA_TEXT, content);//添加分享内容
        //创建分享的Dialog
        share_intent = Intent.createChooser(share_intent, title);
        startActivity(share_intent);
    }

    public Bitmap getViewBp(WebView webView) {
        if (null == webView) {
            return null;
        }
        webView.setDrawingCacheEnabled(true);
        webView.buildDrawingCache();//这句话可加可不加，因为getDrawingCache()执行的主体就是buildDrawingCache()
        Bitmap bitmap = Bitmap.createBitmap(webView.getDrawingCache(), 0, 0, webView.getMeasuredWidth(), webView.getMeasuredHeight() - webView.getPaddingBottom());
        webView.setDrawingCacheEnabled(false);
        webView.destroyDrawingCache();
        return bitmap;

    }

    /**
     * 保存指纹图片
     *
     * @param bitmap
     */
    private Uri saveBitmap(Bitmap bitmap) {
        final String fileName = SCREEN_SHOTS_FILE_NAME + "_" + System.currentTimeMillis() + ".jpg";
        File file = new File(SCREEN_SHOTS_DIR, fileName);
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!file.exists()) {
            Toast.makeText(this, "获取截图失败", Toast.LENGTH_LONG).show();
            return null;
        }

        // 把文件插入到系统图库
        try {
            MediaStore.Images.Media.insertImage(this.getContentResolver(),
                    file.getAbsolutePath(), fileName, null);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        // 通知图库更新
//        sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory())));
        Uri uri = FileProvider.getUriForFile(this, "com.agile.punchhelp.fileprovider", file.getAbsoluteFile());
        ;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Intent mediaScanIntent = new Intent(
                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(uri);
            this.sendBroadcast(mediaScanIntent);
        } else {
            sendBroadcast(new Intent(
                    Intent.ACTION_MEDIA_MOUNTED,
                    Uri.parse("file://"
                            + Environment.getExternalStorageDirectory())));
        }
//        Uri pothoUri = FileProvider.getUriForFile(this, "com.agile.punchhelp.fileprovider", file.getAbsoluteFile());
        return uri;

    }

    private void requestPermission() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_CONTACTS)) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.INTERNET},
                        0);

            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.INTERNET},
                        0);
            }
        }
    }
}
