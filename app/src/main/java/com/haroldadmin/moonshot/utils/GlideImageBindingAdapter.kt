package com.haroldadmin.moonshot.utils

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.load.engine.DiskCacheStrategy

@BindingAdapter(
    "glideImage",
    "glideImageError",
    "glideImageFallback",
    "glideCircleCrop",
    requireAll = false
)
fun loadImage(view: ImageView,
              glideImage: String?,
              glideImageError: Drawable,
              glideImageFallback: Drawable,
              glideCircleCrop: Boolean) {
    val request = GlideApp.with(view)
        .load(glideImage)
        .error(glideImageError)
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .fallback(glideImageFallback)

    if (glideCircleCrop) request.circleCrop()

    request.into(view)
}