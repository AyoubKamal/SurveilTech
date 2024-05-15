package com.example.surveiltechproject

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
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
                textViewScanResults.text = scanResults.joinToString("\n")
            }
        }
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
