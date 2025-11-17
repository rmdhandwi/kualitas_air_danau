package com.example.cleanlake

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.cleanlake.Auth.LoginActivity
import com.example.cleanlake.Fragment.*
import com.example.cleanlake.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private var userRole: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Users")

        loadUserData()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        // Tombol logout
        binding.logout.setOnClickListener {
            showLogoutDialog()
        }
    }

    /**
     * 🔹 Ambil data user yang sedang login dari Firebase
     * 🔹 Jika belum login, langsung ke LoginActivity
     */
    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // Langsung arahkan ke login
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        // 🔹 Tampilkan indikator loading
        binding.progressLoading.visibility = View.VISIBLE
        binding.tvMenu.text = "Memuat data ..."
        binding.container.visibility = View.GONE
        binding.customNav.visibility = View.GONE

        val userRef = database.child(currentUser.uid)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.progressLoading.visibility = View.GONE
                binding.container.visibility = View.VISIBLE
                binding.customNav.visibility = View.VISIBLE

                if (!snapshot.exists()) {
                    val intent = Intent(this@MainActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                    return
                }

                val name = snapshot.child("namaLengkap").getValue(String::class.java) ?: "Guest"
                userRole = snapshot.child("role").getValue(String::class.java) ?: "-"

                binding.tvUsername.text = name
                setupMenuByRole(userRole)
            }

            override fun onCancelled(error: DatabaseError) {
                binding.progressLoading.visibility = View.GONE
                binding.tvMenu.text = "Gagal memuat data"
            }
        })
    }


    /**
     * 🔹 Atur menu berdasarkan role user
     */
    private fun setupMenuByRole(role: String?) {
        when (role?.lowercase()) {
            "admin" -> {
                binding.navHome.visibility = View.VISIBLE
                binding.navHistory.visibility = View.VISIBLE
                binding.navSetting.visibility = View.VISIBLE
                binding.navUsers.visibility = View.VISIBLE
                binding.navProfile.visibility = View.VISIBLE

                setupMenuListeners()
                replaceFragment(BerandaFragment())
                setActiveMenu(binding.navHome, "Beranda")
            }

            "user" -> {
                binding.navHome.visibility = View.VISIBLE
                binding.navProfile.visibility = View.VISIBLE
                binding.navHistory.visibility = View.GONE
                binding.navSetting.visibility = View.GONE
                binding.navUsers.visibility = View.GONE

                setupMenuListeners()
                replaceFragment(BerandaFragment())
                setActiveMenu(binding.navHome, "Beranda")
            }

            else -> {
                // Tidak mungkin karena user sudah login, tapi aman
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }

    /**
     * 🔹 Listener untuk menu bawah
     */
    private fun setupMenuListeners() {
        binding.navHome.setOnClickListener {
            replaceFragment(BerandaFragment())
            setActiveMenu(binding.navHome, "Beranda")
        }
        binding.navHistory.setOnClickListener {
            replaceFragment(RiwayatFragment())
            setActiveMenu(binding.navHistory, "Riwayat")
        }
        binding.navSetting.setOnClickListener {
            replaceFragment(AmbangFragment())
            setActiveMenu(binding.navSetting, "Setting")
        }
        binding.navProfile.setOnClickListener {
            replaceFragment(ProfileFragment())
            setActiveMenu(binding.navProfile, "Profile")
        }
        binding.navUsers.setOnClickListener {
            replaceFragment(UsersFragment())
            setActiveMenu(binding.navUsers, "Users")
        }
    }

    /**
     * 🔹 Ganti Fragment
     */
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            .replace(R.id.container, fragment)
            .commit()
    }

    /**
     * 🔹 Menu aktif → ubah warna icon & text + ubah judul tvMenu
     */
    private fun setActiveMenu(active: LinearLayout, title: String) {
        val inactiveColor = ContextCompat.getColor(this, R.color.menu_off)
        val activeColor = ContextCompat.getColor(this, R.color.menu_on)

        val allMenus = listOf(
            binding.navHome,
            binding.navHistory,
            binding.navSetting,
            binding.navProfile,
            binding.navUsers
        )

        for (menu in allMenus) {
            val icon = menu.getChildAt(0) as ImageView
            val text = menu.getChildAt(1) as TextView
            if (menu == active) {
                icon.setColorFilter(activeColor)
                text.setTextColor(activeColor)
                menu.animate().scaleX(1.1f).scaleY(1.1f).setDuration(150).start()
            } else {
                icon.setColorFilter(inactiveColor)
                text.setTextColor(inactiveColor)
                menu.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
            }
        }

        binding.tvMenu.text = title
    }

    /**
     * 🔹 Tampilkan dialog konfirmasi logout
     */
    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Logout")
            .setMessage("Apakah kamu yakin ingin keluar dari akun ini?")
            .setPositiveButton("Ya") { _, _ ->
                logout()
            }
            .setNegativeButton("Tidak", null)
            .setCancelable(true)
            .show()
    }

    /**
     * 🔹 Logout user dan arahkan ke LoginActivity
     */
    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
