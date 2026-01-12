package com.stickyapp.adblocker.ui

import android.app.Dialog
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SearchView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.stickyapp.adblocker.service.AppFilterManager

class AppSelectorDialog(private val context: Context) {
    
    private val appFilterManager = AppFilterManager(context)
    private lateinit var dialog: Dialog
    
    fun show(onAppsSelected: (Set<String>) -> Unit) {
        val builder = MaterialAlertDialogBuilder(context)
        
        // Create custom layout with search
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(16, 16, 16, 16)
        }
        
        // Search bar
        val searchView = SearchView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        layout.addView(searchView)
        
        // Apps list
        val recyclerView = RecyclerView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                500
            )
            layoutManager = LinearLayoutManager(context)
        }
        layout.addView(recyclerView)
        
        // Get installed apps
        val apps = getInstalledApps().sortedBy { it.name }
        val selectedApps = appFilterManager.getSelectedApps().toMutableSet()
        
        val adapter = AppListAdapter(apps, selectedApps) { packageName, isSelected ->
            if (isSelected) {
                selectedApps.add(packageName)
            } else {
                selectedApps.remove(packageName)
            }
        }
        recyclerView.adapter = adapter
        
        // Search functionality
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = true
            
            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter(newText ?: "")
                return true
            }
        })
        
        builder.setView(layout)
            .setTitle("Select Apps to Filter")
            .setPositiveButton("OK") { _, _ ->
                appFilterManager.clearAllApps()
                selectedApps.forEach { appFilterManager.addSelectedApp(it) }
                onAppsSelected(selectedApps)
            }
            .setNegativeButton("Cancel", null)
        
        dialog = builder.create()
        dialog.show()
    }

    private fun getInstalledApps(): List<AppInfo> {
        val pm = context.packageManager
        val apps = mutableListOf<AppInfo>()
        
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PackageManager.GET_META_DATA
        } else {
            PackageManager.GET_META_DATA
        }
        
        try {
            val installedApps = pm.getInstalledApplications(flags)
            installedApps.forEach { appInfo ->
                // Skip system apps unless they are selected
                if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 ||
                    appFilterManager.isAppSelected(appInfo.packageName)
                ) {
                    apps.add(
                        AppInfo(
                            name = pm.getApplicationLabel(appInfo).toString(),
                            packageName = appInfo.packageName,
                            isSelected = appFilterManager.isAppSelected(appInfo.packageName)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return apps
    }
    
    data class AppInfo(
        val name: String,
        val packageName: String,
        var isSelected: Boolean = false
    )
}

class AppListAdapter(
    private val apps: List<AppSelectorDialog.AppInfo>,
    private val selectedApps: MutableSet<String>,
    private val onAppToggled: (String, Boolean) -> Unit
) : RecyclerView.Adapter<AppListAdapter.ViewHolder>() {
    
    private var filteredApps = apps
    
    inner class ViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val checkbox: CheckBox = itemView.findViewById(android.R.id.checkbox)
        private val appName: TextView = itemView.findViewById(android.R.id.text1)
        private val packageName: TextView = itemView.findViewById(android.R.id.text2)
        
        fun bind(app: AppSelectorDialog.AppInfo) {
            appName.text = app.name
            packageName.text = app.packageName
            checkbox.isChecked = selectedApps.contains(app.packageName)
            
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                onAppToggled(app.packageName, isChecked)
            }
            
            itemView.setOnClickListener {
                checkbox.toggle()
            }
        }
    }
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val layout = android.widget.LinearLayout(parent.context).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = android.widget.LinearLayout.HORIZONTAL
            setPadding(16, 16, 16, 16)
        }
        
        val checkbox = CheckBox(parent.context).apply {
            id = android.R.id.checkbox
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        layout.addView(checkbox)
        
        val textLayout = android.widget.LinearLayout(parent.context).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                0,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(16, 0, 0, 0)
        }
        
        val appNameText = TextView(parent.context).apply {
            id = android.R.id.text1
            textSize = 14f
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        textLayout.addView(appNameText)
        
        val packageNameText = TextView(parent.context).apply {
            id = android.R.id.text2
            textSize = 12f
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        textLayout.addView(packageNameText)
        
        layout.addView(textLayout)
        
        return ViewHolder(layout)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(filteredApps[position])
    }
    
    override fun getItemCount() = filteredApps.size
    
    fun filter(query: String) {
        filteredApps = if (query.isEmpty()) {
            apps
        } else {
            apps.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }
}
