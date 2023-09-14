package org.dslul.openboard.inputmethod.latin.common

import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toBitmap
import org.dslul.openboard.inputmethod.keyboard.KeyboardTheme.THEME_STYLE_HOLO
import org.dslul.openboard.inputmethod.keyboard.KeyboardTheme.THEME_STYLE_MATERIAL
import org.dslul.openboard.inputmethod.keyboard.MainKeyboardView
import org.dslul.openboard.inputmethod.keyboard.MoreKeysKeyboardView
import org.dslul.openboard.inputmethod.keyboard.clipboard.ClipboardHistoryView
import org.dslul.openboard.inputmethod.keyboard.emoji.EmojiPageKeyboardView
import org.dslul.openboard.inputmethod.keyboard.emoji.EmojiPalettesView
import org.dslul.openboard.inputmethod.latin.InputView
import org.dslul.openboard.inputmethod.latin.KeyboardWrapperView
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.suggestions.MoreSuggestionsView
import org.dslul.openboard.inputmethod.latin.suggestions.SuggestionStripView
import org.dslul.openboard.inputmethod.latin.utils.*

class Colors (
    val themeStyle: String,
    val hasKeyBorders: Boolean,
    val accent: Int,
    val background: Int,
    val keyBackground: Int,
    val functionalKey: Int,
    val spaceBar: Int,
    val keyText: Int,
    val keyHintText: Int
) {
    val navBar: Int
    val adjustedBackground: Int
    val adjustedKeyText: Int
    val spaceBarText: Int

    // todo (later): evaluate which colors, colorFilters and colorStateLists are actually necessary
    //  also, ideally the color filters would be private and chosen internally depending on type
    val backgroundFilter: ColorFilter
    val adjustedBackgroundFilter: ColorFilter
    val keyBackgroundFilter: ColorFilter
    val functionalKeyBackgroundFilter: ColorFilter
    val spaceBarFilter: ColorFilter
    val keyTextFilter: ColorFilter
    val accentColorFilter: ColorFilter
    val actionKeyIconColorFilter: ColorFilter?
    val clipboardPinFilter: ColorFilter?

    private val backgroundStateList: ColorStateList
    private val keyStateList: ColorStateList
    private val functionalKeyStateList: ColorStateList
    private val actionKeyStateList: ColorStateList
    private val spaceBarStateList: ColorStateList
    private val adjustedBackgroundStateList: ColorStateList

    val keyboardBackground: Drawable?

    init {
        accentColorFilter = colorFilter(accent)
        if (themeStyle == THEME_STYLE_HOLO) {
            val darkerBackground = adjustLuminosityAndKeepAlpha(background, -0.2f)
            navBar = darkerBackground
            keyboardBackground = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(background, darkerBackground))
            spaceBarText = keyText
            clipboardPinFilter = accentColorFilter
        } else {
            navBar = background
            keyboardBackground = null
            spaceBarText = keyHintText
            clipboardPinFilter = null
        }

        // create color filters, todo: maybe better / simplify
        val states = arrayOf(intArrayOf(android.R.attr.state_pressed), intArrayOf(-android.R.attr.state_pressed))
        fun stateList(pressed: Int, normal: Int) =
            ColorStateList(states, intArrayOf(pressed, normal))
        // todo (idea): make better use of the states?
        //  could also use / create StateListDrawables in colors (though that's a style than a color...)
        //  this would better allow choosing e.g. cornered/rounded drawables for moreKeys or moreSuggestions
        backgroundFilter = colorFilter(background)
        adjustedKeyText = brightenOrDarken(keyText, true)

        // color to be used if exact background color would be bad contrast, e.g. more keys popup or no border space bar
        if (isDarkColor(background)) {
            adjustedBackground = brighten(background)
            adjustedBackgroundStateList = stateList(brighten(adjustedBackground), adjustedBackground)
        } else {
            adjustedBackground = darken(background)
            adjustedBackgroundStateList = stateList(darken(adjustedBackground), adjustedBackground)
        }
        adjustedBackgroundFilter = colorFilter(adjustedBackground)
        if (hasKeyBorders) {
            keyBackgroundFilter = colorFilter(keyBackground)
            functionalKeyBackgroundFilter = colorFilter(functionalKey)
            spaceBarFilter = colorFilter(spaceBar)
            backgroundStateList = stateList(brightenOrDarken(background, true), background)
            keyStateList = if (themeStyle == THEME_STYLE_HOLO) stateList(keyBackground, keyBackground)
                else stateList(brightenOrDarken(keyBackground, true), keyBackground)
            functionalKeyStateList = stateList(brightenOrDarken(functionalKey, true), functionalKey)
            actionKeyStateList = if (themeStyle == THEME_STYLE_HOLO) functionalKeyStateList
                else stateList(brightenOrDarken(accent, true), accent)
            spaceBarStateList = if (themeStyle == THEME_STYLE_HOLO) stateList(spaceBar, spaceBar)
                else stateList(brightenOrDarken(spaceBar, true), spaceBar)
        } else {
            // need to set color to background if key borders are disabled, or there will be ugly keys
            keyBackgroundFilter = backgroundFilter
            functionalKeyBackgroundFilter = keyBackgroundFilter
            spaceBarFilter = colorFilter(spaceBar)
            backgroundStateList = stateList(brightenOrDarken(background, true), background)
            keyStateList = backgroundStateList
            functionalKeyStateList = backgroundStateList
            actionKeyStateList = if (themeStyle == THEME_STYLE_HOLO) functionalKeyStateList
                else stateList(brightenOrDarken(accent, true), accent)
            spaceBarStateList = stateList(brightenOrDarken(spaceBar, true), spaceBar)
        }
        keyTextFilter = colorFilter(keyText, BlendModeCompat.SRC_ATOP)
        actionKeyIconColorFilter = when {
            themeStyle == THEME_STYLE_HOLO -> keyTextFilter
            // the white icon may not have enough contrast, and can't be adjusted by the user
            isBrightColor(accent) -> colorFilter(Color.DKGRAY, BlendModeCompat.SRC_ATOP)
            else -> null
        }
    }

    /** set background colors including state list to the drawable  */
    fun setBackgroundColor(background: Drawable, type: BackgroundType) {
        val colorStateList = when (type) {
            BackgroundType.BACKGROUND -> backgroundStateList
            BackgroundType.KEY -> keyStateList
            BackgroundType.FUNCTIONAL -> functionalKeyStateList
            BackgroundType.ACTION -> actionKeyStateList
            BackgroundType.SPACE -> spaceBarStateList
            BackgroundType.ADJUSTED_BACKGROUND -> adjustedBackgroundStateList
            BackgroundType.SUGGESTION -> if (!hasKeyBorders && themeStyle == THEME_STYLE_MATERIAL)
                    adjustedBackgroundStateList
                else backgroundStateList
            BackgroundType.ACTION_MORE_KEYS -> if (themeStyle == THEME_STYLE_HOLO)
                    adjustedBackgroundStateList
                else actionKeyStateList
        }
        DrawableCompat.setTintMode(background, PorterDuff.Mode.MULTIPLY)
        DrawableCompat.setTintList(background, colorStateList)
    }

    // using !! for the color filter because null is only returned for unsupported modes, which are not used
    private fun colorFilter(color: Int, mode: BlendModeCompat = BlendModeCompat.MODULATE): ColorFilter =
        BlendModeColorFilterCompat.createBlendModeColorFilterCompat(color, mode)!!

    fun getDrawable(type: BackgroundType, attr: TypedArray): Drawable {
        val drawable = when (type) {
            BackgroundType.KEY, BackgroundType.ADJUSTED_BACKGROUND, BackgroundType.BACKGROUND,
            BackgroundType.SUGGESTION, BackgroundType.ACTION_MORE_KEYS ->
                attr.getDrawable(R.styleable.KeyboardView_keyBackground)
            BackgroundType.FUNCTIONAL -> attr.getDrawable(R.styleable.KeyboardView_functionalKeyBackground)
            BackgroundType.SPACE -> attr.getDrawable(R.styleable.KeyboardView_spacebarBackground)
            BackgroundType.ACTION -> {
                if (themeStyle == THEME_STYLE_HOLO && hasKeyBorders) // no borders has a very small pressed drawable otherwise
                    attr.getDrawable(R.styleable.KeyboardView_functionalKeyBackground)
                else
                    attr.getDrawable(R.styleable.KeyboardView_keyBackground)
            }
        }?.mutate() ?: attr.getDrawable(R.styleable.KeyboardView_keyBackground)?.mutate()!! // keyBackground always exists

        setBackgroundColor(drawable, type)
        return drawable
    }

    fun setKeyboardBackground(view: View) {
        when (view) {
            is MoreSuggestionsView -> view.background.colorFilter = backgroundFilter
            is MoreKeysKeyboardView -> view.background.colorFilter = adjustedBackgroundFilter
            is SuggestionStripView -> setBackgroundColor(view.background, BackgroundType.SUGGESTION) // todo: maybe change?
            is EmojiPageKeyboardView, // to make EmojiPalettesView background visible, which does not scroll
            is MainKeyboardView -> view.setBackgroundColor(Color.TRANSPARENT) // otherwise causes issues with wrapper view when using one-handed mode
            is KeyboardWrapperView -> {
                val bg = ContextCompat.getDrawable(view.context, R.drawable.setup_welcome_image)!!
                // suggestion strip height: config_suggestions_strip_height
                view.background = BitmapDrawable(bg.toBitmap(height = view.height, width = view.width))
            }
            is EmojiPalettesView, is ClipboardHistoryView -> {
                // todo: adjust size
                view.background = ContextCompat.getDrawable(view.context, R.drawable.setup_welcome_image)
            }

            /* this is not working, sets the image on the full screen. maybe mInputViewRect helps
            is EmojiPalettesView, is ClipboardHistoryView, is KeyboardWrapperView -> view.setBackgroundColor(Color.TRANSPARENT)
            is InputView -> {
                val bg = ContextCompat.getDrawable(view.context, R.drawable.setup_welcome_image)!!
                view.background = BitmapDrawable(bg.toBitmap(height = view.height, width = view.width))
            }
            */

            else -> view.background.colorFilter = backgroundFilter
        }
    }

}

enum class BackgroundType {
    BACKGROUND, KEY, FUNCTIONAL, ACTION, ACTION_MORE_KEYS, SPACE, ADJUSTED_BACKGROUND, SUGGESTION
}
