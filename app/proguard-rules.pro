-keep class androidx.room.** { *; }
-keep @kotlinx.serialization.Serializable class ** { *; }

# This compatibility Parcelable must retain Azahar's exact class name and field layout.
-keep class org.citra.citra_emu.model.Game { *; }
-keep class org.citra.citra_emu.model.Game$MediaType { *; }
