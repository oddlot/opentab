package io.oddlot.ledger.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View

private const val TAG = "RED MAN VIEW"

class RedManView
    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : View(context, attrs, defStyleAttr) {

    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {

    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        Log.d(TAG, "Drawing Red Man")
    }
}