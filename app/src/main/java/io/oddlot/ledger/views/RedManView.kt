package io.oddlot.ledger.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import android.view.View

private const val TAG = "RED MAN VIEW"

class RedManView(context: Context) : View(context) {

    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {

    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        Log.d(TAG, "Drawing Red Man")
    }
}