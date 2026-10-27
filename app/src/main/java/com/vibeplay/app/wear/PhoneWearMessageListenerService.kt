package com.vibeplay.app.wear

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.vibeplay.core.wear.WearSyncProtocol

class PhoneWearMessageListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == WearSyncProtocol.MESSAGE_PATH_COMMAND) {
            val command = String(messageEvent.data, Charsets.UTF_8)
            PhoneWearBridgeEvents.dispatchCommand(command)
        }
    }
}
