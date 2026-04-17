package com.familytracker.app.ui

import android.graphics.*
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.familytracker.app.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MapActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var targetUid = ""
    private var targetName = ""
    private var myMarker: Marker? = null
    private var theirMarker: Marker? = null
    private var hasInitiallyZoomed = false

    private val colors = listOf(
        Color.parseColor("#1E88E5"), // blue - me
        Color.parseColor("#E53935"), // red - them
        Color.parseColor("#43A047"), // green
        Color.parseColor("#FB8C00"), // orange
        Color.parseColor("#8E24AA")  // purple
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = packageName
        setContentView(R.layout.activity_map)

        targetUid = intent.getStringExtra("uid") ?: ""
        targetName = intent.getStringExtra("name") ?: ""

        findViewById<TextView>(R.id.tvMapTitle).text = "${targetName}'s Location"

        map = findViewById(R.id.mapView)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.isTilesScaledToDpi = true
        map.controller.setZoom(15.0)

        trackMyLocation()
        trackOtherLocation()
    }

    private fun createInitialsBitmap(initials: String, color: Int): android.graphics.drawable.BitmapDrawable {
        val size = 96
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, circlePaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 3, borderPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            textSize = 32f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        val textY = size / 2f - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(initials, size / 2f, textY, textPaint)

        return android.graphics.drawable.BitmapDrawable(resources, bitmap)
    }

    private fun getInitials(name: String): String {
        val parts = name.trim().split(" ")
        return if (parts.size >= 2) "${parts[0][0]}${parts[1][0]}".uppercase()
        else name.take(2).uppercase()
    }

    private fun trackMyLocation() {
        val currentUid = auth.currentUser?.uid ?: return
        db.collection("users").document(currentUid)
            .addSnapshotListener { snapshot, _ ->
                val lat = snapshot?.getDouble("latitude") ?: return@addSnapshotListener
                val lng = snapshot?.getDouble("longitude") ?: return@addSnapshotListener
                val name = snapshot.getString("name") ?: "Me"
                if (lat == 0.0 && lng == 0.0) return@addSnapshotListener

                val point = GeoPoint(lat, lng)
                if (myMarker == null) {
                    myMarker = Marker(map).apply {
                        title = "You"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        map.overlays.add(this)
                    }
                }
                myMarker?.position = point
                myMarker?.icon = createInitialsBitmap(getInitials(name), colors[0])

                if (!hasInitiallyZoomed) {
                    map.controller.setZoom(15.0)
                    map.controller.setCenter(point)
                    hasInitiallyZoomed = true
                }
                map.invalidate()
            }
    }

    private fun trackOtherLocation() {
        db.collection("users").document(targetUid)
            .addSnapshotListener { snapshot, _ ->
                val locationEnabled = snapshot?.getBoolean("locationSharingEnabled") ?: true
                if (!locationEnabled) {
                    Toast.makeText(this, "$targetName has turned off location sharing",
                        Toast.LENGTH_LONG).show()
                    theirMarker?.let { map.overlays.remove(it) }
                    theirMarker = null
                    map.invalidate()
                    return@addSnapshotListener
                }

                val lat = snapshot?.getDouble("latitude") ?: return@addSnapshotListener
                val lng = snapshot?.getDouble("longitude") ?: return@addSnapshotListener
                if (lat == 0.0 && lng == 0.0) return@addSnapshotListener

                val point = GeoPoint(lat, lng)
                if (theirMarker == null) {
                    theirMarker = Marker(map).apply {
                        title = targetName
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        map.overlays.add(this)
                    }
                }
                theirMarker?.position = point
                theirMarker?.icon = createInitialsBitmap(getInitials(targetName), colors[1])
                map.invalidate()
            }
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}
