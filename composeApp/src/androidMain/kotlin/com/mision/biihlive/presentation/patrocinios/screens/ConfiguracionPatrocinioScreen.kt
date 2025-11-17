package com.mision.biihlive.presentation.patrocinios.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mision.biihlive.ui.theme.BiihliveBlue
import com.mision.biihlive.ui.theme.BiihliveOrangeLight
import com.mision.biihlive.presentation.patrocinios.viewmodel.ConfiguracionPatrocinioViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracionPatrocinioScreen(
    navController: NavController,
    viewModel: ConfiguracionPatrocinioViewModel = viewModel()
) {
    // Estados de UI desde ViewModel
    val uiState by viewModel.uiState.collectAsState()

    // Estados para dropdowns
    var showCurrencyDropdown by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    // Opciones para los dropdowns
    val currencyOpciones = listOf("€", "$", "£", "¥")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Configuración de Patrocinio",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BiihliveBlue)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Subtítulo
                Text(
                    text = "Configura tus opciones de patrocinio",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Switch Permitir patrocinios
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Permitir patrocinios",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Los usuarios podrán patrocinarte",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.config.isEnabled,
                        onCheckedChange = { viewModel.updateIsEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = BiihliveBlue, // Color corporativo correcto
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }

                // Dropdown de Moneda
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Moneda",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    ExposedDropdownMenuBox(
                        expanded = showCurrencyDropdown,
                        onExpandedChange = { showCurrencyDropdown = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = uiState.config.currency,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Moneda para todas las opciones") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCurrencyDropdown)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = showCurrencyDropdown,
                            onDismissRequest = { showCurrencyDropdown = false }
                        ) {
                            currencyOpciones.forEach { currency ->
                                DropdownMenuItem(
                                    text = { Text(currency) },
                                    onClick = {
                                        viewModel.updateCurrency(currency)
                                        showCurrencyDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Sección Opciones de patrocinio
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header con título y botón agregar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Opciones de patrocinio (1)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        TextButton(
                            onClick = { /* TODO: Agregar nuevo plan */ },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = BiihliveBlue // Color corporativo correcto
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Agregar",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Agregar",
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Card del plan
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color.White,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = Color(0xFFE0E0E0) // Gris muy claro para el borde
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Información del plan
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "Plan Patrocinio Mensual",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.Black
                                )
                                Text(
                                    text = "${uiState.config.currency} ${uiState.config.options.firstOrNull()?.price ?: "19.99"} / ${uiState.config.options.firstOrNull()?.duration ?: "1 mes"}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = BiihliveBlue,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "30 días",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    fontSize = 13.sp
                                )
                            }

                            // Botones de acción
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Botón editar
                                IconButton(
                                    onClick = { showEditDialog = true },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Editar",
                                        tint = BiihliveBlue,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // Botón eliminar
                                IconButton(
                                    onClick = { /* TODO: Eliminar plan */ },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Eliminar",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Campo Descripción
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Descripción",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = uiState.config.description,
                        onValueChange = { viewModel.updateDescription(it) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        maxLines = 4,
                        minLines = 3
                    )

                    Text(
                        text = "Descripción que verán todos los usuarios",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Botón Guardar
                Button(
                    onClick = { viewModel.saveConfiguration() },
                    enabled = !uiState.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BiihliveBlue,
                        contentColor = Color.White,
                        disabledContainerColor = BiihliveBlue.copy(alpha = 0.6f),
                        disabledContentColor = Color.White.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (uiState.isSaving) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Text("Guardando...")
                        }
                    } else {
                        Text(
                            text = if (uiState.hasChanges)
                                "Guardar configuración"
                            else
                                "Todo guardado",
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Dialog para editar plan
        if (showEditDialog) {
            PatrocinioOptionDialog(
                currentConfig = uiState.config,
                onDismiss = { showEditDialog = false },
                onSave = { price, duration ->
                    viewModel.updatePrice(price)
                    viewModel.updateDuration(duration)
                    showEditDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PatrocinioOptionDialog(
    currentConfig: com.mision.biihlive.domain.perfil.model.PatrocinioConfig,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var price by remember { mutableStateOf(currentConfig.options.firstOrNull()?.price ?: "19.99") }
    var selectedDuration by remember { mutableStateOf(currentConfig.options.firstOrNull()?.duration ?: "1 mes") }
    var showDurationDropdown by remember { mutableStateOf(false) }

    val durationOptions = listOf("1 mes", "3 meses", "anual")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Editar Plan de Patrocinio",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Campo precio
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Precio") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // Dropdown duración
                ExposedDropdownMenuBox(
                    expanded = showDurationDropdown,
                    onExpandedChange = { showDurationDropdown = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedDuration,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Duración") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDurationDropdown)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = showDurationDropdown,
                        onDismissRequest = { showDurationDropdown = false }
                    ) {
                        durationOptions.forEach { duration ->
                            DropdownMenuItem(
                                text = { Text(duration) },
                                onClick = {
                                    selectedDuration = duration
                                    showDurationDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(price, selectedDuration) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = BiihliveBlue // Color corporativo correcto
                )
            ) {
                Text("Guardar", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    )
}