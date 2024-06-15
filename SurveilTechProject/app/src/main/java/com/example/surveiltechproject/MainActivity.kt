package com.example.surveiltechproject

import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ImageSpan
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.FileReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var textViewIPAddress: TextView
    private lateinit var textViewScanResults: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textViewIPAddress = findViewById(R.id.textViewIPAddress)
        textViewScanResults = findViewById(R.id.textViewScanResults)

        val ipAddress = getDeviceIPAddress()
        textViewIPAddress.text = "IP Address: $ipAddress"

        if (ipAddress != "IP address not found") {
            CoroutineScope(Dispatchers.Main).launch {
                val scanResults = withContext(Dispatchers.IO) { scanNetwork(ipAddress) }
                setClickableText(textViewScanResults, extractIPAddress(scanResults))
            }
        }
    }

    private fun extractIPAddress(scanResults: List<String>): List<String> {
        val ipAddresses = mutableListOf<String>()
        val ipRegex = Regex("IP: (\\d+\\.\\d+\\.\\d+\\.\\d+)")
        for (result in scanResults) {
            val matchResult = ipRegex.find(result)
            if (matchResult != null) {
                val ipAddress = matchResult.groupValues[1]
                ipAddresses.add(ipAddress)
            }
        }
        return ipAddresses
    }
    private fun setClickableText(textView: TextView, scanResults: List<String>) {
        val spannableString = SpannableStringBuilder()
        val lines = scanResults

        for (line in lines) {
            val spannableLine = SpannableString("$line\n")
            spannableLine.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    val intent = Intent(this@MainActivity, AffichageAppareil::class.java)
                    intent.putExtra("ipAddress", line)
                    startActivity(intent)
                }
            }, 0, spannableLine.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            spannableString.append(spannableLine)
        }

        textView.text = spannableString
        textView.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun getDeviceIPAddress(): String {
        try {
            val interfaces: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface: NetworkInterface = interfaces.nextElement()
                val addresses: Enumeration<InetAddress> = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr: InetAddress = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "IP address not found"
    }

    private suspend fun scanNetwork(localIp: String): List<String> = withContext(Dispatchers.IO) {
        val ipParts = localIp.split(".")
        val subnet = "${ipParts[0]}.${ipParts[1]}.${ipParts[2]}."

        val activeHosts = Collections.synchronizedList(mutableListOf<String>())
        val jobs = mutableListOf<Job>()

        for (i in 1..254) {
            val host = "$subnet$i"
            jobs.add(launch {
                if (isHostReachable(host, 1000)) {
                    val details = getHostDetails(host)
                    activeHosts.add(details)
                }
            })
        }

        jobs.forEach { it.join() }

        // Adding ARP scan results
        val arpResults = getArpDetails()
        activeHosts.addAll(arpResults)

        return@withContext activeHosts
    }

    private fun isHostReachable(host: String, timeout: Int = 1000): Boolean {
        return try {
            val address = InetAddress.getByName(host)
            address.isReachable(timeout)
        } catch (e: Exception) {
            false
        }
    }

    private fun getHostDetails(ip: String): String {
        val macAddress = getMacAddress(ip)
        val hostName = try {
            InetAddress.getByName(ip).hostName
        } catch (e: Exception) {
            "Unknown"
        }
        return "IP: $ip, MAC: $macAddress, Hostname: $hostName"
    }

    private fun getMacAddress(ip: String): String {
        try {
            val br = BufferedReader(FileReader("/proc/net/arp"))
            var line: String?
            while (br.readLine().also { line = it } != null) {
                val splitted = line!!.split("\\s+".toRegex()).toTypedArray()
                if (splitted[0] == ip) {
                    br.close()
                    return splitted[3]
                }
            }
            br.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "MAC address not found"
    }

    private fun getArpDetails(): List<String> {
        val arpList = mutableListOf<String>()
        try {
            val process = Runtime.getRuntime().exec("cat /proc/net/arp")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line!!.contains("IP address")) continue
                val splitted = line!!.split("\\s+".toRegex()).toTypedArray()
                if (splitted.size >= 4) {
                    val ip = splitted[0]
                    val mac = splitted[3]
                    arpList.add("IP: $ip, MAC: $mac, Hostname: Unknown")
                }
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return arpList
    }
}
