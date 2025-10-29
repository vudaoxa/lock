package com.toh.usagestat

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.toh.usagestat.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ViewBinding
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // NavHost đã được khai báo trong XML → không cần setup gì thêm
        // Hilt tự inject các Fragment + ViewModel
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}