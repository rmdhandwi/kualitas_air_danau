package com.example.cleanlake.Auth

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.cleanlake.MainActivity
import com.example.cleanlake.R
import com.example.cleanlake.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        dbRef = FirebaseDatabase.getInstance().getReference("Users")

        // 🔹 Jika sudah login, langsung masuk ke MainActivity
        val currentUser = auth.currentUser
        if (currentUser != null) {
            goToMain()
            return
        }

        setupPasswordVisibilityToggle()
        setupActions()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupPasswordVisibilityToggle() {
        val etPassword = binding.edtKatasandi

        etPassword.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP &&
                event.rawX >= (etPassword.right - etPassword.compoundDrawables[2].bounds.width())
            ) {
                isPasswordVisible = !isPasswordVisible
                val selection = etPassword.selectionEnd

                etPassword.inputType = if (isPasswordVisible)
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                else
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

                etPassword.setSelection(selection)

                val icon = if (isPasswordVisible)
                    R.drawable.ic_visibility_off_resized
                else
                    R.drawable.ic_visibility_resized
                etPassword.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_lock_resized, 0, icon, 0
                )
                true
            } else false
        }
    }

    private fun setupActions() {
        binding.btnLogin.setOnClickListener {
            val username = binding.edtUsername.text.toString().trim()
            val password = binding.edtKatasandi.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Username dan kata sandi wajib diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.btnLogin.isEnabled = false
            binding.btnLogin.text = "Memproses..."

            dbRef.orderByChild("username").equalTo(username)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            for (userSnapshot in snapshot.children) {
                                val email = userSnapshot.child("email").getValue(String::class.java)
                                if (!email.isNullOrEmpty()) {
                                    loginWithEmail(email, password)
                                    return
                                }
                            }
                        } else {
                            binding.btnLogin.isEnabled = true
                            binding.btnLogin.text = "Login"
                            Toast.makeText(this@LoginActivity, "Username tidak ditemukan", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        binding.btnLogin.isEnabled = true
                        binding.btnLogin.text = "Login"
                        Toast.makeText(this@LoginActivity, "Gagal menghubungi server", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        binding.txtForget.setOnClickListener {
            startActivity(Intent(this, ForgetPasswordActivity::class.java))
        }
    }

    private fun loginWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                binding.btnLogin.isEnabled = true
                binding.btnLogin.text = "Login"

                if (task.isSuccessful) {
                    Toast.makeText(this, "Login berhasil!", Toast.LENGTH_SHORT).show()
                    goToMain()
                } else {
                    Toast.makeText(this, "Login gagal: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        // agar tidak bisa kembali ke login pakai tombol back
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
