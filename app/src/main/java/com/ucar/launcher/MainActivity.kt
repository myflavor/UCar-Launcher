package com.ucar.launcher

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap

internal data class UcarApp(
    val name: String,
    val packageName: String,
    val className: String?,
    val icon: Drawable
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val apps = queryUcarApps()

        setContent {
            UcarLauncherScreen(apps) { launchUcarApp(it) }
        }
    }

    private fun queryUcarApps(): List<UcarApp> {
        val intent = Intent("com.ucar.intent.action.UCAR").apply {
            addCategory("com.ucar.intent.category.UCAR")
        }

        return packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .filter { it.activityInfo.packageName != packageName }
            .map { info ->
                UcarApp(
                    name = info.loadLabel(packageManager).toString(),
                    packageName = info.activityInfo.packageName,
                    className = info.activityInfo.name,
                    icon = info.loadIcon(packageManager)
                )
            }
            .sortedBy { it.name }
    }

    private fun launchUcarApp(app: UcarApp) {
        val intent = Intent("com.ucar.intent.action.UCAR").apply {
            addCategory("com.ucar.intent.category.UCAR")
            putExtra("isUcarMode", true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (app.className != null) {
                component = ComponentName(app.packageName, app.className)
            } else {
                setPackage(app.packageName)
            }
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "启动失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
internal fun UcarLauncherScreen(apps: List<UcarApp>, onLaunch: (UcarApp) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111111))
    ) {
        if (apps.isEmpty()) {
            Text(
                text = "没有找到 UCAR 应用\n请确保已安装支持车联的应用",
                color = Color(0x66FFFFFF),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // 标题栏
                val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
                Text(
                    text = "车联启动器",
                    color = Color(0xCCFFFFFF),
                    fontSize = 20.sp,
                    modifier = Modifier.padding(
                        start = 24.dp,
                        top = statusBarPadding.calculateTopPadding() + 16.dp,
                        bottom = 12.dp
                    )
                )

                // 应用网格
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 100.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(apps) { app ->
                        AppItem(app = app) { onLaunch(app) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AppItem(app: UcarApp, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState()

    val bgColor = if (isPressed.value) Color(0x22FFFFFF) else Color(0x11FFFFFF)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor, RoundedCornerShape(16.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(vertical = 16.dp, horizontal = 8.dp)
    ) {
        Image(
            bitmap = app.icon.toBitmap(128, 128).asImageBitmap(),
            contentDescription = app.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
        )

        Text(
            text = app.name,
            color = Color(0xDDFFFFFF),
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        )
    }
}
