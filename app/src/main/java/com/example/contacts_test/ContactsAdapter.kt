package com.example.contacts_test

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.contacts_test.model.Contact
import com.squareup.picasso.Picasso

class ContactAdapter(
    private val context: Context,
    private var contacts: List<Contact>
) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    internal var contactsList: List<Contact> = contacts
        private set


    private val mainActivity = context as MainActivity

    inner class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val photoImageView: ImageView = itemView.findViewById(R.id.contact_image)
        val nameTextView: TextView = itemView.findViewById(R.id.contact_name)
        val phoneNumberTextView: TextView = itemView.findViewById(R.id.contact_phone)
        val callButton: ImageButton = itemView.findViewById(R.id.callbutton)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deletebutton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.contact_item, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contacts[position]

        holder.nameTextView.text = contact.name
        holder.phoneNumberTextView.text = contact.phoneNumber

        // Load photo using Picasso or set default
        if (!contact.photoUri.isNullOrEmpty()) {
            Picasso.get().load(contact.photoUri).into(holder.photoImageView)
        } else {
            holder.photoImageView.setImageResource(R.drawable.baseline_account_circle_24)
        }

        // Handle call button click
        holder.callButton.setOnClickListener {
            val callIntent = Intent(Intent.ACTION_CALL)
            callIntent.data = Uri.parse("tel:${contact.phoneNumber}")

            // Check if the CALL_PHONE permission is granted
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                context.startActivity(callIntent)
            } else {
                ActivityCompat.requestPermissions(
                    context as Activity,
                    arrayOf(Manifest.permission.CALL_PHONE),
                    1
                )
            }
        }

        // Handle delete button click
        holder.deleteButton.setOnClickListener {
            val popupView = LayoutInflater.from(context).inflate(R.layout.popup_delete_contact, null)
            val popupWindow = PopupWindow(
                popupView,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true
            )
            popupWindow.showAtLocation(holder.itemView, Gravity.CENTER, 0, 0)

            val deleteButton = popupView.findViewById<Button>(R.id.delete_contact_button)
            val cancelButton = popupView.findViewById<Button>(R.id.cancel_delete_button)

            // Handle delete action
            deleteButton.setOnClickListener {
                // Delete the contact from the device's contact list
                deleteContactFromDevice(contact.id)
                // Dismiss the popup

                // Remove contact from local list and update the UI


                popupWindow.dismiss()
                Toast.makeText(context, "Contact deleted", Toast.LENGTH_SHORT).show()
            }

            // Handle cancel action
            cancelButton.setOnClickListener {
                popupWindow.dismiss()
            }
        }

        // Handle edit popup for editing the contact
        holder.itemView.setOnClickListener {
            val popupView = LayoutInflater.from(context).inflate(R.layout.popup_edit_contact, null)
            val popupWindow = PopupWindow(
                popupView,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true
            )
            popupWindow.showAtLocation(holder.itemView, Gravity.CENTER, 0, 0)

            val editButton = popupView.findViewById<Button>(R.id.edit_contact_button)
            editButton.setOnClickListener {
                popupWindow.dismiss()

                // Get the contact ID
                val contactId = contact.id
                Log.d("ContactAdapter", "Contact ID: $contactId")

                // Ensure contactId is not null or blank
                if (!contactId.isNullOrBlank()) {
                    val intent = Intent(context, EditContactActivity::class.java)
                    intent.putExtra("CONTACT_ID", contactId) // Passing the ID to the activity
                    intent.putExtra("CONTACT_NAME", contact.name) // Passing the contact name
                    intent.putExtra("CONTACT_NUMBER", contact.phoneNumber) // Passing the contact phone number
                    intent.putExtra("CONTACT_PHOTO_URI", contact.photoUri) // Passing the photo URI
                    Log.d("ContactAdapter", "Intent Contact ID: $contactId")
                    context.startActivity(intent)
                } else {
                    Toast.makeText(context, "Contact ID is missing!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun getItemCount(): Int = contacts.size

    fun updateContacts(newContacts: List<Contact>) {
        contacts = newContacts
        notifyDataSetChanged()
    }

    private fun deleteContactFromDevice(contactId: String) {
        try {
            val contactUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId)
            val contentResolver: ContentResolver = context.contentResolver
            val rowsDeleted = contentResolver.delete(contactUri, null, null)
            if (rowsDeleted > 0) {
                MainActivity.refreshContacts(mainActivity)
                Log.d("ContactAdapter", "Contact deleted from device.")
            } else {
                Log.d("ContactAdapter", "Failed to delete contact.")
            }
        } catch (e: Exception) {
            Log.e("ContactAdapter", "Error deleting contact: ${e.message}")
        }
    }
}
