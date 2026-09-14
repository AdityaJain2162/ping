package com.aditya.ping.ui

import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.util.Log
import com.aditya.ping.data.AutomationRepository
import com.aditya.ping.util.AutomationExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Transparent activity that handles NFC tag scans and triggers automations.
 * Trigger type: 5=nfc_tag
 */
class NfcTriggerActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleNfcIntent(intent)
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNfcIntent(intent)
        finish()
    }

    private fun handleNfcIntent(intent: Intent) {
        val tagId = when (intent.action) {
            NfcAdapter.ACTION_NDEF_DISCOVERED,
            NfcAdapter.ACTION_TECH_DISCOVERED,
            NfcAdapter.ACTION_TAG_DISCOVERED -> {
                val tag = intent.getParcelableExtra<android.nfc.Tag>(NfcAdapter.EXTRA_TAG)
                tag?.id?.joinToString(":") { "%02X".format(it) } ?: ""
            }
            else -> return
        }

        Log.d("NfcTrigger", "Tag ID: $tagId")

        CoroutineScope(Dispatchers.IO).launch {
            val repo = AutomationRepository.from(this@NfcTriggerActivity)
            val automations = repo.getByTriggerType(5)
            automations.forEach { automation ->
                if (automation.triggerData.isBlank() || automation.triggerData.equals(tagId, ignoreCase = true)) {
                    AutomationExecutor.execute(this@NfcTriggerActivity, automation)
                }
            }
        }
    }
}
