package com.lapreuvenumerique.wifimanager

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat

class WifiNetworkAdapter(private val wifiNetworks: List<ScanResult>, private val onClick: (ScanResult) -> Unit) :
    RecyclerView.Adapter<WifiNetworkAdapter.WifiNetworkViewHolder>() {

    class WifiNetworkViewHolder(view: View, onClick: (Int) -> Unit) : RecyclerView.ViewHolder(view) {
        private val ssid: TextView = view.findViewById(R.id.tv_ssid)
        private val signalStrength: ImageView = view.findViewById(R.id.iv_signal_strength)


        init {
            view.setOnClickListener { onClick(adapterPosition) }
        }

        fun bind(wifiNetwork: ScanResult) {
            ssid.text = wifiNetwork.SSID;

            val signalStrengthPercentage = WifiManager.calculateSignalLevel(wifiNetwork.level, 100)

            val signalStrengthIcon = when {
                signalStrengthPercentage > 75 -> R.drawable.ic_signal_strength_4
                signalStrengthPercentage > 50 -> R.drawable.ic_signal_strength_3
                signalStrengthPercentage > 25 -> R.drawable.ic_signal_strength_2
                else -> R.drawable.ic_signal_strength_1
            }

            signalStrength.setImageResource(signalStrengthIcon)
            val wifiLayout: LinearLayout = itemView.findViewById(R.id.wifi_layout)

            if (wifiNetwork.SSID == (itemView.context as MainActivity).getSelectedWifiSSID()) {
                wifiLayout.setBackgroundResource(R.drawable.rounded_background_selected)
                ssid.setTextColor(Color.WHITE)
            } else {
                wifiLayout.setBackgroundResource(R.drawable.rounded_background)
                ssid.setTextColor(Color.BLACK)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WifiNetworkViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_wifi_network, parent, false)
        return WifiNetworkViewHolder(view) { position -> onClick(wifiNetworks[position]) }
    }

    override fun onBindViewHolder(holder: WifiNetworkViewHolder, position: Int) {
        holder.bind(wifiNetworks[position])
    }

    override fun getItemCount() = wifiNetworks.size
}
