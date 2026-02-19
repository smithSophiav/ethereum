# Keep EtherWeb and bridge public API for consumers who enable minify/R8
-keep class com.smithsophiav.etherweb.EtherWeb { *; }
-keep class com.smithsophiav.etherweb.bridge.WebViewJavascriptBridge { *; }
-keep class com.smithsophiav.etherweb.bridge.WebViewJavascriptBridge$AndroidBridge { *; }
# Keep @JavascriptInterface methods so JS can call into Android
-keepclassmembers class com.smithsophiav.etherweb.bridge.WebViewJavascriptBridge$AndroidBridge {
    @android.webkit.JavascriptInterface <methods>;
}
