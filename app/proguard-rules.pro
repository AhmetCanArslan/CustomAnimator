-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses

-keep class com.arslan.customanimator.data.** { *; }
-keep class com.arslan.customanimator.notify.data.** { *; }
-keepclassmembers enum com.arslan.customanimator.** { *; }

-keep class com.arslan.customanimator.service.WifiUserService { *; }
-keep class com.arslan.customanimator.service.HotspotUserService { *; }
-keep class com.arslan.customanimator.service.CarrierUserService { *; }
-keep class com.arslan.customanimator.service.AppMonitorUserService { *; }
-keep class com.arslan.customanimator.service.AppMonitorUserService$* { *; }
-keep interface com.arslan.customanimator.service.IWifiUserService { *; }
-keep interface com.arslan.customanimator.service.IHotspotUserService { *; }
-keep interface com.arslan.customanimator.service.ICarrierUserService { *; }
-keep class com.arslan.customanimator.service.IWifiUserService$* { *; }
-keep class com.arslan.customanimator.service.IHotspotUserService$* { *; }
-keep class com.arslan.customanimator.service.ICarrierUserService$* { *; }
-keep interface com.arslan.customanimator.service.IAppMonitorUserService { *; }
-keep class com.arslan.customanimator.service.IAppMonitorUserService$* { *; }
-keep interface com.arslan.customanimator.service.IForegroundAppListener { *; }
-keep class com.arslan.customanimator.service.IForegroundAppListener$* { *; }
-keep interface android.app.IProcessObserver { *; }
-keep class android.app.IProcessObserver$* { *; }

-keep class rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }

-keep class * extends androidx.room.RoomDatabase { void <init>(); }

-keep class * extends com.google.gson.reflect.TypeToken
-keep class * implements com.google.gson.TypeAdapter
-keep class com.google.gson.reflect.TypeToken { *; }

-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
