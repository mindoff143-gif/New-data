package com.example.service

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.FacebookPublishResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

object FacebookPublisherService {
    private const val TAG = "FacebookPublisher"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Publishes directly to a Facebook Page via Meta Graph API v19.0
     * Supports both Photo posting (with caption) and Link/Text Feed posting.
     * No mock or simulated mode.
     */
    suspend fun publishToFacebookPage(
        pageId: String,
        pageAccessToken: String,
        message: String,
        linkUrl: String? = null,
        imageUrl: String? = null
    ): FacebookPublishResult = withContext(Dispatchers.IO) {
        val effectivePageId = pageId.trim()
        val effectiveToken = pageAccessToken.trim()

        if (effectiveToken.isBlank()) {
            return@withContext FacebookPublishResult(
                isSuccess = false,
                postId = null,
                message = "फेसबुक पेज पर ऑटो-पोस्ट के लिए Page Access Token दर्ज करें (Settings में उपलब्ध), अथवा सीधे नीचे 'फेसबुक ऐप में शेयर करें' बटन दबाएं।",
                isSimulatedOrIntent = false,
                openedDirectApp = false
            )
        }

        if (effectivePageId.isBlank()) {
            return@withContext FacebookPublishResult(
                isSuccess = false,
                postId = null,
                message = "फेसबुक पेज आईडी (Page ID) दर्ज करना आवश्यक है।",
                isSimulatedOrIntent = false,
                openedDirectApp = false
            )
        }

        try {
            // Determine if publishing as a Photo with caption or as a Feed post
            val hasPhoto = !imageUrl.isNullOrBlank() && (imageUrl.startsWith("http://") || imageUrl.startsWith("https://"))
            val graphEndpoint = if (hasPhoto) {
                "https://graph.facebook.com/v19.0/$effectivePageId/photos"
            } else {
                "https://graph.facebook.com/v19.0/$effectivePageId/feed"
            }

            val formBodyBuilder = FormBody.Builder()
            formBodyBuilder.add("access_token", effectiveToken)

            if (hasPhoto) {
                formBodyBuilder.add("url", imageUrl!!)
                formBodyBuilder.add("caption", message)
            } else {
                formBodyBuilder.add("message", message)
                if (!linkUrl.isNullOrBlank() && linkUrl.startsWith("http")) {
                    formBodyBuilder.add("link", linkUrl)
                }
            }

            val request = Request.Builder()
                .url(graphEndpoint)
                .post(formBodyBuilder.build())
                .build()

            val response = httpClient.newCall(request).execute()
            val responseString = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                val json = JSONObject(responseString)
                val postId = json.optString("id", json.optString("post_id", "published_${System.currentTimeMillis()}"))
                return@withContext FacebookPublishResult(
                    isSuccess = true,
                    postId = postId,
                    message = "सफलतापूर्वक फेसबुक पेज पर पोस्ट हो गया! (Post ID: $postId)",
                    isSimulatedOrIntent = false,
                    openedDirectApp = false
                )
            } else {
                Log.e(TAG, "Facebook Graph API HTTP ${response.code}: $responseString")
                val errorMsg = try {
                    val errObj = JSONObject(responseString).optJSONObject("error")
                    errObj?.optString("message") ?: "HTTP ${response.code}"
                } catch (e: Exception) {
                    "HTTP ${response.code}: $responseString"
                }

                return@withContext FacebookPublishResult(
                    isSuccess = false,
                    postId = null,
                    message = "फेसबुक एरर: $errorMsg। कृपया जांचें कि टोकन में 'pages_manage_posts' अनुमति सक्रिय है।",
                    isSimulatedOrIntent = false,
                    openedDirectApp = false
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error publishing to Facebook: ${e.message}", e)
            return@withContext FacebookPublishResult(
                isSuccess = false,
                postId = null,
                message = "नेटवर्क एरर: ${e.message ?: "फेसबुक सर्वर से संपर्क नहीं हो पाया"}",
                isSimulatedOrIntent = false,
                openedDirectApp = false
            )
        }
    }

    /**
     * Downloads an image to app cache and provides a content URI using FileProvider
     */
    private suspend fun downloadImageForSharing(
        context: Context,
        imageUrl: String,
        articleId: Long
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(imageUrl)
                .header("User-Agent", "Mozilla/5.0")
                .build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bytes = response.body?.bytes()
                if (bytes != null && bytes.isNotEmpty()) {
                    val sharedDir = File(context.cacheDir, "shared_images").apply { mkdirs() }
                    val imageFile = File(sharedDir, "fb_share_${articleId}.jpg")
                    imageFile.writeBytes(bytes)
                    return@withContext FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        imageFile
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not download image for Facebook share: ${e.message}")
        }
        null
    }

    /**
     * Directly opens the Facebook app without prompting for generic app choosers or unnecessary permissions.
     * Attaches the photo (if available) via secure FileProvider URI, copies the formatted text with copyright
     * attribution to the clipboard, and launches Facebook's composer directly.
     */
    suspend fun shareViaFacebookAppDirect(
        context: Context,
        postText: String,
        imageUrl: String? = null,
        linkUrl: String? = null,
        articleId: Long = System.currentTimeMillis()
    ): FacebookPublishResult = withContext(Dispatchers.Main) {
        try {
            // 1. Copy the full formatted text (including copyright attribution) to clipboard
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("Saharanpur Facebook Post", postText)
            clipboard?.setPrimaryClip(clip)

            // 2. If image is available, download to cache for sharing
            var contentUri: Uri? = null
            if (!imageUrl.isNullOrBlank() && imageUrl.startsWith("http")) {
                contentUri = downloadImageForSharing(context, imageUrl, articleId)
            }

            // 3. Detect installed Facebook packages
            val pm = context.packageManager
            val targetPackage = when {
                isPackageInstalled("com.facebook.katana", pm) -> "com.facebook.katana"
                isPackageInstalled("com.facebook.lite", pm) -> "com.facebook.lite"
                else -> null
            }

            val shareIntent = if (contentUri != null) {
                Intent(Intent.ACTION_SEND).apply {
                    type = "image/jpeg"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_TEXT, postText)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, postText)
                    putExtra(Intent.EXTRA_SUBJECT, "सहारनपुर ताज़ा समाचार")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

            if (targetPackage != null) {
                // Open directly in Facebook app
                shareIntent.setPackage(targetPackage)
                if (contentUri != null) {
                    context.grantUriPermission(targetPackage, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(shareIntent)

                Toast.makeText(
                    context,
                    if (contentUri != null)
                        "सीधे फेसबुक ऐप खोला जा रहा है (फोटो व कॉपीराइट टेक्स्ट संलग्न है)!"
                    else
                        "सीधे फेसबुक ऐप खोला जा रहा है (टेक्स्ट क्लिपबोर्ड पर कॉपी हो गया)!",
                    Toast.LENGTH_LONG
                ).show()

                return@withContext FacebookPublishResult(
                    isSuccess = true,
                    postId = null,
                    message = "फेसबुक ऐप सीधे खोला गया। फोटो और कॉपीराइट टेक्स्ट तैयार है!",
                    isSimulatedOrIntent = false,
                    openedDirectApp = true
                )
            } else {
                // If Facebook native app is not installed, open Facebook directly in browser (never open random apps)
                val fbWebUrl = if (!linkUrl.isNullOrBlank() && linkUrl.startsWith("http")) {
                    "https://www.facebook.com/sharer/sharer.php?u=${Uri.encode(linkUrl)}&quote=${Uri.encode(postText)}"
                } else {
                    "https://m.facebook.com"
                }

                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fbWebUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)

                Toast.makeText(
                    context,
                    "फेसबुक सीधे खोला गया (पोस्ट मैटर क्लिपबोर्ड पर कॉपी है)!",
                    Toast.LENGTH_LONG
                ).show()

                return@withContext FacebookPublishResult(
                    isSuccess = true,
                    postId = null,
                    message = "फेसबुक वेब सीधे खोला गया (टेक्स्ट कॉपी हो चुका है)।",
                    isSimulatedOrIntent = false,
                    openedDirectApp = true
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening Facebook directly: ${e.message}", e)
            Toast.makeText(context, "फेसबुक खोलने में त्रुटि: ${e.message}", Toast.LENGTH_SHORT).show()
            return@withContext FacebookPublishResult(
                isSuccess = false,
                postId = null,
                message = "फेसबुक खोलने में त्रुटि: ${e.message}",
                isSimulatedOrIntent = false,
                openedDirectApp = false
            )
        }
    }

    private fun isPackageInstalled(packageName: String, packageManager: PackageManager): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }
}
