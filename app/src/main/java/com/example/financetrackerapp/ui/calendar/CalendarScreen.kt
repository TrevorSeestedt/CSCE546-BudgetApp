package com.example.financetrackerapp.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.financetrackerapp.data.model.Budget
import com.example.financetrackerapp.data.model.BudgetPeriod
import com.example.financetrackerapp.data.model.Expense
import com.example.financetrackerapp.ui.calendar.DeleteDialogState
import com.example.financetrackerapp.ui.calendar.NavigationEvent
import com.example.financetrackerapp.util.CalendarUtil
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CalendarScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditBudget: (String) -> Unit,
    viewModel: CalendarViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val displayMonth by viewModel.displayMonth.collectAsState()
    val calendarDates by viewModel.calendarDates.collectAsState()
    val selectedDayExpenses by viewModel.selectedDayExpenses.collectAsState()
    val deleteDialogState by viewModel.showDeleteDialog.collectAsState()
    val navigationEvent by viewModel.navigationEvent.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Handle navigation events
    LaunchedEffect(navigationEvent) {
        navigationEvent?.let {
            when (it) {
                is NavigationEvent.NavigateToEditBudget -> {
                    onNavigateToEditBudget(it.budgetId)
                    viewModel.consumeNavigationEvent()
                }
                is NavigationEvent.NavigateToEditExpense -> {
                    // For now, just show a delete dialog as we don't have an edit expense screen yet
                    viewModel.showDeleteExpenseDialog(it.expense)
                    viewModel.consumeNavigationEvent()
                }
            }
        }
    }
    
    LaunchedEffect(uiState) {
        if (uiState is CalendarUiState.Error) {
            snackbarHostState.showSnackbar((uiState as CalendarUiState.Error).message)
        }
    }
    
    // Delete confirmation dialogs
    when (val state = deleteDialogState) {
        is DeleteDialogState.ConfirmBudgetDelete -> {
            AlertDialog(
                onDismissRequest = { viewModel.hideDeleteDialog() },
                title = { Text("Delete Budget") },
                text = { Text("Are you sure you want to delete budget \"${state.budget.name}\"? This will also delete all associated expenses.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteBudget(state.budget)
                            viewModel.hideDeleteDialog()
                        }
                    ) {
                        Text(
                            "Delete",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.hideDeleteDialog() }) {
                        Text("Cancel")
                    }
                }
            )
        }
        is DeleteDialogState.ConfirmExpenseDelete -> {
            AlertDialog(
                onDismissRequest = { viewModel.hideDeleteDialog() },
                title = { Text("Delete Expense") },
                text = { 
                    val description = if (state.expense.description.isNotBlank()) 
                        state.expense.description else "this expense"
                    Text("Are you sure you want to delete ${description}?") 
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteExpense(state.expense)
                            viewModel.hideDeleteDialog()
                        }
                    ) {
                        Text(
                            "Delete",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.hideDeleteDialog() }) {
                        Text("Cancel")
                    }
                }
            )
        }
        DeleteDialogState.Hidden -> {}
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget Calendar") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Month navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.moveToPreviousMonth() }) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Previous month"
                    )
                }
                
                Text(
                    text = CalendarUtil.formatMonthYear(displayMonth),
                    style = MaterialTheme.typography.titleLarge
                )
                
                IconButton(onClick = { viewModel.moveToNextMonth() }) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Next month"
                    )
                }
            }
            
            // Days of week header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                daysOfWeek.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Calendar grid
            when (uiState) {
                is CalendarUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is CalendarUiState.Success -> {
                    val successState = uiState as CalendarUiState.Success
                    
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    ) {
                        items(calendarDates) { date ->
                            CalendarDay(
                                date = date,
                                isSelected = CalendarUtil.isSameDay(date, selectedDate),
                                isCurrentMonth = CalendarUtil.isInCurrentMonth(date, displayMonth),
                                hasExpenses = successState.allExpenses.any { expense -> 
                                    CalendarUtil.isSameDay(expense.date, date) 
                                },
                                isBudgetResetDay = successState.resetDates.any { (_, resetDate) -> 
                                    CalendarUtil.isSameDay(resetDate, date) 
                                },
                                budgetResetCount = successState.resetDates.count { (_, resetDate) ->
                                    CalendarUtil.isSameDay(resetDate, date)
                                },
                                onClick = { viewModel.setSelectedDate(date) }
                            )
                        }
                    }
                    
                    Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    
                    // Selected day details
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "Expenses on ${CalendarUtil.formatDate(selectedDate)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Budget resets on selected day
                        val budgetResetsToday = successState.resetDates
                            .filter { (_, resetDate) -> CalendarUtil.isSameDay(resetDate, selectedDate) }
                            .sortedBy { (budget, _) -> budget.name } // Sort by budget name for consistency
                        
                        if (budgetResetsToday.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    // Use singular or plural title based on number of budgets
                                    Text(
                                        text = if (budgetResetsToday.size == 1) "Budget Reset Today" else "Budget Resets Today",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // List each budget with its name and amount
                                    budgetResetsToday.forEachIndexed { index, (budget, _) ->
                                        val formatter = NumberFormat.getCurrencyInstance(Locale.US)
                                        val dotColors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary,
                                            MaterialTheme.colorScheme.tertiary
                                        )
                                        val dotColor = dotColors[index % dotColors.size]
                                        
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                // Colored dot indicator matching calendar dots
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clip(CircleShape)
                                                        .background(dotColor)
                                                )
                                                
                                                Spacer(modifier = Modifier.width(8.dp))
                                                
                                                Text(
                                                    text = "${budget.name} (${formatter.format(budget.amount)})",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                            
                                            // Edit and delete buttons
                                            Row {
                                                IconButton(
                                                    onClick = { viewModel.navigateToEditBudget(budget) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit budget",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                
                                                IconButton(
                                                    onClick = { viewModel.showDeleteBudgetDialog(budget) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete, 
                                                        contentDescription = "Delete budget",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                        
                                        // Add a divider between items (except for the last one)
                                        if (index < budgetResetsToday.size - 1) {
                                            Divider(
                                                modifier = Modifier.padding(vertical = 4.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        // Expenses list
                        if (selectedDayExpenses.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No expenses on this day",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn {
                                items(selectedDayExpenses) { expense ->
                                    ExpenseItem(
                                        expense = expense,
                                        budget = successState.budgets.find { it.id == expense.budgetId },
                                        onEditExpense = { viewModel.navigateToEditExpense(it) },
                                        onDeleteExpense = { viewModel.showDeleteExpenseDialog(it) }
                                    )
                                    Divider()
                                }
                            }
                        }
                    }
                }
                is CalendarUiState.Error -> {
                    // Error state is handled via Snackbar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Error loading calendar data")
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarDay(
    date: Long,
    isSelected: Boolean,
    isCurrentMonth: Boolean,
    hasExpenses: Boolean,
    isBudgetResetDay: Boolean,
    budgetResetCount: Int = 0,
    onClick: () -> Unit
) {
    // Color variations for budget dots
    val budgetDotColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary
    )
    
    Box(
        modifier = Modifier
            .aspectRatio(1f) // Square shape
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                    !isCurrentMonth -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    else -> Color.Transparent
                }
            )
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = CalendarUtil.formatDayOfMonth(date),
                color = when {
                    !isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                },
                fontSize = 14.sp
            )
            
            if (hasExpenses || isBudgetResetDay) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Indicator for expense
                    if (hasExpenses) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer)
                        )
                    }
                    
                    // Small space between indicators
                    if (hasExpenses && isBudgetResetDay) {
                        Spacer(modifier = Modifier.size(2.dp))
                    }
                    
                    // Indicators for budget resets
                    if (isBudgetResetDay) {
                        // Display dots for budget resets (max 3 dots, then "+")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            val displayDots = minOf(budgetResetCount, 3)
                            
                            repeat(displayDots) { index ->
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(budgetDotColors[index % budgetDotColors.size])
                                )
                            }
                            
                            // If there are more than 3 budget resets, show a "+" indicator
                            if (budgetResetCount > 3) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+",
                                        fontSize = 7.sp,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseItem(
    expense: Expense,
    budget: Budget?,
    onEditExpense: (Expense) -> Unit,
    onDeleteExpense: (Expense) -> Unit
) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Budget type indicator
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = budget?.period?.name?.take(1) ?: "E",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = if (expense.description.isNotBlank()) expense.description else "Expense",
                style = MaterialTheme.typography.bodyLarge
            )
            
            Text(
                text = budget?.name ?: "Unknown Budget",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = currencyFormatter.format(expense.amount),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            
            // Edit and delete buttons
            Row {
                IconButton(
                    onClick = { onEditExpense(expense) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit expense",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                IconButton(
                    onClick = { onDeleteExpense(expense) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete, 
                        contentDescription = "Delete expense",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
} 