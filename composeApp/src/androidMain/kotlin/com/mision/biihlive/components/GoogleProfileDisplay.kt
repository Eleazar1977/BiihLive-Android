package com.mision.biihlive.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mision.biihlive.utils.SessionManager

@Composable
fun GoogleProfileDisplay(
    modifier: Modifier = Modifier,
    onProfileClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val userInfo = remember { SessionManager.getGoogleUserInfo(context) }
    var showDropdown by remember { mutableStateOf(false) }
    
    if (userInfo != null) {
        Box(modifier = modifier) {
            // Profile button
            Surface(
                modifier = Modifier
                    .clickable { showDropdown = true },
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Profile photo
                    if (userInfo.photoUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(userInfo.photoUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Profile photo",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile",
                            modifier = Modifier.size(32.dp),
                            tint = Color.White
                        )
                    }
                    
                    // User name
                    Column {
                        Text(
                            text = userInfo.name ?: userInfo.email.substringBefore("@"),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = userInfo.email,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Menu",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            // Dropdown menu
            DropdownMenu(
                expanded = showDropdown,
                onDismissRequest = { showDropdown = false },
                modifier = Modifier.background(Color(0xFF2A2A2A))
            ) {
                // User info in dropdown
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .width(200.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (userInfo.photoUrl != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(userInfo.photoUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Profile photo",
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Profile",
                                modifier = Modifier.size(48.dp),
                                tint = Color.White
                            )
                        }
                        
                        Column {
                            Text(
                                text = userInfo.name ?: userInfo.email.substringBefore("@"),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = userInfo.email,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
                
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.1f),
                    thickness = 1.dp
                )
                
                // Profile option
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Ver perfil",
                                tint = Color.White
                            )
                            Text(
                                "Ver perfil",
                                color = Color.White
                            )
                        }
                    },
                    onClick = {
                        showDropdown = false
                        onProfileClick()
                    },
                    colors = MenuDefaults.itemColors(
                        textColor = Color.White
                    )
                )
            }
        }
    }
}

@Composable
fun MiniGoogleProfile(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val userInfo = remember { SessionManager.getGoogleUserInfo(context) }
    
    if (userInfo != null) {
        Row(
            modifier = modifier
                .clickable { onClick() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Mini profile photo
            if (userInfo.photoUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(userInfo.photoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Profile",
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Profile",
                    modifier = Modifier.size(28.dp),
                    tint = Color.White
                )
            }
            
            Text(
                text = userInfo.name?.split(" ")?.firstOrNull() ?: userInfo.email.substringBefore("@"),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}