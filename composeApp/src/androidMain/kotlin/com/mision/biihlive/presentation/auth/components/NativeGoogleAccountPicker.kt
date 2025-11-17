package com.mision.biihlive.screens

import android.accounts.AccountManager
import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.common.AccountPicker

@Composable
fun NativeGoogleAccountPicker(
    onAccountSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    
    // Launcher para el selector de cuentas
    val accountPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val accountName = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (accountName != null) {
                Log.d("AccountPicker", "✅ Cuenta seleccionada: $accountName")
                onAccountSelected(accountName)
            } else {
                Log.d("AccountPicker", "No se seleccionó ninguna cuenta")
            }
        } else {
            Log.d("AccountPicker", "Selección cancelada")
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Biihlive",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF7300),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Selecciona tu cuenta de Google",
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 48.dp)
            )
            
            Button(
                onClick = {
                    // ESTO MUESTRA EL DIÁLOGO NATIVO
                    val intent = AccountPicker.newChooseAccountIntent(
                        null, // selectedAccount
                        null, // allowableAccounts  
                        arrayOf("com.google"), // allowableAccountTypes - SOLO Google
                        false, // alwaysPromptForAccount
                        null, // descriptionOverrideText
                        null, // addAccountAuthTokenType
                        null, // addAccountRequiredFeatures
                        null  // optionsBundle
                    )
                    
                    Log.d("AccountPicker", "Mostrando selector nativo de cuentas...")
                    accountPickerLauncher.launch(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = "Continuar con Google",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Text(
                text = "Se mostrará el selector nativo de Android",
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}