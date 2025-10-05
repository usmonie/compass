import com.usmonie.compass.state.Action
import com.usmonie.compass.state.Effect
import com.usmonie.compass.state.Event
import com.usmonie.compass.state.State

internal sealed class LoginAction : Action {
    data class EnterEmail(val email: String) : LoginAction()
    data class EnterPassword(val password: String) : LoginAction()
    object Submit : LoginAction()
}

internal sealed class LoginEvent : Event {
    data class EmailChanged(val email: String) : LoginEvent()
    data class PasswordChanged(val password: String) : LoginEvent()
    object LoadingStarted : LoginEvent()
    data class LoginSuccess(val user: User) : LoginEvent()
    data class LoginFailed(val error: Throwable) : LoginEvent()
}

internal sealed class LoginEffect : Effect {
    data class NavigateToProfile(val user: User) : LoginEffect()
    data class ShowToast(val message: String) : LoginEffect()
}

internal data class LoginState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
) : State