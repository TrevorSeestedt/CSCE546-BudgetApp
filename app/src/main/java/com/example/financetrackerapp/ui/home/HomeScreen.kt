package com.example.financetrackerapp.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.financetrackerapp.data.model.Budget
import com.example.financetrackerapp.data.model.BudgetPeriod
import com.example.financetrackerapp.ui.budget.BudgetUiState
import com.example.financetrackerapp.ui.budget.BudgetViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToBudgetCreation: () -> Unit,
    onNavigateToBudgetDetail: (String) -> Unit,
    onNavigateToCalendar: () -> Unit,
    onLogout: () -> Unit,
    budgetViewModel: BudgetViewModel
) {
    val uiState by budgetViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Observe lifecycle to refresh data when returning to this screen
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Reload budgets when screen becomes active again
                budgetViewModel.loadUserBudgets()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Initial load of budgets
    LaunchedEffect(Unit) {
        budgetViewModel.loadUserBudgets()
    }
    
    LaunchedEffect(uiState) {
        if (uiState is BudgetUiState.Error) {
            snackbarHostState.showSnackbar((uiState as BudgetUiState.Error).message)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget Helper") },
                actions = {
                    IconButton(onClick = onNavigateToCalendar) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Calendar View"
                        )
                    }
                    
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToBudgetCreation) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Budget"
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is BudgetUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is BudgetUiState.BudgetList -> {
                    val budgets = (uiState as BudgetUiState.BudgetList).budgets
                    if (budgets.isEmpty()) {
                        EmptyBudgetView()
                    } else {
                        BudgetList(
                            budgets = budgets,
                            onBudgetClick = onNavigateToBudgetDetail
                        )
                    }
                }
                else -> {
                    // Initial or other states
                    EmptyBudgetView()
                }
            }
        }
    }
}

@Composable
fun EmptyBudgetView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No budgets yet",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Start by creating a new budget with the + button",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun BudgetList(
    budgets: List<Budget>,
    onBudgetClick: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(budgets) { budget ->
            BudgetCard(
                budget = budget,
                onClick = { onBudgetClick(budget.id) }
            )
        }
    }
}

@Composable
fun BudgetCard(
    budget: Budget,
    onClick: () -> Unit
) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = budget.name,
                style = MaterialTheme.typography.titleMedium
            )
            
            Text(
                text = when (budget.period) {
                    BudgetPeriod.WEEKLY -> "Weekly Budget" + (if (!budget.repeating) " (One-time)" else "")
                    BudgetPeriod.MONTHLY -> "Monthly Budget" + (if (!budget.repeating) " (One-time)" else "")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Amount: ${currencyFormatter.format(budget.amount)}",
                style = MaterialTheme.typography.bodyLarge
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Start Date: ${dateFormatter.format(Date(budget.startDate))}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (!budget.repeating) {
                        "Next Reset: N/A"
                    } else {
                        "Next Reset: ${dateFormatter.format(Date(budget.getNextResetDate()))}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Tap to view details and add expenses",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
} 