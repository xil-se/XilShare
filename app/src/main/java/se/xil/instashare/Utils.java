package se.xil.instashare;

import android.app.Activity;
import android.widget.Toast;

/**
 * Created by Konrad on 2017-07-12.
 */

public class Utils {
    public static void showToast(final Activity activity, final String message) {
        activity.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
