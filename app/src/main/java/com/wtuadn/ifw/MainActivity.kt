package com.wtuadn.ifw

import android.Manifest
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.jetbrains.anko.*
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.jetbrains.anko.sdk25.listeners.onClick
import org.jetbrains.anko.sdk25.listeners.onQueryTextListener
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {
    companion object {
        @JvmField val ifwFolder = "${Environment.getExternalStorageDirectory().absolutePath}/ifw"
        @JvmField val appInfoLiveData = MutableLiveData<AppInfo>()
    }

    private val infoList by lazy(LazyThreadSafetyMode.NONE) {
        val pm = packageManager
        val pkgList = pm.getInstalledPackages(PackageManager.GET_SERVICES or PackageManager.GET_RECEIVERS)
                .filterNot {
                    (ApplicationInfo.FLAG_SYSTEM and it.applicationInfo.flags) != 0
                            || (it.services == null && it.receivers == null)
                }
        val list = ArrayList<AppInfo>(pkgList.size)
        for (p in pkgList) {
            val applicationInfo = p.applicationInfo
            val appInfo = AppInfo(applicationInfo.loadLabel(pm).toString(),
                    p.packageName,
                    applicationInfo.loadIcon(pm).apply { setBounds(0, 0, dip(35), dip(35)) },
                    p.receivers,
                    p.services)
            val ifwFilePath = "$ifwFolder/${p.packageName}.xml"
            val ifwFile = File(ifwFilePath)
            if (ifwFile.exists()) {
                var isService = false
                val bufferedReader = BufferedReader(InputStreamReader(FileInputStream(ifwFile)))
                var line: String?
                while (true) {
                    line = bufferedReader.readLine()?.trim()
                    if (line == null) break
                    if (line!!.startsWith("<service")) {
                        isService = true
                        continue
                    }
                    if (line!!.startsWith("<broadcast")) {
                        isService = false
                        continue
                    }
                    if (line!!.startsWith("<component-filter")) {
                        val startIndex = line!!.indexOf("\"") + 1
                        val endIndex = line!!.indexOf("\"", startIndex + 1)
                        val element = line!!.substring(startIndex, endIndex)
                        if (isService) appInfo.disabledServices.add(element)
                        else appInfo.disabledReceivers.add(element)
                    }
                }
                bufferedReader.close()
            }
            list.add(appInfo)
        }
        list
    }
    private lateinit var recyclerView: RecyclerView
    private lateinit var myAdapter: MyAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()
        appInfoLiveData.observe(this, Observer {
            if (it == null) return@Observer
            val i = myAdapter.list.indexOf(it)
            if (i >= 0) {
                myAdapter.list[i].disabledReceivers = it.disabledReceivers
                myAdapter.list[i].disabledServices = it.disabledServices
                myAdapter.notifyItemChanged(i)
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        appInfoLiveData.value = null
    }

    private fun ui() {
        verticalLayout {
            searchView {
                post { clearFocus() }
                onActionViewExpanded()
                onQueryTextListener {
                    onQueryTextChange { s ->
                        if (s.isNullOrEmpty()) {
                            myAdapter.list.clear()
                            myAdapter.list.addAll(infoList)
                            myAdapter.notifyDataSetChanged()
                            recyclerView.scrollToPosition(0)
                        } else {
                            infoList.filter {
                                it.name.toLowerCase().contains(s!!.toString().toLowerCase())
                            }.let {
                                myAdapter.list.clear()
                                myAdapter.list.addAll(it)
                                myAdapter.notifyDataSetChanged()
                                recyclerView.scrollToPosition(0)
                            }
                        }
                        true
                    }
                }
            }
            recyclerView = recyclerView {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(ctx)
                myAdapter = MyAdapter(ArrayList(infoList))
                adapter = myAdapter
                horizontalPadding = dip(8.5f)
            }.lparams(matchParent, matchParent)
        }
    }

    private fun checkPermissions() {
        val process = Runtime.getRuntime().exec("su")
        PermissionUtils.checkPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            if (PermissionUtils.isAllGranted(it)) {
                val file = File(ifwFolder)
                if (file.exists() && file.listFiles()?.isNotEmpty() == true) {
                    process.outputStream.write("rm -rf /data/system/ifw/*\n".toByteArray())
                    process.outputStream.write("cp -rf $ifwFolder /data/system\n".toByteArray())
                    process.outputStream.write("chmod -R 644 /data/system/ifw\n".toByteArray())
                    process.outputStream.flush()
                }
                ui()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionUtils.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private inner class MyAdapter(val list: ArrayList<AppInfo>) : RecyclerView.Adapter<Holder>() {
        override fun getItemCount(): Int {
            return list.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            return Holder(_RelativeLayout(parent.context))
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val info = list[position]
            holder.tvName.text = info.name
            holder.tvName.setCompoundDrawables(info.icon, null, null, null)
            var ss = SpannableString("S:${info.disabledServices.size}/${info.services?.size ?: 0}")
            ss.setSpan(ForegroundColorSpan(Color.RED), 2, ss.indexOf("/"), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            holder.tvService.text = ss
            ss = SpannableString("R:${info.disabledReceivers.size}/${info.receivers?.size ?: 0}")
            ss.setSpan(ForegroundColorSpan(Color.RED), 2, ss.indexOf("/"), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            holder.tvReceiver.text = ss
        }
    }

    private inner class Holder(itemView: _RelativeLayout) : RecyclerView.ViewHolder(itemView) {
        lateinit var tvName: TextView
        lateinit var tvService: TextView
        lateinit var tvReceiver: TextView

        init {
            itemView.apply {
                backgroundDrawable = LineDrawable(Color.LTGRAY, dip(1).toFloat())
                lparams(matchParent, wrapContent)
                tvReceiver = textView {
                    id = View.generateViewId()
                    textSize = 16f
                    textColor = 0xff3f3f3f.toInt()
                    includeFontPadding = false
                    gravity = Gravity.CENTER_VERTICAL
                }.lparams(wrapContent, dip(50)) {
                    alignParentRight()
                }
                tvService = textView {
                    id = View.generateViewId()
                    textSize = 16f
                    textColor = 0xff3f3f3f.toInt()
                    includeFontPadding = false
                    gravity = Gravity.CENTER_VERTICAL
                }.lparams(wrapContent, dip(50)) {
                    leftOf(tvReceiver)
                    rightMargin = dip(10)
                }
                tvName = textView {
                    textSize = 17f
                    textColor = 0xff3f3f3f.toInt()
                    includeFontPadding = false
                    gravity = Gravity.CENTER_VERTICAL
                    compoundDrawablePadding = dip(10)
                    horizontalPadding = dip(10)
                }.lparams(matchParent, dip(50)) {
                    leftOf(tvService)
                }
            }.onClick {
                startActivity<ComponentActivity>("AppInfo" to myAdapter.list[adapterPosition])
            }
        }
    }
}