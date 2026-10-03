plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose") }
android {
 namespace = "app.akitostation.android"
 compileSdk = 36
 buildToolsVersion = "36.0.0"
 defaultConfig {
  applicationId = "app.akitostation.android"
  minSdk = 26
  targetSdk = 36
  versionCode = 3
  versionName = "1.0.2"
  testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  ndk { abiFilters += "arm64-v8a" }
 }
 val ownerKey = providers.environmentVariable("AKITO_SIGNING_STORE").orNull
 signingConfigs {
  if (ownerKey != null) create("owner") {
   storeFile = file(ownerKey)
   storePassword = providers.environmentVariable("AKITO_SIGNING_STORE_PASSWORD").get()
   keyAlias = providers.environmentVariable("AKITO_SIGNING_ALIAS").get()
   keyPassword = providers.environmentVariable("AKITO_SIGNING_KEY_PASSWORD").get()
  }
 }
 buildTypes {
  release { isMinifyEnabled = true; isShrinkResources = true
   proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
   if (ownerKey != null) signingConfig = signingConfigs.getByName("owner")
  }
 }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 buildFeatures { compose = true; buildConfig = true }
 testOptions { unitTests.isIncludeAndroidResources = true }
 packaging { resources.excludes += setOf("META-INF/AL2.0", "META-INF/LGPL2.1") }
}
kotlin { jvmToolchain(17) }
dependencies {
 implementation(platform("androidx.compose:compose-bom:2025.12.00"))
 implementation("androidx.activity:activity-compose:1.12.2")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.material:material-icons-extended")
 implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
 implementation("androidx.documentfile:documentfile:1.1.0")
 implementation("io.coil-kt:coil-compose:2.7.0")
 testImplementation("junit:junit:4.13.2")
 testImplementation("org.robolectric:robolectric:4.16.1")
 testImplementation("androidx.test:core:1.7.0")
 androidTestImplementation(platform("androidx.compose:compose-bom:2025.12.00"))
 androidTestImplementation("androidx.compose.ui:ui-test-junit4")
 androidTestImplementation("androidx.test.ext:junit:1.3.0")
 debugImplementation("androidx.compose.ui:ui-test-manifest")
}
