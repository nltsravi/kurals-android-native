package com.nltsravi.kurals.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext

object AssetImageLoader {

    private val bitmapCache = HashMap<String, Bitmap>()

    @Synchronized
    fun loadAssetBitmap(context: Context, assetPath: String): Bitmap? {
        bitmapCache[assetPath]?.let { return it }
        return try {
            context.assets.open(assetPath).use { stream ->
                BitmapFactory.decodeStream(stream)?.also { bmp ->
                    bitmapCache[assetPath] = bmp
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    fun loadUriBitmap(context: Context, uriString: String): Bitmap? {
        return try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (_: Exception) {
            null
        }
    }
}

@Composable
fun rememberAssetImageBitmap(assetPath: String): ImageBitmap? {
    val context = LocalContext.current
    return remember(assetPath) {
        AssetImageLoader.loadAssetBitmap(context, assetPath)?.asImageBitmap()
    }
}

@Composable
fun rememberUriImageBitmap(uriString: String?): ImageBitmap? {
    val context = LocalContext.current
    return remember(uriString) {
        if (uriString.isNullOrBlank()) null
        else AssetImageLoader.loadUriBitmap(context, uriString)?.asImageBitmap()
    }
}

