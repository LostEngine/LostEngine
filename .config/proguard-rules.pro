-keep class dev.misieur.justamaterial.** { *; }
-keep class dev.lost.engine.LostEngine { *; }
-keep class dev.lost.engine.bootstrap.ResourceInjector { *; }

-dontwarn
-dontoptimize
-dontobfuscate

-keepattributes StackMapTable,Signature,*Annotation*,InnerClasses,EnclosingMethod