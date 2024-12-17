package com.example.contacts_test

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import android.telecom.TelecomManager
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.contacts_test.model.Contact
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var menuButton: ImageButton
    private lateinit var navigationView: NavigationView
    private lateinit var recyclerView: RecyclerView
    private lateinit var contactAdapter: ContactAdapter
    private lateinit var searchIcon: ImageView
    private lateinit var searchInput: EditText
    private lateinit var contactsTitle: TextView
    private lateinit var fabEdit: View

    private val PERMISSIONS_REQUEST_READ_CONTACTS = 100
    private val PERMISSIONS_REQUEST_WRITE_CONTACTS = 102
    private val PERMISSIONS_REQUEST_CALL_PHONE = 101

    companion object {
        // Define a function to refresh contacts
        fun refreshContacts(activity: MainActivity) {
            activity.displayContacts() // Reuse the displayContacts method
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView)
        searchIcon = findViewById(R.id.search_icon)
        searchInput = findViewById(R.id.search_input)
        contactsTitle = findViewById(R.id.contacts_title)
        fabEdit = findViewById(R.id.fab_edit)
        drawerLayout = findViewById(R.id.drawerLayout)
        menuButton = findViewById(R.id.btn_drawer)
        navigationView = findViewById(R.id.drawer_menu)

        recyclerView.layoutManager = LinearLayoutManager(this)

        refreshContacts(this)

        // Drawer menu button click
        menuButton.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        // Navigation drawer item click
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navMenu -> Toast.makeText(this, "Menu clicked", Toast.LENGTH_SHORT).show()
                R.id.menuCart -> Toast.makeText(this, "Cart clicked", Toast.LENGTH_SHORT).show()
                R.id.settings -> Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // Start the permission check process
        checkPermissions()

        // Floating Action Button click to add/edit contact
        fabEdit.setOnClickListener {
            val intent = Intent(this, AddContactActivity::class.java)
            startActivity(intent)
        }

        // Search functionality
        searchIcon.setOnClickListener { toggleSearchInput()}
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterContacts(s.toString())
            }
        })
    }

    override fun onResume() {
        super.onResume()
        // Refresh contacts whenever the activity comes to the foreground
        refreshContacts(this)
    }

    // Handle permissions request result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_READ_CONTACTS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Once READ_CONTACTS is granted, proceed to the next permission
                    checkCallPermission()
                } else {
                    Toast.makeText(this, "Permission denied! Cannot display contacts.", Toast.LENGTH_SHORT).show()
                }
            }
            PERMISSIONS_REQUEST_CALL_PHONE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Once CALL_PHONE is granted, proceed to the next permission
                    checkWriteContactPermission()
                } else {
                    Toast.makeText(this, "Call permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            PERMISSIONS_REQUEST_WRITE_CONTACTS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // If WRITE_CONTACTS is granted, display contacts
                    displayContacts()
                } else {
                    Toast.makeText(this, "Permission denied! Cannot add contacts.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Check and request READ_CONTACTS permission
    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                PERMISSIONS_REQUEST_READ_CONTACTS
            )
        } else {
            // If READ_CONTACTS is already granted, proceed with the next permission
            checkCallPermission()
        }
    }

    // Check and request CALL_PHONE permission
    private fun checkCallPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CALL_PHONE),
                PERMISSIONS_REQUEST_CALL_PHONE
            )
        } else {
            // If CALL_PHONE is already granted, proceed with the next permission
            checkWriteContactPermission()
        }
    }

    // Check and request WRITE_CONTACTS permission
    private fun checkWriteContactPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_CONTACTS),
                PERMISSIONS_REQUEST_WRITE_CONTACTS
            )
        } else {
            // If all permissions are granted, display the contacts
            displayContacts()
        }
    }

    // Display contacts after permission is granted
    private fun displayContacts() {
        val contacts = fetchContacts()
        if (contacts.isNotEmpty()) {
            val sortedContacts = contacts.sortedBy { it.name }
            contactAdapter = ContactAdapter(this, sortedContacts)
            recyclerView.adapter = contactAdapter
        } else {
            Toast.makeText(this, "No contacts found!", Toast.LENGTH_SHORT).show()
        }
    }

    // Fetch contacts from the device
    private fun fetchContacts(): List<Contact> {
        val contactsList = mutableListOf<Contact>()
        val contactMap = mutableMapOf<String, Contact>() // Store unique contacts based on ID

        val cursor: Cursor? = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            null, null, null, null
        )

        cursor?.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                val name = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))
                val hasPhoneNumber = c.getInt(c.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER))

                if (hasPhoneNumber > 0) {
                    val phoneCursor: Cursor? = contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                        arrayOf(id), null
                    )

                    phoneCursor?.use { pCursor ->
                        while (pCursor.moveToNext()) {
                            val phoneNumber = pCursor.getString(
                                pCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            )
                            val photoUri = pCursor.getString(
                                pCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Photo.PHOTO_URI)
                            )

                            // Add unique contacts to map
                            if (!contactMap.containsKey(id)) {
                                contactMap[id] = Contact(id, name ?: "Unknown", phoneNumber ?: "No Number", photoUri)
                            }
                        }
                    }
                }
            }
        }

        contactsList.addAll(contactMap.values)
        return contactsList
    }

    // Toggle search input visibility
    private fun toggleSearchInput() {
        contactAdapter.notifyDataSetChanged()
        if (searchInput.visibility == View.GONE) {
            contactsTitle.visibility = View.GONE
            searchInput.visibility = View.VISIBLE
            searchInput.requestFocus()
        } else {
            contactsTitle.visibility = View.VISIBLE
            searchInput.visibility = View.GONE
        }
    }

    // Filter contacts based on search query
    private fun filterContacts(query: String) {
        val filteredContacts = contactAdapter.contactsList.filter {
            it.name.contains(query, ignoreCase = true)
        }
        displayContacts()
        contactAdapter.updateContacts(filteredContacts)

    }
}
