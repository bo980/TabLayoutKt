package com.liang.tablayoutkt

import android.content.Context
import android.graphics.*
import com.liang.tabs.ItemDecoration


class TestDecoration :
    ItemDecoration() {


    override fun getItemOffsets(outRect: Rect) {
        outRect.set(0, 0, 40, 60)
    }

    override fun onDraw(canvas: Canvas, rect: Rect) {

        val mShader: Shader = LinearGradient(
            0f, 0f, 40f, 60f, intArrayOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW),
            null, Shader.TileMode.REPEAT
        )
        paint.shader = mShader
        paint.style = Paint.Style.FILL

        paint.setShadowLayer(45f, 10f, 10f, Color.BLACK)
        canvas.drawRect(rect, paint)
    }
}

class ImgDecoration(private val context: Context, private val img: Int) :
    ItemDecoration() {


    override fun getItemOffsets(outRect: Rect) {
        outRect.set(0, 0, 40, 0)
    }

    override fun onDraw(canvas: Canvas, rect: Rect) {
        val bitmap = BitmapFactory.decodeResource(context.resources, img)
        canvas.drawBitmap(bitmap, null, rect, paint)

    }
}