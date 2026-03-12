/*
 * Original: Whisper To Input, Copyright (c) 2023-2025 Yan-Bin Diau, Johnson Sun (GPLv3)
 * STMNA Voice redesign: Copyright (c) 2026 STMNA (GPLv3)
 */

package com.example.whispertoinput.keyboard

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.math.MathUtils
import com.example.whispertoinput.R
import kotlin.math.log10
import kotlin.math.pow

private const val AMPLITUDE_CLAMP_MIN: Int = 10
private const val AMPLITUDE_CLAMP_MAX: Int = 25000
private const val LOG_10_10: Float = 1.0F
private const val LOG_10_25000: Float = 4.398F
private const val AMPLITUDE_ANIMATION_DURATION: Long = 500
private val amplitudePowers: Array<Float> = arrayOf(0.5f, 1.0f, 2f, 3f)

/**
 * STMNA Voice keyboard UI controller.
 *
 * Manages three keyboard states (Idle, Recording, Transcribing) and updates
 * the UI accordingly: brand/status text, mic button appearance, glow ring,
 * cancel/backspace visibility, and amplitude ripple animations.
 */
class WhisperKeyboard {
    private enum class KeyboardStatus {
        Idle,             // Ready to start recording
        Recording,        // Currently recording
        Transcribing,     // Waiting for transcription results
    }

    // Keyboard event listeners
    private var onStartRecording: () -> Unit = { }
    private var onCancelRecording: () -> Unit = { }
    private var onStartTranscribing: (attachToEnd: String) -> Unit = { }
    private var onCancelTranscribing: () -> Unit = { }
    private var onButtonBackspace: () -> Unit = { }
    private var onSwitchIme: () -> Unit = { }
    private var onOpenSettings: () -> Unit = { }
    private var onEnter: () -> Unit = { }
    private var onSpaceBar: () -> Unit = { }
    private var shouldShowRetry: () -> Boolean = { false }

    // Keyboard Status
    private var keyboardStatus: KeyboardStatus = KeyboardStatus.Idle

    // Views & Keyboard Layout
    private var keyboardView: LinearLayout? = null
    private var buttonMic: ImageButton? = null
    private var buttonEnter: ImageButton? = null
    private var buttonCancel: ImageButton? = null
    private var buttonRetry: ImageButton? = null
    private var buttonSpaceBar: TextView? = null
    private var waitingIcon: ProgressBar? = null
    private var buttonBackspace: BackspaceButton? = null
    private var buttonPreviousIme: ImageButton? = null
    private var buttonSettings: ImageButton? = null
    private var micRippleContainer: ConstraintLayout? = null
    private var micRipples: Array<ImageView> = emptyArray()

    // STMNA-specific views
    private var labelBrandStmna: TextView? = null
    private var labelBrandUnderscore: TextView? = null
    private var labelBrandVoice: TextView? = null
    private var labelStatus: TextView? = null
    private var labelStatusDots: TextView? = null
    private var glowRing: View? = null

    // Glow ring pulse animation
    private var glowPulseAnimator: ObjectAnimator? = null

    // Alt theme: black background with gray accents instead of navy + orange
    private var isAltTheme: Boolean = false

    // Top accent bar view reference
    private var topBar: View? = null

