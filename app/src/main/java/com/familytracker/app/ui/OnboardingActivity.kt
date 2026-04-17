package com.familytracker.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.familytracker.app.R
import com.familytracker.app.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class OnboardingActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (auth.currentUser != null) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_onboarding)

        val nameInput = findViewById<EditText>(R.id.etName)
        val ageInput = findViewById<EditText>(R.id.etAge)
        val genderSpinner = findViewById<Spinner>(R.id.spinnerGender)
        val relationSpinner = findViewById<Spinner>(R.id.spinnerRelation)
        val emailInput = findViewById<EditText>(R.id.etEmail)
        val passwordInput = findViewById<EditText>(R.id.etPassword)
        val groupCodeInput = findViewById<EditText>(R.id.etGroupCode)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        ArrayAdapter.createFromResource(this, R.array.gender_array,
            android.R.layout.simple_spinner_item).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            genderSpinner.adapter = it
        }

        ArrayAdapter.createFromResource(this, R.array.relation_array,
            android.R.layout.simple_spinner_item).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            relationSpinner.adapter = it
        }

        btnRegister.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val name = nameInput.text.toString().trim()
            val age = ageInput.text.toString().toIntOrNull() ?: 0
            val gender = genderSpinner.selectedItem.toString()
            val relation = relationSpinner.selectedItem.toString()
            val groupCode = groupCodeInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val uid = result.user!!.uid
                    // if group code entered, join that group, else create new group
                    val groupId = if (groupCode.isNotEmpty()) groupCode.trim().uppercase() else (100000..999999).random().toString()
                    val user = User(uid, name, gender, relation, age, groupId = groupId)
                    db.collection("users").document(uid).set(user)
                        .addOnSuccessListener {
                            startActivity(Intent(this, HomeActivity::class.java))
                            finish()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                }
        }
    }
}
