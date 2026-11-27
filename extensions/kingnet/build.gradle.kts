extension { name = "extensions/kingnet-push.mpe" }
android { namespace = "app.pigfoot.extension.kingnet" }

android {
    buildTypes.getByName("release") {
        isMinifyEnabled = true
        proguardFiles("proguard-rules.pro")
    }
}