    fun setup(
        layoutInflater: LayoutInflater,
        shouldOfferImeSwitch: Boolean,
        isAltTheme: Boolean = false,
        onStartRecording: () -> Unit,
        onCancelRecording: () -> Unit,
        onStartTranscribing: (attachToEnd: String) -> Unit,
        onCancelTranscribing: () -> Unit,
        onButtonBackspace: () -> Unit,
        onEnter: () -> Unit,
        onSpaceBar: () -> Unit,
        onSwitchIme: () -> Unit,
        onOpenSettings: () -> Unit,
        shouldShowRetry: () -> Boolean,
    ): View {
        // Inflate the keyboard layout & assign views
        keyboardView = layoutInflater.inflate(R.layout.keyboard_view, null) as LinearLayout
        buttonMic = keyboardView!!.findViewById(R.id.btn_mic)
        buttonEnter = keyboardView!!.findViewById(R.id.btn_enter)
        buttonCancel = keyboardView!!.findViewById(R.id.btn_cancel)
        buttonRetry = keyboardView!!.findViewById(R.id.btn_retry)
        buttonSpaceBar = keyboardView!!.findViewById(R.id.btn_space_bar)
        waitingIcon = keyboardView!!.findViewById(R.id.pb_waiting_icon)
        buttonBackspace = keyboardView!!.findViewById(R.id.btn_backspace)
        buttonPreviousIme = keyboardView!!.findViewById(R.id.btn_previous_ime)
        buttonSettings = keyboardView!!.findViewById(R.id.btn_settings)
        micRippleContainer = keyboardView!!.findViewById(R.id.mic_ripples)
        micRipples = arrayOf(
            keyboardView!!.findViewById(R.id.mic_ripple_0),
            keyboardView!!.findViewById(R.id.mic_ripple_1),
            keyboardView!!.findViewById(R.id.mic_ripple_2),
            keyboardView!!.findViewById(R.id.mic_ripple_3),
        )

        // STMNA brand/status views
        labelBrandStmna = keyboardView!!.findViewById(R.id.label_brand_stmna)
        labelBrandUnderscore = keyboardView!!.findViewById(R.id.label_brand_underscore)
        labelBrandVoice = keyboardView!!.findViewById(R.id.label_brand_voice)
        labelStatus = keyboardView!!.findViewById(R.id.label_status)
        labelStatusDots = keyboardView!!.findViewById(R.id.label_status_dots)
        glowRing = keyboardView!!.findViewById(R.id.glow_ring)
        topBar = keyboardView!!.findViewById(R.id.view_top_bar)

        // Apply alt theme colors (black bg, gray accents)
        this.isAltTheme = isAltTheme
        if (isAltTheme) {
            applyAltTheme()
        }

        // Hide IME switch if not available
        if (!shouldOfferImeSwitch) {
            buttonPreviousIme!!.visibility = View.GONE
        }

        // Set onClick listeners
        buttonMic!!.setOnClickListener { onButtonMicClick() }
        buttonEnter!!.setOnClickListener { onButtonEnterClick() }
        buttonCancel!!.setOnClickListener { onButtonCancelClick() }
        buttonRetry!!.setOnClickListener { onButtonRetryClick() }
        buttonSettings!!.setOnClickListener { onButtonSettingsClick() }
        buttonBackspace!!.setBackspaceCallback { onButtonBackspaceClick() }
        buttonSpaceBar!!.setOnClickListener { onButtonSpaceBarClick() }

        if (shouldOfferImeSwitch) {
            buttonPreviousIme!!.setOnClickListener { onButtonPreviousImeClick() }
        }

        // Set event listeners
        this.onStartRecording = onStartRecording
        this.onCancelRecording = onCancelRecording
        this.onStartTranscribing = onStartTranscribing
        this.onCancelTranscribing = onCancelTranscribing
        this.onButtonBackspace = onButtonBackspace
        this.onSwitchIme = onSwitchIme
        this.onOpenSettings = onOpenSettings
        this.onEnter = onEnter
        this.onSpaceBar = onSpaceBar
        this.shouldShowRetry = shouldShowRetry

        // Reset to idle
        reset()

        return keyboardView!!
    }

    fun reset() {
        setKeyboardStatus(KeyboardStatus.Idle)
    }

    /**
     * Update the alt theme setting at runtime (e.g., when keyboard re-shown after settings change).
     * Reapplies theme colors without recreating the view.
     */
    fun updateAltTheme(altTheme: Boolean) {
        if (this.isAltTheme == altTheme) return
        this.isAltTheme = altTheme
        if (altTheme) {
            applyAltTheme()
        } else {
            // Revert to default theme colors
            val ctx = keyboardView?.context ?: return
            val secondaryColor = ContextCompat.getColor(ctx, R.color.text_secondary)
            keyboardView?.setBackgroundColor(ContextCompat.getColor(ctx, R.color.bg_primary))
            topBar?.setBackgroundResource(R.drawable.bg_top_accent_bar)
            buttonSpaceBar?.setBackgroundResource(R.drawable.bg_spacebar)
            // Revert cancel/retry button backgrounds to default
            buttonCancel?.setBackgroundResource(R.drawable.bg_cancel_button)
            buttonRetry?.setBackgroundResource(R.drawable.bg_cancel_button)
            // Revert icons to default gray
            buttonPreviousIme?.setColorFilter(secondaryColor)
            buttonSettings?.setColorFilter(secondaryColor)
            buttonEnter?.setColorFilter(secondaryColor)
            buttonBackspace?.setColorFilter(secondaryColor)
            buttonCancel?.setColorFilter(secondaryColor)
            buttonRetry?.setColorFilter(secondaryColor)
        }
        // Re-apply current keyboard status to update mic button colors
        setKeyboardStatus(keyboardStatus)
    }

    fun updateMicrophoneAmplitude(amplitude: Int) {
        if (keyboardStatus != KeyboardStatus.Recording) {
            return
        }

        val clampedAmplitude = MathUtils.clamp(
            amplitude,
            AMPLITUDE_CLAMP_MIN,
            AMPLITUDE_CLAMP_MAX
        )

        // Decibel-like normalization (0 to 1)
        val normalizedPower =
            (log10(clampedAmplitude * 1f) - LOG_10_10) / (LOG_10_25000 - LOG_10_10)

        // Inner ripples are most sensitive (gamma curve)
        for (micRippleIdx in micRipples.indices) {
            micRipples[micRippleIdx].clearAnimation()
            micRipples[micRippleIdx].alpha = normalizedPower.pow(amplitudePowers[micRippleIdx])
            micRipples[micRippleIdx].animate().alpha(0f).setDuration(AMPLITUDE_ANIMATION_DURATION)
                .start()
        }
    }

