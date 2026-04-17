package com.familytracker.app.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.familytracker.app.R
import com.familytracker.app.model.User
import com.familytracker.app.service.LocationService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val userList = mutableListOf<User>()
    private lateinit var adapter: UserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        requestLocationPermission()
        handleIncomingIntent(intent)

        val recyclerView = findViewById<RecyclerView>(R.id.rvUsers)

        adapter = UserAdapter(userList) { user ->
            val i = Intent(this, MapActivity::class.java)
            i.putExtra("uid", user.uid)
            i.putExtra("name", user.name)
            startActivity(i)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadFamilyMembers()
        listenForLocationRequests()
        listenForViewingNotifications()

        findViewById<ImageButton>(R.id.btnProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnShareApp)
            .setOnClickListener { shareApp() }
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
        } else {
            startLocationService()
        }
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<String>, results: IntArray) {
        super.onRequestPermissionsResult(code, perms, results)
        if (code == 100 && results.isNotEmpty() && results[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationService()
        }
    }

    private fun startLocationService() {
        val intent = Intent(this, LocationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun loadFamilyMembers() {
        val currentUid = auth.currentUser?.uid ?: return
        db.collection("users").document(currentUid).get()
            .addOnSuccessListener { doc ->
                val groupId = doc.getString("groupId") ?: return@addOnSuccessListener
                findViewById<TextView>(R.id.tvGroupCode).text = "Group code: $groupId"
                db.collection("users")
                    .whereEqualTo("groupId", groupId)
                    .addSnapshotListener { snapshot, _ ->
                        userList.clear()
                        snapshot?.documents?.forEach { d ->
                            val user = d.toObject(User::class.java)
                            if (user != null && user.uid != currentUid) userList.add(user)
                        }
                        adapter.notifyDataSetChanged()
                    }
            }
    }

    private fun listenForLocationRequests() {
        val currentUid = auth.currentUser?.uid ?: return
        db.collection("locationRequests")
            .whereEqualTo("toUid", currentUid)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, _ ->
                snapshot?.documents?.forEach { doc ->
                    val fromUid = doc.getString("fromUid") ?: return@forEach
                    val requestId = doc.id
                    db.collection("users").document(fromUid).get()
                        .addOnSuccessListener { userDoc ->
                            val name = userDoc.getString("name") ?: "Someone"
                            showPermissionDialog(name, fromUid, requestId)
                        }
                }
            }
    }

    private fun listenForViewingNotifications() {
        val currentUid = auth.currentUser?.uid ?: return
        db.collection("locationRequests")
            .whereEqualTo("toUid", currentUid)
            .whereEqualTo("status", "allowed")
            .addSnapshotListener { snapshot, _ ->
                snapshot?.documentChanges?.forEach { change ->
                    if (change.type == DocumentChange.Type.ADDED ||
                        change.type == DocumentChange.Type.MODIFIED) {
                        val fromUid = change.document.getString("fromUid") ?: return@forEach
                        db.collection("users").document(fromUid).get()
                            .addOnSuccessListener { doc ->
                                val name = doc.getString("name") ?: "Someone"
                                showViewingNotification(name)
                            }
                    }
                }
            }
    }

    private fun showViewingNotification(name: String) {
        val channelId = "viewing_channel"
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(channelId, "Viewing Alerts",
                NotificationManager.IMPORTANCE_DEFAULT)
        )
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("📍 Location Viewed")
            .setContentText("$name is currently viewing your location")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setAutoCancel(true)
            .build()
        manager.notify(name.hashCode(), notification)
    }

    private fun showPermissionDialog(name: String, fromUid: String, requestId: String) {
        AlertDialog.Builder(this)
            .setTitle("Location Request")
            .setMessage("$name wants to view your location. Allow?")
            .setPositiveButton("Allow") { _, _ ->
                db.collection("locationRequests").document(requestId)
                    .update("status", "allowed")
            }
            .setNegativeButton("Deny") { _, _ ->
                db.collection("locationRequests").document(requestId)
                    .update("status", "denied")
            }
            .show()
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val action = intent?.getStringExtra("action") ?: return
        val requestId = intent.getStringExtra("requestId") ?: return
        val status = if (action == "allow") "allowed" else "denied"
        db.collection("locationRequests").document(requestId).update("status", status)
    }

    private fun shareApp() {
        val i = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT,
                "Join me on FamilyTracker! Download: https://play.google.com/store/apps/details?id=com.familytracker.app")
        }
        startActivity(Intent.createChooser(i, "Invite Family Member"))
    }
}

class UserAdapter(
    private val users: List<User>,
    private val onTrack: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: TextView = view.findViewById(R.id.tvAvatar)
        val name: TextView = view.findViewById(R.id.tvName)
        val relation: TextView = view.findViewById(R.id.tvRelation)
        val btnTrack: com.google.android.material.button.MaterialButton =
            view.findViewById(R.id.btnTrack)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val user = users[position]
        holder.name.text = user.name
        holder.relation.text = user.relation
        holder.avatar.text = user.name.take(2).uppercase()
        holder.btnTrack.setOnClickListener { onTrack(user) }
    }

    override fun getItemCount() = users.size
}
