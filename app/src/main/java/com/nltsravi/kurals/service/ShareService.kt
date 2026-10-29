package com.nltsravi.kurals.service

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import com.nltsravi.kurals.model.Kural
import com.nltsravi.kurals.model.SocialPlatform
import com.nltsravi.kurals.model.SocialShareOptions
import com.nltsravi.kurals.util.TextUtils
import java.io.File
import java.io.FileOutputStream

object ShareService {

    fun copyToClipboard(context: Context, text: String, label: String = "Thirukkural"): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
            true
        } catch (_: Exception) {
            false
        }
    }

    fun shareKuralText(context: Context, kural: Kural) {
        val text = TextUtils.formatKuralText(kural, "both")
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Thirukkural #${kural.number}")
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(sendIntent, "Thirukkural #${kural.number}").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun shareTextToSocialPlatform(
        context: Context,
        kural: Kural,
        platform: SocialPlatform,
        options: SocialShareOptions = SocialShareOptions()
    ) {
        val formattedText = TextUtils.formatKuralForSocialShare(kural, options)
        copyToClipboard(context, formattedText)

        when (platform) {
            SocialPlatform.WHATSAPP, SocialPlatform.WHATSAPP_STATUS -> {
                val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    setPackage("com.whatsapp")
                    putExtra(Intent.EXTRA_TEXT, formattedText)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(whatsappIntent)
                    return
                } catch (_: Exception) {
                    // Fallback to wa.me or system share
                }
            }
            SocialPlatform.TWITTER -> {
                val encoded = Uri.encode(formattedText)
                val twitterIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://twitter.com/intent/tweet?text=$encoded")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(twitterIntent)
                    return
                } catch (_: Exception) {}
            }
            SocialPlatform.THREADS -> {
                val encoded = Uri.encode(formattedText)
                val threadsIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.threads.net/intent/post?text=$encoded")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(threadsIntent)
                    return
                } catch (_: Exception) {}
            }
            else -> {}
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "திருக்குறள் #${kural.number}")
            putExtra(Intent.EXTRA_TEXT, formattedText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(sendIntent, "திருக்குறள் #${kural.number}").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun saveCardBitmapToCache(context: Context, bitmap: Bitmap, kuralNumber: Int): File {
        val cardsDir = File(context.cacheDir, "shared_cards").apply { mkdirs() }
        val file = File(cardsDir, "thirukkural_card_$kuralNumber.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }

    fun shareImageToSocialPlatform(
        context: Context,
        bitmap: Bitmap,
        kural: Kural,
        platform: SocialPlatform = SocialPlatform.SYSTEM,
        options: SocialShareOptions = SocialShareOptions()
    ) {
        val caption = TextUtils.formatKuralForSocialShare(kural, options)
        copyToClipboard(context, caption)

        val imageFile = saveCardBitmapToCache(context, bitmap, kural.number)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )

        val targetPackage = when (platform) {
            SocialPlatform.WHATSAPP, SocialPlatform.WHATSAPP_STATUS -> "com.whatsapp"
            SocialPlatform.INSTAGRAM -> "com.instagram.android"
            SocialPlatform.TWITTER -> "com.twitter.android"
            SocialPlatform.THREADS -> "com.instagram.barcelona"
            SocialPlatform.FACEBOOK -> "com.facebook.katana"
            SocialPlatform.SYSTEM -> null
        }

        if (targetPackage != null) {
            val directIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                setPackage(targetPackage)
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, caption)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(directIntent)
                return
            } catch (_: Exception) {
                // Fallback to universal share chooser below
            }
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, caption)
            putExtra(Intent.EXTRA_TITLE, "Share Thirukkural #${kural.number} Status / Post")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(
            shareIntent,
            "Share Thirukkural #${kural.number} Status / Post"
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun shareExportedFile(
        context: Context,
        file: File,
        mimeType: String,
        chooserTitle: String
    ) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}

