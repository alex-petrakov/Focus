package me.alex.pet.apps.focus.presentation.timer

import android.app.Application
import android.content.Context
import android.text.Annotation
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.graphics.ColorUtils
import androidx.core.text.getSpans
import androidx.core.text.set
import androidx.core.text.toSpanned
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import me.alex.pet.apps.focus.R
import me.alex.pet.apps.focus.common.SingleLiveEvent
import me.alex.pet.apps.focus.common.extensions.getColorCompat
import me.alex.pet.apps.focus.domain.Pomodoro
import me.alex.pet.apps.focus.domain.SessionType
import me.alex.pet.apps.focus.domain.Timer.State


class TimerModel(
        private val app: Application,
        private val pomodoro: Pomodoro
) : ViewModel() {

    private val _viewState = MutableLiveData<ViewState>()

    val viewState: LiveData<ViewState> get() = _viewState

    private val _viewEffect = SingleLiveEvent<ViewEffect>()

    val viewEffect: LiveData<ViewEffect> get() = _viewEffect

    private val pomodoroObserver = object : Pomodoro.Observer {
        override fun onUpdate() {
            _viewState.value = pomodoro.toViewState(app)
        }
    }

    init {
        _viewState.value = pomodoro.toViewState(app)
        pomodoro.addObserver(pomodoroObserver)
    }

    fun onToggleTimer() {
        if (pomodoro.isAwaitingSessionSwitch) {
            pomodoro.startNextSession()
        } else {
            pomodoro.toggleSession()
            _viewEffect.value = ViewEffect.START_NOTIFICATIONS
        }
    }

    fun onReset() {
        pomodoro.reset()
    }

    override fun onCleared() {
        pomodoro.removeObserver(pomodoroObserver)
    }
}


private fun Pomodoro.toViewState(context: Context): ViewState {
    return ViewState(
            toTimerViewState(),
            toTransitionPromptViewState(context),
            toSessionCountViewState(),
            toSessionIndicatorViewState(),
            toToggleBtnViewState(),
            toResetBtnViewState()
    )
}

private fun Pomodoro.toResetBtnViewState(): ViewState.ResetButton {
    val isVisible = timerState == State.PAUSED || isAwaitingSessionSwitch
    return ViewState.ResetButton(isVisible)
}

private fun Pomodoro.toTransitionPromptViewState(context: Context): ViewState.TransitionPrompt {
    val styledText = when (nextSessionType) {
        SessionType.WORK -> R.string.app_ready_to_start_a_work_session
        else -> R.string.app_ready_to_take_a_break
    }.let { resId -> context.getStyledSpannable(resId) }
    return ViewState.TransitionPrompt(
            isAwaitingSessionSwitch,
            styledText
    )
}

private fun Pomodoro.toTimerViewState(): ViewState.Timer {
    return ViewState.Timer(
            !isAwaitingSessionSwitch,
            remainingDuration.seconds.toString(),
            timerState == State.PAUSED
    )
}

private fun Pomodoro.toSessionIndicatorViewState(): ViewState.SessionIndicator {
    val iconRes = when (sessionType) {
        SessionType.WORK -> R.drawable.ic_session_work
        else -> R.drawable.ic_session_break
    }
    return ViewState.SessionIndicator(
            !isReset && !isAwaitingSessionSwitch,
            iconRes
    )
}

private fun Pomodoro.toSessionCountViewState(): ViewState.SessionCount {
    return ViewState.SessionCount(
            !isReset,
            completedWorkSessionCount.toString()
    )
}

private fun Pomodoro.toToggleBtnViewState(): ViewState.ToggleButton {
    val action = when {
        isAwaitingSessionSwitch -> when (nextSessionType) {
            SessionType.WORK -> ViewState.ToggleButton.Action.START_WORK
            SessionType.LONG_BREAK, SessionType.SHORT_BREAK -> ViewState.ToggleButton.Action.START_BREAK
        }
        else -> when (timerState) {
            State.READY -> ViewState.ToggleButton.Action.START
            State.RUNNING -> ViewState.ToggleButton.Action.PAUSE
            State.PAUSED -> ViewState.ToggleButton.Action.RESUME
            else -> ViewState.ToggleButton.Action.START
        }
    }
    return ViewState.ToggleButton(action)
}

private val Pomodoro.isReset: Boolean
    get() = timerState == State.READY && completedWorkSessionCount == 0

private fun Context.getStyledSpannable(@StringRes id: Int): Spannable {
    val originalText = this.getText(id).toSpanned()
    val styledSpannable = SpannableString(originalText)
    originalText.getSpans<Annotation>().forEach { annotation ->
        if (annotation.key == TextEmphasis.KEY) {
            // TODO: Extract the color from the app theme
            val color = getColorCompat(TextEmphasis.from(annotation.value).colorResId)
            val selectionColor = ColorUtils.setAlphaComponent(color, 24 * 255 / 100)
            val start = originalText.getSpanStart(annotation)
            val end = originalText.getSpanEnd(annotation)
            styledSpannable[start, end] = ForegroundColorSpan(color)
            styledSpannable[start, end] = BackgroundColorSpan(selectionColor)
        }
    }
    return styledSpannable
}

private enum class TextEmphasis(val key: String, @ColorRes val colorResId: Int) {
    FOCUS("focus", R.color.colorFocus),
    REST("rest", R.color.colorRest);

    companion object {
        const val KEY = "emphasis"

        fun from(value: String): TextEmphasis {
            return values().find { it.key == value }
                    ?: throw IllegalArgumentException("Unknown emphasis style: $value")
        }
    }
}