plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
 namespace = "com.multi0819.nearvoice"
 compileSdk = 35
 defaultConfig { applicationId = "com.multi0819.nearvoice"; minSdk = 26; targetSdk = 35; versionCode = 15; versionName = "1.0.14" }
 signingConfigs { getByName("debug") { storeFile = rootProject.file("signing/nearvoice-debug.jks"); storePassword = "android"; keyAlias = "androiddebugkey"; keyPassword = "android" } }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
 lint { abortOnError = true }
}
dependencies { implementation("com.google.android.gms:play-services-wearable:19.0.0"); testImplementation("junit:junit:4.13.2") }
