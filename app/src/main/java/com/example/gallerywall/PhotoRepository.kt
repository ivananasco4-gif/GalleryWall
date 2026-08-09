package com.example.gallerywall

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Photo(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val dateTaken: Long
)

object PhotoRepository {

    /** Consulta las fotos más recientes del dispositivo (hasta [limit]). */
    suspend fun loadPhotos(context: Context, limit: Int = 400): List<Photo> =
        withContext(Dispatchers.IO) {
            val photos = mutableListOf<Photo>()
            val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_TAKEN
            )
            val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

            context.contentResolver.query(
                collection, projection, null, null, sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)

                while (cursor.moveToNext() && photos.size < limit) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "Foto"
                    val date = cursor.getLong(dateCol)
                    val uri = ContentUris.withAppendedId(collection, id)
                    photos.add(Photo(id, uri, name, date))
                }
            }
            photos
        }

    /** Decodifica una miniatura pequeña y liviana para usar como celda de la malla. */
    suspend fun loadThumbnail(context: Context, photo: Photo, sizePx: Int = 96): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(
                        photo.uri, Size(sizePx, sizePx), null
                    )
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Thumbnails.getThumbnail(
                        context.contentResolver,
                        photo.id,
                        MediaStore.Images.Thumbnails.MICRO_KIND,
                        null
                    )
                }
            } catch (e: Exception) {
                null
            }
        }
}
