extension { name = "extensions/camscanner-policy.mpe" }
android { namespace = "app.pigfoot.extension.camscanner" }
android {
    buildTypes.getByName("release") {
        isMinifyEnabled = true
        proguardFiles("proguard-rules.pro")
    }
}

android { sourceSets.getByName("main").java.exclude("app/pigfoot/diagnostics/**") }
