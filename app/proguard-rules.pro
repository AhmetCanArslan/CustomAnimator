-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses

-keep class com.riyadmondol2006.customanimator.data.** { *; }
-keep class com.riyadmondol2006.customanimator.notify.data.** { *; }
-keepclassmembers enum com.riyadmondol2006.customanimator.** { *; }

-keep class com.riyadmondol2006.customanimator.service.WifiUserService { *; }
-keep class com.riyadmondol2006.customanimator.service.HotspotUserService { *; }
-keep class com.riyadmondol2006.customanimator.service.CarrierUserService { *; }
-keep class com.riyadmondol2006.customanimator.service.CarrierConfigInstrumentation { *; }
-keep class com.riyadmondol2006.customanimator.service.ShortcutUserService { *; }
-keep class com.riyadmondol2006.customanimator.service.AppMonitorUserService { *; }
-keep class com.riyadmondol2006.customanimator.service.AppMonitorUserService$* { *; }
-keep interface com.riyadmondol2006.customanimator.service.IWifiUserService { *; }
-keep interface com.riyadmondol2006.customanimator.service.IHotspotUserService { *; }
-keep interface com.riyadmondol2006.customanimator.service.ICarrierUserService { *; }
-keep class com.riyadmondol2006.customanimator.service.IWifiUserService$* { *; }
-keep class com.riyadmondol2006.customanimator.service.IHotspotUserService$* { *; }
-keep class com.riyadmondol2006.customanimator.service.ICarrierUserService$* { *; }
-keep interface com.riyadmondol2006.customanimator.service.IShortcutUserService { *; }
-keep class com.riyadmondol2006.customanimator.service.IShortcutUserService$* { *; }
-keep interface com.riyadmondol2006.customanimator.service.IAppMonitorUserService { *; }
-keep class com.riyadmondol2006.customanimator.service.IAppMonitorUserService$* { *; }
-keep interface com.riyadmondol2006.customanimator.service.IForegroundAppListener { *; }
-keep class com.riyadmondol2006.customanimator.service.IForegroundAppListener$* { *; }
-keep class com.riyadmondol2006.customanimator.service.PowerButtonUserService { *; }
-keep class com.riyadmondol2006.customanimator.service.PowerButtonUserService$* { *; }
-keep interface com.riyadmondol2006.customanimator.service.IPowerButtonUserService { *; }
-keep class com.riyadmondol2006.customanimator.service.IPowerButtonUserService$* { *; }
-keep interface com.riyadmondol2006.customanimator.service.IPowerButtonListener { *; }
-keep class com.riyadmondol2006.customanimator.service.IPowerButtonListener$* { *; }
-keep interface android.app.IProcessObserver { *; }
-keep class android.app.IProcessObserver$* { *; }

-keep class rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }

-keep class * extends com.google.gson.reflect.TypeToken
-keep class * implements com.google.gson.TypeAdapter
-keep class com.google.gson.reflect.TypeToken { *; }

-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
