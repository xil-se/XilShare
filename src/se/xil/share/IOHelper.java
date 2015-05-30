package se.xil.share;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;

public class IOHelper {

	public static String readInputStreamAsString(InputStream in)
			throws IOException {

		BufferedInputStream bis = new BufferedInputStream(in);
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		int result = bis.read();
		while (result != -1) {
			byte b = (byte) result;
			buf.write(b);
			result = bis.read();
		}
		return buf.toString();
	}
}
