package com.example.financetrackerapp.ui.budget

import android.app.DatePickerDialog
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.financetrackerapp.data.model.BudgetPeriod
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetCreationScreen(
    onNavigateBack: () -> Unit,
    onBudgetCreated: () -> Unit,
    budgetViewModel: BudgetViewModel
) {
    val uiState by budgetViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // form state
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var isCustomDateRange by remember { mutableStateOf(false) }
    var isRepeating by remember { mutableStateOf(true) }
    var budgetPeriod by remember { mutableStateOf(BudgetPeriod.MONTHLY) }
    var startDate by remember { mutableStateOf(Calendar.getInstance().timeInMillis) }
    var endDate by remember { mutableStateOf<Long?>(null) }

    // calculate default end date based on period if not repeating
    val calculatedEndDate by remember {
        derivedStateOf {
            if (!isRepeating && endDate == null && !isCustomDateRange) {
                val calendar = Calendar.getInstance().apply { timeInMillis = startDate }
                when (budgetPeriod) {
                    BudgetPeriod.WEEKLY -> {
                        calendar.add(Calendar.WEEK_OF_YEAR, 1)
                        calendar.timeInMillis
                    }
                    BudgetPeriod.MONTHLY -> {
                        calendar.add(Calendar.MONTH, 1)
                        calendar.timeInMillis
                    }
                }
            } else {
                endDate
            }
        }
    }
    
    // Date pickers
    val context = LocalContext.current
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    val startDatePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            startDate = calendar.timeInMillis
        },
        Calendar.getInstance().get(Calendar.YEAR),
        Calendar.getInstance().get(Calendar.MONTH),
        Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    )
    
    val endDatePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            endDate = calendar.timeInMillis
        },
        Calendar.getInstance().get(Calendar.YEAR),
        Calendar.getInstance().get(Calendar.MONTH),
        Calendar.getInstance().get(Calendar.DAY_OF_MONTH) + 7 // Default to a week later
    )
    
    LaunchedEffect(uiState) {
        when (uiState) {
            is BudgetUiState.Success -> {
                // navigate back to home screen
                onBudgetCreated()
                // Reset state after navigation
                budgetViewModel.resetState()
            }
            is BudgetUiState.Error -> {
                snackbarHostState.showSnackbar((uiState as BudgetUiState.Error).message)
            }
            else -> {}
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Budget") },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // budget field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Budget Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // amount field
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Budget Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    prefix = { Text("$") }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // budget Period (always shown)
                Text(
                    text = "Budget Period",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Start)
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = budgetPeriod == BudgetPeriod.WEEKLY,
                        onClick = { budgetPeriod = BudgetPeriod.WEEKLY }
                    )
                    Text("Weekly")
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    RadioButton(
                        selected = budgetPeriod == BudgetPeriod.MONTHLY,
                        onClick = { budgetPeriod = BudgetPeriod.MONTHLY }
                    )
                    Text("Monthly")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Repeating toggler
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
                
                // Custom date toggler
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
                        onCheckedChange = { isCustomDateRange = it }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // start date 
                Text(
                    text = "Start Date",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Start)
                )
                
                OutlinedTextField(
                    value = dateFormatter.format(Date(startDate)),
                    onValueChange = { },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { startDatePickerDialog.show() },
                    trailingIcon = {
                        IconButton(onClick = { startDatePickerDialog.show() }) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Select start date"
                            )
                        }
                    }
                )
                
                // end date 
                if (isCustomDateRange || !isRepeating) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "End Date" + if (!isRepeating && !isCustomDateRange) " (Auto)" else "",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    
                    OutlinedTextField(
                        value = if (isCustomDateRange) {
                            endDate?.let { dateFormatter.format(Date(it)) } ?: "Not set"
                        } else if (!isRepeating) {
                            calculatedEndDate?.let { dateFormatter.format(Date(it)) } ?: "Not set"
                        } else {
                            "Not set"
                        },
                        onValueChange = { },
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                if (isCustomDateRange) {
                                    endDatePickerDialog.show() 
                                }
                            },
                        trailingIcon = {
                            if (isCustomDateRange) {
                                IconButton(onClick = { endDatePickerDialog.show() }) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "Select end date"
                                    )
                                }
                            }
                        },
                        enabled = isCustomDateRange
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // create button
                Button(
                    onClick = {
                        val amountValue = amount.toDoubleOrNull()
                        if (amountValue != null && amountValue > 0) {
                            val budgetName = if (name.isBlank()) "My Budget" else name
                            
                            val finalEndDate = when {
                                isCustomDateRange -> endDate
                                !isRepeating -> calculatedEndDate
                                else -> null
                            }
                            
                            budgetViewModel.createBudget(
                                name = budgetName,
                                amount = amountValue,
                                period = budgetPeriod,
                                startDate = startDate,
                                endDate = finalEndDate,
                                repeating = isRepeating
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = amount.isNotBlank() && 
                             amount.toDoubleOrNull() != null && 
                             amount.toDoubleOrNull()!! > 0 &&
                             (!isCustomDateRange || endDate != null) &&
                             uiState !is BudgetUiState.Loading
                ) {
                    Text("Create Budget")
                }
            }
            
            // loading indicator
            if (uiState is BudgetUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
} 