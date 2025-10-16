@file:Suppress("DEPRECATION")
package com.example.cleanlake.Fragment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.cleanlake.R
import com.example.cleanlake.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.InputStream

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference

    private var isEditMode = false
    private var imageUri: Uri? = null
    private val PICK_IMAGE = 101

    private var originalName = ""
    private var originalUsername = ""
    private var originalTelp = ""
    private var originalPhotoUrl: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        dbRef = FirebaseDatabase.getInstance().getReference("Users")

        loadUserData()
        setupListeners()

        return binding.root
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser ?: return
        dbRef.child(currentUser.uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || _binding == null) return

                val name = snapshot.child("namaLengkap").value?.toString() ?: "-"
                val username = snapshot.child("username").value?.toString() ?: "-"
                val email = snapshot.child("email").value?.toString() ?: "-"
                val noTelp = snapshot.child("noTelp").value?.toString() ?: "-"
                val photoUrl = snapshot.child("imageUrl").value?.toString()

                originalName = name
                originalUsername = username
                originalTelp = noTelp
                originalPhotoUrl = photoUrl

                binding.tvName.text = name
                binding.tvUsername.text = username
                binding.tvEmail.text = email
                binding.tvTelp.text = noTelp

                binding.etName.setText(name)
                binding.etUsername.setText(username)
                binding.etNoTelp.setText(noTelp)

                val imageTarget = if (!photoUrl.isNullOrEmpty()) photoUrl else R.drawable.ic_user_placeholder
                Glide.with(requireContext())
                    .load(imageTarget)
                    .centerCrop()
                    .placeholder(R.drawable.ic_user_placeholder)
                    .into(binding.ivProfile)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun setupListeners() {
        binding.btnEdit.setOnClickListener { toggleEditMode(true) }
        binding.btnSave.setOnClickListener { saveChanges() }

        // Klik foto profil untuk pilih gambar baru
        binding.btnUploadFoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE)
        }
    }

    private fun toggleEditMode(editMode: Boolean) {
        isEditMode = editMode
        binding.layoutView.visibility = if (editMode) View.GONE else View.VISIBLE
        binding.layoutEdit.visibility = if (editMode) View.VISIBLE else View.GONE
        binding.btnEdit.visibility = if (editMode) View.GONE else View.VISIBLE
        binding.btnSave.visibility = if (editMode) View.VISIBLE else View.GONE
    }

    private fun saveChanges() {
        val name = binding.etName.text.toString().trim()
        val username = binding.etUsername.text.toString().trim()
        val noTelp = binding.etNoTelp.text.toString().trim()

        if (name.isEmpty() || username.isEmpty() || noTelp.isEmpty()) {
            Toast.makeText(requireContext(), "Isi semua data dengan benar", Toast.LENGTH_SHORT).show()
            return
        }

        if (name == originalName && username == originalUsername && noTelp == originalTelp && imageUri == null) {
            Toast.makeText(requireContext(), "Tidak ada perubahan untuk disimpan", Toast.LENGTH_SHORT).show()
            toggleEditMode(false)
            return
        }

        val currentUser = auth.currentUser ?: return

        if (imageUri != null) {
            uploadImageToImgBB(imageUri!!) { uploadedUrl ->
                if (uploadedUrl != null) {
                    updateUserData(currentUser.uid, name, username, noTelp, uploadedUrl)
                } else {
                    Toast.makeText(requireContext(), "Gagal mengunggah foto", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            updateUserData(currentUser.uid, name, username, noTelp, null)
        }
    }

    private fun updateUserData(uid: String, name: String, username: String, noTelp: String, imageUrl: String?) {
        val userMap = mutableMapOf<String, Any>(
            "namaLengkap" to name,
            "username" to username,
            "noTelp" to noTelp
        )

        if (imageUrl != null) {
            userMap["imageUrl"] = imageUrl
        }

        dbRef.child(uid).updateChildren(userMap)
            .addOnSuccessListener {
                if (!isAdded || _binding == null) return@addOnSuccessListener
                Toast.makeText(requireContext(), "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                imageUri = null
                toggleEditMode(false)
                loadUserData()
            }
            .addOnFailureListener {
                if (!isAdded || _binding == null) return@addOnFailureListener
                Toast.makeText(requireContext(), "Gagal menyimpan perubahan", Toast.LENGTH_SHORT).show()
            }
    }

    /** Upload gambar ke ImgBB **/
    private fun uploadImageToImgBB(imageUri: Uri, callback: (String?) -> Unit) {
        val apiKey = "7716379e94705e8e620c1c13b1de3de4"
        val inputStream: InputStream? = requireContext().contentResolver.openInputStream(imageUri)
        val imageBytes = inputStream?.readBytes() ?: return callback(null)

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("key", apiKey)
            .addFormDataPart(
                "image", "user_image.jpg",
                imageBytes.toRequestBody("image/*".toMediaTypeOrNull(), 0, imageBytes.size)
            )
            .build()

        val request = Request.Builder()
            .url("https://api.imgbb.com/1/upload")
            .post(requestBody)
            .build()

        val client = OkHttpClient()
        Thread {
            try {
                val response = client.newCall(request).execute()
                val bodyString = response.body?.string()
                if (response.isSuccessful && bodyString != null) {
                    val json = JSONObject(bodyString)
                    val url = json.getJSONObject("data").getString("display_url")
                    activity?.runOnUiThread { callback(url) }
                } else {
                    activity?.runOnUiThread { callback(null) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                activity?.runOnUiThread { callback(null) }
            }
        }.start()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK && data?.data != null) {
            imageUri = data.data
            Glide.with(this)
                .load(imageUri)
                .centerCrop()
                .placeholder(R.drawable.ic_user_placeholder)
                .into(binding.ivProfile)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
