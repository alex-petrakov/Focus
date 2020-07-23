package me.alex.pet.apps.focus.presentation.timer

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import me.alex.pet.apps.focus.R
import me.alex.pet.apps.focus.domain.Pomodoro
import me.alex.pet.apps.focus.domain.SessionType
import me.alex.pet.apps.focus.domain.TimerState
import kotlin.math.roundToInt

class TimerModel(
        private val app: Application,
        private val pomodoro: Pomodoro
) : ViewModel() {

    private val _viewState = MutableLiveData<ViewState>()

    val viewState: LiveData<ViewState> get() = _viewState

    init {
        _viewState.value = pomodoro.toViewState()
        pomodoro.addObserver(object : Pomodoro.Observer {
            override fun onUpdate() {
                _viewState.value = pomodoro.toViewState()
            }
        })
    }

    fun onToggleTimer() {
        pomodoro.toggleSession()
    }

    fun onReset() {
        pomodoro.reset()
    }

    fun onSwitchToNextSession() {
        pomodoro.apply {
            switchToNextSession()
            startSession()
        }
    }

    private fun Pomodoro.toViewState(): ViewState {
        val resetBtnIsVisible = timerState == TimerState.PAUSED
        val visiblePanel = if (isAwaitingSessionSwitch) {
            if (nextSessionType == SessionType.WORK) {
                ViewState.Panel.WORK_INTRO
            } else {
                ViewState.Panel.BREAK_INTRO
            }
        } else {
            ViewState.Panel.TIMER
        }
        return ViewState(
                toTimerViewState(),
                toProgressViewState(),
                toSessionCountViewState(),
                toToggleViewState(),
                resetBtnIsVisible,
                visiblePanel
        )
    }

    private fun Pomodoro.toTimerViewState(): ViewState.Timer {
        return ViewState.Timer(
                remainingSeconds.toString(),
                timerState == TimerState.PAUSED
        )
    }

    private fun Pomodoro.toProgressViewState(): ViewState.Progress {
        return ViewState.Progress(
                sessionIsActive,
                (passedSeconds * 100.0 / workDuration).roundToInt(),
                0
        )
    }

    private fun Pomodoro.toSessionCountViewState(): ViewState.SessionCount {
        return ViewState.SessionCount(
                sessionIsActive,
                app.getString(R.string.timer_completed_sessions_format, completedWorkSessionCount)
        )
    }

    private fun Pomodoro.toToggleViewState(): ViewState.Toggle {
        val iconId = when (timerState) {
            TimerState.RUNNING -> R.drawable.ic_action_pause
            else -> R.drawable.ic_action_start
        }
        val isVisible = when (timerState) {
            TimerState.READY, TimerState.RUNNING, TimerState.PAUSED -> true
            else -> false
        }
        val strId = when (timerState) {
            TimerState.READY -> R.string.app_action_start
            TimerState.RUNNING -> R.string.app_action_pause
            TimerState.PAUSED -> R.string.app_action_resume
            else -> R.string.empty
        }
        return ViewState.Toggle(isVisible, app.getString(strId), app.getDrawable(iconId)!!)
    }

    private val Pomodoro.sessionIsActive get() = (timerState == TimerState.RUNNING || timerState == TimerState.PAUSED)
}