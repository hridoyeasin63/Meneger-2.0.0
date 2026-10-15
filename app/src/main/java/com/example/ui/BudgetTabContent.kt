package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.ui.screens.CategoryScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.theme.AdvancedExpenseTrackerTheme
import com.example.ui.viewmodel.ExpenseViewModel

@Composable
fun BudgetTabContent(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val userPrefs by viewModel.userPreferences.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCategoryScreen by remember { mutableStateOf(false) }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    if (showCategoryScreen) {
        BackHandler {
            showCategoryScreen = false
        }
    }

    AdvancedExpenseTrackerTheme(darkTheme = userPrefs.isDarkMode) {
        Box(
            modifier = modifier.fillMaxSize()
        ) {
            AnimatedContent(
                targetState = showCategoryScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.fillMaxSize(),
                label = "budget_screen_transition"
            ) { isCategory ->
                if (isCategory) {
                    CategoryScreen(
                        viewModel = viewModel,
                        onBack = { showCategoryScreen = false }
                    )
                } else {
                    DashboardScreen(
                        viewModel = viewModel,
                        onCategoryClick = { showCategoryScreen = true }
                    )
                }
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
