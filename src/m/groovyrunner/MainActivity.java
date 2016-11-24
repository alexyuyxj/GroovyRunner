package m.groovyrunner;

import android.app.Activity;
import android.os.Bundle;

import java.lang.reflect.Method;

public class MainActivity extends Activity {

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			Grooroo.SourceSet sourceSet = new Grooroo.SourceSet();
			sourceSet.appendAssets();
			Grooroo.load(this, sourceSet);

			Class<?> Main = Class.forName("Main");
			Method mth = Main.getMethod("print", Object.class);
			mth.invoke(null, this);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

}
