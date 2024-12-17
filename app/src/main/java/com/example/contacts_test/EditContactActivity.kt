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

class EditContactActivity : AppCompatActivity() {

    private lateinit var nameInput: EditText
    private lateinit var phoneInput: EditText
    private lateinit var saveButton: Button
    private lateinit var profileImageView: ImageView
    private var contactId: String? = null
    private var rawContactId: String? = null
    private var profileImageUri: Uri? = null

    companion object {
        const val REQUEST_IMAGE_PICK = 1001
        const val TAG = "EditContactActivity"
        const val PERMISSION_REQUEST_CODE = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_contact)

        nameInput = findViewById(R.id.name_input)
        phoneInput = findViewById(R.id.phone_input)
        saveButton = findViewById(R.id.save_button)
        profileImageView = findViewById(R.id.edit_profile_image)

        // Retrieve contact data from intent (if editing)
        contactId = intent.getStringExtra("CONTACT_ID")
        val contactName = intent.getStringExtra("CONTACT_NAME")
        val contactNumber = intent.getStringExtra("CONTACT_NUMBER")
        val contactPhotoUri = intent.getStringExtra("CONTACT_PHOTO_URI")

        // Set initial values if editing
        if (contactName != null) nameInput.setText(contactName)
        if (contactNumber != null) phoneInput.setText(contactNumber)
        contactPhotoUri?.let {
            profileImageUri = Uri.parse(it)
            profileImageView.setImageURI(profileImageUri)
        }

        // Fetch the raw contact ID for updating
        rawContactId = getRawContactId(contactId)

        // Allow user to select a new profile image
        profileImageView.setOnClickListener { showImageOptionsDialog() }

        // Save or update contact on button click
        saveButton.setOnClickListener { saveContact() }

        // Check for permissions to read and write contacts
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS),
                PERMISSION_REQUEST_CODE)
        }
    }

    private fun showImageOptionsDialog() {
        val options = arrayListOf("Choose New Image", "Proceed Without Selecting")
        if (profileImageUri != null) {
            options.add(0, "Remove Image")
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Profile Picture Options")
        builder.setItems(options.toTypedArray()) { dialog, which ->
            when (options[which]) {
                "Remove Image" -> {
                    profileImageUri = null
                    profileImageView.setImageResource(R.drawable.baseline_account_circle_24)
                    Toast.makeText(this, "Image removed", Toast.LENGTH_SHORT).show()
                }
                "Choose New Image" -> pickImageFromGallery()
                "Proceed Without Selecting" -> dialog.dismiss()
            }
        }
        builder.show()
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK) {
            profileImageUri = data?.data
            profileImageView.setImageURI(profileImageUri)
        }
    }

    private fun saveContact() {
        val newName = nameInput.text.toString().trim()
        val newPhone = phoneInput.text.toString().trim()

        if (newName.isEmpty() || newPhone.isEmpty()) {
            Toast.makeText(this, "Name and phone number cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        if (rawContactId.isNullOrEmpty()) {
            Toast.makeText(this, "Unable to edit contact. Raw contact ID is missing!", Toast.LENGTH_SHORT).show()
        } else {
            updateExistingContact(newName, newPhone)
        }
    }

    private fun getRawContactId(contactId: String?): String? {
        var rawContactId: String? = null
        if (contactId != null) {
            val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId)
            val cursor = contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(ContactsContract.Data.RAW_CONTACT_ID),
                "${ContactsContract.Data.CONTACT_ID} = ?",
                arrayOf(contactId),
                null
            )
            if (cursor != null && cursor.moveToFirst()) {
                rawContactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.RAW_CONTACT_ID))
                cursor.close()
            }
        }
        return rawContactId
    }

    private fun updateExistingContact(newName: String, newPhone: String) {
        val ops = ArrayList<ContentProviderOperation>()

        // Update name
        ops.add(
            ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                .withSelection(
                    "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                    arrayOf(rawContactId, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                )
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, newName)
                .build()
        )

        // Update phone number
        ops.add(
            ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                .withSelection(
                    "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                    arrayOf(rawContactId, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                )
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, newPhone)
                .build()
        )

        // Update or add profile picture
        if (profileImageUri != null) {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, profileImageUri)
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                val photoBytes = stream.toByteArray()

                // Check if contact already has a photo
                val photoSelection = "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?"
                val photoSelectionArgs = arrayOf(rawContactId, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)

                val cursor = contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    arrayOf(ContactsContract.Data._ID),
                    photoSelection,
                    photoSelectionArgs,
                    null
                )

                if (cursor != null && cursor.moveToFirst()) {
                    val photoId = cursor.getLong(cursor.getColumnIndex(ContactsContract.Data._ID))
                    ops.add(
                        ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                            .withSelection("${ContactsContract.Data._ID} = ?", arrayOf(photoId.toString()))
                            .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, photoBytes)
                            .build()
                    )
                    cursor.close()
                } else {
                    ops.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, photoBytes)
                            .build()
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating photo", e)
                Toast.makeText(this, "Failed to update photo: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        try {
            contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            Toast.makeText(this, "Contact updated successfully", Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update contact", e)
            Toast.makeText(this, "Failed to update contact: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
