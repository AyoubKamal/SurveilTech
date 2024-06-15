package com.example.surveiltechproject

import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView

import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.surveiltechproject.databinding.AffichageAppareilBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.UnknownHostException

class AffichageAppareil : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: AffichageAppareilBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.affichage_appareil)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val listViewPorts = findViewById<ListView>(R.id.listViewPorts)
        val ipAddress = intent.getStringExtra("ipAddress")
        val textViewIPAddress = findViewById<TextView>(R.id.textViewIPAddress)
        textViewIPAddress.text = "IP Address: $ipAddress"
        scanPorts(ipAddress.toString()) { openPorts ->
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, openPorts.map { "Port $it" })
            listViewPorts.adapter = adapter
        }




    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun scanPorts(ipAddress: String, timeout: Int = 1000, listener: (List<Int>) -> Unit) {
        PortScanner(ipAddress, timeout, listener).execute()
    }
    private suspend fun getHostName(ipAddress: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val inetAddress = InetAddress.getByName(ipAddress)
                inetAddress.hostName
            } catch (e: UnknownHostException) {
                "Unknown"
            }
        }
    }




}

