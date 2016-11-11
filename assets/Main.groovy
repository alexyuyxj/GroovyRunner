class Main {
    static main(def context, def msg) {
        def test = new Test(context, msg)
        test.onTest()
    }

}
