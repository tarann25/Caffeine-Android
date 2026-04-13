package com.example.caffeine

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

class TimeoutTileService : TileService() {

    // 15s, 30s, 1m, 5m, 10m
    private val timeouts = longArrayOf(15000, 30000, 60000, 300000, 600000)
    private val labels = arrayOf("15s", "30s", "1m", "5m", "10m")

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()

        // Check permission first
        if (!Settings.System.canWrite(this)) {
            Toast.makeText(this, "Permission required! Open app.", Toast.LENGTH_SHORT).show()
            
            // Open the app main activity
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("StartActivityAndCollapseDeprecated")
                startActivityAndCollapse(intent)
            }
            return
        }

        try {
            // Get current timeout
            val currentTimeout = try {
                Settings.System.getInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT)
            } catch (e: Settings.SettingNotFoundException) {
                60000 // default to 1m if something is wrong
            }

            // Find next timeout
            var nextIndex = 0
            
            // Loop to find match
            for (i in timeouts.indices) {
                if (currentTimeout.toLong() == timeouts[i]) {
                    nextIndex = i + 1
                    break
                }
            }

            // If we reached the end or didn't find it (custom value), start from 0
            if (nextIndex >= timeouts.size) {
                nextIndex = 0
            }

            val newTimeout = timeouts[nextIndex]

            // Set the new timeout
            Settings.System.putInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, newTimeout.toInt())

            // Immediately update tile to reflect change
            updateTile()
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error changing timeout", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateTile() {
        val tile = qsTile ?: return

        // Get current setting to update UI
        val currentTimeout = try {
            Settings.System.getInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT)
        } catch (e: Settings.SettingNotFoundException) {
            0
        }

        // Find label
        var label = "Caffeine"
        var displayText = "?"
        var state = Tile.STATE_INACTIVE

        var matchFound = false
        for (i in timeouts.indices) {
            if (currentTimeout.toLong() == timeouts[i]) {
                label = "Caffeine: ${labels[i]}" // Accessibility label
                displayText = labels[i]
                state = Tile.STATE_ACTIVE
                matchFound = true
                break
            }
        }
        
        if (!matchFound) {
             // If custom value, show generic or calculate
             displayText = "${currentTimeout / 1000}s"
             // Try to make it nicer if minutes
             if (currentTimeout >= 60000) {
                 displayText = "${currentTimeout / 60000}m"
             }
        }

        tile.state = state
        tile.label = label
        
        // Generate text icon
        tile.icon = createTextIcon(displayText)
        
        tile.updateTile()
    }

    private fun createTextIcon(text: String): Icon {
        val width = 120
        val height = 120
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        
        paint.color = Color.WHITE
        paint.textSize = 64f // Adjusted font size
        paint.textAlign = Paint.Align.CENTER
        paint.isAntiAlias = true
        
        // Center text vertically
        val xPos = (canvas.width / 2).toFloat()
        val yPos = (canvas.height / 2 - (paint.descent() + paint.ascent()) / 2)
        
        canvas.drawText(text, xPos, yPos, paint)
        return Icon.createWithBitmap(bitmap)
    }
}