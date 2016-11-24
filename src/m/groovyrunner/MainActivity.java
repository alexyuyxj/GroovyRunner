package m.groovyrunner;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import java.lang.reflect.Method;

public class MainActivity extends Activity {

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			Grooroo.loadFromAssets(this, "");
			Class<?> Main = getClassLoader().loadClass("Main");
			Method mth = Main.getMethod("main", Context.class);
			mth.invoke(null, this);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

}
