package com.familytracker.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.familytracker.app.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val uid = auth.currentUser?.uid ?: return
        val email = auth.currentUser?.email ?: ""

        val tvAvatar = findViewById<TextView>(R.id.tvProfileAvatar)
        val tvName = findViewById<TextView>(R.id.tvProfileName)
        val tvEmail = findViewById<TextView>(R.id.tvProfileEmail)
        val etName = findViewById<TextInputEditText>(R.id.etEditName)
        val etAge = findViewById<TextInputEditText>(R.id.etEditAge)
        val btnSave = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSaveProfile)
        val btnSignOut = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSignOut)
        val switchLocation = findViewById<Switch>(R.id.switchLocation)

        tvEmail.text = email

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: ""
                val age = doc.getLong("age")?.toString() ?: ""
                val locationEnabled = doc.getBoolean("locationSharingEnabled") ?: true

                tvName.text = name
                tvAvatar.text = name.take(2).uppercase()
                etName.setText(name)
                etAge.setText(age)
                switchLocation.isChecked = locationEnabled
            }

        switchLocation.setOnCheckedChangeListener { _, isChecked ->
            db.collection("users").document(uid)
                .update("locationSharingEnabled", isChecked)
            if (!isChecked) {
                stopService(Intent(this, com.familytracker.app.service.LocationService::class.java))
                Toast.makeText(this, "Location sharing disabled", Toast.LENGTH_SHORT).show()
            } else {
                val i = Intent(this, com.familytracker.app.service.LocationService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(i)
                } else startService(i)
                Toast.makeText(this, "Location sharing enabled", Toast.LENGTH_SHORT).show()
            }
        }

        btnSave.setOnClickListener {
            val newName = etName.text.toString().trim()
            val newAge = etAge.text.toString().toIntOrNull() ?: 0
            if (newName.isEmpty()) {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            db.collection("users").document(uid)
                .update(mapOf("name" to newName, "age" to newAge))
                .addOnSuccessListener {
                    tvName.text = newName
                    tvAvatar.text = newName.take(2).uppercase()
                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
                }
        }

        btnSignOut.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, OnboardingActivity::class.java))
            finishAffinity()
        }
    }
}
