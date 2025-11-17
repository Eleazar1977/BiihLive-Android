package com.mision.biihlive.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mision.biihlive.language.SupportedLanguage
import com.mision.biihlive.language.LocalLanguageManager
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import biihlive.composeapp.generated.resources.Res
import biihlive.composeapp.generated.resources.language
import biihlive.composeapp.generated.resources.cancel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelector(
    modifier: Modifier = Modifier,
    onLanguageChanged: () -> Unit = {}
) {
    val languageManager = LocalLanguageManager.current
    val currentLanguage by languageManager.currentLanguage.collectAsState()
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = currentLanguage.nativeName,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(Res.string.language)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            SupportedLanguage.entries.forEach { language ->
                DropdownMenuItem(
                    text = { 
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(language.nativeName)
                            if (language == currentLanguage) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    onClick = {
                        scope.launch {
                            languageManager.setLanguage(language)
                            onLanguageChanged()
                            expanded = false
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun LanguageSelectorListItem(
    modifier: Modifier = Modifier,
    onLanguageChanged: (SupportedLanguage) -> Unit = {}
) {
    val languageManager = LocalLanguageManager.current
    val currentLanguage by languageManager.currentLanguage.collectAsState()
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    
    ListItem(
        headlineContent = { Text(stringResource(Res.string.language)) },
        supportingContent = { Text(currentLanguage.nativeName) },
        leadingContent = {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null
            )
        },
        modifier = modifier.clickable { showDialog = true }
    )
    
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(Res.string.language)) },
            text = {
                Column {
                    SupportedLanguage.entries.forEach { language ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        languageManager.setLanguage(language)
                                        onLanguageChanged(language)
                                        showDialog = false
                                    }
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = language.nativeName,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (language == currentLanguage) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }
}