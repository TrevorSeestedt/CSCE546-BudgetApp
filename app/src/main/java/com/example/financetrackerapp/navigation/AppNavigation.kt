package com.example.financetrackerapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.financetrackerapp.ui.auth.AuthViewModel
import com.example.financetrackerapp.ui.auth.LoginScreen
import com.example.financetrackerapp.ui.auth.RegisterScreen
import com.example.financetrackerapp.ui.budget.BudgetCreationScreen
import com.example.financetrackerapp.ui.budget.BudgetDetailScreen
import com.example.financetrackerapp.ui.budget.BudgetDetailViewModel
import com.example.financetrackerapp.ui.budget.BudgetViewModel
import com.example.financetrackerapp.ui.calendar.CalendarScreen
import com.example.financetrackerapp.ui.calendar.CalendarViewModel
import com.example.financetrackerapp.ui.home.HomeScreen

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val authViewModel: AuthViewModel = viewModel()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    
    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) Screen.Home.route else Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onLoginSuccess = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Login.route) { inclusive = true } } },
                authViewModel = authViewModel
            )
        }
        
        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = { navController.navigate(Screen.Login.route) },
                onRegisterSuccess = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Register.route) { inclusive = true } } },
                authViewModel = authViewModel
            )
        }
        
        composable(Screen.Home.route) {
            val budgetViewModel: BudgetViewModel = viewModel()
            HomeScreen(
                onNavigateToBudgetCreation = { navController.navigate(Screen.BudgetCreation.route) },
                onNavigateToBudgetDetail = { budgetId ->
                    navController.navigate("${Screen.BudgetDetail.route}/$budgetId")
                },
                onNavigateToCalendar = { navController.navigate(Screen.Calendar.route) },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                budgetViewModel = budgetViewModel
            )
        }
        
        composable(Screen.BudgetCreation.route) {
            val budgetViewModel: BudgetViewModel = viewModel()
            BudgetCreationScreen(
                onNavigateBack = { navController.popBackStack() },
                onBudgetCreated = { 
                    navController.popBackStack()
                },
                budgetViewModel = budgetViewModel
            )
        }
        
        composable(
            route = "${Screen.BudgetDetail.route}/{budgetId}",
            arguments = listOf(navArgument("budgetId") { type = NavType.StringType })
        ) { backStackEntry ->
            val budgetId = backStackEntry.arguments?.getString("budgetId") ?: return@composable
            val budgetDetailViewModel: BudgetDetailViewModel = viewModel()
            BudgetDetailScreen(
                budgetId = budgetId,
                onNavigateBack = { navController.popBackStack() },
                viewModel = budgetDetailViewModel
            )
        }
        
        composable(Screen.Calendar.route) {
            val calendarViewModel: CalendarViewModel = viewModel()
            CalendarScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditBudget = { budgetId ->
                    navController.navigate("${Screen.BudgetDetail.route}/$budgetId")
                },
                viewModel = calendarViewModel
            )
        }
    }
}

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Home : Screen("home")
    data object BudgetCreation : Screen("budget_creation")
    data object BudgetDetail : Screen("budget_detail")
    data object Calendar : Screen("calendar")
} 