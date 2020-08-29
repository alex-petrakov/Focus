package me.alex.pet.apps.focus.presentation.timer

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import me.alex.pet.apps.focus.R
import me.alex.pet.apps.focus.common.SingleLiveEvent
import me.alex.pet.apps.focus.domain.Pomodoro
import me.alex.pet.apps.focus.domain.SessionType
import me.alex.pet.apps.focus.domain.Timer.State
import kotlin.math.roundToInt

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
            _viewState.value = pomodoro.toViewState()
        }
    }

    init {
        _viewState.value = pomodoro.toViewState()
        pomodoro.addObserver(pomodoroObserver)
    }

    fun onToggleTimer() {
        pomodoro.toggleSession()
        _viewEffect.value = ViewEffect.START_NOTIFICATIONS
    }

    fun onReset() {
        pomodoro.reset()
    }

    fun onSwitchToNextSession() {
        pomodoro.startNextSession()
    }

    override fun onCleared() {
        pomodoro.removeObserver(pomodoroObserver)
    }

    private fun Pomodoro.toViewState(): ViewState {
        val resetBtnIsVisible = timerState == State.PAUSED
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
                remainingDuration.seconds.toString(),
                timerState == State.PAUSED
        )
    }

    private fun Pomodoro.toProgressViewState(): ViewState.Progress {
        return ViewState.Progress(
                sessionIsActive,
                progress.roundToInt(),
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
            State.RUNNING -> R.drawable.ic_action_pause
            else -> R.drawable.ic_action_start
        }
        val isVisible = when (timerState) {
            State.READY, State.RUNNING, State.PAUSED -> true
            else -> false
        }
        val strId = when (timerState) {
            State.READY -> R.string.app_action_start
            State.RUNNING -> R.string.app_action_pause
            State.PAUSED -> R.string.app_action_resume
            else -> R.string.empty
        }
        return ViewState.Toggle(isVisible, app.getString(strId), app.getDrawable(iconId)!!)
    }

    private val Pomodoro.sessionIsActive get() = (timerState == State.RUNNING || timerState == State.PAUSED)
}