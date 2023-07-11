package com.lapreuvenumerique.wifimanager

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.os.Bundle
import android.text.InputType
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.thanosfisherman.wifiutils.WifiUtils
import com.thanosfisherman.wifiutils.wifiConnect.ConnectionErrorCode
import com.thanosfisherman.wifiutils.wifiConnect.ConnectionSuccessListener

class MainActivity : AppCompatActivity() {

    private val PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 1

    private lateinit var wifiNetworkAdapter: WifiNetworkAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)
        recyclerView = findViewById(R.id.wifi_recycler_view)

        recyclerView.layoutManager = LinearLayoutManager(this)

        swipeRefreshLayout.setOnRefreshListener {
            scanForWifiNetworks()
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION)
        } else {
            scanForWifiNetworks()

            val savedSSID = getSelectedWifiSSID()
            if(savedSSID != null) {
                val savedPassword = getWifiPassword(savedSSID)
                if(savedPassword != null) {
                    connectToWifi(savedSSID, savedPassword)
                }
            }
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    scanForWifiNetworks()
                } else {
                    Toast.makeText(this, "Permission denied to access location", Toast.LENGTH_SHORT).show()
                }
                return
            }
            else -> {
            }
        }
    }

    private fun scanForWifiNetworks() {
        swipeRefreshLayout.isRefreshing = true
        WifiUtils.withContext(applicationContext)
            .scanWifi { wifiScanResults ->
                wifiNetworkAdapter = WifiNetworkAdapter(wifiScanResults) { wifiNetwork ->
                    showPasswordDialog(wifiNetwork)
                }
                recyclerView.adapter = wifiNetworkAdapter
                swipeRefreshLayout.isRefreshing = false
            }.start()
    }
    fun setSelectedWifiSSID(ssid: String) {
        val sharedPreferences = getSharedPreferences("Wifi_Credentials", Context.MODE_PRIVATE)
        with (sharedPreferences.edit()) {
            putString("SELECTED_SSID", ssid)
            apply()
        }
    }

    fun getSelectedWifiSSID(): String? {
        val sharedPreferences = getSharedPreferences("Wifi_Credentials", Context.MODE_PRIVATE)
        return sharedPreferences.getString("SELECTED_SSID", null)
    }

    private fun showPasswordDialog(wifiNetwork: ScanResult) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enter password")

        val view = layoutInflater.inflate(R.layout.dialog_password, null)
        val input = view.findViewById<EditText>(R.id.et_password)
        val showPassword = view.findViewById<CheckBox>(R.id.cb_show_password)

        showPassword.setOnCheckedChangeListener { _, isChecked ->
            val cursorPosition = input.selectionStart
            input.inputType = if (isChecked)
                InputType.TYPE_CLASS_TEXT
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            input.setSelection(cursorPosition)
        }

        input.requestFocus()
        builder.setView(view)

        builder.setPositiveButton("OK") { dialog, _ ->
            val password = input.text.toString()
            connectToWifi(wifiNetwork.SSID, password)
            saveWifiCredentials(wifiNetwork.SSID, password)
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        val dialog = builder.show()

        // Show the keyboard
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun saveWifiCredentials(ssid: String, password: String) {
        val sharedPreferences = getSharedPreferences("Wifi_Credentials", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(ssid, password)
        editor.apply()
    }

    private fun getWifiPassword(ssid: String): String? {
        val sharedPreferences = getSharedPreferences("Wifi_Credentials", Context.MODE_PRIVATE)
        return sharedPreferences.getString(ssid, null)
    }

    private fun connectToWifi(ssid: String, password: String) {
        WifiUtils.withContext(applicationContext)
            .connectWith(ssid, password)
            .setTimeout(40000)
            .onConnectionResult(object : ConnectionSuccessListener {
                override fun success() {
                    Toast.makeText(this@MainActivity, "Successfully connected to $ssid", Toast.LENGTH_SHORT).show()
                    setSelectedWifiSSID(ssid)
                    wifiNetworkAdapter.notifyDataSetChanged() // to refresh the color of the list items
                }

                override fun failed(errorCode: ConnectionErrorCode) {
                    Toast.makeText(this@MainActivity, "Failed to connect to $ssid", Toast.LENGTH_SHORT).show()
                }
            })
            .start()
    }
}
