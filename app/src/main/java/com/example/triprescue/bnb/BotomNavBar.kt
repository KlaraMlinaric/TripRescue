package com.example.triprescue.bnb

import android.app.Activity
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.triprescue.HomeActivity
import com.example.triprescue.MainActivity
import com.example.triprescue.TextRecognitionActivity

@Composable
fun BotomNavBar() {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    // name of current activity
    val currentActivityName = (context as? Activity)?.javaClass?.simpleName

    NavigationBar(
        containerColor = colorScheme.surface,
        contentColor = colorScheme.onSecondary
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.CameraAlt, contentDescription = "Scan") },
            label = { Text("Scan", style = MaterialTheme.typography.labelMedium) },
            selected = currentActivityName == "MainActivity",
            onClick = { context.startActivity(Intent(context, MainActivity::class.java)) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = colorScheme.onSecondary,
                unselectedIconColor = colorScheme.onSecondary,
                selectedTextColor = colorScheme.onSecondary,
                unselectedTextColor = colorScheme.onSecondary,
                indicatorColor = colorScheme.scrim
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home", style = MaterialTheme.typography.labelMedium) },
            selected = currentActivityName == "HomeActivity",
                onClick = { context.startActivity(Intent(context, HomeActivity::class.java)) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = colorScheme.onSecondary,
                unselectedIconColor = colorScheme.onSecondary,
                selectedTextColor = colorScheme.onSecondary,
                unselectedTextColor = colorScheme.onSecondary,
                indicatorColor = colorScheme.scrim
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Translate, contentDescription = "Translate") },
            label = { Text("Translate", style = MaterialTheme.typography.labelMedium) },
            selected = currentActivityName == "TextRecognitionActivity",
            onClick = { context.startActivity(Intent(context, TextRecognitionActivity::class.java)) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = colorScheme.onSecondary,
                unselectedIconColor = colorScheme.onSecondary,
                selectedTextColor = colorScheme.onSecondary,
                unselectedTextColor = colorScheme.onSecondary,
                indicatorColor = colorScheme.scrim
            )
        )
    }
}