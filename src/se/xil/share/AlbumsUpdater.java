package se.xil.share;

import java.util.Arrays;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

public class AlbumsUpdater extends AsyncTask<Object, Integer, String[]>
		implements OnItemSelectedListener {
	private Spinner spinner;
	private SpinnerAdapter adapter;
	private Context context;
	private String selectedAlbum;
	private String[] albums;

	public AlbumsUpdater(Context context) {
		this.context = context;
	}

	public void setSpinner(Spinner spinner) {
		this.spinner = spinner;

		spinner.setOnItemSelectedListener(this);

	}

	protected void updateAdapter(String[] items) {
		ArrayAdapter<?> aa = new ArrayAdapter<Object>(context,
				android.R.layout.simple_spinner_item, items);

		aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(aa);

		albums = items;
	}

	public void update() {
		execute(adapter);
	}

	public String getSelectedAlbum() {
		if (selectedAlbum == null) {
			return "incoming";
		} else {
			return selectedAlbum;
		}
	}

	@Override
	protected String[] doInBackground(Object... params) {
		HttpClient httpClient = new DefaultHttpClient();
		HttpContext httpContext = new BasicHttpContext();
		HttpGet httpGet = new HttpGet("stuff");
		HttpResponse r;
		String[] albums = null;

		try {
			r = httpClient.execute(httpGet, httpContext);

			Log.e("ASDF", "Response status: " + r.getStatusLine());

			String response = IOHelper.readInputStreamAsString(r.getEntity()
					.getContent());

			Log.e("ASDF", "Response: " + response);

			albums = response.split("\n");
			Arrays.sort(albums);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return albums;
	}

	@Override
	protected void onPostExecute(final String[] albums) {
		updateAdapter(albums);
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {
		selectedAlbum = albums[position];
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
	}

}
