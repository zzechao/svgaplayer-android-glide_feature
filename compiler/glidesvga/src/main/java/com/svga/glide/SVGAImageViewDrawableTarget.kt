package com.svga.glide

import android.animation.ValueAnimator
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.request.SingleRequest
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.opensource.svgaplayer.SVGADynamicEntity
import com.svga.glide.SVGAGlideEx.log

/**
 * Time:2022/11/26 16:07
 * Author: zhouzechao
 * Description:
 */
private val regexModel by lazy { "model=([a-zA-Z0-9://._-]*)?".toRegex() }

open class SVGAImageViewDrawableTarget(
    imageView: ImageView, var times: Int = 0,
    var repeatMode: Int = ValueAnimator.RESTART,
    val dynamicItem: SVGADynamicEntity = SVGADynamicEntity(),
    var svgaCallback: SVGACallback2? = null,
    var showLastFrame: Boolean = false
) : CustomViewTarget<ImageView, SVGAResource>(imageView) {

    private val TAG = "SVGAImageViewDrawableTarget"

    init {
        request?.clear()
        request = null
    }

    override fun onLoadFailed(errorDrawable: Drawable?) {
        clearDrawable("onLoadFailed")
        svgaCallback?.onFailure()
    }

    override fun onResourceReady(
        resource: SVGAResource,
        transition: Transition<in SVGAResource>?
    ) {
        if (resource.model == "") {
            resource.model = " ${(request as? SingleRequest<*>)?.toString()}"
        }
        resource.videoItem ?: kotlin.run {
            svgaCallback?.onFailure()
            return
        }
        val drawable = SVGAAnimationDrawable(
            resource.videoItem, times - 1, repeatMode, dynamicItem, showLastFrame
        )
        drawable.tag = resource.model
        drawable.svgaCallback = svgaCallback
        drawable.scaleType = view.scaleType
        view.setImageDrawable(drawable)
        prepare(resource, drawable)
    }

    private fun prepare(resource: SVGAResource, drawable: SVGAAnimationDrawable) {
        try {
            resource.videoItem ?: return
            val method = resource.videoItem::class.java.methods.firstOrNull { it.name == "prepare\$com_opensource_svgaplayer" }
            method?.invoke(resource.videoItem, {
                drawable.start()
            }, null)
        } catch (_: Throwable) {
        }
    }

    override fun onResourceCleared(placeholder: Drawable?) {
        clearDrawable("")
    }

    private fun clearDrawable(reason: String) {
        (view.drawable as? SVGAAnimationDrawable)?.apply {
            if (reason.isNotEmpty())
                log.d(TAG, "clearDrawable $reason")
        }?.stop()
        view.setImageDrawable(null)
    }

    override fun onStart() {
        (view.drawable as? SVGAAnimationDrawable)?.apply { log.d(TAG, "onStart") }?.resume()
    }

    override fun onStop() {
        (view.drawable as? SVGAAnimationDrawable)?.apply { log.d(TAG, "onStop") }?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        clearDrawable("onDestroy")
    }
}