@file:Suppress("DEPRECATION")

package com.example.cleanlake.Fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.cleanlake.R
import com.example.cleanlake.Adapter.UserAdapter
import com.example.cleanlake.databinding.FragmentUsersBinding
import com.example.cleanlake.Model.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class UsersFragment : Fragment() {

    private var _binding: FragmentUsersBinding? = null
    private val binding get() = _binding!!
    private var userListener: ValueEventListener? = null

    private lateinit var userAdapter: UserAdapter
    private val userList = mutableListOf<UserModel>()
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private var imageUri: Uri? = null
    private var currentDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        database = FirebaseDatabase.getInstance().getReference("Users")
        auth = FirebaseAuth.getInstance()

        userAdapter = UserAdapter(
            userList,
            onEdit = { user -> showAddUserDialog(user) },
            onDelete = { user -> confirmDeleteUser(user) }
        )

        binding.rvUsers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUsers.adapter = userAdapter

        loadUserData()
        binding.fabAddUser.setOnClickListener { showAddUserDialog() }
    }

    /** Load hanya user dengan role == "User" **/
    private fun loadUserData() {
        userListener = database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val safeBinding = _binding ?: return
                userList.clear()

                for (child in snapshot.children) {
                    val user = child.getValue(UserModel::class.java)
                    if (user != null && user.role.equals("User", true)) {
                        userList.add(user)
                    }
                }

                userAdapter.updateData(userList)
                safeBinding.tvEmpty.visibility = if (userList.isEmpty()) View.VISIBLE else View.GONE
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // === TAMBAH / EDIT USER ===
    @SuppressLint("ClickableViewAccessibility")
    private fun showAddUserDialog(userToEdit: UserModel? = null) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.popup_tambah_user, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()
        currentDialog = dialog

        val etNama = dialogView.findViewById<EditText>(R.id.etNamaLengkap)
        val etUsername = dialogView.findViewById<EditText>(R.id.etUsername)
        val etEmail = dialogView.findViewById<EditText>(R.id.etEmail)
        val etTelp = dialogView.findViewById<EditText>(R.id.etNoTelp)
        val etPassword = dialogView.findViewById<EditText>(R.id.etPassword)
        val etConfirm = dialogView.findViewById<EditText>(R.id.etConfirmPassword)
        val btnSimpan = dialogView.findViewById<Button>(R.id.btnSimpanUser)
        val imgProfile = dialogView.findViewById<ImageView>(R.id.imgProfilePreview)
        val btnUpload = dialogView.findViewById<Button>(R.id.btnUploadFoto)
        val btnClose = dialogView.findViewById<ImageButton>(R.id.btnClose)

        var isPasswordVisible = false
        var isConfirmVisible = false

        // === TOGGLE PASSWORD VISIBILITY ===
        etPassword.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP &&
                event.rawX >= etPassword.right - etPassword.compoundDrawables[2].bounds.width()
            ) {
                isPasswordVisible = !isPasswordVisible
                val cursorPos = etPassword.selectionStart
                etPassword.inputType = if (isPasswordVisible)
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                else
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

                val drawableEnd = if (isPasswordVisible)
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_visibility_resized)
                else
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_visibility_off_resized)

                etPassword.setCompoundDrawablesWithIntrinsicBounds(null, null, drawableEnd, null)
                etPassword.setSelection(cursorPos)
                true
            } else false
        }

        // === TOGGLE CONFIRM PASSWORD VISIBILITY ===
        etConfirm.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP &&
                event.rawX >= etConfirm.right - etConfirm.compoundDrawables[2].bounds.width()
            ) {
                isConfirmVisible = !isConfirmVisible
                val cursorPos = etConfirm.selectionStart
                etConfirm.inputType = if (isConfirmVisible)
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                else
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

                val drawableEnd = if (isConfirmVisible)
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_visibility_resized)
                else
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_visibility_off_resized)

                etConfirm.setCompoundDrawablesWithIntrinsicBounds(null, null, drawableEnd, null)
                etConfirm.setSelection(cursorPos)
                true
            } else false
        }

        // === MODE EDIT ===
        userToEdit?.let { user ->
            etNama.setText(user.namaLengkap)
            etUsername.setText(user.username)
            etEmail.setText(user.email)
            etTelp.setText(user.noTelp)
            etPassword.text.clear()
            etConfirm.text.clear()

            etEmail.isEnabled = false
            etEmail.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            etEmail.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.status_bg))

            Glide.with(this).load(user.imageUrl)
                .placeholder(R.drawable.ic_user_placeholder)
                .into(imgProfile)
        }

        btnUpload.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, 101)
        }

        btnClose.setOnClickListener { dialog.dismiss() }

        btnSimpan.setOnClickListener {
            val nama = etNama.text.toString().trim()
            val username = etUsername.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val telp = etTelp.text.toString().trim()
            val password = etPassword.text.toString()
            val confirm = etConfirm.text.toString()
            val role = "User"

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(requireContext(), "Email tidak valid", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (userToEdit == null && password.length < 6) {
                Toast.makeText(requireContext(), "Password minimal 6 karakter", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.isNotEmpty() && password != confirm) {
                Toast.makeText(requireContext(), "Password tidak cocok", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (imageUri != null) {
                uploadImageToImgBB(imageUri!!) { uploadedUrl ->
                    if (userToEdit == null) {
                        createUserInAuth(email, password) { uid ->
                            if (uid != null) {
                                saveUserData(uid, nama, username, email, role, telp, uploadedUrl)
                            }
                        }
                    } else {
                        saveUserData(userToEdit.id ?: "", nama, username, email, role, telp, uploadedUrl ?: userToEdit.imageUrl)
                    }
                }
            } else {
                if (userToEdit == null) {
                    createUserInAuth(email, password) { uid ->
                        if (uid != null) {
                            saveUserData(uid, nama, username, email, role, telp, null)
                        }
                    }
                } else {
                    saveUserData(userToEdit.id ?: "", nama, username, email, role, telp, userToEdit.imageUrl)
                }
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    /** 🔹 Buat user di Firebase Authentication **/
    private fun createUserInAuth(email: String, password: String, callback: (String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                callback(result.user?.uid)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Gagal membuat akun Auth: ${it.message}", Toast.LENGTH_LONG).show()
                callback(null)
            }
    }

    /** 🔹 Simpan data user di Realtime Database **/
    private fun saveUserData(
        userId: String, nama: String, username: String, email: String,
        role: String, telp: String, imageUrl: String?
    ) {
        val user = UserModel(
            id = userId,
            namaLengkap = nama,
            username = username,
            email = email,
            role = role,
            noTelp = telp,
            imageUrl = imageUrl
        )

        database.child(userId).setValue(user)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Data user tersimpan", Toast.LENGTH_SHORT).show()
                imageUri = null
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Gagal menyimpan data", Toast.LENGTH_SHORT).show()
            }
    }

    /** Hapus User **/
    private fun confirmDeleteUser(user: UserModel) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus User")
            .setMessage("Apakah Anda yakin ingin menghapus ${user.namaLengkap}?")
            .setPositiveButton("Ya") { _, _ ->
                database.child(user.id ?: return@setPositiveButton).removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "User dihapus", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Gagal menghapus user", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    /** Upload gambar ke ImgBB **/
    private fun uploadImageToImgBB(imageUri: Uri, callback: (String?) -> Unit) {
        val apiKey = "7716379e94705e8e620c1c13b1de3de4"
        val inputStream = requireContext().contentResolver.openInputStream(imageUri)
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
                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "")
                    val data = json.getJSONObject("data")
                    val rawUrl = data.getString("display_url")
                    val fixedUrl = rawUrl.replace("i.ibb.co", "i.ibb.co.com")

                    requireActivity().runOnUiThread { callback(fixedUrl) }
                } else requireActivity().runOnUiThread { callback(null) }
            } catch (e: Exception) {
                e.printStackTrace()
                requireActivity().runOnUiThread { callback(null) }
            }
        }.start()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101 && resultCode == Activity.RESULT_OK && data?.data != null) {
            imageUri = data.data
            currentDialog?.findViewById<ImageView>(R.id.imgProfilePreview)?.let {
                Glide.with(this).load(imageUri).centerCrop()
                    .placeholder(R.drawable.ic_user_placeholder).into(it)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        userListener?.let { database.removeEventListener(it) }
        _binding = null
    }
}
