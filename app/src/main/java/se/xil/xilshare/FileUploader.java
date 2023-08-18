package se.xil.xilshare;

/**
 * Created by Konrad on 2017-07-10.
 */

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

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
    private static final String TAG = "XilShare";

    private static String secret = "";
    private static Vector<Call> calls = new Vector<Call>();

    public static void setSecret(String _secret) {
        secret = _secret;
    }

    public enum ContentType {
        Filename,
        ByteArray,
    }

    ;

    public static class Content {
        public ContentType type;
        public String filename;
        public byte[] bytes;

        public String toString() {
            return type.toString() + ": " + filename;
        }
    }

    public static void abort() {
        @SuppressWarnings("unchecked") Vector<Call> callsCopy = (Vector<Call>) calls.clone();
        for (Call call : callsCopy) {
            call.cancel();
            calls.remove(call);
        }
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

                this.listener.update(0, contentLength(), false);
                while ((read = source.read(sink.getBuffer(), SEGMENT_SIZE)) != -1) {
                    total += read;
                    sink.flush();
                    this.listener.update(total, contentLength(), false);
                }
                this.listener.update(total, contentLength(), true);
            } finally {
                Util.closeQuietly(source);
            }
        }
    }

    public interface ProgressListener {
        void update(long bytesRead, long contentLength, boolean done);

        void onResponse(Response response);
    }

    public static void upload(Content content, String url, final ProgressListener progressListener) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "Uploading " + content);
        }

        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);

        if (content.type == ContentType.ByteArray) {
            final MediaType mediaType = MediaType.parse(content.filename);
            builder.addFormDataPart("userfile", "placeholder.jpg", RequestBody.create(content.bytes, mediaType));
            // TODO: Add progress crap here as well
        } else if (content.type == ContentType.Filename) {
            final File file = new File(content.filename);
            final MediaType mediaType = MediaType.parse(content.filename);
            builder.addFormDataPart("userfile", file.getName(), RequestBody.create(file, mediaType));
            builder.addPart(new CountingFileRequestBody(file, "", progressListener));
        }
        builder.addFormDataPart("secret", secret);


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

        calls.add(call);
    }
}
