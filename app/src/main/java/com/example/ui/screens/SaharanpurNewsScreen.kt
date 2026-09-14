package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Copyright
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.FacebookPageConfig
import com.example.data.model.SaharanpurNewsEntity
import com.example.service.AppInviteService
import com.example.service.SaharanpurNewsService

private val FacebookBlue = Color(0xFF1877F2)
private val GreenPublished = Color(0xFF10B981)
private val DeepIndigo = Color(0xFF1E1B4B)
private val IndigoAccent = Color(0xFF4F46E5)

@Composable
fun SaharanpurNewsScreen(
    newsArticles: List<SaharanpurNewsEntity>,
    facebookConfig: FacebookPageConfig,
    isSearchingNews: Boolean,
    isPublishingToFb: Boolean,
    preferredLanguage: String,
    onSearchNews: (query: String) -> Unit,
    onPublishToFacebook: (article: SaharanpurNewsEntity, customText: String?) -> Unit,
    onShareViaIntent: (context: Context, article: SaharanpurNewsEntity) -> Unit,
    onSaveFacebookConfig: (config: FacebookPageConfig) -> Unit,
    onUpdateArticle: (updated: SaharanpurNewsEntity) -> Unit = {},
    onUnpublishArticle: (article: SaharanpurNewsEntity) -> Unit = {},
    onSpeakNews: (text: String) -> Unit,
    onShowDownloadDialog: () -> Unit,
    onShowInviteDialog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedFeedTab by remember { mutableIntStateOf(0) } // 0: मेरी ताज़ा फ़ीड (Unshared), 1: शेयर की गई खबरें (Shared)
    var selectedCategoryFilter by remember { mutableStateOf("सभी") }
    var searchQuery by remember { mutableStateOf("") }
    var showConfigDialog by remember { mutableStateOf(false) }
    var editingArticle by remember { mutableStateOf<SaharanpurNewsEntity?>(null) }

    val categories = listOf(
        "सभी",
        "ताज़ा ख़बर",
        "विकास व स्मार्ट सिटी",
        "शिक्षा व विश्वविद्यालय",
        "किसान व कृषि",
        "प्रशासन व पुलिस",
        "उद्योग व व्यापार"
    )

    // Strictly separate posts that are already shared from the fresh feed!
    val unsharedArticles = newsArticles.filter { !it.isPublishedToFb }
    val sharedArticles = newsArticles.filter { it.isPublishedToFb }

    val activeFeedArticles = if (selectedFeedTab == 0) unsharedArticles else sharedArticles

    val filteredArticles = activeFeedArticles.filter { article ->
        val matchesCategory = if (selectedCategoryFilter == "सभी") true else article.category == selectedCategoryFilter
        val matchesQuery = if (searchQuery.isBlank()) true else {
            article.title.contains(searchQuery, ignoreCase = true) ||
                    article.summaryHindi.contains(searchQuery, ignoreCase = true) ||
                    article.category.contains(searchQuery, ignoreCase = true)
        }
        matchesCategory && matchesQuery
    }

    val publishedCount = newsArticles.count { it.isPublishedToFb }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Header Hero Card
            item {
                SaharanpurHeroBanner(
                    newsCount = newsArticles.size,
                    publishedCount = publishedCount,
                    isAutoPublish = facebookConfig.autoPublishEnabled,
                    onSearchClick = { onSearchNews(searchQuery) },
                    onDownloadClick = onShowDownloadDialog,
                    onInviteClick = onShowInviteDialog,
                    isSearching = isSearchingNews
                )
            }

            // 2. Facebook Connected Page Card
            item {
                FacebookPageStatusCard(
                    config = facebookConfig,
                    onConfigureClick = { showConfigDialog = true },
                    onToggleAutoPublish = { enabled ->
                        onSaveFacebookConfig(facebookConfig.copy(autoPublishEnabled = enabled))
                    }
                )
            }

            // 3. Search Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("सहारनपुर समाचार खोजें (उदा. घंटाघर, विश्वविद्यालय)...", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = IndigoAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (isSearchingNews) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("news_search_field")
                    )

                    Button(
                        onClick = { onSearchNews(searchQuery) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                        modifier = Modifier.testTag("news_search_button")
                    ) {
                        Text("खोजें", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            // 4. Category Filter Chips Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = selectedCategoryFilter == category,
                            onClick = { selectedCategoryFilter = category },
                            label = { Text(category, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IndigoAccent,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }

            // 5. News List Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ताज़ा समाचार फीड (${filteredArticles.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    TextButton(
                        onClick = { onSearchNews("") },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ताज़ा करें", fontSize = 12.sp)
                    }
                }
            }

            // 6. News Cards List
            if (filteredArticles.isEmpty()) {
                item {
                    EmptyNewsPlaceholder(
                        searchQuery = searchQuery,
                        onFetchNews = { onSearchNews("") }
                    )
                }
            } else {
                items(filteredArticles, key = { it.id }) { article ->
                    SaharanpurNewsCard(
                        article = article,
                        isPublishing = isPublishingToFb,
                        onPublish = { onPublishToFacebook(article, null) },
                        onShareDirect = { onShareViaIntent(context, article) },
                        onEdit = { editingArticle = article },
                        onSpeak = { onSpeakNews("${article.title}। ${article.summaryHindi}") },
                        onToggleCopyright = { isChecked ->
                            val updatedFormatted = SaharanpurNewsService.generateFacebookPost(
                                title = article.title,
                                summary = article.summaryHindi,
                                category = article.category,
                                source = article.source,
                                url = article.originalUrl,
                                includeCopyright = isChecked,
                                copyrightNotice = article.copyrightAttribution
                            )
                            onUpdateArticle(
                                article.copy(
                                    includeCopyright = isChecked,
                                    formattedFbPost = updatedFormatted
                                )
                            )
                        }
                    )
                }
            }
        }

        // Edit dialog before publishing
        editingArticle?.let { article ->
            EditPostBeforePublishDialog(
                article = article,
                onDismiss = { editingArticle = null },
                onShareDirect = { customText ->
                    val updated = article.copy(formattedFbPost = customText)
                    onUpdateArticle(updated)
                    onShareViaIntent(context, updated)
                    editingArticle = null
                },
                onPublishApi = { customText ->
                    onPublishToFacebook(article, customText)
                    editingArticle = null
                }
            )
        }

        // Facebook Settings Dialog
        if (showConfigDialog) {
            FacebookConfigDialog(
                currentConfig = facebookConfig,
                onDismiss = { showConfigDialog = false },
                onSave = { newConfig ->
                    onSaveFacebookConfig(newConfig)
                    showConfigDialog = false
                }
            )
        }
    }
}

