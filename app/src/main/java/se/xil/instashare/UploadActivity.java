package se.xil.instashare;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Response;

/**
 * Created by Konrad on 2017-07-10.
 */

public class UploadActivity extends AppCompatActivity {
    private static final String TAG = "InstaShare";
    private ProgressBar uploadProgress;
    private Button uploadAbort;
    private String[] filenames;
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
        this.uploadCount = filenames.length;
        this.remainingUploads = filenames.length;

        for (String filename : filenames) {
            if (filename == null) {
                Log.d(TAG, "Filename is null");
            } else {
                Log.d(TAG, "Uploading " + filename);
                Log.d(TAG, "Uploading " + uploadCount);
                FileUploader.upload(filename, uploadUrl, new FileUploader.ProgressListener() {
                    @Override
                    public void update(long bytesWritten, long contentLength, boolean done) {
                        final int progress = (int) ((bytesWritten * 1.0 / contentLength) * 1000);
                        Log.e(TAG, "Progress: " + progress);
                        if (uploadCount == 1) {
                            uploadProgress.setProgress(progress);
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
                            if (remainingUploads == 0) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        setClipboard(r);
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
        // TODO
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
                filenames = new String[]{MediaStoreHelper.getRealPathFromURI(
                        this, uri)};
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            ArrayList<Uri> uris = intent
                    .getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            filenames = new String[uris.size()];

            for (int i = 0; i < uris.size(); i++) {
                if (uris.get(i) == null) {
                    Log.e(TAG, "Wants to share multiple: NULL");
                } else {
                    Log.e(TAG, "Wants to share multiple: "
                            + uris.get(i).getPath());
                    filenames[i] = MediaStoreHelper.getRealPathFromURI(this,
                            uris.get(i));
                }
            }
        }

    }


}
