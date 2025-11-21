package com.example.cleanlake.Auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cleanlake.databinding.ActivityForgetPasswordBinding
import com.google.firebase.auth.FirebaseAuth

class ForgetPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgetPasswordBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityForgetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.btnSubmit.setOnClickListener {
            val email = binding.edtEmail.text.toString().trim()
            sendResetEmail(email)
        }

        binding.btnBack.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun sendResetEmail(email: String) {

        // Validasi email
        if (email.isEmpty()) {
            binding.edtEmail.error = "Email tidak boleh kosong"
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.edtEmail.error = "Format email tidak valid"
            return
        }

        // Kirim email reset password
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Link reset password telah dikirim ke email Anda",
                    Toast.LENGTH_LONG
                ).show()

                // Arahkan kembali ke login
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Gagal mengirim email: ${it.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}