@Composable
private fun SaharanpurHeroBanner(
    newsCount: Int,
    publishedCount: Int,
    isAutoPublish: Boolean,
    onSearchClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onInviteClick: () -> Unit,
    isSearching: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("saharanpur_hero_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF1E1B4B),
                            Color(0xFF312E81),
                            Color(0xFF1E3A8A)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "सहारनपुर न्यूज़ (उ.प्र.)",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "सच्ची खबरें • फोटो सहित • सीधा FB शेयर",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Action buttons: Invite + Download APK
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = onInviteClick,
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPublished),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("invite_friends_hero_button")
                        ) {
                            Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Invite", modifier = Modifier.size(14.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("इनवाइट", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        OutlinedButton(
                            onClick = onDownloadClick,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("download_apk_hero_button")
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = "Download APK", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("APK", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "$newsCount ताज़ा ख़बरें उपलब्ध",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$publishedCount फेसबुक पर पोस्टेड",
                            color = GreenPublished,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isAutoPublish) GreenPublished.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isAutoPublish) "⚡ ऑटो-पोस्ट सक्रिय" else "📲 डायरेक्ट शेयर मोड",
                                color = if (isAutoPublish) Color(0xFF6EE7B7) else Color.White.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FacebookPageStatusCard(
    config: FacebookPageConfig,
    onConfigureClick: () -> Unit,
    onToggleAutoPublish: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("facebook_status_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(FacebookBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("f", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    }
                    Column {
                        Text(
                            text = config.pageName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (config.pageAccessToken.isNotBlank()) "Page ID: ${config.pageId} • Meta Graph API कनेक्टेड" else "सीधे फेसबुक ऐप में वन-क्लिक शेयरिंग सक्रिय",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onConfigureClick,
                    modifier = Modifier.testTag("configure_fb_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configure Facebook",
                        tint = IndigoAccent
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "नए समाचार खोजते ही स्वतः FB पेज पर पोस्ट करें",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (config.autoPublishEnabled) "स्वतः पब्लिशिंग चालू है (Graph API)" else "बंद (प्रत्येक समाचार पर अपनी पसंद से सीधे शेयर करें)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = config.autoPublishEnabled,
                    onCheckedChange = onToggleAutoPublish,
                    colors = SwitchDefaults.colors(checkedThumbColor = FacebookBlue),
                    modifier = Modifier.testTag("auto_publish_switch")
                )
            }
        }
    }
}