    fun tryStartRecording() {
        if (keyboardStatus == KeyboardStatus.Idle) {
            setKeyboardStatus(KeyboardStatus.Recording)
            onStartRecording()
        }
    }

    fun tryCancelRecording() {
        if (keyboardStatus == KeyboardStatus.Recording) {
            setKeyboardStatus(KeyboardStatus.Idle)
            onCancelRecording()
        }
    }

    fun tryStartTranscribing(attachToEnd: String) {
        if (keyboardStatus == KeyboardStatus.Recording) {
            setKeyboardStatus(KeyboardStatus.Transcribing)
            onStartTranscribing(attachToEnd)
        }
    }

    private fun onButtonSpaceBarClick() {
        if (keyboardStatus == KeyboardStatus.Recording) {
            setKeyboardStatus(KeyboardStatus.Transcribing)
            onStartTranscribing(" ")
        } else {
            onSpaceBar()
        }
    }

    private fun onButtonBackspaceClick() {
        this.onButtonBackspace()
    }

    private fun onButtonPreviousImeClick() {
        this.onSwitchIme()
    }

    private fun onButtonSettingsClick() {
        this.onOpenSettings()
    }

    private fun onButtonMicClick() {
        when (keyboardStatus) {
            KeyboardStatus.Idle -> {
                setKeyboardStatus(KeyboardStatus.Recording)
                onStartRecording()
            }
            KeyboardStatus.Recording -> {
                setKeyboardStatus(KeyboardStatus.Transcribing)
                onStartTranscribing("")
            }
            KeyboardStatus.Transcribing -> {
                return
            }
        }
    }

    private fun onButtonEnterClick() {
        if (keyboardStatus == KeyboardStatus.Recording) {
            setKeyboardStatus(KeyboardStatus.Transcribing)
            onStartTranscribing("\r\n")
        } else {
            onEnter()
        }
    }

    private fun onButtonCancelClick() {
        if (keyboardStatus == KeyboardStatus.Recording) {
            setKeyboardStatus(KeyboardStatus.Idle)
            onCancelRecording()
        } else if (keyboardStatus == KeyboardStatus.Transcribing) {
            setKeyboardStatus(KeyboardStatus.Idle)
            onCancelTranscribing()
        }
    }

    private fun onButtonRetryClick() {
        if (keyboardStatus == KeyboardStatus.Idle) {
            setKeyboardStatus(KeyboardStatus.Transcribing)
            onStartTranscribing("")
        }
    }

    /**
     * Updates UI for each keyboard state:
     * - Idle: brand text visible, muted mic button, no glow
     * - Recording: "Listening..." status, vibrant mic, orange glow ring pulse
     * - Transcribing: "Transcribing..." status, surface mic, gold glow ring pulse, spinner
     */
    private fun setKeyboardStatus(newStatus: KeyboardStatus) {
        // Stop any existing glow animation
        glowPulseAnimator?.cancel()
        glowPulseAnimator = null

        when (newStatus) {
            KeyboardStatus.Idle -> {
                // Show brand text, hide status
                showBrandText(true)
                showStatusText(false)

                // Mic button: muted orange (or gray in alt theme)
                if (isAltTheme) {
                    val ctx = keyboardView!!.context
                    buttonMic!!.background = makeCircle(ContextCompat.getColor(ctx, R.color.alt_mic_button))
                } else {
                    buttonMic!!.setBackgroundResource(R.drawable.bg_mic_button_idle)
                }
                buttonMic!!.setImageResource(R.drawable.mic_idle)
                buttonMic!!.visibility = View.VISIBLE

                // Hide glow ring, spinner, cancel; maybe show retry
                glowRing!!.visibility = View.INVISIBLE
                waitingIcon!!.visibility = View.INVISIBLE
                buttonCancel!!.visibility = View.INVISIBLE
                buttonRetry!!.visibility = if (shouldShowRetry()) View.VISIBLE else View.INVISIBLE
                micRippleContainer!!.visibility = View.GONE
                keyboardView!!.keepScreenOn = false
            }

            KeyboardStatus.Recording -> {
                // Show "Listening..." status, hide brand
                showBrandText(false)
                showStatusText(true, R.string.recording)

                // Mic button: vibrant Ember Orange in both themes during recording
                buttonMic!!.setBackgroundResource(R.drawable.bg_mic_button_recording)
                buttonMic!!.setImageResource(R.drawable.mic_pressed)
                buttonMic!!.visibility = View.VISIBLE

                // Show glow ring (recording style) with pulse animation
                glowRing!!.setBackgroundResource(R.drawable.bg_glow_ring_recording)
                glowRing!!.visibility = View.VISIBLE
                startGlowPulse()

                waitingIcon!!.visibility = View.INVISIBLE
                buttonCancel!!.visibility = View.VISIBLE
                buttonRetry!!.visibility = View.INVISIBLE
                micRippleContainer!!.visibility = View.VISIBLE
                keyboardView!!.keepScreenOn = true
            }

            KeyboardStatus.Transcribing -> {
                // Show "Transcribing..." status, hide brand
                showBrandText(false)
                showStatusText(true, R.string.transcribing)

                // Mic button: surface color (or dark gray in alt theme)
                if (isAltTheme) {
                    val ctx = keyboardView!!.context
                    buttonMic!!.background = makeCircle(ContextCompat.getColor(ctx, R.color.alt_mic_button))
                } else {
                    buttonMic!!.setBackgroundResource(R.drawable.bg_mic_button_transcribing)
                }
                buttonMic!!.setImageResource(R.drawable.mic_transcribing)
                buttonMic!!.visibility = View.VISIBLE

                // Show glow ring (transcribing style) with pulse animation
                glowRing!!.setBackgroundResource(R.drawable.bg_glow_ring_transcribing)
                glowRing!!.visibility = View.VISIBLE
                startGlowPulse()

                waitingIcon!!.visibility = View.VISIBLE
                buttonCancel!!.visibility = View.VISIBLE
                buttonRetry!!.visibility = View.INVISIBLE
                micRippleContainer!!.visibility = View.GONE
                keyboardView!!.keepScreenOn = true
            }
        }

        keyboardStatus = newStatus
    }

