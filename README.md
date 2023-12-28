# Zygote

Restart your application programmatically, compact for Android 10(API level 29)

## Sample

| window background      | homepage               | restart homepage       |
|------------------------|------------------------|------------------------|
| ![1.png](images/1.png) | ![2.png](images/2.png) | ![1.png](images/3.png) |

## How to use

Kill process directly:

```kotlin
App.restart(context)
```

Kill process by yourself:

```kotlin
App.restart(context) {
    Log.i(tag, "do some clean task, such as flush pending log request")
    // TODO xxx
    Process.killProcess(Process.myPid())
}
```
## How it works

Restart main process via sub-process instead of AlarmManager.

![zygote-flow.webp](images/zygote-flow.webp)