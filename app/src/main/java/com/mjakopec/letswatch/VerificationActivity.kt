package com.mjakopec.letswatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

class VerificationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VerificationScreen()
        }
    }
}

@Composable
fun VerificationScreen() {
    // Generate a 6-digit code
    val code = remember { (100000..999999).random().toString() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("This is your code", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text(code, style = MaterialTheme.typography.displayMedium)

        var enteredCode by remember { mutableStateOf("") }

        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(
            value = enteredCode,
            onValueChange = { enteredCode = it },
            label = { Text("Enter your code") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (enteredCode == code) {
                    // Do something on correct code
                } else {
                    // Handle wrong code
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Verify")
        }
    }
}
