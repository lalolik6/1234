package ru.kgeu.lk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.kgeu.lk.ui.AppViewModel
import ru.kgeu.lk.ui.AppViewModelFactory
import ru.kgeu.lk.ui.screens.LoginScreen
import ru.kgeu.lk.ui.screens.MainScreen
import ru.kgeu.lk.ui.theme.KgeuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val locator = (application as KgeuApp).locator

        setContent {
            KgeuTheme {
                val viewModel: AppViewModel = viewModel(
                    factory = AppViewModelFactory(locator.repository),
                )
                val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
                val loginState by viewModel.loginState.collectAsStateWithLifecycle()

                if (isLoggedIn) {
                    MainScreen(viewModel = viewModel)
                } else {
                    LoginScreen(
                        state = loginState,
                        onLogin = viewModel::login,
                    )
                }
            }
        }
    }
}
