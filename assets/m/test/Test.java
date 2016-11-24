package m.test;

import android.content.Context;
import android.widget.Toast;

public class Test {
	public void start(Context context) {
		Toast.makeText(context, getClass().getName(), Toast.LENGTH_SHORT).show();
		new Thread() {
			public void run() {
				for (int i = 0; i < 100; i++) {
					System.out.println("============== " + i);
					try {
						Thread.sleep(10);
					} catch (Throwable t) {}
				}
			}
		}.start();
		new Thread() {
			public void run() {
				for (int i = 0; i < 100; i++) {
					System.err.println("============== " + i);
					try {
						Thread.sleep(10);
					} catch (Throwable t) {}
				}
			}
		}.start();
	}
}