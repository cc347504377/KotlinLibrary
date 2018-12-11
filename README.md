# KotlinLibrary
基于Kotlin封装的工具库，主要包含网络、日志、RecycleView相关。

## 集成方式
基于repo进行管理，在主工程根目录下执行以下代码即可：
```
  repo init
  repo init -u https://github.com/cc347504377/KotlinLibrary.git
  repo sync
  repo start master --all
```
另外在主工程中编译和依赖

```gradle
include ':app', ':kotlinLibrary'
```

```
dependencies {
    implementation project(':kotlinLibrary')
}
```

为了统一项目sdk等版本号，在BuildScript中统一进行管理。如不需要，可自行删除`default.xml`中的

```
    <copyfile src="BuildScript/build.gradle" dest="build.gradle" />
    <copyfile src="BuildScript/build_app.gradle" dest="app/build.gradle" />
```
