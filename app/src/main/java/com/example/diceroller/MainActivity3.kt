package com.example.diceroller

import android.app.Activity
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.LocaleList
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.util.*

class MainActivity3 : AppCompatActivity() {

    var auth: FirebaseAuth = Firebase.auth
    var uid = ""

    val storage = Firebase.storage

    var storageRef = storage.reference

    private val PICK_IMAGE_REQUEST = 71

    private var filePath: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadLocale()
        setContentView(R.layout.activity_main3)

        auth = FirebaseAuth.getInstance()

        val upload: Button = findViewById(R.id.button5)
        val welcomeTextView: TextView = findViewById(R.id.textView4)
        val changeLangButton: Button = findViewById(R.id.button6)
        welcomeTextView.setText(R.string.textViewWelcome)
        changeLangButton.setText(R.string.change_language)

        // Upload button logic to open image storage

        upload.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(
                Intent.createChooser(intent, "Select Picture"),
                PICK_IMAGE_REQUEST
            )
        }

        changeLangButton.setOnClickListener {
            val languages = arrayOf("English", "Afrikaans")
            val langSelectorBuilder = AlertDialog.Builder(this@MainActivity3)
            langSelectorBuilder.setTitle("Choose language:")
            langSelectorBuilder.setSingleChoiceItems(languages, -1) { dialog, selection ->
                when (selection) {
                    0 -> {
                        setLocale("en")
                    }
                    1 -> {
                        setLocale("af")
                    }
                }
                recreate()
                dialog.dismiss()
            }
            langSelectorBuilder.create().show()
        }
    }

    // Allows user to store any image or file to firebase storage

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data == null || data.data == null) {
                return
            }
            filePath = data.data
            if (filePath != null) {
                val ref = storageRef?.child("Images/" + UUID.randomUUID().toString())
                val uploadTask = ref?.putFile(filePath!!)
            }
//            try {
//                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, filePath)
////                imagePreview.setImageBitmap(bitmap)
//            } catch (e: IOException) {
//                e.printStackTrace()
//            }


        }
    }

    override fun onStart() {
        super.onStart()
        var currentUser = auth!!.currentUser

        if (currentUser != null) {
            Log.d(ContentValues.TAG, currentUser.displayName.toString())
        }

        val signUp = findViewById<Button>(R.id.button4)
        val username = findViewById<EditText>(R.id.editTextTextEmailAddress)
        val password = findViewById<EditText>(R.id.editTextTextPassword2)

        val regButton: Button = findViewById(R.id.button4)
        regButton.setOnClickListener { reg() }

    }

    // UpdateUI in Firbase

    private fun updateUI(currentUser: FirebaseUser?) {
        if (currentUser != null) {
            Log.d(ContentValues.TAG, currentUser.displayName.toString())
            val intent = Intent(this, MainActivity2::class.java)
            intent.putExtra("user", currentUser.displayName.toString())
            startActivity(intent)
        }
    }

    // Create your account and store it in Firebase

    private fun createAccount(email: String, password: String, name: String) {
        auth!!.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this)

        { task ->
            if (task.isSuccessful) {
                println(task.getResult().getUser())
                uid = task.getResult().getUser()?.getUid().toString();

                val db = Firebase.firestore

                val user = hashMapOf(
                    "Email" to email,
                    "Name" to name,
                    "Password" to password,
                    "User Uid" to uid
                )

                db.collection("Users").document(uid)
                    .set(user)
                    .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully written!") }
                    .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }

                db.collection("users")
                    .get()
                    .addOnSuccessListener { result ->
                        for (document in result) {
                            Log.d(TAG, "${document.id} => ${document.data}")
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.w(TAG, "Error getting documents.", exception)
                    }
                updateUI(task.getResult().getUser())
            } else {
                updateUI(null)
            }
        }
    }

    private fun setLocale(localeToSet: String) {
        val localeListToSet = LocaleList(Locale(localeToSet))
        LocaleList.setDefault(localeListToSet)
        resources.configuration.setLocales(localeListToSet)
        resources.updateConfiguration(resources.configuration, resources.displayMetrics)
        val sharedPref = getSharedPreferences("Settings", Context.MODE_PRIVATE).edit()
        sharedPref.putString("locale_to_set", localeToSet)
        sharedPref.apply()
    }

    private fun loadLocale() {
        val sharedPref = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val localeToSet: String = sharedPref.getString("locale_to_set", "")!!
        setLocale(localeToSet)
    }

    private fun reg() {

        val password: TextView = findViewById(R.id.editTextTextPassword2)
        val password1: TextView = findViewById(R.id.editTextTextPassword3)
        val name: TextView = findViewById(R.id.editTextTextPersonName2)
        val email: TextView = findViewById(R.id.editTextTextEmailAddress)
        val password_Test = password.getText().toString().trim()
        val confirmpassword = password1.getText().toString().trim()
        val name1 = name.getText().toString().trim()
        val email1 = email.getText().toString().trim()
        val nameEmail = email.getText().toString().trim()

        // Checks to see if fields are empty and runs createAccount method

        if (!password_Test.isNullOrEmpty() || !confirmpassword.isNullOrEmpty() || !name1.isNullOrEmpty() || !email1.isNullOrEmpty()) {

            if (password_Test == confirmpassword) {
                println("Password does match")
                createAccount(email1, password_Test, name1)
            } else {
                println("Password does not match")
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Alert")
                builder.setMessage("Passwords do not match ! ")
                builder.setPositiveButton(android.R.string.yes) { dialog, which ->
                    Toast.makeText(
                        applicationContext,
                        android.R.string.yes, Toast.LENGTH_SHORT
                    ).show()
                }
                builder.show()
            }
        } else {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Alert")
            builder.setMessage("Please Make Sure All Fields Are Filled in !")
            builder.setPositiveButton(android.R.string.yes)
            { dialog, which ->
                Toast.makeText(
                    applicationContext,
                    android.R.string.yes, Toast.LENGTH_SHORT
                ).show()
            }
            builder.show()
        }
    }
}