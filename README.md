android上运行时编译并执行groovy脚本的工具，灵感源自于项目[grooidshell-example][1]。不过已经被大面积精简，而且做了对7.0系统的兼容。

已知的缺点是，只能import编译前已经存在的类，而不能import groovy脚本中的类。所以每次加载编译的groovy脚本都必须在同一个包下。


  [1]: https://github.com/melix/grooidshell-example
