@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.financetrackerapp.ui.budget

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.financetrackerapp.data.model.Budget
import com.example.financetrackerapp.data.model.BudgetPeriod
import com.example.financetrackerapp.data.model.Expense
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// create a CompositionLocal for the BudgetDetailViewModel
val LocalBudgetDetailViewModel = compositionLocalOf<BudgetDetailViewModel> { 
    error("No BudgetDetailViewModel provided") 
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDetailScreen(
    budgetId: String,
    onNavigateBack: () -> Unit,
    viewModel: BudgetDetailViewModel
) {
    // provide the ViewModel through CompositionLocal
    CompositionLocalProvider(LocalBudgetDetailViewModel provides viewModel) {
        val uiState by viewModel.uiState.collectAsState()
        val budgetDeleted by viewModel.budgetDeleted.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        val sheetState = rememberModalBottomSheetState()
        val scope = rememberCoroutineScope()
        
        var showAddExpenseSheet by remember { mutableStateOf(false) }
        var showEditBudgetSheet by remember { mutableStateOf(false) }
        var showDeleteConfirmation by remember { mutableStateOf(false) }
        
        LaunchedEffect(budgetId) {
            viewModel.loadBudgetDetails(budgetId)
        }
        
        LaunchedEffect(uiState) {
            if (uiState is BudgetDetailUiState.Error) {
                snackbarHostState.showSnackbar((uiState as BudgetDetailUiState.Error).message)
            }
        }
        
        // navigate back if budget deleted
        LaunchedEffect(budgetDeleted) {
            if (budgetDeleted) {
                onNavigateBack()
            }
        }
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Budget Details") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Go back"
                            )
                        }
                    },
                    actions = {
                        // edit
                        IconButton(onClick = { showEditBudgetSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Budget"
                            )
                        }
                        
                        // delete
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Budget",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showAddExpenseSheet = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Expense"
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
                    is BudgetDetailUiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    is BudgetDetailUiState.Success -> {
                        val data = uiState as BudgetDetailUiState.Success
                        BudgetDetailContent(
                            budget = data.budget,
                            expenses = data.expenses,
                            totalSpent = data.totalSpent,
                            percentUsed = data.percentUsed,
                            isOverBudget = data.isOverBudget
                        )
                        
                        // delete confirmation
                        if (showDeleteConfirmation) {
                            AlertDialog(
                                onDismissRequest = { showDeleteConfirmation = false },
                                title = { Text("Delete Budget") },
                                text = { Text("Are you sure you want to delete this budget? This will also delete all associated expenses.") },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            viewModel.deleteBudget(budgetId)
                                            showDeleteConfirmation = false
                                        }
                                    ) {
                                        Text(
                                            "Delete",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteConfirmation = false }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }
                        
                        // edit budget bottom sheet
                        if (showEditBudgetSheet) {
                            ModalBottomSheet(
                                onDismissRequest = { showEditBudgetSheet = false },
                                sheetState = sheetState
                            ) {
                                EditBudgetContent(
                                    budget = data.budget,
                                    onUpdateBudget = { name, amount, period, startDate, endDate, isRepeating ->
                                        viewModel.updateBudget(
                                            budget = data.budget,
                                            newName = name,
                                            newAmount = amount,
                                            newPeriod = period,
                                            newStartDate = startDate,
                                            newEndDate = endDate,
                                            newRepeating = isRepeating
                                        )
                                        scope.launch {
                                            sheetState.hide()
                                            showEditBudgetSheet = false
                                        }
                                    },
                                    onDismiss = { 
                                        scope.launch {
                                            sheetState.hide()
                                            showEditBudgetSheet = false
                                        }
                                    }
                                )
                            }
                        }
                    }
                    is BudgetDetailUiState.Error -> {
                        // error shown thru snackbar
                        Text(
                            text = "Error loading budget details",
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        )
                    }
                }
            }
        }
        
        if (showAddExpenseSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAddExpenseSheet = false },
                sheetState = sheetState
            ) {
                AddExpenseContent(
                    onAddExpense = { amount, date, description ->
                        viewModel.addExpense(budgetId, amount, date, description)
                        scope.launch {
                            sheetState.hide()
                            showAddExpenseSheet = false
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun BudgetDetailContent(
    budget: Budget,
    expenses: List<Expense>,
    totalSpent: Double,
    percentUsed: Double,
    isOverBudget: Boolean
) {
    // state for editing 
    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }
    var showEditExpenseSheet by remember { mutableStateOf(false) }
    var showDeleteExpenseConfirmation by remember { mutableStateOf<Expense?>(null) }
    
    // create sheet state for edit expense
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    // function for editing expense
    val onEditExpense = { expense: Expense ->
        expenseToEdit = expense
        showEditExpenseSheet = true
    }
    
    // Function for deleting expense confirmation
    val onDeleteExpense = { expense: Expense ->
        showDeleteExpenseConfirmation = expense
    }
    
    // get current ViewModel
    val viewModel = LocalBudgetDetailViewModel.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // summary card for budget
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // budget name as main title
                Text(
                    text = budget.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                // budget type as subtitle
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
                    text = "Total Budget: ${currencyFormatter.format(budget.amount)}",
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Start Date: ${dateFormatter.format(Date(budget.startDate))}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // budget usage 
                Text(
                    text = "Budget Usage: ${currencyFormatter.format(totalSpent)} of ${currencyFormatter.format(budget.amount)} " +
                            if (isOverBudget) "🚨" else "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // budget progression percent bar
                Box(modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        progress = { minOf(1f, (totalSpent / budget.amount).toFloat()) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        color = when {
                            isOverBudget -> MaterialTheme.colorScheme.error
                            percentUsed > 80 -> Color(0xFFFF9800) // Orange
                            else -> MaterialTheme.colorScheme.primary
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = String.format("%.1f%%", percentUsed),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // expenses list
        Text(
            text = "Expenses",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (expenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No expenses yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn {
                items(expenses) { expense ->
                    ExpenseItem(
                        expense = expense,
                        onEdit = { onEditExpense(expense) },
                        onDelete = { onDeleteExpense(expense) }
                    )
                    Divider()
                }
            }
        }
    }
    
    // edit expense bottom sheet 
    if (showEditExpenseSheet && expenseToEdit != null) {
        ModalBottomSheet(
            onDismissRequest = { 
                showEditExpenseSheet = false
                expenseToEdit = null
            },
            sheetState = sheetState
        ) {
            EditExpenseContent(
                expense = expenseToEdit!!,
                onUpdateExpense = { amount, date, description ->
                    viewModel.updateExpense(
                        expenseToEdit!!,
                        newAmount = amount,
                        newDescription = description
                    )
                    scope.launch {
                        sheetState.hide()
                        showEditExpenseSheet = false
                        expenseToEdit = null
                    }
                },
                onDismiss = {
                    scope.launch {
                        sheetState.hide()
                        showEditExpenseSheet = false
                        expenseToEdit = null
                    }
                }
            )
        }
    }
    
    // delete the expense confirmation and text
    showDeleteExpenseConfirmation?.let { expense ->
        AlertDialog(
            onDismissRequest = { showDeleteExpenseConfirmation = null },
            title = { Text("Delete Expense") },
            text = { 
                val description = if (expense.description.isNotBlank()) 
                    "\"${expense.description}\"" else "this expense"
                Text("Are you sure you want to delete $description?") 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteExpense(expense)
                        showDeleteExpenseConfirmation = null
                    }
                ) {
                    Text(
                        "Delete",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteExpenseConfirmation = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ExpenseItem(
    expense: Expense,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
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
                text = dateFormatter.format(Date(expense.date)),
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
            
            // edit & delete button
            Row {
                IconButton(
                    onClick = onEdit,
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
                    onClick = onDelete,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseContent(
    onAddExpense: (amount: Double, date: Long, description: String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var expenseDate by remember { mutableStateOf(Calendar.getInstance().timeInMillis) }
    
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val context = LocalContext.current
    
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            expenseDate = calendar.timeInMillis
        },
        Calendar.getInstance().get(Calendar.YEAR),
        Calendar.getInstance().get(Calendar.MONTH),
        Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Add Expense",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            prefix = { Text("$") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description (Optional)") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = dateFormatter.format(Date(expenseDate)),
            onValueChange = { },
            readOnly = true,
            label = { Text("Date") },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Select date",
                    modifier = Modifier.clickable { datePickerDialog.show() }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { datePickerDialog.show() }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                val amountValue = amount.toDoubleOrNull()
                if (amountValue != null && amountValue > 0) {
                    onAddExpense(amountValue, expenseDate, description)
                }
            },
            enabled = amount.isNotBlank() && amount.toDoubleOrNull() != null && amount.toDoubleOrNull()!! > 0,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Expense")
        }
    }
}

// composable for editing budget
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBudgetContent(
    budget: Budget,
    onUpdateBudget: (name: String, amount: Double, period: BudgetPeriod, startDate: Long, endDate: Long?, isRepeating: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(budget.name) }
    var amount by remember { mutableStateOf(budget.amount.toString()) }
    var selectedPeriod by remember { mutableStateOf(budget.period) }
    var startDate by remember { mutableStateOf(budget.startDate) }
    var endDate by remember { mutableStateOf(budget.endDate) }
    var isRepeating by remember { mutableStateOf(budget.repeating) }
    var isCustomDateRange by remember { mutableStateOf(budget.endDate != null) }
    
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val context = LocalContext.current
    
    val startDatePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            startDate = calendar.timeInMillis
        },
        Calendar.getInstance().apply { timeInMillis = startDate }.get(Calendar.YEAR),
        Calendar.getInstance().apply { timeInMillis = startDate }.get(Calendar.MONTH),
        Calendar.getInstance().apply { timeInMillis = startDate }.get(Calendar.DAY_OF_MONTH)
    )
    
    val endDatePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            endDate = calendar.timeInMillis
        },
        Calendar.getInstance().apply { timeInMillis = endDate ?: System.currentTimeMillis() }.get(Calendar.YEAR),
        Calendar.getInstance().apply { timeInMillis = endDate ?: System.currentTimeMillis() }.get(Calendar.MONTH),
        Calendar.getInstance().apply { timeInMillis = endDate ?: System.currentTimeMillis() }.get(Calendar.DAY_OF_MONTH)
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Edit Budget",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Budget Name") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            prefix = { Text("$") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // budget period select
        Text(
            text = "Budget Period",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(start = 4.dp, bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BudgetPeriod.values().forEach { period ->
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    PeriodSelectionButton(
                        period = period,
                        isSelected = selectedPeriod == period,
                        onClick = { selectedPeriod = period }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // repeat toggler
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Repeating Budget",
                style = MaterialTheme.typography.bodyLarge
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Switch(
                checked = isRepeating,
                onCheckedChange = { isRepeating = it }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // custom date toggler
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Use custom date range",
                style = MaterialTheme.typography.bodyLarge
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Switch(
                checked = isCustomDateRange,
                onCheckedChange = { 
                    isCustomDateRange = it
                    // No end date when custom date range is disabled/off
                    if (!it) {
                        endDate = null
                    }
                }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = dateFormatter.format(Date(startDate)),
            onValueChange = { },
            readOnly = true,
            label = { Text("Start Date") },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Select start date",
                    modifier = Modifier.clickable { startDatePickerDialog.show() }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { startDatePickerDialog.show() }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // end date shown when custom date range is enabled
        if (isCustomDateRange) {
            OutlinedTextField(
                value = endDate?.let { dateFormatter.format(Date(it)) } ?: "Not set",
                onValueChange = { },
                readOnly = true,
                label = { Text("End Date" + (if (!isRepeating) " (Required)" else " (Optional)")) },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Select end date",
                        modifier = Modifier.clickable { endDatePickerDialog.show() }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { endDatePickerDialog.show() }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text("Cancel")
            }
            
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    if (amountValue != null && amountValue > 0 && name.isNotBlank()) {
                        val finalEndDate = if (isCustomDateRange) endDate else null
                        onUpdateBudget(name, amountValue, selectedPeriod, startDate, finalEndDate, isRepeating)
                    }
                },
                enabled = amount.isNotBlank() && 
                          amount.toDoubleOrNull() != null && 
                          amount.toDoubleOrNull()!! > 0 && 
                          name.isNotBlank() &&
                          (!isCustomDateRange || endDate != null || isRepeating),
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
fun PeriodSelectionButton(
    period: BudgetPeriod,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val periodText = when (period) {
        BudgetPeriod.WEEKLY -> "Weekly"
        BudgetPeriod.MONTHLY -> "Monthly"
    }
    
    Button(
        onClick = onClick,
        modifier = Modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(periodText)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExpenseContent(
    expense: Expense,
    onUpdateExpense: (amount: Double, date: Long, description: String) -> Unit,
    onDismiss: () -> Unit
) {
    var amount by remember { mutableStateOf(expense.amount.toString()) }
    var description by remember { mutableStateOf(expense.description) }
    var expenseDate by remember { mutableStateOf(expense.date) }
    
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val context = LocalContext.current
    
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            expenseDate = calendar.timeInMillis
        },
        Calendar.getInstance().apply { timeInMillis = expenseDate }.get(Calendar.YEAR),
        Calendar.getInstance().apply { timeInMillis = expenseDate }.get(Calendar.MONTH),
        Calendar.getInstance().apply { timeInMillis = expenseDate }.get(Calendar.DAY_OF_MONTH)
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Edit Expense",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            prefix = { Text("$") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description (Optional)") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = dateFormatter.format(Date(expenseDate)),
            onValueChange = { },
            readOnly = true,
            label = { Text("Date") },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Select date",
                    modifier = Modifier.clickable { datePickerDialog.show() }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { datePickerDialog.show() }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text("Cancel")
            }
            
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    if (amountValue != null && amountValue > 0) {
                        onUpdateExpense(amountValue, expenseDate, description)
                    }
                },
                enabled = amount.isNotBlank() && amount.toDoubleOrNull() != null && amount.toDoubleOrNull()!! > 0,
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }
        }
    }
} 