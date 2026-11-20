plugins {
    alias(libs.plugins.android.application) apply false

    // Google Services / Firebase: declarado na raiz, aplicado no modulo app.
    id("com.google.gms.google-services") version "4.6.0" apply false
}
