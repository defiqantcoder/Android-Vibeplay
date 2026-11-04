package com.vibeplay.wear

import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService

class WearPlaybackListenerService : WearableListenerService() {
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        WearSyncManager.getInstance(applicationContext).onDataChanged(dataEvents)
    }
}
