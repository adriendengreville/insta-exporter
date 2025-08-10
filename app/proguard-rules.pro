# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/user/Library/Android/sdk/tools/proguard/proguard-android-optimize.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If you use reflection to access classes in shrinking code...
#-keep class com.example.usefulclass
#-keepclassmembers class com.example.usefulclass {
#   public <fields>;
#   public <methods>;
#}

# If you use Gson with shrinking code...
#-keep class com.google.gson.stream.** { *; }

# If you use Retrolambda with shrinking code...
#-dontwarn java.lang.invoke.*
