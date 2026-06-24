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
        val apps = mutableListOf<UcarApp>()
        val seen = mutableSetOf<String>()

        // 方式1：queryIntentActivities 全局查询
        val intent = Intent("com.ucar.intent.action.UCAR").apply {
            addCategory("com.ucar.intent.category.UCAR")
        }
        val resolveInfos = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        for (info in resolveInfos) {
            val pkg = info.activityInfo.packageName
            if (pkg != packageName && !seen.contains(pkg)) {
                seen.add(pkg)
                apps.add(UcarApp(
                    name = info.loadLabel(packageManager).toString(),
                    packageName = pkg,
                    className = info.activityInfo.name,
                    icon = info.loadIcon(packageManager)
                ))
            }
        }

        // 方式2：逐个检查已知包名
        val knownPackages = listOf(
            "com.autonavi.minimap",
            "com.baidu.BaiduMap",
            "com.tencent.qqmusic",
            "com.ximalaya.ting.android",
            "com.tencent.map",
            "com.kugou.android.lite",
            "cn.kuwo.kwmusichd",
            "com.tencent.qqmusicpad",
            "com.luna.music",
            "com.netease.cloudmusic",
            "com.kugou.android",
            "cmccwm.mobilemusic",
            "com.yueme.itv",
        )

        for (pkg in knownPackages) {
            if (seen.contains(pkg)) continue
            try {
                val checkIntent = Intent("com.ucar.intent.action.UCAR").apply {
                    addCategory("com.ucar.intent.category.UCAR")
                    setPackage(pkg)
                }
                var infos = packageManager.queryIntentActivities(checkIntent, PackageManager.MATCH_ALL)

                if (infos.isEmpty()) {
                    checkIntent.removeCategory("com.ucar.intent.category.UCAR")
                    checkIntent.addCategory("com.ucar.intent.category.MAP_PREVIEW")
                    infos = packageManager.queryIntentActivities(checkIntent, PackageManager.MATCH_ALL)
                }

                if (infos.isNotEmpty()) {
                    seen.add(pkg)
                    apps.add(UcarApp(
                        name = infos[0].loadLabel(packageManager).toString(),
                        packageName = pkg,
                        className = infos[0].activityInfo.name,
                        icon = infos[0].loadIcon(packageManager)
                    ))
                }
            } catch (_: Exception) {}
        }

        return apps.sortedBy { it.name }
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
