package com.mision.biihlive.presentation.perfil

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mision.biihlive.domain.perfil.model.PerfilUsuario
import com.mision.biihlive.di.PerfilModule
import com.mision.biihlive.ui.theme.BiihliveBlue
import com.mision.biihlive.ui.theme.BiihliveOrange
import com.mision.biihlive.ui.theme.BiihliveOrangeLight
import com.mision.biihlive.ui.theme.BiihliveGreen
import com.mision.biihlive.R
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import coil.request.CachePolicy
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarPerfilScreen(
    navController: NavController,
    viewModel: PerfilPersonalLogueadoViewModel? = null
) {
    val context = LocalContext.current.applicationContext as Application
    val actualViewModel = viewModel ?: viewModel {
        PerfilModule.providePerfilPersonalLogueadoViewModel(context)
    }

    val uiState by actualViewModel.uiState.collectAsState()
    val perfil = uiState.perfil
    // Estados para campos editables - inicializar con datos del perfil cuando estén disponibles
    var nickname by remember { mutableStateOf(TextFieldValue("")) }
    var description by remember { mutableStateOf(TextFieldValue("")) }
    var mostrarEstado by remember { mutableStateOf(true) }
    var compartirGeolocalizacion by remember { mutableStateOf(false) }

    // Estados para desplegables
    var rankingPreference by remember { mutableStateOf("local") }
    var tipoCuenta by remember { mutableStateOf("persona") }
    var paisSeleccionado by remember { mutableStateOf("") }
    var provinciaSeleccionada by remember { mutableStateOf("") }
    var ciudadSeleccionada by remember { mutableStateOf("") }

    // Estados para dropdowns
    var showRankingDropdown by remember { mutableStateOf(false) }
    var showTipoDropdown by remember { mutableStateOf(false) }
    var showPaisDropdown by remember { mutableStateOf(false) }
    var showProvinciaDropdown by remember { mutableStateOf(false) }
    var showCiudadDropdown by remember { mutableStateOf(false) }

    // Estados para manejo de imagen
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showImagePreview by remember { mutableStateOf(false) }

    // Detección automática de cambios
    val hasChanges = perfil?.let { profile ->
        nickname.text.trim() != profile.nickname ||
        description.text.trim() != profile.description ||
        rankingPreference != profile.rankingPreference ||
        tipoCuenta != profile.tipo ||
        paisSeleccionado != profile.ubicacion.pais ||
        provinciaSeleccionada != profile.ubicacion.provincia ||
        ciudadSeleccionada != profile.ubicacion.ciudad ||
        mostrarEstado != profile.mostrarEstado
    } ?: false

    // Actualizar campos cuando el perfil cambie
    LaunchedEffect(perfil) {
        perfil?.let { profile ->
            nickname = TextFieldValue(profile.nickname)
            description = TextFieldValue(profile.description)
            rankingPreference = profile.rankingPreference
            tipoCuenta = profile.tipo
            paisSeleccionado = profile.ubicacion.pais
            provinciaSeleccionada = profile.ubicacion.provincia
            ciudadSeleccionada = profile.ubicacion.ciudad
            mostrarEstado = profile.mostrarEstado
        }
    }

    // Mostrar feedback de éxito
    LaunchedEffect(uiState.updateSuccess) {
        if (uiState.updateSuccess) {
            delay(2000) // Esperar 2 segundos
            actualViewModel.limpiarUpdateSuccess()
            // hasChanges se actualiza automáticamente basado en comparación
        }
    }

    // Image picker launcher - conectado al ViewModel
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            actualViewModel.uploadProfileImage(it)
        }
    }

    // Función para guardar cambios
    fun guardarCambios() {
        perfil?.let { currentProfile ->
            // Actualizar nickname si ha cambiado
            if (nickname.text.trim() != currentProfile.nickname) {
                actualViewModel.actualizarNickname(nickname.text.trim())
            }

            // Actualizar descripción si ha cambiado
            if (description.text.trim() != currentProfile.description) {
                actualViewModel.actualizarDescripcion(description.text.trim())
            }

            // Actualizar ranking preference si ha cambiado
            if (rankingPreference != currentProfile.rankingPreference) {
                actualViewModel.actualizarRankingPreference(rankingPreference)
            }

            // Actualizar tipo de cuenta si ha cambiado
            if (tipoCuenta != currentProfile.tipo) {
                actualViewModel.actualizarTipoCuenta(tipoCuenta)
            }

            // Actualizar ubicación si ha cambiado
            val ubicacionCambiada = paisSeleccionado != currentProfile.ubicacion.pais ||
                                   provinciaSeleccionada != currentProfile.ubicacion.provincia ||
                                   ciudadSeleccionada != currentProfile.ubicacion.ciudad

            if (ubicacionCambiada) {
                actualViewModel.actualizarUbicacion(
                    pais = paisSeleccionado,
                    provincia = provinciaSeleccionada,
                    ciudad = ciudadSeleccionada
                )
            }

            // Actualizar mostrar estado si ha cambiado
            if (mostrarEstado != currentProfile.mostrarEstado) {
                actualViewModel.actualizarMostrarEstado(mostrarEstado)
            }
        }
    }

    BackHandler {
        navController.popBackStack()
    }

    // Mostrar SnackBar para feedback
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // El error se muestra en la UI, se podría añadir SnackBar aquí
        }
    }

    if (uiState.isLoading && perfil == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = BiihliveOrange)
        }
        return
    }

    if (perfil == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "No se pudo cargar el perfil",
                    style = MaterialTheme.typography.bodyLarge
                )
                Button(
                    onClick = { navController.popBackStack() }
                ) {
                    Text("Volver")
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            EditarPerfilTopBar(
                onBackClick = { navController.popBackStack() },
                onSaveClick = { guardarCambios() },
                isLoading = uiState.isUpdating,
                hasChanges = hasChanges
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Mostrar errores
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { actualViewModel.limpiarError() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Mostrar éxito
            if (uiState.updateSuccess) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = BiihliveGreen.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = BiihliveGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Perfil actualizado correctamente",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            item {
                // Avatar con badge de editar
                AvatarSection(
                    perfil = perfil,
                    profileImageUrl = uiState.profileImageUrl,
                    profileThumbnailUrl = uiState.profileThumbnailUrl,
                    isUploadingImage = uiState.isUploadingImage,
                    shouldBypassCache = uiState.shouldBypassImageCache,
                    onSelectImage = {
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            }

            item {
                // Campos de texto editables (sin fullName)
                CamposTextoSection(
                    nickname = nickname,
                    onNicknameChange = {
                        nickname = it
                        // hasChanges se detecta automáticamente
                    },
                    description = description,
                    onDescriptionChange = {
                        description = it
                        // hasChanges se detecta automáticamente
                    },
                    isLoading = uiState.isUpdating
                )
            }

            item {
                // Switches
                SwitchesSection(
                    mostrarEstado = mostrarEstado,
                    onMostrarEstadoChange = {
                        mostrarEstado = it
                        // hasChanges se detecta automáticamente
                    },
                    compartirGeolocalizacion = compartirGeolocalizacion,
                    onCompartirGeolocalizacionChange = {
                        compartirGeolocalizacion = it
                        // hasChanges se detecta automáticamente
                    }
                )
            }

            item {
                // Desplegables de preferencias
                PreferenciasSection(
                    rankingPreference = rankingPreference,
                    showRankingDropdown = showRankingDropdown,
                    onRankingDropdownToggle = { showRankingDropdown = it },
                    onRankingSelected = {
                        rankingPreference = it
                        showRankingDropdown = false
                        // hasChanges se detecta automáticamente
                    },
                    tipoCuenta = tipoCuenta,
                    showTipoDropdown = showTipoDropdown,
                    onTipoDropdownToggle = { showTipoDropdown = it },
                    onTipoSelected = {
                        tipoCuenta = it
                        showTipoDropdown = false
                        // hasChanges se detecta automáticamente
                    }
                )
            }

            item {
                // Sección de localización
                LocalizacionSection(
                    paisSeleccionado = paisSeleccionado,
                    showPaisDropdown = showPaisDropdown,
                    onPaisDropdownToggle = { showPaisDropdown = it },
                    onPaisSelected = {
                        paisSeleccionado = it
                        showPaisDropdown = false
                        // Reset provincia y ciudad cuando cambia país
                        provinciaSeleccionada = ""
                        ciudadSeleccionada = ""
                        // hasChanges se detecta automáticamente
                    },
                    provinciaSeleccionada = provinciaSeleccionada,
                    showProvinciaDropdown = showProvinciaDropdown,
                    onProvinciaDropdownToggle = { showProvinciaDropdown = it },
                    onProvinciaSelected = {
                        provinciaSeleccionada = it
                        showProvinciaDropdown = false
                        // Reset ciudad cuando cambia provincia
                        ciudadSeleccionada = ""
                        // hasChanges se detecta automáticamente
                    },
                    ciudadSeleccionada = ciudadSeleccionada,
                    showCiudadDropdown = showCiudadDropdown,
                    onCiudadDropdownToggle = { showCiudadDropdown = it },
                    onCiudadSelected = {
                        ciudadSeleccionada = it
                        showCiudadDropdown = false
                        // hasChanges se detecta automáticamente
                    }
                )
            }
            } // Cierra LazyColumn
        } // Cierra Column
    } // Cierra Scaffold
} // Cierra función EditarPerfilScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditarPerfilTopBar(
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    isLoading: Boolean = false,
    hasChanges: Boolean = false
) {
    TopAppBar(
        title = {
            Text(
                "Editar Perfil",
                style = MaterialTheme.typography.titleLarge
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
        },
        actions = {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = BiihliveOrange
                )
            } else {
                TextButton(
                    onClick = onSaveClick,
                    enabled = hasChanges
                ) {
                    Text(
                        text = "Guardar",
                        color = if (hasChanges) BiihliveOrangeLight else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun AvatarSection(
    perfil: PerfilUsuario,
    profileImageUrl: String?,
    profileThumbnailUrl: String?,
    isUploadingImage: Boolean,
    shouldBypassCache: Boolean,
    onSelectImage: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(120.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onSelectImage() }
                    .border(2.dp, BiihliveBlue, CircleShape),
                shape = CircleShape,
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(profileThumbnailUrl ?: profileImageUrl)
                            .crossfade(true)
                            .memoryCachePolicy(if (shouldBypassCache) CachePolicy.DISABLED else CachePolicy.ENABLED)
                            .diskCachePolicy(if (shouldBypassCache) CachePolicy.DISABLED else CachePolicy.ENABLED)
                            .memoryCacheKey("profile_${perfil.userId}")
                            .build(),
                        contentDescription = "Foto de Perfil",
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(R.drawable.ic_default_avatar),
                        error = painterResource(R.drawable.ic_default_avatar),
                        fallback = painterResource(R.drawable.ic_default_avatar)
                    )

                    // Overlay de loading durante upload
                    if (isUploadingImage) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 3.dp
                            )
                        }
                    }
                }
            }

            // Badge de editar (mismo que en perfil personal)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-6).dp, y = (-6).dp) // Posición calculada matemáticamente
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(BiihliveOrange)
                    .clickable(onClick = onSelectImage),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = "Cambiar foto",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Text(
            text = if (isUploadingImage) "Subiendo imagen..." else "Tocar para cambiar foto",
            style = MaterialTheme.typography.bodySmall,
            color = if (isUploadingImage) BiihliveOrange else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CamposTextoSection(
    nickname: TextFieldValue,
    onNicknameChange: (TextFieldValue) -> Unit,
    description: TextFieldValue,
    onDescriptionChange: (TextFieldValue) -> Unit,
    isLoading: Boolean = false
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Nickname
        OutlinedTextField(
            value = nickname,
            onValueChange = onNicknameChange,
            label = { Text("Nickname") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BiihliveBlue,
                focusedLabelColor = BiihliveBlue
            ),
            shape = RoundedCornerShape(8.dp),
            supportingText = {
                Text(
                    text = "Tu nombre público en la aplicación",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        )

        // Description
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Descripción") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            minLines = 3,
            maxLines = 5,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BiihliveBlue,
                focusedLabelColor = BiihliveBlue
            ),
            shape = RoundedCornerShape(8.dp),
            supportingText = {
                Text(
                    text = "Describe tu perfil en pocas palabras",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        )

        // Mostrar error si existe
        if (isLoading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = BiihliveOrange
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Guardando cambios...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SwitchesSection(
    mostrarEstado: Boolean,
    onMostrarEstadoChange: (Boolean) -> Unit,
    compartirGeolocalizacion: Boolean,
    onCompartirGeolocalizacionChange: (Boolean) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Configuración de Privacidad",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Mostrar estado online/offline
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Mostrar estado",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Permite que otros vean si estás online",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = mostrarEstado,
                onCheckedChange = onMostrarEstadoChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = BiihliveGreen,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = MaterialTheme.colorScheme.outline
                )
            )
        }

        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // Compartir geolocalización
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Compartir geolocalización",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Usa tu ubicación actual para rankings locales",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = compartirGeolocalizacion,
                onCheckedChange = onCompartirGeolocalizacionChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = BiihliveGreen,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = MaterialTheme.colorScheme.outline
                )
            )
        }
    }
}

