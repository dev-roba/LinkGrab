package dev.personal.linkgrab.tools

import android.content.Intent
import android.app.PendingIntent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import dev.personal.linkgrab.ShareActivity

class ClipboardTileService : TileService() {
    override fun onClick() {
        super.onClick()
        val text = getSystemService(android.content.ClipboardManager::class.java).primaryClip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
        qsTile?.state = Tile.STATE_ACTIVE
        qsTile?.updateTile()
        if (text.isNotBlank()) {
            val intent = Intent(this, ShareActivity::class.java).apply {
                action = Intent.ACTION_SEND; type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text)
            }
            val pending = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            if (Build.VERSION.SDK_INT >= 34) startActivityAndCollapse(pending) else startActivity(intent)
        }
    }
}
