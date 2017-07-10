package se.xil.instashare;

/**
 * Created by Konrad on 2017-07-10.
 */

import android.util.Log;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;


public class FileUploader {
    private static final String TAG = "InstaShare";

    private static String secret = "";

    public static void setSecret(String _secret) {
        secret = _secret;
    }

    public static class CountingFileRequestBody extends RequestBody {

        private static final int SEGMENT_SIZE = 2048;

        private final File file;
        private final ProgressListener listener;
        private final String contentType;

        public CountingFileRequestBody(File file, String contentType, ProgressListener listener) {
            this.file = file;
            this.contentType = contentType;
            this.listener = listener;
        }

        @Override
        public long contentLength() {
            return file.length();
        }

        @Override
        public MediaType contentType() {
            return MediaType.parse(contentType);
        }

        @Override
        public void writeTo(BufferedSink sink) throws IOException {
            Source source = null;
            try {
                source = Okio.source(file);
                long total = 0;
                long read;

                while ((read = source.read(sink.buffer(), SEGMENT_SIZE)) != -1) {
                    total += read;
                    sink.flush();
                    this.listener.update(total, contentLength(), false);

                }
            } finally {
                Util.closeQuietly(source);
            }
        }

    }

    public interface ProgressListener {
        void update(long bytesRead, long contentLength, boolean done);

        void onResponse(Response response);
    }

    public static void upload(String filename, String url, final ProgressListener progressListener) {
        Log.e(TAG, "Uploading " + filename);

        final File file = new File(filename);

        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);

        final MediaType mediaType = MediaType.parse(filename);
        builder.addFormDataPart("userfile", file.getName(), RequestBody.create(mediaType, file));
        builder.addFormDataPart("secret", secret);
        builder.addPart(new CountingFileRequestBody(file, "", progressListener));


        RequestBody requestBody = builder.build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();


        OkHttpClient client = new OkHttpClient.Builder().build();

        Call call = client.newCall(request);

        call.enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                progressListener.onResponse(response);
            }

        });

    }
}
