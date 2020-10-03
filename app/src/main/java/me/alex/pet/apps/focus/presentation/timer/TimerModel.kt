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


private fun Pomodoro.toViewState(): ViewState {
    return ViewState(
            toTimerViewState(),
            toWorkIntro(),
            toBreakIntro(),
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

private fun Pomodoro.toWorkIntro(): ViewState.WorkIntro {
    return ViewState.WorkIntro(isAwaitingSessionSwitch && nextSessionType == SessionType.WORK)
}

private fun Pomodoro.toBreakIntro(): ViewState.BreakIntro {
    return ViewState.BreakIntro(isAwaitingSessionSwitch && nextSessionType != SessionType.WORK)
}

private fun Pomodoro.toTimerViewState(): ViewState.Timer {
    val minutes = remainingDuration.seconds / 60
    val seconds = remainingDuration.seconds % 60
    val timerText = when (minutes) {
        0L -> seconds.toString()
        else -> String.format("%d:%02d", minutes, seconds)
    }
    return ViewState.Timer(
            !isAwaitingSessionSwitch,
            timerText,
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