package com.bangkit.inscure.ui.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bangkit.inscure.R
import com.bangkit.inscure.databinding.FragmentProfileBinding
import com.bangkit.inscure.network.RetrofitClient
import com.bangkit.inscure.network.UserResponse
import com.bangkit.inscure.ui.auth.AuthActivity
import com.bangkit.inscure.ui.disease.ListHistoryActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import jp.wasabeef.glide.transformations.CropCircleWithBorderTransformation
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        playAnimText()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()

        // Fetch user profile
        val sharedPreferences = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val authToken = sharedPreferences.getString("authToken", null)
        authToken?.let {
            fetchUserProfile(it)
        }
    }

    private fun playAnimText() {
        val context = requireContext()

        // Load animations
        val slideInFadeIn = AnimationUtils.loadAnimation(context, R.anim.slide_in_right_fade_in)
        val slideInFadeInLeft = AnimationUtils.loadAnimation(context, R.anim.slide_in_left_in)
        val slideInFadeInUp = AnimationUtils.loadAnimation(context, R.anim.slide_in_up_fade_in)
        val slideInFadeInBottom = AnimationUtils.loadAnimation(context, R.anim.slide_in_bottom_fade_in)

        // Apply animations to views
        applyAnimation(binding.ivProfilePicture, slideInFadeInUp)
        applyAnimation(binding.tvUsername, slideInFadeInBottom)
        applyAnimation(binding.tvNumber, slideInFadeInBottom)
        applyAnimation(binding.tvEmail, slideInFadeInBottom)
        applyAnimation(binding.btnHistory, slideInFadeIn)
        applyAnimation(binding.btnCameraPermis, slideInFadeInLeft)
        applyAnimation(binding.btnLocationPermis, slideInFadeIn)
        applyAnimation(binding.btnAboutDev, slideInFadeInLeft)
        applyAnimation(binding.btnLogout, slideInFadeIn)
    }

    private fun applyAnimation(view: View, animation: Animation) {
        view.startAnimation(animation)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupListeners() {
        binding.btnLogout.setOnClickListener {
            logOut()
        }

        binding.btnCameraPermis.setOnClickListener {
            requestCameraPermission()
        }

        binding.btnAboutDev.setOnClickListener {
            navigateToWeb()
        }

        binding.btnHistory.setOnClickListener {
            navigateToHistory()
        }

        binding.btnLocationPermis.setOnClickListener {
            checkAndRequestLocationPermission()
        }
    }

    private fun fetchUserProfile(token: String) {
        val apiService = RetrofitClient.instance
        apiService.getUserByToken("Bearer $token").enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                if (response.isSuccessful) {
                    val user = response.body()?.data
                    user?.let {
                        Glide.with(requireContext())
                            .load(it.picture)
                            .transform(CircleCrop(), CropCircleWithBorderTransformation(8, Color.WHITE))
                            .placeholder(R.drawable.person_24px)
                            .error(R.drawable.person_24px)
                            .into(binding.ivProfilePicture)

                        binding.tvUsername.text = it.name
                        binding.tvNumber.text = it.notelp
                        binding.tvEmail.text = it.email
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to fetch user profile", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun navigateToWeb() {
        val intent = Intent(requireContext(), WebViewActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToHistory() {
        val intent = Intent(requireContext(), ListHistoryActivity::class.java)
        startActivity(intent)
    }

    private fun logOut() {
        // Clear the auth token from SharedPreferences
        val sharedPreferences = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove("authToken")
        editor.apply()

        // Navigate to AuthActivity
        val intent = Intent(requireContext(), AuthActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }

    private fun checkAndRequestLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                // Permission is already granted
                Toast.makeText(requireContext(), "Location permission is already granted", Toast.LENGTH_SHORT).show()
            }
            else -> {
                // Request permission
                requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted
            Toast.makeText(requireContext(), "Location permission granted", Toast.LENGTH_SHORT).show()
        } else {
            // Permission denied
            Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            Toast.makeText(requireContext(), "Camera permission already granted", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted
            Toast.makeText(requireContext(), "Camera permission granted", Toast.LENGTH_SHORT).show()
        } else {
            // Permission denied
            Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

}
