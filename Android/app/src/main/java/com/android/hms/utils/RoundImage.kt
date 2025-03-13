package com.android.hms.utils

/**
 * Created by SivaMalini on 16-03-2018.
 */

import android.graphics.*
import android.graphics.drawable.Drawable

class RoundImage(val bitmap: Bitmap) : Drawable() {
    private val paint = Paint()
    private val rectF = RectF()
    private val bitmapWidth: Int
    private val bitmapHeight: Int

    init {
        paint.isAntiAlias = true
        paint.isDither = true
        val shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        paint.shader = shader

        bitmapWidth = this.bitmap.width
        bitmapHeight = this.bitmap.height
    }

    override fun draw(canvas: Canvas) {
        canvas.drawOval(rectF, paint)
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        rectF.set(bounds)
    }

    override fun setAlpha(alpha: Int) {
        if (paint.alpha != alpha) {
            paint.alpha = alpha
            invalidateSelf()
        }
    }

    override fun setColorFilter(cf: ColorFilter?) {
        paint.colorFilter = cf
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    /*    @Override
    public int getIntrinsicWidth() {
        return bitmapWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return bitmapHeight;
    }  */

    fun setAntiAlias(aa: Boolean) {
        paint.isAntiAlias = aa
        invalidateSelf()
    }

    override fun setFilterBitmap(filter: Boolean) {
        paint.isFilterBitmap = filter
        invalidateSelf()
    }

    override fun setDither(dither: Boolean) {
        paint.isDither = dither
        invalidateSelf()
    }

    override fun getIntrinsicWidth(): Int {
        return if (bitmapWidth < bitmapHeight) bitmapWidth else bitmapHeight
    }

    override fun getIntrinsicHeight(): Int {
        return if (bitmapHeight < bitmapWidth) bitmapHeight else bitmapWidth
    }
}