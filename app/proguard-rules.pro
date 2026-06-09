# Add project specific ProGuard rules here.

# 保留启源 OpenSDK 类
-keep class com.changan.sda.opensdk.** { *; }
-keepclassmembers class com.changan.sda.opensdk.** { *; }

# 保留 Gson 序列化的数据类
-keep class com.qiyuan.keytools.model.** { *; }
-keepclassmembers class com.qiyuan.keytools.model.** { *; }

# 保留 enum 类
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
