Guidebook
=============

Maven Dependency (Gradle)
------

```gradle
repositories {
    maven {
        url 'http://dogforce-games.com/maven'
    }
}\c
```

```gradle
dependencies {
    // required as a dependency of guidebook
    deobfCompile "gigaherz.commons:gigaherz.commons-1.12.1:0.6.4"
    deobfCompile "gigaherz.guidebook:Guidebook-1.12.2:2.9.0"
}
```
