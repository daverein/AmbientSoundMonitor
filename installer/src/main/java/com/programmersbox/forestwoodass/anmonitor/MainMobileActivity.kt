/*
 * Copyright 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.programmersbox.forestwoodass.anmonitor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityClient.OnCapabilityChangedListener
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import com.programmersbox.forestwoodass.anmonitor.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.tasks.await

/**
 * Checks if the sample's Wear app is installed on remote Wear device(s). If it is not, allows the
 * user to open the app listing on the Wear devices' Play Store.
 */
class MainMobileActivity : AppCompatActivity(), OnCapabilityChangedListener {

    private lateinit var binding: ActivityMainBinding

    private lateinit var capabilityClient: CapabilityClient
    private lateinit var nodeClient: NodeClient
    private lateinit var remoteActivityHelper: RemoteActivityHelper

    private var wearNodesWithApp: Set<Node>? = null
    private var allConnectedNodes: List<Node>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate()")
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        capabilityClient = Wearable.getCapabilityClient(this)
        nodeClient = Wearable.getNodeClient(this)
        remoteActivityHelper = RemoteActivityHelper(this)

        binding.remoteOpenButton.setOnClickListener {
            openPlayStoreOnWearDevicesWithoutApp()
        }

        // Perform the initial update of the UI
        updateUI()

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    // Initial request for devices with our capability, aka, our Wear app installed.
                    findWearDevicesWithApp()
                }
                launch {
                    // Initial request for all Wear devices connected (with or without our capability).
                    // Additional Note: Because there isn't a listener for ALL Nodes added/removed from network
                    // that isn't deprecated, we simply update the full list when the Google API Client is
                    // connected and when capability changes come through in the onCapabilityChanged() method.
                    findAllWearDevices()
                }
            }
        }
    }

    override fun onPause() {
        Log.d(TAG, "onPause()")
        super.onPause()
        capabilityClient.removeListener(this, CAPABILITY_WEAR_APP)
    }

    override fun onResume() {
        Log.d(TAG, "onResume()")
        super.onResume()
        capabilityClient.addListener(this, CAPABILITY_WEAR_APP)
    }

    /*
     * Updates UI when capabilities change (install/uninstall wear app).
     */
    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        Log.d(TAG, "onCapabilityChanged(): $capabilityInfo")
        wearNodesWithApp = capabilityInfo.nodes

        lifecycleScope.launch {
            // Because we have an updated list of devices with/without our app, we need to also update
            // our list of active Wear devices.
            findAllWearDevices()
        }
    }

    private suspend fun findWearDevicesWithApp() {
        Log.d(TAG, "findWearDevicesWithApp()")

        try {
            val capabilityInfo = capabilityClient
                .getCapability(CAPABILITY_WEAR_APP, CapabilityClient.FILTER_ALL)
                .await()


            withContext(Dispatchers.Main.immediate) {
                Log.d(TAG, "Capability request succeeded.")
                wearNodesWithApp = capabilityInfo.nodes
                Log.d(TAG, "Capable Nodes: $wearNodesWithApp")
                updateUI()
            }
        } catch (cancellationException: CancellationException) {
            // Request was cancelled normally
            throw cancellationException
        } catch (throwable: Throwable) {
            Log.d(TAG, "Capability request failed to return any results.")
        }
    }

    private suspend fun findAllWearDevices() {
        Log.d(TAG, "findAllWearDevices()")

        try {
            val connectedNodes = nodeClient.connectedNodes.await()

            withContext(Dispatchers.Main.immediate) {
                allConnectedNodes = connectedNodes
                updateUI()
            }
        } catch (cancellationException: CancellationException) {
            // Request was cancelled normally
        } catch (throwable: Throwable) {
            Log.d(TAG, "Node request failed to return any results.")
        }
    }

    private fun updateUI() {
        Log.d(TAG, "updateUI()")

        val wearNodesWithApp = wearNodesWithApp
        val allConnectedNodes = allConnectedNodes

        when {
            wearNodesWithApp == null || allConnectedNodes == null -> {
                Log.d(TAG, "Waiting on Results for both connected nodes and nodes with app")
                binding.informationTextView.text = getString(R.string.message_checking)
                binding.remoteOpenButton.isInvisible = true
            }
            allConnectedNodes.isEmpty() -> {
                Log.d(TAG, "No devices")
                binding.informationTextView.text = getString(R.string.message_checking)
                binding.remoteOpenButton.isInvisible = true
            }
            wearNodesWithApp.isEmpty() -> {
                Log.d(TAG, "Missing on all devices")
                binding.informationTextView.text = getString(R.string.message_missing_all)
                binding.remoteOpenButton.isVisible = true
            }
            wearNodesWithApp.size < allConnectedNodes.size -> {
                Log.d(TAG, "Installed on some devices")
                val devices = getNodeString(wearNodesWithApp)
                binding.informationTextView.text =
                    getString(R.string.message_some_installed, devices)
                binding.remoteOpenButton.isVisible = true
            }
            else -> {
                Log.d(TAG, "Installed on all devices")
                val devices = getNodeString(wearNodesWithApp)
                binding.informationTextView.text =
                    getString(R.string.message_all_installed, devices)
                binding.remoteOpenButton.isInvisible = true
            }
        }
    }

    private fun getNodeString(wearNodesWithApp: Set<Node>): String {
        var devices = ""

        for (node in wearNodesWithApp) {
            if (devices.isNotEmpty()) {
                devices += "\n\n"
            }
            devices += node.displayName
        }
        return devices
    }

    private fun openPlayStoreOnWearDevicesWithoutApp() {
        Log.d(TAG, "openPlayStoreOnWearDevicesWithoutApp()")

        val wearNodesWithApp = wearNodesWithApp ?: return
        val allConnectedNodes = allConnectedNodes ?: return

        // Determine the list of nodes (wear devices) that don't have the app installed yet.
        val nodesWithoutApp = allConnectedNodes - wearNodesWithApp

        Log.d(TAG, "Number of nodes without app: " + nodesWithoutApp.size)
        val intent = Intent(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setData(Uri.parse(PLAY_STORE_APP_URI))

        // In parallel, start remote activity requests for all wear devices that don't have the app installed yet.
        nodesWithoutApp.forEach { node ->
            lifecycleScope.launch {
                try {
                    remoteActivityHelper
                        .startRemoteActivity(
                            targetIntent = intent,
                            targetNodeId = node.id
                        )
                        .await()

                    Toast.makeText(
                        this@MainMobileActivity,
                        getString(R.string.store_request_successful),
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (cancellationException: CancellationException) {
                    // Request was cancelled normally
                } catch (throwable: Throwable) {
                    Toast.makeText(
                        this@MainMobileActivity,
                        getString(R.string.store_request_unsuccessful),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    companion object {
        private const val TAG = "MainMobileActivity"

        // Name of capability listed in Wear app's wear.xml.
        // IMPORTANT NOTE: This should be named differently than your Phone app's capability.
        private const val CAPABILITY_WEAR_APP = "verify_remote_com_programmersbox_forestwoodass_anmonitor_wear_app"

        // Links to Wear app (Play Store).
        private const val PLAY_STORE_APP_URI =
            "market://details?id=com.programmersbox.forestwoodass.anmonitor"
    }
}