    /** Toggle brand text (STMNA_Voice) visibility */
    private fun showBrandText(visible: Boolean) {
        val vis = if (visible) View.VISIBLE else View.GONE
        labelBrandStmna?.visibility = vis
        labelBrandUnderscore?.visibility = vis
        labelBrandVoice?.visibility = vis
    }

    /** Toggle status text ("Listening..." / "Transcribing...") visibility */
    private fun showStatusText(visible: Boolean, stringResId: Int = 0) {
        if (visible && stringResId != 0) {
            labelStatus?.setText(stringResId)
            labelStatus?.visibility = View.VISIBLE
            labelStatusDots?.visibility = View.VISIBLE
        } else {
            labelStatus?.visibility = View.GONE
            labelStatusDots?.visibility = View.GONE
        }
    }

    /**
     * Apply alt theme colors: pure black background, gray accents, white icons.
     * Underscore stays orange per Pencil design.
     */
    private fun applyAltTheme() {
        val ctx = keyboardView?.context ?: return
        val altBg = ContextCompat.getColor(ctx, R.color.alt_bg_primary)
        val altTopBar = ContextCompat.getColor(ctx, R.color.alt_top_bar)
        val altSurface = ContextCompat.getColor(ctx, R.color.alt_bg_surface)
        val white = ContextCompat.getColor(ctx, R.color.text_primary)

        // Keyboard background → pure black
        keyboardView?.setBackgroundColor(altBg)

        // Top accent bar → solid gray (replaces gradient)
        topBar?.setBackgroundColor(altTopBar)

        // Spacebar → match mic button gray
        buttonSpaceBar?.let { bar ->
            val spacebarBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(altSurface)
                cornerRadius = 4f * ctx.resources.displayMetrics.density
            }
            bar.background = spacebarBg
        }

        // Cancel/retry button backgrounds → gray (matching alt theme surface)
        val cancelBg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(altSurface)
            setSize(40, 40)
        }
        buttonCancel?.background = cancelBg
        val retryBg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(altSurface)
            setSize(40, 40)
        }
        buttonRetry?.background = retryBg

        // Icons → white in alt theme (instead of gray)
        buttonPreviousIme?.setColorFilter(white)
        buttonSettings?.setColorFilter(white)
        buttonEnter?.setColorFilter(white)
        buttonBackspace?.setColorFilter(white)
        buttonCancel?.setColorFilter(white)
        buttonRetry?.setColorFilter(white)
    }

    /**
     * Create a circular GradientDrawable with the given color.
     * Used for alt theme mic button backgrounds.
     */
    private fun makeCircle(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setSize(88, 88)
        }
    }

    /** Animate the glow ring with a breathing pulse effect */
    private fun startGlowPulse() {
        glowRing?.let { ring ->
            val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.08f, 1.0f)
            val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.08f, 1.0f)
            val alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.6f, 0.9f, 0.6f)

            glowPulseAnimator = ObjectAnimator.ofPropertyValuesHolder(ring, scaleX, scaleY, alpha).apply {
                duration = 2000
                repeatCount = ObjectAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }
        }
    }
}
