package com.nixac.z.ui

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.util.Log.INFO
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.util.Pools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Runnable
import java.util.ArrayList
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Emoji rain animates in this layout.
 *
 * @author LuoLiangchen
 * @since 2016/12/13
 * @author NikhilShankar
 */
class NZEmojiShower @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private var mWindowHeight = 0
    private var mWindowWidth = 0
    private var mEmojis: MutableList<String>? = mutableListOf<String>("")
    private var properties = NZEmojiShowerProperties()
    private var mEmojiPool: Pools.SimplePool<AppCompatTextView?>? = null

    fun addCharEmoji(text: String) {
        for (c in text.toCharArray()) {
            mEmojis!!.add(text)
        }
    }

    fun addUnicodeEmoji(unicodeEmoji: Int) {
        mEmojis!!.add(Character.toChars(unicodeEmoji).toString())
    }

    fun addSingleTextEmoji(text: String) {
        mEmojis!!.clear()
        mEmojis!!.add(text)
    }

    fun setShowerProperties(property: NZEmojiShowerProperties) {
        properties = property
        VIEW_MAX_SIZE = dip2px(property.textViewSizeInDp.toFloat())
        VIEW_TEXT_SIZE = dip2px(property.textSizeInDp.toFloat())
    }

    val emojiPoolInitialized = AtomicBoolean(false)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        mWindowWidth = measureDimension(windowWidth, widthMeasureSpec)
        mWindowHeight = measureDimension(windowHeight, heightMeasureSpec)
        if (emojiPoolInitialized.compareAndSet(false, true)) {
            CoroutineScope(Dispatchers.IO).launch {
                initEmojisPool()
            }
        }
    }

    private fun measureDimension(desiredSize: Int, measureSpec: Int): Int {
        var result: Int
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize
        } else {
            result = desiredSize
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize)
            }
        }
        if (result < desiredSize) {
            Log.i(TAG, "Not desired size")
        }
        return result
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.NZEmojiShowerLayout)
        mEmojis = ArrayList()
        properties.count = ta.getInteger(R.styleable.NZEmojiShowerLayout_nzCount, DEFAULT_PER)
        properties.totalDuration =
            ta.getInteger(R.styleable.NZEmojiShowerLayout_nzTotalDuration, DEFAULT_DURATION)
        properties.singleEmojiDuration = ta.getInteger(
            R.styleable.NZEmojiShowerLayout_nzDropDuration,
            DEFAULT_DROP_DURATION
        )
        properties.frequency = ta.getInteger(
            R.styleable.NZEmojiShowerLayout_nzDropFrequency,
            DEFAULT_DROP_FREQUENCY
        )
        ta.recycle()
        mEmojiPool = Pools.SimplePool(MAX_TOTAL_EMOJI_COUNT)
    }

    fun startDropping(dropType: NZEmojiShowerProperties.DROPTYPE) {
        when(dropType) {
            NZEmojiShowerProperties.DROPTYPE.STATIC -> startStaticDropping()
            NZEmojiShowerProperties.DROPTYPE.DYNAMIC -> startDynamicDropping()
        }
    }

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
    val mainHandler = Handler(Looper.getMainLooper())
    private fun startDynamicDropping() {
        var frequescyCompletedCount = 0
        val currentWindowWidth = mWindowWidth
        val currentWindowHeight = mWindowHeight
        val runnable = object : Runnable {
            override fun run() {
                if(frequescyCompletedCount >= properties.frequency) {
                    return
                }
                for (num in 0 until properties.count) {
                    val emoji = mEmojiPool?.acquire()
                    try {
                        configureEmoji(emoji, mEmojis?.get(0))
                        startDropAnimationForSingleEmoji(emoji, windowWidth = currentWindowWidth.toFloat(), windowHeight = currentWindowHeight.toFloat())
                    } catch (e: Exception) {
                        Log.i(TAG, "Emoji pool is empty or encountered a Index out of bounds exception with message : ${e.message}")
                    }
                }
                frequescyCompletedCount++
                mainHandler.postDelayed(this, (properties.totalDuration/properties.frequency).toLong())
            }
        }
        mainHandler.post(runnable)
    }

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
    private fun startStaticDropping() {
        var frequescyCompletedCount = 0
        while(frequescyCompletedCount < properties.frequency) {
            for (num in 0 until properties.count) {
                val emoji = mEmojiPool?.acquire()
                try {
                    configureEmoji(emoji, mEmojis?.get(0))
                    startDropAnimationForSingleEmoji(emoji, (properties.totalDuration / properties.frequency) * (frequescyCompletedCount + num))
                } catch (e: Exception) {
                    Log.i(TAG, "Emoji pool is empty or encountered a Index out of bounds exception with message : ${e.message}")
                }
            }
            frequescyCompletedCount++
        }
    }

    private fun initEmojisPool() {
        for (i in 0 until MAX_TOTAL_EMOJI_COUNT) {
            val emoji = generateEmoji()
            if (emoji != null) {
                mEmojiPool?.release(emoji)
            }
            CoroutineScope(Dispatchers.Main).launch {
                addView(emoji)
            }
        }
    }

    private fun startDropAnimationForSingleEmoji(
        emoji: AppCompatTextView?, offsetTime: Int? = 0,
        windowWidth: Float? = null,
        windowHeight: Float? = null
    ) {
        if (emoji == null) {
            return
        }
        val set = AnimationSet(true)
        val translateAnimation = properties.getTranslateAnimation(
            windowWidth ?: mWindowWidth.toFloat(),
            windowHeight ?: mWindowHeight.toFloat(),
            emoji.width.toFloat(),
            emoji.height.toFloat()
        )
        val rotateAnimation = properties.getRotationAnimation()
        if (rotateAnimation != null) {
            set.addAnimation(rotateAnimation)
        }
        set.addAnimation(translateAnimation)
        set.duration = (properties.singleEmojiDuration * NZRandom.floatAround(
            1f,
            RELATIVE_DROP_DURATION_OFFSET
        )).toInt().toLong()
        set.startTime = 0
        set.startOffset = offsetTime?.toLong() ?: 0
        set.interpolator = AccelerateInterpolator()
        set.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {

            }

            override fun onAnimationEnd(animation: Animation?) {
                configureBackToInitialState(emoji)
                mEmojiPool?.release(emoji)
            }

            override fun onAnimationRepeat(animation: Animation?) {

            }

        })
        emoji.startAnimation(set)
    }

    private val windowHeight: Int
        private get() {
            val windowManager = context.applicationContext
                .getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val point = Point()
            windowManager.defaultDisplay.getSize(point)
            return point.y
        }
    private val windowWidth: Int
        private get() {
            val windowManager = context.applicationContext
                .getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val point = Point()
            windowManager.defaultDisplay.getSize(point)
            return point.x
        }

    private fun generateEmoji(): AppCompatTextView? {
        val emoji = AppCompatTextView(context)
        emoji.id = generateViewId()
        var randomScale = 1.0f
        if (!properties.isfixedSize) {
            randomScale = NZRandom.positiveGaussian().toFloat()
            randomScale = Math.max(randomScale, 0.80f)
        }
        val width = (VIEW_MAX_SIZE * randomScale).toInt()
        val height = (VIEW_MAX_SIZE * randomScale).toInt()
        emoji.setTextSize(TypedValue.COMPLEX_UNIT_PX, VIEW_TEXT_SIZE * randomScale)
        emoji.setTextColor(resources.getColor(R.color.color_333333))
        val params = LayoutParams(width, height)
        params.leftMargin =
            NZRandom.floatInRange(0f, (dp2px(mWindowWidth.toFloat()) - VIEW_MAX_SIZE).toFloat())
                .toInt()
        params.topMargin = (-height * 1.5).toInt()
        emoji.layoutParams = params
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            emoji.elevation = 100f
        }
        return emoji
    }

    private fun configureEmoji(
        emoji: AppCompatTextView?,
        displayChar: String?
    ): AppCompatTextView? {
        if (emoji == null) {
            return null
        }
        emoji.alpha = 1.0f
        emoji.text = displayChar
        return emoji
    }

    private fun configureBackToInitialState(emoji: AppCompatTextView?): AppCompatTextView? {
        if (emoji == null)
            return null
        emoji.alpha = 0.0f
        return emoji
    }

    private fun forceStop() {
        if (mEmojiPool != null) {
            var dirtyEmoji: AppCompatTextView?
            while (mEmojiPool!!.acquire().also { dirtyEmoji = it } != null) {
                removeView(dirtyEmoji)
            }
        }
    }

    private fun dip2px(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp,
            context.resources.displayMetrics
        ).toInt()
    }

    private fun dp2px(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_PX, dp,
            context.resources.displayMetrics
        ).toInt()
    }

    companion object {
        private const val TAG = "NZEMOJISHOWER"
        private var VIEW_MAX_SIZE = 0
        private var VIEW_TEXT_SIZE = 0
        private const val RELATIVE_DROP_DURATION_OFFSET = 0.15f
        private const val DEFAULT_PER = 6
        private const val DEFAULT_DURATION = 8000
        private const val DEFAULT_DROP_DURATION = 2400
        private const val DEFAULT_DROP_FREQUENCY = 500
        private const val MAX_TOTAL_EMOJI_COUNT = 300
    }

    init {
        VIEW_MAX_SIZE = dip2px(36f)
        VIEW_TEXT_SIZE = dip2px(24f)
    }

    init {
        if (!isInEditMode) init(context, attrs)
    }
}