# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /path/to/sdk/tools/proguard/proguard-android.txt

# Firebase Firestore — keep data model classes for Firestore deserialization
-keep class com.example.threadslite.data.model.** { *; }

# Keep Firebase Auth
-keepattributes Signature
-keepattributes *Annotation*
