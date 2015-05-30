package se.xil.share;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Spinner;

public class XilShareActivity extends Activity implements OnClickListener {
	private String[] filenames;
	private AlbumsUpdater updater;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.e("ASDF", "Hello");
		setContentView(R.layout.main);

		getFilenamesFromIntent();
//		setupUi();
		share(filenames);
	}

	protected void setupUi() {
		Spinner spinner = (Spinner) findViewById(R.id.albums);
		updater = new AlbumsUpdater(this);
		updater.setSpinner(spinner);
		updater.update();

		Button uploadButton = (Button) findViewById(R.id.upload);
		uploadButton.setOnClickListener(this);

		if (filenames.length == 1)
			uploadButton.setText("Upload 1 image");
		else
			uploadButton.setText("Upload " + filenames.length + " images");
	}

	@Override
	public void onClick(View arg0) {
		share(filenames);
	}

	protected void getFilenamesFromIntent() {
		final Intent intent = getIntent();
		final String action = intent.getAction();

		if (Intent.ACTION_SEND.equals(action)) {
			Bundle bundle = intent.getExtras();
			Uri uri = (Uri) bundle.get(Intent.EXTRA_STREAM);

			if (uri == null) {
				Log.e("ASDF", "Wants to share: NULL");
			} else {
				Log.e("ASDF", "Wants to share: " + uri.getPath());
				filenames = new String[] { MediaStoreHelper.getRealPathFromURI(
						this, uri) };
			}
		} else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
			ArrayList<Uri> uris = intent
					.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
			filenames = new String[uris.size()];

			for (int i = 0; i < uris.size(); i++) {
				if (uris.get(i) == null) {
					Log.e("ASDF", "Wants to share multiple: NULL");
				} else {
					Log.e("ASDF", "Wants to share multiple: "
							+ uris.get(i).getPath());
					filenames[i] = MediaStoreHelper.getRealPathFromURI(this,
							uris.get(i));
				}
			}
		}
	}

	private void share(String[] paths) {
		Log.e("ASDF", "starting FileSender thread");

		FileSender x = new FileSender(this);
		FileSenderAttributes attributes = new FileSenderAttributes(paths,
				""/*updater.getSelectedAlbum()*/);
		x.execute(attributes);

		Log.e("ASDF", "FileSender thread started");
	}
}