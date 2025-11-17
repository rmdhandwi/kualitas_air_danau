@file:Suppress("DEPRECATION")

package com.example.cleanlake.Fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
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
import com.example.cleanlake.R
import com.example.cleanlake.Adapter.UserAdapter
import com.example.cleanlake.databinding.FragmentUsersBinding
import com.example.cleanlake.Model.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class UsersFragment : Fragment() {

    private var _binding: FragmentUsersBinding? = null
    private val binding get() = _binding!!
    private var userListener: ValueEventListener? = null

    private lateinit var userAdapter: UserAdapter
    private val userList = mutableListOf<UserModel>()
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
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

        setupRecyclerView()
        loadUserData()
        setupFab()
    }

    /** 🔹 Setup RecyclerView */
    private fun setupRecyclerView() {
        userAdapter = UserAdapter(
            userList,
            onEdit = { user -> showAddUserDialog(user) },
            onDelete = { user -> confirmDeleteUser(user) }
        )

        binding.rvUsers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUsers.adapter = userAdapter
    }

    /** 🔹 Tombol tambah user */
    private fun setupFab() {
        binding.fabAddUser.setOnClickListener { showAddUserDialog() }
    }

    /** 🔹 Load hanya user dengan role == "User" */
    private fun loadUserData() {
        userListener = database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val safeBinding = _binding ?: return
                userList.clear()

                for (child in snapshot.children) {
                    val user = child.getValue(UserModel::class.java)
                    if (user?.role.equals("User", true)) {
                        userList.add(user!!)
                    }
                }

                userAdapter.updateData(userList)
                safeBinding.tvEmpty.visibility =
                    if (userList.isEmpty()) View.VISIBLE else View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Gagal memuat data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /** 🔹 Tambah atau Edit User */
    @SuppressLint("ClickableViewAccessibility", "MissingInflatedId")
    private fun showAddUserDialog(userToEdit: UserModel? = null) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.popup_tambah_user, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        currentDialog = dialog

        val etNama = dialogView.findViewById<EditText>(R.id.etNamaLengkap)
        val etUsername = dialogView.findViewById<EditText>(R.id.etUsername)
        val etEmail = dialogView.findViewById<EditText>(R.id.etEmail)
        val etTelp = dialogView.findViewById<EditText>(R.id.etNoTelp)
        val etPassword = dialogView.findViewById<EditText>(R.id.etPassword)
        val etConfirm = dialogView.findViewById<EditText>(R.id.etConfirmPassword)
        val btnSimpan = dialogView.findViewById<Button>(R.id.btnSimpanUser)
        val btnClose = dialogView.findViewById<ImageButton>(R.id.btnClose)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)

        // 🔸 Tambahkan judul agar interaktif
        tvTitle?.text = if (userToEdit == null) "Tambah User Baru" else "Edit Data User"

        // Toggle visibility password
        setupPasswordVisibility(etPassword)
        setupPasswordVisibility(etConfirm)

        // Mode edit
        userToEdit?.let { user ->
            etNama.setText(user.namaLengkap)
            etUsername.setText(user.username)
            etEmail.setText(user.email)
            etTelp.setText(user.noTelp)
            etEmail.isEnabled = false
            etEmail.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_200))
            etEmail.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.status_bg))
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

            if (nama.isEmpty() || username.isEmpty() || email.isEmpty() || telp.isEmpty()) {
                Toast.makeText(requireContext(), "Isi semua kolom", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

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

            if (userToEdit == null) {
                // Buat akun baru
                createUserInAuth(email, password) { uid ->
                    if (uid != null) saveUserData(uid, nama, username, email, role, telp)
                }
            } else {
                // Update data
                saveUserData(userToEdit.id ?: "", nama, username, email, role, telp)
            }

            dialog.dismiss()
        }

        dialog.show()
    }

    /** 🔹 Toggle visibilitas password */
    @SuppressLint("ClickableViewAccessibility")
    private fun setupPasswordVisibility(editText: EditText) {
        var isVisible = false
        editText.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP &&
                event.rawX >= editText.right - editText.compoundDrawables[2].bounds.width()
            ) {
                isVisible = !isVisible
                val cursorPos = editText.selectionStart
                editText.inputType = if (isVisible)
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                else
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

                val drawableEnd = if (isVisible)
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_visibility_resized)
                else
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_visibility_off_resized)

                editText.setCompoundDrawablesWithIntrinsicBounds(null, null, drawableEnd, null)
                editText.setSelection(cursorPos)
                true
            } else false
        }
    }

    /** 🔹 Buat user di Firebase Auth */
    private fun createUserInAuth(email: String, password: String, callback: (String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result -> callback(result.user?.uid) }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Gagal membuat akun: ${it.message}", Toast.LENGTH_LONG).show()
                callback(null)
            }
    }

    /** 🔹 Simpan / Update user di database */
    private fun saveUserData(
        userId: String,
        nama: String,
        username: String,
        email: String,
        role: String,
        telp: String
    ) {
        val user = UserModel(
            id = userId,
            namaLengkap = nama,
            username = username,
            email = email,
            role = role,
            noTelp = telp
        )

        database.child(userId).setValue(user)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Data user tersimpan", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Gagal menyimpan data", Toast.LENGTH_SHORT).show()
            }
    }

    /** 🔹 Konfirmasi hapus user */
    private fun confirmDeleteUser(user: UserModel) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus User")
            .setMessage("Yakin ingin menghapus ${user.namaLengkap}?")
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

    override fun onDestroyView() {
        super.onDestroyView()
        userListener?.let { database.removeEventListener(it) }
        currentDialog?.dismiss()
        _binding = null
    }
}
