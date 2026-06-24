package com.ucar.launcher

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private data class UcarApp(
        val name: String,
        val packageName: String,
        val className: String?,
        val icon: Drawable
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 沉浸式状态栏
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            window.insetsController?.systemBarsBehavior =
                android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }

        val gridView = findViewById<GridView>(R.id.app_grid)
        val emptyText = findViewById<TextView>(R.id.empty_text)

        val apps = queryUcarApps()

        if (apps.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            gridView.visibility = View.GONE
            return
        }

        emptyText.visibility = View.GONE
        gridView.visibility = View.VISIBLE

        gridView.adapter = AppAdapter(apps)
        gridView.setOnItemClickListener { _, _, position, _ ->
            launchUcarApp(apps[position])
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

            // 优先用具体组件名启动
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

    private inner class AppAdapter(private val apps: List<UcarApp>) : BaseAdapter() {

        override fun getCount(): Int = apps.size

        override fun getItem(position: Int): UcarApp = apps[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(this@MainActivity)
                .inflate(R.layout.item_app, parent, false)

            val app = apps[position]

            view.findViewById<ImageView>(R.id.app_icon).setImageDrawable(app.icon)
            view.findViewById<TextView>(R.id.app_name).text = app.name

            return view
        }
    }
}
