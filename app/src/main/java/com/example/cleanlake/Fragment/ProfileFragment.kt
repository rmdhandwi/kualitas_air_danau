@file:Suppress("DEPRECATION")
package com.example.cleanlake.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.cleanlake.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference

    private var isEditMode = false

    private var originalName = ""
    private var originalUsername = ""
    private var originalTelp = ""

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

                originalName = name
                originalUsername = username
                originalTelp = noTelp

                binding.tvName.text = name
                binding.tvUsername.text = username
                binding.tvEmail.text = email
                binding.tvTelp.text = noTelp

                binding.etName.setText(name)
                binding.etUsername.setText(username)
                binding.etNoTelp.setText(noTelp)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun setupListeners() {
        binding.btnEdit.setOnClickListener { toggleEditMode(true) }
        binding.btnSave.setOnClickListener { saveChanges() }
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

        if (name == originalName && username == originalUsername && noTelp == originalTelp) {
            Toast.makeText(requireContext(), "Tidak ada perubahan untuk disimpan", Toast.LENGTH_SHORT).show()
            toggleEditMode(false)
            return
        }

        val currentUser = auth.currentUser ?: return

        val userMap = mapOf(
            "namaLengkap" to name,
            "username" to username,
            "noTelp" to noTelp
        )

        dbRef.child(currentUser.uid).updateChildren(userMap)
            .addOnSuccessListener {
                if (!isAdded || _binding == null) return@addOnSuccessListener
                Toast.makeText(requireContext(), "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                toggleEditMode(false)
                loadUserData()
            }
            .addOnFailureListener {
                if (!isAdded || _binding == null) return@addOnFailureListener
                Toast.makeText(requireContext(), "Gagal menyimpan perubahan", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
