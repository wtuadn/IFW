package com.wtuadn.ifw

import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Gravity
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.TextView
import org.jetbrains.anko.*
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.jetbrains.anko.sdk25.listeners.onClick
import org.jetbrains.anko.sdk25.listeners.onQueryTextListener
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.*

class ComponentActivity : AppCompatActivity() {
    private lateinit var appInfo: AppInfo
    private lateinit var ifwFilePath: String
    private var serviceList: List<String>? = null
    private var broadcastList: List<String>? = null
    private lateinit var searchView: SearchView
    private lateinit var recyclerView: RecyclerView
    private lateinit var myAdapter: MyAdapter
    private var isService = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appInfo = intent.getParcelableExtra("AppInfo")
        serviceList = appInfo.services?.map { it.name }
        broadcastList = appInfo.receivers?.map { it.name }
        if (broadcastList == null) isService = false
        ifwFilePath = "${MainActivity.ifwFolder}/${appInfo.pkgName}.xml"

        verticalLayout {
            toolbar {
                backgroundResource = R.color.colorPrimary
                title = appInfo.name
                val allListener = MenuItem.OnMenuItemClickListener {
                    if (isService) appInfo.disabledServices.clear() else appInfo.disabledReceivers.clear()
                    if (it.itemId == 1) for (s in myAdapter.list) {
                        handleIfwRule(s, true)
                    }
                    handleIfwFile()
                    myAdapter.notifyDataSetChanged()
                    true
                }
                val enableAll = menu.add(0, 0, 0, "o all")
                enableAll.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                enableAll.setOnMenuItemClickListener(allListener)
                val disableAll = menu.add(0, 1, 0, "x all")
                disableAll.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                disableAll.setOnMenuItemClickListener(allListener)
                if (serviceList != null && broadcastList != null) {
                    val switchItem = menu.add("service")
                    switchItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                    switchItem.setOnMenuItemClickListener {
                        isService = !isService
                        it.title = if (isService) "service" else "broadcast"
                        onTextChange("")
                        true
                    }
                }
            }
            searchView = searchView {
                post { clearFocus() }
                onActionViewExpanded()
                onQueryTextListener {
                    onQueryTextChange { s ->
                        onTextChange(s)
                        true
                    }
                }
            }
            recyclerView = recyclerView {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(ctx)
                myAdapter = MyAdapter(ArrayList(serviceList))
                adapter = myAdapter
            }.lparams(matchParent, matchParent)
        }
    }

    private fun onTextChange(s: String?) {
        if (s.isNullOrEmpty()) {
            myAdapter.list.clear()
            myAdapter.list.addAll(if (isService) serviceList!! else broadcastList!!)
            myAdapter.notifyDataSetChanged()
            recyclerView.scrollToPosition(0)
            searchView.setQuery("", false)
        } else {
            myAdapter.list.filter {
                it.toLowerCase().contains(s!!.toString().toLowerCase())
            }.let {
                myAdapter.list.clear()
                myAdapter.list.addAll(it)
                myAdapter.notifyDataSetChanged()
                recyclerView.scrollToPosition(0)
            }
        }
    }

    private fun handleIfwRule(name: String, isAdd: Boolean) {
        val disabledList = if (isService) appInfo.disabledServices else appInfo.disabledReceivers
        val element = "${appInfo.pkgName}/$name"
        if (isAdd) {
            disabledList.add(element)
        } else {
            disabledList.removeIf { it == element }
        }
    }

    private fun handleIfwFile() {
        val sb = StringBuilder("<rules>\n")
        if (appInfo.disabledServices.isNotEmpty()) {
            sb.append("<service block=\"true\" log=\"false\">\n")
            for (name in appInfo.disabledServices) sb.append("<component-filter name=\"$name\"/>\n")
            sb.append("</service>\n")
        }
        if (appInfo.disabledReceivers.isNotEmpty()) {
            sb.append("<broadcast block=\"true\" log=\"false\">\n")
            for (name in appInfo.disabledReceivers) sb.append("<component-filter name=\"$name\"/>\n")
            sb.append("</broadcast>\n")
        }
        sb.append("</rules>")
        val ifwFile = File(ifwFilePath)
        val ifwFolder = ifwFile.parentFile
        if (!ifwFolder.exists()) ifwFolder.mkdirs()
        val writer = BufferedWriter(FileWriter(ifwFile))
        writer.write(sb.toString())
        writer.close()

        val systemFile = "/data/system/ifw/${ifwFile.name}"
        val process = Runtime.getRuntime().exec("su")
        process.outputStream.write("cp -f $ifwFilePath $systemFile\n".toByteArray())
        process.outputStream.write("chmod 644 $systemFile\n".toByteArray())
        process.outputStream.flush()

        MainActivity.appInfoLiveData.value = appInfo
    }

    private inner class MyAdapter(val list: ArrayList<String>) : RecyclerView.Adapter<Holder>() {
        override fun getItemCount(): Int {
            return list.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            return Holder(TextView(parent.context))
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val disabledList = if (isService) appInfo.disabledServices else appInfo.disabledReceivers
            holder.textView.text = list[position]
            holder.textView.textColor = if (disabledList.find { it.contains(list[position]) } != null) Color.RED else 0xff3f3f3f.toInt()
        }
    }

    private inner class Holder(val textView: TextView) : RecyclerView.ViewHolder(textView) {
        init {
            textView.apply {
                textSize = 16f
                includeFontPadding = false
                gravity = Gravity.CENTER_VERTICAL
                padding = dip(8)
                backgroundDrawable = LineDrawable(Color.LTGRAY, dip(1).toFloat())
                minHeight = dip(50)
                layoutParams = RecyclerView.LayoutParams(matchParent, wrapContent).apply {
                    horizontalMargin = dip(8.5f)
                }
            }.onClick {
                val name = myAdapter.list[adapterPosition]
                handleIfwRule(name, textView.currentTextColor != Color.RED)
                handleIfwFile()
                myAdapter.notifyItemChanged(adapterPosition)
            }
        }
    }
}
