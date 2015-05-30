package se.xil.share;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

public class MediaStoreHelper {
	public static String getRealPathFromURI(final Activity activity, final Uri contentUri) {
		String[] proj = { MediaStore.Images.Media.DATA };
		Cursor cursor = activity.managedQuery(contentUri, proj, null, null, null);
		int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		cursor.moveToFirst();
		return cursor.getString(column_index);
	}
}
