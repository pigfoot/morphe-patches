extension { name = "extensions/camscanner-word-diagnostics.mpe" }
android { namespace = "app.pigfoot.extension.camscanner" }
android {
    buildTypes.getByName("release") {
        isMinifyEnabled = true
        proguardFiles("proguard-rules.pro")
    }
}