@Composable
fun SaharanpurNewsCard(
    article: SaharanpurNewsEntity,
    isPublishing: Boolean,
    onPublish: () -> Unit,
    onShareDirect: () -> Unit,
    onEdit: () -> Unit,
    onSpeak: () -> Unit,
    onToggleCopyright: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var showPostPreview by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("saharanpur_news_card_${article.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Category & Source header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(IndigoAccent.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = article.category,
                            color = IndigoAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = article.source,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = article.publishedDate,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )

                    IconButton(
                        onClick = onSpeak,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Read News Aloud",
                            tint = IndigoAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // News Photo (Prominently displayed)
            if (!article.imageUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(article.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = article.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Photo badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "फ़ोटो सहित समाचार",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Headline
            Text(
                text = article.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Summary
            Text(
                text = article.summaryHindi,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            // Copyright & Source Attribution Control Section
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Copyright,
                            contentDescription = "Copyright",
                            tint = if (article.includeCopyright) IndigoAccent else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = "स्रोत व कॉपीराइट साभार (Copyright Attribution)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (article.includeCopyright)
                                    (article.copyrightAttribution ?: "©️ समाचार स्रोत: ${article.source} | सर्वाधिकार सुरक्षित।")
                                else
                                    "कॉपीराइट साभार पोस्ट से हटा दिया गया है",
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (article.includeCopyright) IndigoAccent else MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    Switch(
                        checked = article.includeCopyright,
                        onCheckedChange = { onToggleCopyright(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = IndigoAccent),
                        modifier = Modifier.testTag("copyright_switch_${article.id}")
                    )
                }
            }

            // Facebook Published Status Badge
            if (article.isPublishedToFb) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(GreenPublished.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = GreenPublished,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "फेसबुक पर साझा हो चुका है • Post: ${article.fbPostId ?: "Shared"}",
                        color = GreenPublished,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Expandable Facebook Post Preview
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPostPreview = !showPostPreview }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (showPostPreview) "▼ पोस्ट का पूरा मैटर छिपाएं" else "▶ पोस्ट का पूरा मैटर व कॉपीराइट देखें",
                    fontSize = 12.sp,
                    color = FacebookBlue,
                    fontWeight = FontWeight.Medium
                )

                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("FB Post", article.formattedFbPost))
                        Toast.makeText(context, "पोस्ट कॉपी हो गई!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy text",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = showPostPreview,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = article.formattedFbPost,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Action Buttons: Direct Facebook App Share (Primary) + API Auto-post (Secondary)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // PRIMARY BUTTON: Directly opens Facebook app without asking for other app permissions
                Button(
                    onClick = onShareDirect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FacebookBlue,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("direct_fb_share_button_${article.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("f", color = FacebookBlue, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "सीधे फेसबुक पर शेयर करें",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(फोटो व कॉपीराइट सहित)",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                // Secondary row: Auto-Post via Page API + Edit with AI
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onPublish,
                        enabled = !isPublishing,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("publish_news_to_fb_${article.id}")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = IndigoAccent
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (article.isPublishedToFb) "पेज पर री-पोस्ट" else "पेज पर ऑटो-पोस्ट",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    OutlinedButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("edit_news_button_${article.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Post",
                            modifier = Modifier.size(14.dp),
                            tint = IndigoAccent
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("एडिट", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyNewsPlaceholder(
    searchQuery: String,
    onFetchNews: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Article,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Text(
                text = if (searchQuery.isNotBlank()) "'$searchQuery' से संबंधित कोई खबर नहीं मिली" else "सहारनपुर की कोई खबर अभी लोड नहीं है",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onFetchNews,
                colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("सहारनपुर ताज़ा समाचार लोड करें", fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun FacebookConfigDialog(
    currentConfig: FacebookPageConfig,
    onDismiss: () -> Unit,
    onSave: (FacebookPageConfig) -> Unit
) {
    var pageId by remember { mutableStateOf(currentConfig.pageId) }
    var pageName by remember { mutableStateOf(currentConfig.pageName) }
    var pageToken by remember { mutableStateOf(currentConfig.pageAccessToken) }
    var autoPublish by remember { mutableStateOf(currentConfig.autoPublishEnabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(FacebookBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Text("f", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Text("फेसबुक पेज इंटीग्रेशन सेटिंग्स", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "अपने फेसबुक पेज पर समाचार स्वतः बैकग्राउंड में पब्लिश करने के लिए पेज आईडी व एक्सेस टोकन दर्ज करें:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = pageName,
                    onValueChange = { pageName = it },
                    label = { Text("पेज का नाम (Page Name)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = pageId,
                    onValueChange = { pageId = it },
                    label = { Text("फेसबुक पेज आईडी (Page ID)") },
                    placeholder = { Text("उदा. 109876543210987") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = pageToken,
                    onValueChange = { pageToken = it },
                    label = { Text("Page Access Token (Meta Graph API)") },
                    placeholder = { Text("Meta Graph Token पेस्ट करें") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "💡 नोट: टोकन के बिना भी आप कार्ड पर 'सीधे फेसबुक पर शेयर करें' बटन दबाकर एक क्लिक में सीधे फेसबुक ऐप पर फोटो व कॉपीराइट सहित पोस्ट कर सकते हैं।",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        currentConfig.copy(
                            pageId = pageId.trim(),
                            pageName = pageName.trim(),
                            pageAccessToken = pageToken.trim(),
                            autoPublishEnabled = autoPublish
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue)
            ) {
                Text("सेटिंग्स सुरक्षित करें")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}

@Composable
fun EditPostBeforePublishDialog(
    article: SaharanpurNewsEntity,
    onDismiss: () -> Unit,
    onShareDirect: (customText: String) -> Unit,
    onPublishApi: (customText: String) -> Unit
) {
    var postText by remember { mutableStateOf(article.formattedFbPost) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = IndigoAccent)
                Text("पोस्ट मैटर व कॉपीराइट एडिट करें", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!article.imageUrl.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = article.imageUrl,
                            contentDescription = "News Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Text(
                    text = "फेसबुक पर शेयर करने से पहले पोस्ट मैटर, कॉपीराइट साभार या हैशटैग संपादित करें:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = postText,
                    onValueChange = { postText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    maxLines = 12
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = { onShareDirect(postText) },
                    colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue)
                ) {
                    Text("सीधे FB पर शेयर")
                }

                OutlinedButton(
                    onClick = { onPublishApi(postText) }
                ) {
                    Text("पेज API पोस्ट")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}

@Composable
fun DownloadAppDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val apkUrl = AppInviteService.DIRECT_APK_URL

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = IndigoAccent)
                Text("डायरेक्ट APK व इनवाइट लिंक", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "इस लिंक को खोलते ही सीधे Personal AI Assistant की APK आपके फोन में डाउनलोड हो जाएगी:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Direct APK Download Box
                Card(
                    colors = CardDefaults.cardColors(containerColor = IndigoAccent.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("🌐 डायरेक्ट डाउनलोड लिंक:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IndigoAccent)
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    clipboard?.setPrimaryClip(ClipData.newPlainText("APK Link", apkUrl))
                                    Toast.makeText(context, "APK लिंक कॉपी हो गया!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp))
                            }
                        }
                        Text(
                            text = apkUrl,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Text(
                    text = "✅ कोई इंस्टॉलेशन परमिशन या जटिल प्रक्रिया नहीं — लिंक पर क्लिक करते ही APK फाइल सुरक्षित डाउनलोड होती है।",
                    fontSize = 11.sp,
                    color = Color(0xFF10B981)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    AppInviteService.downloadApkDirectly(context)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent)
            ) {
                Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("अभी डाउनलोड करें")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("बंद करें")
            }
        }
    )
}