@Composable
private fun PreferenciasSection(
    rankingPreference: String,
    showRankingDropdown: Boolean,
    onRankingDropdownToggle: (Boolean) -> Unit,
    onRankingSelected: (String) -> Unit,
    tipoCuenta: String,
    showTipoDropdown: Boolean,
    onTipoDropdownToggle: (Boolean) -> Unit,
    onTipoSelected: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Preferencias de Cuenta",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Preferencia de ranking
        Column {
            Text(
                text = "Preferencia de ranking",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            DropdownMenuBox(
                value = when(rankingPreference) {
                    "local" -> "Local"
                    "provincial" -> "Provincial"
                    "nacional" -> "Nacional"
                    "mundial" -> "Mundial"
                    else -> "Local"
                },
                expanded = showRankingDropdown,
                onExpandedChange = onRankingDropdownToggle,
                options = listOf(
                    "Local" to "local",
                    "Provincial" to "provincial",
                    "Nacional" to "nacional",
                    "Mundial" to "mundial"
                ),
                onOptionSelected = { _, value -> onRankingSelected(value) }
            )
        }

        // Tipo de cuenta
        Column {
            Text(
                text = "Tipo de cuenta",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            DropdownMenuBox(
                value = when(tipoCuenta) {
                    "persona" -> "Personal"
                    "empresa" -> "Empresa"
                    else -> "Personal"
                },
                expanded = showTipoDropdown,
                onExpandedChange = onTipoDropdownToggle,
                options = listOf(
                    "Personal" to "persona",
                    "Empresa" to "empresa"
                ),
                onOptionSelected = { _, value -> onTipoSelected(value) }
            )
        }
    }
}

@Composable
private fun LocalizacionSection(
    paisSeleccionado: String,
    showPaisDropdown: Boolean,
    onPaisDropdownToggle: (Boolean) -> Unit,
    onPaisSelected: (String) -> Unit,
    provinciaSeleccionada: String,
    showProvinciaDropdown: Boolean,
    onProvinciaDropdownToggle: (Boolean) -> Unit,
    onProvinciaSelected: (String) -> Unit,
    ciudadSeleccionada: String,
    showCiudadDropdown: Boolean,
    onCiudadDropdownToggle: (Boolean) -> Unit,
    onCiudadSelected: (String) -> Unit
) {
    // Datos hardcodeados para España (como solicitado)
    val provinciasEspana = listOf(
        "Madrid", "Barcelona", "Valencia", "Sevilla", "Murcia", "Vizcaya",
        "Alicante", "Cádiz", "A Coruña", "Asturias", "Zaragoza", "Málaga",
        "Las Palmas", "Valladolid", "Córdoba", "Granada", "Almería"
    )

    val ciudadesPorProvincia = mapOf(
        "Madrid" to listOf("Madrid", "Getafe", "Móstoles", "Alcalá de Henares", "Leganés"),
        "Barcelona" to listOf("Barcelona", "Hospitalet de Llobregat", "Terrassa", "Badalona", "Sabadell"),
        "Valencia" to listOf("Valencia", "Alicante", "Castellón de la Plana", "Elche", "Gandía"),
        "Sevilla" to listOf("Sevilla", "Dos Hermanas", "Alcalá de Guadaíra", "Utrera", "Mairena del Aljarafe"),
        "Murcia" to listOf("Murcia", "Cartagena", "Molina de Segura", "Lorca", "Águilas"),
        "Vizcaya" to listOf("Bilbao", "Barakaldo", "Getxo", "Portugalete", "Santurtzi")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = BiihliveBlue,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Localización",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // País
        Column {
            Text(
                text = "País",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            DropdownMenuBox(
                value = paisSeleccionado,
                expanded = showPaisDropdown,
                onExpandedChange = onPaisDropdownToggle,
                options = listOf("España" to "España"), // Solo España por ahora
                onOptionSelected = { display, value -> onPaisSelected(value) }
            )
        }

        // Provincia (solo si España está seleccionado)
        if (paisSeleccionado == "España") {
            Column {
                Text(
                    text = "Provincia",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                DropdownMenuBox(
                    value = provinciaSeleccionada.ifEmpty { "Seleccionar provincia" },
                    expanded = showProvinciaDropdown,
                    onExpandedChange = onProvinciaDropdownToggle,
                    options = provinciasEspana.map { it to it },
                    onOptionSelected = { _, value -> onProvinciaSelected(value) }
                )
            }
        }

        // Ciudad (solo si hay provincia seleccionada)
        if (provinciaSeleccionada.isNotEmpty() && ciudadesPorProvincia.containsKey(provinciaSeleccionada)) {
            Column {
                Text(
                    text = "Ciudad",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                DropdownMenuBox(
                    value = ciudadSeleccionada.ifEmpty { "Seleccionar ciudad" },
                    expanded = showCiudadDropdown,
                    onExpandedChange = onCiudadDropdownToggle,
                    options = ciudadesPorProvincia[provinciaSeleccionada]?.map { it to it } ?: emptyList(),
                    onOptionSelected = { _, value -> onCiudadSelected(value) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownMenuBox(
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: List<Pair<String, String>>, // (display, value)
    onOptionSelected: (String, String) -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { },
            readOnly = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BiihliveBlue,
                focusedLabelColor = BiihliveBlue
            ),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            options.forEach { (display, optionValue) ->
                DropdownMenuItem(
                    text = { Text(display) },
                    onClick = {
                        onOptionSelected(display, optionValue)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

