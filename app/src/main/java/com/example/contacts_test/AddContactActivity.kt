package com.example.contacts_test

import android.Manifest
import android.app.Activity
import android.content.ContentProviderOperation
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream

class AddContactActivity : AppCompatActivity() {



    private lateinit var nameEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var profileImageView: ImageView
    private lateinit var saveButton: Button
    private var selectedImageUri: Uri? = null

    companion object {
        const val REQUEST_WRITE_CONTACTS = 1
        const val REQUEST_SELECT_IMAGE = 2
        const val TAG = "AddContactApp"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_contact)

        nameEditText = findViewById(R.id.nameEditText)
        phoneEditText = findViewById(R.id.phoneEditText)
        profileImageView = findViewById(R.id.profileImageView)
        saveButton = findViewById(R.id.saveButton)

        profileImageView.setOnClickListener {
            showImageOptionsDialog()
        }

        saveButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val phone = phoneEditText.text.toString().trim()

            if (name.isNotEmpty() && phone.isNotEmpty()) {
                if (checkPermission()) {
                    try {
                        addContact(name, phone, selectedImageUri)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error adding contact", e)
                        Toast.makeText(this, "Failed to save contact", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    requestPermission()
                }
            } else {
                Toast.makeText(this, "Please enter both name and phone number", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_CONTACTS), REQUEST_WRITE_CONTACTS)
    }

    private fun showImageOptionsDialog() {
        val options = arrayListOf("Choose New Image", "Proceed Without Selecting")
        if (selectedImageUri != null) {
            options.add(0, "Remove Image")
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Profile Picture Options")
        builder.setItems(options.toTypedArray()) { dialog, which ->
            when (options[which]) {
                "Remove Image" -> {
                    selectedImageUri = null
                    profileImageView.setImageResource(R.drawable.baseline_account_circle_24)
                    Toast.makeText(this, "Image removed", Toast.LENGTH_SHORT).show()
                }
                "Choose New Image" -> openImagePicker()
                "Proceed Without Selecting" -> dialog.dismiss()
            }
        }
        builder.show()
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_SELECT_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SELECT_IMAGE && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            profileImageView.setImageURI(selectedImageUri)
        }
    }

    private fun addContact(name: String, phone: String, imageUri: Uri?) {
        val operations = ArrayList<ContentProviderOperation>()

        operations.add(
            ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build()
        )

        operations.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                .build()
        )

        operations.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build()
        )

        if (imageUri != null) {
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val photoBytes = stream.toByteArray()

            operations.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, photoBytes)
                    .build()
            )
        }

        contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
        Toast.makeText(this, "Contact saved successfully!", Toast.LENGTH_SHORT).show()

        finish()
    }
}
