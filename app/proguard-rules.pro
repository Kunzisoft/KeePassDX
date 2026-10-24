# Add project specific ProGuard rules here.
# See http://developer.android.com/guide/developing/tools/proguard.html

# Keep Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    static final android.os.Parcelable$Creator CREATOR;
}

# Keep Room database and entities
-keep class androidx.room.RoomDatabase { *; }
-keep class com.kunzisoft.keepass.database.** { *; }

# Keep BouncyCastle
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Keep Joda-Time
-keep class org.joda.time.** { *; }
-dontwarn org.joda.time.tz.ZoneInfoCompiler$**
-dontwarn org.joda.convert.FromString
-dontwarn org.joda.convert.ToString

# Keep TokenAutoComplete
-keep class com.splitwise.tokenautocomplete.** { *; }
-dontwarn com.splitwise.tokenautocomplete.**

# Keep Chroma color picker
-keep class com.kunzisoft.androidclearchroma.** { *; }
-dontwarn com.kunzisoft.androidclearchroma.**

# Keep TapTargetView
-keep class com.getkeepsafe.taptargetview.** { *; }
-dontwarn com.getkeepsafe.taptargetview.**

# Keep Nbvcxz (password strength)
-keep class me.gosimple.nbvcxz.** { *; }
-dontwarn me.gosimple.nbvcxz.**

# Keep custom preferences and views
-keep class com.kunzisoft.keepass.settings.preference.** { *; }
-keep class com.kunzisoft.keepass.view.** { *; }
-keep class com.kunzisoft.keepass.credentialprovider.** { *; }
