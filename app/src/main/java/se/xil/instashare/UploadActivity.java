package se.xil.instashare;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

import okhttp3.Response;

/**
 * Created by Konrad on 2017-07-10.
 */

public class UploadActivity extends AppCompatActivity {
    private static final String TAG = "InstaShare";
    private ProgressBar uploadProgress;
    private Button uploadAbort;
    private FileUploader.Content[] uploadContent;
    private String uploadUrl;
    private String uploadSecret;
    private int uploadCount;
    private int remainingUploads;

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

    private void uploadFiles() {
        this.uploadCount = uploadContent.length;
        this.remainingUploads = uploadContent.length;
        final Vector<String> uploadUrls = new Vector<>();

        for (FileUploader.Content content : uploadContent) {
            if (content == null) {
                Log.d(TAG, "Content is null");
            } else {
                Log.d(TAG, "Uploading " + content.filename);
                Log.d(TAG, "Uploading " + uploadCount);
                FileUploader.upload(content, uploadUrl, new FileUploader.ProgressListener() {
                    @Override
                    public void update(long bytesWritten, long contentLength, boolean done) {
                        final int progress = (int) ((bytesWritten * 1.0 / contentLength) * 1000);
                        Log.e(TAG, "Progress: " + progress);
                        if (uploadCount == 1) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    uploadProgress.setProgress(progress);
                                }
                            });
                        }
                    }

                    @Override
                    public void onResponse(Response response) {
                        remainingUploads--;
                        uploadProgress.setProgress(1000 - 1000 * remainingUploads / uploadCount);
                        final String r;
                        try {
                            r = response.body().string();
                            Log.e(TAG, r);
                            uploadUrls.add(r);
                            if (remainingUploads == 0) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        String urls = TextUtils.join(" ", uploadUrls.toArray(new String[uploadUrls.size()]));
                                        setClipboard(urls);
                                        finish();
                                    }
                                });
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
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

        Log.e(TAG, "setting clipboard: " + data);
    }

    private void loadSettings() {
        SharedPreferences settings = getSharedPreferences(Settings.PREFS_NAME, 0);
        this.uploadUrl = settings.getString(Settings.SETTINGS_URL, "");
        this.uploadSecret = settings.getString(Settings.SETTINGS_SECRET, "");

        FileUploader.setSecret(this.uploadSecret);
    }

    private void setupViews() {
        this.uploadProgress = (ProgressBar) findViewById(R.id.uploadProgress);
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
        Log.d(TAG, "Upload canceled");
    }

    protected void getFilenamesFromIntent() {
        final Intent intent = getIntent();
        final String action = intent.getAction();

        if (Intent.ACTION_SEND.equals(action)) {
            Bundle bundle = intent.getExtras();
            Uri uri = (Uri) bundle.get(Intent.EXTRA_STREAM);

            if (uri == null) {
                Log.e(TAG, "Wants to share: NULL");
            } else {
                Log.e(TAG, "Wants to share: " + uri.getPath());
                uploadContent = new FileUploader.Content[]{
                        MediaStoreHelper.getContentFromURI(this, uri)
                };
                for (FileUploader.Content f : uploadContent) {
                    Log.e(TAG, "Path: " + f);
                }
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            ArrayList<Uri> uris = intent
                    .getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            uploadContent = new FileUploader.Content[uris.size()];

            for (int i = 0; i < uris.size(); i++) {
                if (uris.get(i) == null) {
                    Log.e(TAG, "Wants to share multiple: NULL");
                } else {
                    Log.e(TAG, "Wants to share multiple: "
                            + uris.get(i).getPath());
                    uploadContent[i] = MediaStoreHelper.getContentFromURI(this,
                            uris.get(i));
                }
            }
        }

    }

}
