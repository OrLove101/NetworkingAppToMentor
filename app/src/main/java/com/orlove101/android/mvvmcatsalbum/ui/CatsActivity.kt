package com.orlove101.android.mvvmcatsalbum.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.orlove101.android.mvvmcatsalbum.R
import com.orlove101.android.mvvmcatsalbum.databinding.ActivityCatsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CatsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCatsBinding
    private var navController: NavController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment)

        navController = navHostFragment?.findNavController()
    }
}
