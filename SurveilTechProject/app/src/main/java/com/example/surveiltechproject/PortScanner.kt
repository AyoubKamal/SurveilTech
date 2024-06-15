package com.example.surveiltechproject

import android.os.AsyncTask
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

class PortScanner(private val ipAddress: String, private val timeout: Int = 1000, private val listener: (List<Int>) -> Unit) :
    AsyncTask<Void, Int, List<Int>>() {

    override fun doInBackground(vararg params: Void?): List<Int> {
        val openPorts = mutableListOf<Int>()
        val maxPort = 65535
        for (port in 1..maxPort) {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(ipAddress, port), timeout)
                socket.close()
                openPorts.add(port)
            } catch (e: IOException) {
                // Le port est fermé ou la connexion a échoué
            }
        }
        return openPorts
    }

    override fun onPostExecute(result: List<Int>) {
        super.onPostExecute(result)
        listener(result)
    }
}