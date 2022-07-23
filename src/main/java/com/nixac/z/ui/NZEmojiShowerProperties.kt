package com.nixac.z.ui

import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.view.animation.TranslateAnimation
import kotlin.random.Random

/**
 * Created by nikhil shankar on 07/07/21 at 5:07 PM.
 *
 */
class NZEmojiShowerProperties {

    var direction : Direction = Direction.TOP_TO_BOTTOM
    var rotation: Rotation = Rotation.NORMAL
    var singleEmojiDuration : Int = 3000
    var totalDuration : Int = 10000
    var frequency : Int = 50
    var count: Int = 5
    var shouldStartOutsideFrame = true
    var originPointType = EmojiInitialMarginType.DEFAULT
    var isfixedSize = false
    var textSizeInDp : Int = 24
    var textViewSizeInDp : Int = 36
    var shouldEndOutsideFrame = false
    var freeFallStyle: FreeFallStyle = FreeFallStyle.SWAY

    val randomDelay = Random(100)

    enum class FreeFallStyle {
        SWAY,
        GRAVITY
    }

    enum class Direction {
        TOP_TO_BOTTOM,
        BOTTOM_TO_TOP,
    }

    enum class EmojiInitialMarginType {
        DEFAULT, MIDDLE
    }

    enum class Rotation {
        NORMAL,
        NONE
    }

    public fun getTranslateAnimation(width: Float, height: Float, viewWidth: Float = 0.0f, viewHeight: Float = 0.0f ) : TranslateAnimation {
        val fromYValueBottomToTop = if(shouldStartOutsideFrame) height + (viewHeight * 1.5f) else height
        val fromYValueTopToBottom = if(shouldStartOutsideFrame)  -1.0f * (viewHeight * 1.5f) else 0.0f
        val translateAnimation = when(direction) {
            Direction.BOTTOM_TO_TOP -> {
                TranslateAnimation(
                    Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, if(freeFallStyle == FreeFallStyle.SWAY) NZRandom.floatAround(0f, 2f) else 0.0f,
                    Animation.ABSOLUTE, fromYValueBottomToTop,
                    Animation.ABSOLUTE,  if(shouldEndOutsideFrame) -(viewHeight*1.5f) else 0.0f)
            }

            Direction.TOP_TO_BOTTOM -> {
                TranslateAnimation(
                    Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, if(freeFallStyle == FreeFallStyle.SWAY) NZRandom.floatAround(0f, 2f) else 0.0f,
                    Animation.ABSOLUTE, fromYValueTopToBottom,
                    Animation.ABSOLUTE, height + (if (shouldEndOutsideFrame) (viewHeight * 1.5f) else 0.0f))
            }
        }
        return translateAnimation
    }

    public fun getRotationAnimation() : RotateAnimation? {
        val rotationAnimation = when(rotation) {
            Rotation.NORMAL -> {
                RotateAnimation(NZRandom.floatAround(0f, -1f) * NZRandom.floatAround(0f, 360f), NZRandom.floatAround(0f, -1f) * NZRandom.floatAround(0f, 360f),
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f)
            }

            Rotation.NONE -> {
                null
            }
        }
        return rotationAnimation
    }

    fun getDelay() : Int {
        return randomDelay.nextInt() * 30
    }

    enum class DROPTYPE {
        /*
        Static dropping means all textviews are assigned the animation values
        immediately. This means that while in the process of dropping all emojis would have the same
        height and width and thereby the same translation speed.
        Scenario in which it affects :
        Lets say the the shower layout is getting recalculated in size ( eg. due to activity adjustResize )
        then using STATIC drop type wouldnt alter the speed in which the remaining emojis are animated.
        PROS : ANIMATIONS will Respect the start state regardless of redraws.
        CONS : Lower in performace when compared to Dynamic since all necessary emojis are calculated at once.
         */
        STATIC,
        /*
        Dynamic dropping means all textviews are assigned the animation values
        as and when those emojis start animating. This means that while in the process of dropping each emojis
        can have different height width and other animation values in case the layout changes is size like in the below scenario.
        Scenario in which it affects :
        Lets say the the shower layout is getting recalculated in size ( eg. due to activity adjustResize )
        then using DYNAMIC drop type would alter the speed in which the remaining emojis are animated.
        When to use dynamic ?
        Cases where redraw of the layout is not expected this is a better drop mechanism since emojis
        (TextViews) are not calculated and updated at once which could cause inefficiency.
        PROS : Better performance since emojis ( TextViews ) are updated linearly over time.
        CONS : Animations will alter in case the layout height or width changes during animation.
         */
        DYNAMIC
    }


}