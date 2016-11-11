package m.groovyrunner;

import android.app.Activity;
import android.os.Bundle;

import java.lang.reflect.Method;

public class MainActivity extends Activity {

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			Grooroo.loadFromAssets(this, "");
			Class<?> Test = Class.forName("Main");
			Method mth = Test.getMethod("main", Object.class, Object.class);
			mth.invoke(null, this, "Hello Grooroo!");
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

}
