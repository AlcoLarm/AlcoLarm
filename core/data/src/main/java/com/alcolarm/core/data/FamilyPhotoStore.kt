package com.alcolarm.core.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Copies family photos into app-internal storage under filesDir/family_photos/
 * and returns relative paths for DataStore. Prefer these over raw content URIs
 * so prefs do not retain grant-dependent external URIs.
 */
@Singleton
class FamilyPhotoStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val photosDir: File
        get() = File(context.filesDir, DIR_NAME).also { it.mkdirs() }

    /**
     * Import [uri] into app-scoped storage. Attempts persistable read permission
     * (OpenDocument), then always copies bytes locally.
     *
     * @return relative path under filesDir (e.g. `family_photos/<uuid>.jpg`), or null on failure
     */
    fun importPhoto(uri: Uri): String? {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (_: SecurityException) {
            // Persistable flag not granted (or already taken); copy still proceeds.
        }

        return runCatching {
            val destName = "${UUID.randomUUID()}.${guessExtension(uri)}"
            val dest = File(photosDir, destName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            "$DIR_NAME/$destName"
        }.getOrNull()
    }

    /** Delete an app-scoped copy when removing a photo from onboarding. */
    fun deleteIfAppScoped(storedPath: String) {
        if (!storedPath.startsWith("$DIR_NAME/")) return
        val file = File(context.filesDir, storedPath)
        val photosCanonical = photosDir.canonicalFile
        if (file.canonicalFile.startsWith(photosCanonical)) {
            file.delete()
        }
    }

    fun resolveFile(storedPath: String): File? {
        if (!storedPath.startsWith("$DIR_NAME/")) return null
        val file = File(context.filesDir, storedPath)
        return file.takeIf { it.isFile && it.canonicalFile.startsWith(photosDir.canonicalFile) }
    }

    private fun guessExtension(uri: Uri): String {
        val type = context.contentResolver.getType(uri).orEmpty()
        return when {
            type.contains("png") -> "png"
            type.contains("webp") -> "webp"
            type.contains("gif") -> "gif"
            else -> "jpg"
        }
    }

    companion object {
        const val DIR_NAME = "family_photos"
    }
}
