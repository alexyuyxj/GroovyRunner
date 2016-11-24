import android.widget.Toast

class Test {
	def start(def context) {
		Toast.makeText(context, getClass().getName(), Toast.LENGTH_SHORT).show()
		new Thread() {
			public void run() {
				for (int i = 0; i < 100; i++) {
					println("============== " + i);
					Thread.sleep(10);
				}
			}
		}.start()
		new Thread() {
			public void run() {
				for (int i = 0; i < 100; i++) {
					System.err.println("============== " + i);
					Thread.sleep(10);
				}
			}
		}.start()
	}
}