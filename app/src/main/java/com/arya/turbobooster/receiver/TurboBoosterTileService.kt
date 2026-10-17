package com.arya.turbobooster.receiver

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.arya.turbobooster.R
import com.arya.turbobooster.booster.TurboBoosterManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.N)
class TurboBoosterTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val isCurrentlyActive = TurboBoosterManager.isTurboActive(this)
        val newTargetState = !isCurrentlyActive

        val hasPrivilege = com.arya.turbobooster.booster.ShellExecutor.isShizukuAvailable() &&
                com.arya.turbobooster.booster.ShellExecutor.hasShizukuPermission()

        if (!hasPrivilege) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                android.widget.Toast.makeText(this, "Shizuku UID 2000 not active! Open Turbo Booster to authorize.", android.widget.Toast.LENGTH_LONG).show()
            }
            updateTileState()
            return
        }

        qsTile?.state = Tile.STATE_UNAVAILABLE
        qsTile?.updateTile()

        CoroutineScope(Dispatchers.IO).launch {
            if (newTargetState) {
                TurboBoosterManager.applySuperPerformanceMode(this@TurboBoosterTileService) {}
            } else {
                TurboBoosterManager.applyStockMode(this@TurboBoosterTileService) {}
            }
            updateTileState()
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val isActive = TurboBoosterManager.isTurboActive(this)
        tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (isActive) "Turbo: ON" else "Turbo: OFF"
        tile.updateTile()
    }
}
