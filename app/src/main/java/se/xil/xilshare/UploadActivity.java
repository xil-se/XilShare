package se.xil.xilshare;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import okhttp3.Response;

/**
 * Created by Konrad on 2017-07-10.
 */

public class UploadActivity extends AppCompatActivity {
    private static final String TAG = "XilShare";
    private ProgressBar uploadProgress;
    private Button uploadAbort;
    private FileUploader.Content[] uploadContent;
    private String uploadUrl;
    private String uploadSecret;
    private int uploadCount;
    private int remainingUploads;
    private TextView uploadText;
    private TextView uploadPercentage;
    private boolean hasUploadStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        setupViews();
        setupClickListener();
        loadSettings();
        getFilenamesFromIntent();
        uploadFiles();
    }

    private void updateProgress(final long bytesWritten, final long contentLength) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (!hasUploadStarted) {
                    hasUploadStarted = true;
                    uploadProgress.setIndeterminate(false);
                }

                final int progress = (int) ((bytesWritten * 1.0 / contentLength) * 1000);

                uploadProgress.setProgress(progress);

                float percent = (float) progress / 10.0f;
                uploadPercentage.setText(String.format("%.1f%%", percent));
                uploadText.setText(String.format("%,d/%,d kB", bytesWritten / 1024, contentLength / 1024));
            }
        });
    }

    private void uploadFiles() {
        if (uploadContent == null) {
            return;
        }

        this.uploadCount = uploadContent.length;
        this.remainingUploads = uploadContent.length;
        final Vector<String> uploadUrls = new Vector<>();
        final Map<String, Long> fileSizesMap = new HashMap<String, Long>();
        final Map<String, Long> bytesWrittenMap = new HashMap<String, Long>();

        for (final FileUploader.Content content : uploadContent) {
            if (content == null) {
                Log.e(TAG, "uploadFiles: Content is null");
            } else {
                if (BuildConfig.DEBUG) {
                    Log.i(TAG, "Uploading " + content.filename);
                    Log.i(TAG, "Uploading " + uploadCount);
                }
                FileUploader.upload(content, uploadUrl, new FileUploader.ProgressListener() {
                    @Override
                    public void update(long bytesWritten, long contentLength, boolean done) {
                        if (bytesWritten == 0) {
                            fileSizesMap.put(content.filename, contentLength);
                        }

                        bytesWrittenMap.put(content.filename, bytesWritten);

                        long fileSizesSum = 0;
                        for (Map.Entry<String, Long> entry : fileSizesMap.entrySet()) {
                            fileSizesSum += entry.getValue();
                        }

                        long bytesWrittenSum = 0;
                        for (Map.Entry<String, Long> entry : bytesWrittenMap.entrySet()) {
                            bytesWrittenSum += entry.getValue();
                        }

                        updateProgress(bytesWrittenSum, fileSizesSum);
                    }

                    @Override
                    public void onResponse(Response response) {
                        remainingUploads--;
                        final String r;
                        try {
                            r = response.body().string();
                            if (BuildConfig.DEBUG) {
                                Log.i(TAG, r);
                            }
                            uploadUrls.add(r);
                            if (remainingUploads == 0) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        String urls = TextUtils.join(" ", uploadUrls.toArray(new String[uploadUrls.size()]));
                                        setClipboard(urls);
                                        Utils.showToast(UploadActivity.this, "Upload finished");
                                        finish();
                                    }
                                });
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            Utils.showToast(UploadActivity.this, "Error, go home and cry");
                        }
                    }

                });
            }
        }
    }

    private void setClipboard(String data) {
        ClipboardManager myClipboard;
        myClipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData myClip = ClipData.newPlainText("text", data);
        myClipboard.setPrimaryClip(myClip);

        if (BuildConfig.DEBUG) {
            Log.i(TAG, "setting clipboard: " + data);
        }
    }

    private void loadSettings() {
        SharedPreferences settings = getSharedPreferences(Settings.PREFS_NAME, 0);
        this.uploadUrl = settings.getString(Settings.SETTINGS_URL, "");
        this.uploadSecret = settings.getString(Settings.SETTINGS_SECRET, "");

        FileUploader.setSecret(this.uploadSecret);
    }

    private void setupViews() {
        this.uploadProgress = (ProgressBar) findViewById(R.id.uploadProgress);
        this.uploadText = (TextView) findViewById(R.id.uploadText);
        this.uploadPercentage = (TextView) findViewById(R.id.uploadPercentage);
        this.uploadAbort = (Button) findViewById(R.id.uploadAbort);
    }

    private void setupClickListener() {
        this.uploadAbort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                abortUpload();
            }
        });
    }

    private void abortUpload() {
        FileUploader.abort();
        Utils.showToast(this, "Upload canceled");
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "Upload canceled");
        }
        finish();
    }

    protected void getFilenamesFromIntent() {
        final Intent intent = getIntent();
        final String action = intent.getAction();

        if (Intent.ACTION_SEND.equals(action)) {
            Bundle bundle = intent.getExtras();
            Set<String> ks = bundle.keySet();

            if (BuildConfig.DEBUG) {
                for (String k : ks) {
                    Log.i(TAG, String.format("[%s: %s]", k, bundle.get(k)));
                }
            }

            if (ks.contains(Intent.EXTRA_STREAM)) {
                Uri uri = (Uri) bundle.get(Intent.EXTRA_STREAM);

                if (uri == null) {
                    if (BuildConfig.DEBUG) {
                        Log.e(TAG, "Bundle EXTRA_STREAM uri is null");
                    }
                } else {
                    if (BuildConfig.DEBUG) {
                        Log.i(TAG, "Wants to share: " + uri.getPath());
                    }
                    uploadContent = new FileUploader.Content[]{
                            MediaStoreHelper.getContentFromURI(this, uri)
                    };
                    if (BuildConfig.DEBUG) {
                        for (FileUploader.Content f : uploadContent) {
                            Log.i(TAG, "Path: " + f);
                        }
                    }
                }
            } else if (ks.contains(Intent.EXTRA_TEXT)) {
                // Sharing a text entry, this can happens when you share a google url
                String extraText = bundle.getString(Intent.EXTRA_TEXT);
                if (BuildConfig.DEBUG) {
                    Log.i(TAG, String.format("Wants to share text: [%s]", extraText));
                }
                // TODO: Upload the text and put the url in the clipboard
                // For now, let's just set the clipboard to the text...
                setClipboard(extraText);
                Utils.showToast(this, "Shared text now in clipboard");
                finish();
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            ArrayList<Uri> uris = intent
                    .getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            uploadContent = new FileUploader.Content[uris.size()];

            for (int i = 0; i < uris.size(); i++) {
                if (uris.get(i) == null) {
                    if (BuildConfig.DEBUG) {
                        Log.e(TAG, "Wants to share multiple: got a null entry");
                    }
                } else {
                    if (BuildConfig.DEBUG) {
                        Log.i(TAG, "Wants to share multiple: "
                                + uris.get(i).getPath());
                    }
                    uploadContent[i] = MediaStoreHelper.getContentFromURI(this,
                            uris.get(i));
                }
            }
        }

    }

}
