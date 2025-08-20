androidApplication {
    namespace = "org.example.app"

    dependencies {
        // Keep sample module deps if needed by template; app does not use them.
        implementation("org.apache.commons:commons-text:1.11.0")
        implementation(project(":utilities"))

        // AndroidX UI libs (explicit versions as required)
        implementation("androidx.appcompat:appcompat:1.7.0")
        implementation("com.google.android.material:material:1.12.0")
        implementation("androidx.recyclerview:recyclerview:1.3.2")
        implementation("androidx.constraintlayout:constraintlayout:2.1.4")
        implementation("androidx.cardview:cardview:1.0.0")
        implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    }
}
