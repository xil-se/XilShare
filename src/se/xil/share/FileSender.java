package se.xil.share;

import java.io.File;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import se.xil.share.CustomMultiPartEntity.ProgressListener;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public class FileSender extends AsyncTask<FileSenderAttributes, Integer, Integer> {
	private ProgressDialog pd;
	private long totalSize;
	private Activity activity;
	private ProgressDialogUpdater progressDialogUpdater;
	private String url = "";
	
	public FileSender(Activity activity) {
		super();
		this.activity = activity;
		progressDialogUpdater = new ProgressDialogUpdater();
	}

	@Override
	protected void onPreExecute() {
		pd = new ProgressDialog(activity);
		pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		pd.setMessage("Uploading Pictures...");
		pd.setCancelable(false);
		pd.show();
	}

	@Override
	protected Integer doInBackground(FileSenderAttributes... arg0) {
		if (arg0.length == 1) {
			for (String path : arg0[0].paths) {
				doUpload(path, arg0[0].album);
			}
		}
		return 0;
	}
	
	protected void updateProgressDialog(final String path) {
		progressDialogUpdater.path = path;
		activity.runOnUiThread(progressDialogUpdater);
	}

	protected void doUpload(final String path, final String album) {
		HttpClient httpClient = new DefaultHttpClient();
		HttpContext httpContext = new BasicHttpContext();
		HttpPost httpPost = new HttpPost("your url");

		updateProgressDialog(path);

		try {
			CustomMultiPartEntity multipartContent = new CustomMultiPartEntity(
					new ProgressListener() {
						@Override
						public void transferred(long num) {
							publishProgress((int) ((num / (float) totalSize) * 100));
						}
					});

			// We use FileBody to transfer an image
			multipartContent.addPart("secret", new StringBody("supersecret!!"));
			multipartContent.addPart("userfile", new FileBody(new File(path)));
			totalSize = multipartContent.getContentLength();

			// Send it
			httpPost.setEntity(multipartContent);
			HttpResponse r = httpClient.execute(httpPost, httpContext);

			Log.e("ASDF", "Response status: " + r.getStatusLine());

			this.url = IOHelper.readInputStreamAsString(r.getEntity().getContent());
			Log.e("ASDF", "Response: " + this.url);
		}

		catch (Exception e) {
			System.out.println(e);
		}
	}

	@Override
	protected void onProgressUpdate(Integer... progress) {
		pd.setProgress((int) (progress[0]));
	}

	@Override
	protected void onPostExecute(Integer whatever) {
		ClipboardManager myClipboard;
		myClipboard = (ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);
		ClipData myClip = ClipData.newPlainText("text", this.url);
		myClipboard.setPrimaryClip(myClip);
		
		Log.e("ASDF", "setting clipboard: " + this.url);
		
		pd.dismiss();
		activity.finish();
	}
	
	private class ProgressDialogUpdater implements Runnable {
		public String path;
		
		public void run() {
			pd.setMessage("Uploading Picture: " + path);
		}
	}
}
