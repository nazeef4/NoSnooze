package com.nosnooze.alarm.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlin.math.abs

object ImageSimilarity {
    /** Local-only perceptual comparison, tolerant of camera resolution and modest lighting changes. */
    fun compare(referencePath: String, candidatePath: String): Float {
        val a = BitmapFactory.decodeFile(referencePath) ?: return 0f
        val b = BitmapFactory.decodeFile(candidatePath) ?: return 0f
        return try {
            // Relative brightness and edge direction survive broad day/night exposure changes.
            val hashA = hash(a); val hashB = hash(b)
            val sameBits = hashA.indices.count { hashA[it] == hashB[it] } / hashA.size.toFloat()
            val edgesA = edgeHash(a); val edgesB = edgeHash(b)
            val sameEdges = edgesA.indices.count { edgesA[it] == edgesB[it] } / edgesA.size.toFloat()
            val colorA = averageColor(a); val colorB = averageColor(b)
            val colorDistance = (abs(colorA[0] - colorB[0]) + abs(colorA[1] - colorB[1]) + abs(colorA[2] - colorB[2])) / (255f * 3f)
            (sameBits * .55f + sameEdges * .37f + (1f - colorDistance) * .08f).coerceIn(0f, 1f)
        } finally { a.recycle(); b.recycle() }
    }

    private fun hash(source: Bitmap): BooleanArray {
        val bitmap = Bitmap.createScaledBitmap(source, 16, 16, true)
        val values = IntArray(256)
        var total = 0L
        for (y in 0 until 16) for (x in 0 until 16) {
            val p = bitmap.getPixel(x, y)
            val gray = ((p shr 16 and 255) * 30 + (p shr 8 and 255) * 59 + (p and 255) * 11) / 100
            values[y * 16 + x] = gray; total += gray
        }
        if (bitmap !== source) bitmap.recycle()
        val average = total / 256
        return BooleanArray(256) { values[it] >= average }
    }

    private fun edgeHash(source: Bitmap): BooleanArray {
        val bitmap = Bitmap.createScaledBitmap(source, 16, 16, true)
        val gray = IntArray(256)
        for (y in 0 until 16) for (x in 0 until 16) {
            val p = bitmap.getPixel(x, y)
            gray[y * 16 + x] = ((p shr 16 and 255) * 30 + (p shr 8 and 255) * 59 + (p and 255) * 11) / 100
        }
        if (bitmap !== source) bitmap.recycle()
        // Store whether each horizontal edge gets lighter or darker, not its absolute exposure.
        return BooleanArray(16 * 15) { index ->
            val y = index / 15
            val x = index % 15
            gray[y * 16 + x + 1] >= gray[y * 16 + x]
        }
    }

    private fun averageColor(source: Bitmap): IntArray {
        val bitmap = Bitmap.createScaledBitmap(source, 8, 8, true)
        var r = 0; var g = 0; var b = 0
        for (y in 0 until 8) for (x in 0 until 8) { val p = bitmap.getPixel(x, y); r += p shr 16 and 255; g += p shr 8 and 255; b += p and 255 }
        if (bitmap !== source) bitmap.recycle()
        return intArrayOf(r / 64, g / 64, b / 64)
    }
}
