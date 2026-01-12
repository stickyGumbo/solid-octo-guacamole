-keep class com.stickyapp.adblocker.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
-keepclassmembers public class * extends android.app.Service {
    public <methods>;
}
-keepclassmembers public class * extends androidx.appcompat.app.AppCompatActivity {
    public <methods>;
}
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-dontwarn java.lang.invoke.**
-dontwarn sun.reflect.**
-dontnote android.net.http.*
-dontnote org.apache.commons.codec.**
