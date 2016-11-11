import android.widget.Toast

class Test implements ITest {
    def context
    def msg

    void onTest() {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    Test(def context, def msg) {
        this.context = context
        this.msg = msg
    }
}
