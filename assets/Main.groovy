class Main {
    static main(def context, def msg) {
        def manifest = new HashMap()
        manifest << ["package": "m.groovyrunner"]
		manifest << ["android:versionCode": "1"]
		manifest << ["android:versionName": "1.0"]
		manifest << ["uses-sdk": new HashMap()]
		manifest["uses-sdk"]<< ["android:minSdkVersion": "9"]
		manifest["uses-sdk"]<< ["android:targetSdkVersion": "24"]
		manifest << ["application": new HashMap()]
		manifest["application"]<< ["android:debuggable": "true"]
		manifest["application"]<< ["android:label": "GroovyRunner"]
		manifest["application"]<< ["activities": new ArrayList()]
		manifest["application"]["activities"] << (new HashMap())
		manifest["application"]["activities"][0] << ["android:name": ".MainActivity"]
		manifest["application"]["activities"][0] << ["android:configChanges": "keyboardHidden|orientation|screenSize"]
		manifest["application"]["activities"][0] << ["android:screenOrientation": "sensor"]
		manifest["application"]["activities"][0] << ["android:windowSoftInputMode": "adjustPan|stateHidden"]
		manifest["application"]["activities"][0] << ["intent-filters": new ArrayList()]
		manifest["application"]["activities"][0]["intent-filters"] << (new HashMap())
		manifest["application"]["activities"][0]["intent-filters"][0] << ["action": new HashMap()]
		manifest["application"]["activities"][0]["intent-filters"][0]["action"] << ["android:name": "android.intent.action.MAIN"]
		manifest["application"]["activities"][0]["intent-filters"][0] << ["category": new HashMap()]
		manifest["application"]["activities"][0]["intent-filters"][0]["category"] << ["android:name": "android.intent.category.LAUNCHER"]
        println(manifest)

        def test = new Test(context, msg)
        test.onTest()
    }
}
