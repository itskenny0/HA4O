# okhttp 3.12 references optional TLS providers (Conscrypt, BouncyCastle, OpenJSSE) and
# a couple of annotation packages that aren't present on Android. okhttp bundles its own
# keep rules; these -dontwarn lines just stop R8 erroring on the absent optionals.
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# HA4O uses no reflection; the Activities are kept via the manifest. Nothing else to keep.
