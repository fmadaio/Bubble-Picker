package com.igalata.bubblepicker.model

import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.support.annotation.ColorInt

/**
 * Created by irinagalata on 1/19/17.
 */
data class PickerItem @JvmOverloads constructor(
        var id: String = "",
        var title: String? = null,
        var icon: Drawable? = null,
        var iconOnTop: Boolean = true,
        @ColorInt var color: Int? = null,
        var gradient: BubbleGradient? = null,
        var overlayAlpha: Float = 0.5f,
        var typeface: Typeface = Typeface.DEFAULT,
        @ColorInt var textColor: Int? = null,
        var textSize: Float = 40f,
        var backgroundImage: Drawable? = null,
        @ColorInt var backgroundColor: Int? = null,
        var radius: Float = 0f,
        var isSelected: Boolean = false)