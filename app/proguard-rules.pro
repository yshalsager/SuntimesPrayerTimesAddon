# Keep minimal and rely on library consumer rules where possible.
#
# Time4J Android resource loading is reflection-based. R8 can strip the default ctor,
# which breaks startup in release builds.
-keep class net.time4j.android.spi.AndroidResourceLoader { public <init>(); }
