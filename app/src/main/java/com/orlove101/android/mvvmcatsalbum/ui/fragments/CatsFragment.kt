package com.orlove101.android.mvvmcatsalbum.ui.fragments

import android.Manifest
import android.animation.*
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.AbsListView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.orlove101.android.mvvmcatsalbum.R
import com.orlove101.android.mvvmcatsalbum.databinding.FragmentCatsBinding
import com.orlove101.android.mvvmcatsalbum.ui.adapters.CatsAdapter
import com.orlove101.android.mvvmcatsalbum.ui.viewModels.CatsViewModel
import com.orlove101.android.mvvmcatsalbum.util.QUERY_PAGE_SIZE
import com.orlove101.android.mvvmcatsalbum.util.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.*

@AndroidEntryPoint
class CatsFragment: Fragment() {
    val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Log.d(TAG, "Granted")
            }
        }
    private var _binding: FragmentCatsBinding? = null
    private val binding get() = _binding!!
    private val mViewModel: CatsViewModel by viewModels()
    private var mCatsAdapter: CatsAdapter? = null
    private var frontAnim: AnimatorSet? = null
    private var backAnim: AnimatorSet? = null
    private var currentAnimator: Animator? = null
    private var shortAnimationDuration: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCatsBinding.inflate(inflater, container, false)
        setupRecyclerView()

        requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)

        mCatsAdapter?.setOnImageClickListener { imageView, imageUrl ->
            binding.fbSavePhoto.setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {
                    saveImage(Glide.with(binding.root)
                        .asBitmap()
                        .load(imageUrl) // sample image
                        .placeholder(android.R.drawable.progress_indeterminate_horizontal) // need placeholder to avoid issue like glide annotations
                        .error(android.R.drawable.stat_notify_error) // need error to avoid issue like glide annotations
                        .submit()
                        .get())
                }
            }
            zoomImageFromThumb(imageView, imageUrl)
        }

        // Retrieve and cache the system's default "short" animation time.
        shortAnimationDuration = resources.getInteger(android.R.integer.config_shortAnimTime)

        mViewModel.cats.observe(viewLifecycleOwner, Observer { response ->
            when (response) {
                is Resource.Success -> {
                    hideProgressBar()
                    response.data?.let { catsResponse ->
                        mCatsAdapter?.differ?.submitList(catsResponse.toList())
                        if (isLastPage) {
                            binding.rvCats.setPadding(0, 0, 0, 0)
                        }
                    }
                }
                is Resource.Error -> {
                    hideProgressBar()
                    response.message?.let { message ->
                        Toast.makeText(activity, "An error occured $message", Toast.LENGTH_LONG).show()
                    }
                }
                is Resource.Loading -> {
                    showProgressBar()
                }
            }
        })

        return binding.root
    }

    private fun zoomImageFromThumb(thumbView: View, imageUrl: String) {
        currentAnimator?.cancel()
        frontAnim?.cancel()
        backAnim?.cancel()

        val expandedImageView = binding.expandedImage
        Glide.with(binding.root).load(imageUrl).into(expandedImageView)

        frontAnim = AnimatorInflater.loadAnimator(requireContext(), R.animator.front_animator) as AnimatorSet
        backAnim = AnimatorInflater.loadAnimator(requireContext(), R.animator.back_animator) as AnimatorSet

        frontAnim?.setTarget(thumbView)
        backAnim?.setTarget(expandedImageView)
        frontAnim?.start()
        backAnim?.start()

        val scale: Float = requireContext().resources.displayMetrics.density
        thumbView.cameraDistance = 8000 * scale
        expandedImageView.cameraDistance = 8000 * scale

        val startBoundsInt = Rect()
        val finalBoundsInt = Rect()
        val globalOffset = Point()

        thumbView.getGlobalVisibleRect(startBoundsInt)
        binding.root.getGlobalVisibleRect(finalBoundsInt, globalOffset)
        startBoundsInt.offset(-globalOffset.x, -globalOffset.y)
        finalBoundsInt.offset(-globalOffset.x, -globalOffset.y)

        val startBounds = RectF(startBoundsInt)
        val finalBounds = RectF(finalBoundsInt)
        val startScale: Float
        if ((finalBounds.width() / finalBounds.height() > startBounds.width() / startBounds.height())) {
            startScale = startBounds.height() / finalBounds.height()
            val startWidth: Float = startScale * finalBounds.width()
            val deltaWidth: Float = (startWidth - startBounds.width()) / 2
            startBounds.left -= deltaWidth.toInt()
            startBounds.right += deltaWidth.toInt()
        } else {
            startScale = startBounds.width() / finalBounds.width()
            val startHeight: Float = startScale * finalBounds.height()
            val deltaHeight: Float = (startHeight - startBounds.height()) / 2f
            startBounds.top -= deltaHeight.toInt()
            startBounds.bottom += deltaHeight.toInt()
        }
        thumbView.alpha = 0f
        expandedImageView.visibility = View.VISIBLE
        binding.fbSavePhoto.visibility = View.VISIBLE

        expandedImageView.pivotX = 0f
        expandedImageView.pivotY = 0f

        currentAnimator = AnimatorSet().apply {
            play(
                ObjectAnimator.ofFloat(
                expandedImageView,
                View.X,
                startBounds.left,
                finalBounds.left)
            ).apply {
                with(ObjectAnimator.ofFloat(expandedImageView, View.Y, startBounds.top, finalBounds.top))
                with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X, startScale, 1f))
                with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_Y, startScale, 1f))
            }
            duration = shortAnimationDuration.toLong()
            interpolator = DecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {

                override fun onAnimationEnd(animation: Animator) {
                    currentAnimator = null
                }

                override fun onAnimationCancel(animation: Animator) {
                    currentAnimator = null
                }
            })
            start()
        }
        expandedImageView.setOnClickListener {
            currentAnimator?.cancel()

            frontAnim?.setTarget(expandedImageView)
            backAnim?.setTarget(thumbView)
            backAnim?.start()
            frontAnim?.start()

            currentAnimator = AnimatorSet().apply {
                play(ObjectAnimator.ofFloat(expandedImageView, View.X, startBounds.left)).apply {
                    with(ObjectAnimator.ofFloat(expandedImageView, View.Y, startBounds.top))
                    with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X, startScale))
                    with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_Y, startScale))
                }
                duration = shortAnimationDuration.toLong()
                interpolator = DecelerateInterpolator()
                addListener(object : AnimatorListenerAdapter() {

                    override fun onAnimationEnd(animation: Animator) {
                        thumbView.alpha = 1f
                        expandedImageView.visibility = View.GONE
                        binding.fbSavePhoto.visibility = View.GONE
                        currentAnimator = null
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        thumbView.alpha = 1f
                        expandedImageView.visibility = View.GONE
                        binding.fbSavePhoto.visibility = View.GONE
                        currentAnimator = null
                    }
                })
                start()
            }
        }
    }

    private fun saveImage(image: Bitmap): String? {
        var savedImagePath: String? = null
        val imageFileName = "JPEG_" + UUID.randomUUID() + ".jpg"
        val storageDir = File(
            Environment.getExternalStorageDirectory().toString() + "/" + Environment.DIRECTORY_DCIM + "/"
        )
        var success = true
        if (!storageDir.exists()) {
            success = storageDir.mkdirs()
        }
        if (success) {
            val imageFile = File(storageDir, imageFileName)
            savedImagePath = imageFile.getAbsolutePath()
            try {
                val fOut: OutputStream = FileOutputStream(imageFile)
                image.compress(Bitmap.CompressFormat.JPEG, 100, fOut)
                fOut.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Add the image to the system gallery
            galleryAddPic(savedImagePath)
            activity?.runOnUiThread {
                Toast.makeText(activity, "Image Saved!", Toast.LENGTH_SHORT).show()
            }
        }
        return savedImagePath
    }

    private fun galleryAddPic(imagePath: String?) {
        imagePath?.let { path ->
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            val f = File(path)
            val contentUri: Uri = Uri.fromFile(f)
            mediaScanIntent.data = contentUri
        }
    }

    private fun hideProgressBar() {
        binding.paginationProgressBar.visibility = View.INVISIBLE
        isLoading = false
    }

    private fun showProgressBar() {
        binding.paginationProgressBar.visibility = View.VISIBLE
        isLoading = true
    }

    var isLoading = false
    var isLastPage = false
    var isScrolling = false

    val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if ( newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL ) {
                isScrolling = true
            }
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
            val visibleItemCount = layoutManager.childCount
            val totalItemCount = layoutManager.itemCount

            val isNotLoadingAndNotLastPage = !isLoading && !isLastPage
            val isAtLastItem = firstVisibleItemPosition + visibleItemCount >= totalItemCount
            val isNotAtBeginning = firstVisibleItemPosition >= 0
            val isTotalMoreThanVisible = totalItemCount >= QUERY_PAGE_SIZE
            val shouldPaginate = isNotLoadingAndNotLastPage && isAtLastItem && isNotAtBeginning &&
                    isTotalMoreThanVisible && isScrolling
            if (shouldPaginate) {
                mViewModel.getCats()
                isScrolling = false
            }
        }
    }

    private fun setupRecyclerView() {
        mCatsAdapter = CatsAdapter()
        binding.rvCats.apply {
            adapter = mCatsAdapter
            layoutManager = LinearLayoutManager(activity)
            addOnScrollListener(this@CatsFragment.scrollListener)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private const val TAG = "BreakingNewsFragment"
