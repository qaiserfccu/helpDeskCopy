package com.helpdesk.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.helpdesk.app.ui.screens.auth.LoginScreen
import com.helpdesk.app.ui.screens.auth.RegisterScreen
import com.helpdesk.app.ui.screens.dashboard.DashboardScreen
import com.helpdesk.app.ui.screens.ticket.TicketDetailScreen
import com.helpdesk.app.ui.screens.ticket.TicketFormScreen
import com.helpdesk.app.ui.screens.settings.SettingsScreen
import com.helpdesk.app.ui.screens.admin.UserManagementScreen
import com.helpdesk.app.ui.screens.admin.StatusSummaryScreen
import com.helpdesk.app.ui.screens.LoadingScreen

sealed class Screen(val route: String) {
    object Loading : Screen("loading")
    object Login : Screen("login")
    object Register : Screen("register")
    object Dashboard : Screen("dashboard")
    object TicketDetail : Screen("ticket/{ticketId}") {
        fun createRoute(ticketId: String) = "ticket/$ticketId"
    }
    object TicketForm : Screen("ticket_form?ticketId={ticketId}") {
        fun createRoute(ticketId: String? = null) = 
            if (ticketId != null) "ticket_form?ticketId=$ticketId" else "ticket_form"
    }
    object Settings : Screen("settings")
    object UserManagement : Screen("user_management")
    object StatusSummary : Screen("status_summary")
}

@Composable
fun HelpDeskNavHost(
    navController: NavHostController = rememberNavController(),
    viewModel: NavigationViewModel = hiltViewModel()
) {
    val isInitialized by viewModel.isInitialized.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    LaunchedEffect(isInitialized, isLoggedIn) {
        if (isInitialized) {
            val currentRoute = navController.currentDestination?.route
            val targetRoute = if (isLoggedIn) Screen.Dashboard.route else Screen.Login.route
            
            if (currentRoute == Screen.Loading.route || 
                (currentRoute == Screen.Login.route && isLoggedIn) ||
                (currentRoute == Screen.Register.route && isLoggedIn) ||
                (currentRoute != Screen.Login.route && currentRoute != Screen.Register.route && !isLoggedIn)) {
                navController.navigate(targetRoute) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Loading.route
    ) {
        composable(Screen.Loading.route) {
            LoadingScreen()
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onRegisterSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToTicket = { ticketId ->
                    navController.navigate(Screen.TicketDetail.createRoute(ticketId))
                },
                onNavigateToCreateTicket = {
                    navController.navigate(Screen.TicketForm.createRoute())
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToUserManagement = {
                    navController.navigate(Screen.UserManagement.route)
                },
                onNavigateToStatusSummary = {
                    navController.navigate(Screen.StatusSummary.route)
                },
                onSignOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.TicketDetail.route,
            arguments = listOf(navArgument("ticketId") { type = NavType.StringType })
        ) { backStackEntry ->
            val ticketId = backStackEntry.arguments?.getString("ticketId") ?: return@composable
            TicketDetailScreen(
                ticketId = ticketId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { id ->
                    navController.navigate(Screen.TicketForm.createRoute(id))
                }
            )
        }

        composable(
            route = Screen.TicketForm.route,
            arguments = listOf(
                navArgument("ticketId") { 
                    type = NavType.StringType 
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val ticketId = backStackEntry.arguments?.getString("ticketId")
            TicketFormScreen(
                ticketId = ticketId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.UserManagement.route) {
            UserManagementScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.StatusSummary.route) {
            StatusSummaryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTicket = { ticketId ->
                    navController.navigate(Screen.TicketDetail.createRoute(ticketId))
                }
            )
        }
    }
}
