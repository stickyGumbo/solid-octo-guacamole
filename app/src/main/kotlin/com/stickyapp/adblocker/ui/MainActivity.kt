package com.stickyapp.adblocker.ui

import android.app.PowerManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.stickyapp.adblocker.R
import com.stickyapp.adblocker.blocklist.BlocklistRepository
import com.stickyapp.adblocker.service.AppFilterManager
import com.stickyapp.adblocker.service.VpnAdBlockService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var vpnStatusText: TextView
    private lateinit var vpnToggleButton: Button
    private lateinit var selectAppsButton: Button
    private lateinit var refreshBlocklistsButton: Button
    private lateinit var selectedAppsList: RecyclerView
    private lateinit var appFilterManager: AppFilterManager
    private lateinit var blocklistRepository: BlocklistRepository
    private var isVpnRunning = false
    private val scope = CoroutineScope(Dispatchers.Main)
    
    companion object {
        private const val BATTERY_OPTIMIZATION_REQUEST = 1001
        private const val REQUEST_VPN_PERMISSION = 1002
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        appFilterManager = AppFilterManager(this)
        blocklistRepository = BlocklistRepository(this)
        
        initializeViews()
        setupListeners()
        updateUI()
        requestBatteryOptimizationException()
    }
    
    private fun initializeViews() {
        vpnStatusText = findViewById(R.id.vpn_status)
        vpnToggleButton = findViewById(R.id.vpn_toggle_button)
        selectAppsButton = findViewById(R.id.select_apps_button)
        refreshBlocklistsButton = findViewById(R.id.refresh_blocklists_button)
        selectedAppsList = findViewById(R.id.selected_apps_list)
        
        selectedAppsList.layoutManager = LinearLayoutManager(this)
    }
    
    private fun setupListeners() {
        vpnToggleButton.setOnClickListener {
            if (isVpnRunning) {
                stopVpn()
            } else {
                requestVpnPermission()
            }
        }
        
        selectAppsButton.setOnClickListener {
            showAppSelector()
        }
        
        refreshBlocklistsButton.setOnClickListener {
            refreshBlocklists()
        }
    }
    
    private fun requestVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            startActivityForResult(intent, REQUEST_VPN_PERMISSION)
        } else {
            startVpn()
        }
    }
    
    private fun startVpn() {
        val vpnIntent = Intent(this, VpnAdBlockService::class.java).apply {
            action = "com.stickyapp.adblocker.START_VPN"
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(vpnIntent)
        } else {
            startService(vpnIntent)
        }
        
        isVpnRunning = true
        updateUI()
    }
    
    private fun stopVpn() {
        val vpnIntent = Intent(this, VpnAdBlockService::class.java).apply {
            action = "com.stickyapp.adblocker.STOP_VPN"
        }
        startService(vpnIntent)
        
        isVpnRunning = false
        updateUI()
    }
    
    private fun showAppSelector() {
        AppSelectorDialog(this).show { selectedApps ->
            updateUI()
        }
    }
    
    private fun refreshBlocklists() {
        scope.launch {
            blocklistRepository.refreshRemoteBlocklists()
            updateUI()
        }
    }
    
    private fun requestBatteryOptimizationException() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }
        
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.battery_optimization_title)
                .setMessage(R.string.battery_optimization_message)
                .setPositiveButton("OK") { _, _ ->
                    val intent = Intent(Intent.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = android.net.Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                }
                .setNegativeButton("Skip", null)
                .show()
        }
    }
    
    private fun updateUI() {
        vpnStatusText.text = if (isVpnRunning) {
            getString(R.string.vpn_status_on)
        } else {
            getString(R.string.vpn_status_off)
        }
        
        vpnToggleButton.text = if (isVpnRunning) {
            getString(R.string.stop_vpn)
        } else {
            getString(R.string.start_vpn)
        }
        
        val selectedApps = appFilterManager.getSelectedApps().toList()
        val adapter = SelectedAppsAdapter(selectedApps) { packageName ->
            appFilterManager.removeSelectedApp(packageName)
            updateUI()
        }
        selectedAppsList.adapter = adapter
    }
    
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_VPN_PERMISSION -> {
                if (resultCode == RESULT_OK) {
                    startVpn()
                }
            }
        }
    }
}

class SelectedAppsAdapter(
    private val apps: List<String>,
    private val onRemove: (String) -> Unit
) : RecyclerView.Adapter<SelectedAppsAdapter.ViewHolder>() {
    
    inner class ViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val packageNameText: TextView = itemView.findViewById(android.R.id.text1)
        private val removeButton: Button = itemView.findViewById(android.R.id.button1)
        
        fun bind(packageName: String) {
            packageNameText.text = packageName
            removeButton.setOnClickListener {
                onRemove(packageName)
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
            setPadding(16, 8, 16, 8)
        }
        
        val packageNameText = TextView(parent.context).apply {
            id = android.R.id.text1
            textSize = 14f
            layoutParams = android.widget.LinearLayout.LayoutParams(
                0,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
        layout.addView(packageNameText)
        
        val removeButton = Button(parent.context).apply {
            id = android.R.id.button1
            text = "Remove"
            textSize = 12f
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        layout.addView(removeButton)
        
        return ViewHolder(layout)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(apps[position])
    }
    
    override fun getItemCount() = apps.size
}
