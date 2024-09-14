package com.mjakopec.letswatch

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext


private lateinit var db: FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)
        val user = Firebase.auth.currentUser

        if (null === user) {
            this.startActivity(Intent(this, LoginRegisterActivity::class.java))
        }

        db = Firebase.firestore

        setContent {
            // Applying a black color scheme across the entire app
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color.Black,
                    surface = Color.Black,
                    onSurface = Color.White,
                    primary = Color.White
                )
            ) {
                MainScreen(user)
            }
        }
    }
}

@Composable
fun MainScreen(user: FirebaseUser?) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // State to manage the connection status
    var connectedTo by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch user data once and update the UI state
    LaunchedEffect(user) {
        val userData = db.collection("users").document(user?.email.toString())
        userData.get().addOnSuccessListener { document ->
            if (document != null) {
                connectedTo = document.data?.get("connectedTo") as String?
                isLoading = false
            } else {
                Log.d(TAG, "No such document")
                isLoading = false
            }
        }.addOnFailureListener { exception ->
            Log.d(TAG, "get failed with ", exception)
            isLoading = false
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerContent(scope, drawerState)
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black) // Ensuring the main content background is also black
        ) {
            IconButton(
                onClick = {
                    scope.launch {
                        drawerState.apply {
                            if (isClosed) open() else close()
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Toggle Navigation Drawer",
                    tint = Color.White  // Icon color set to white for contrast
                )
            }
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.Red)
            } else {
                if (connectedTo == null || connectedTo == "") {
                    ConnectCodeScreen(user)
                } else {
                    ConnectedWelcomeScreen(user, connectedTo)
                }
            }
        }
    }
}

@Composable
fun ConnectedWelcomeScreen(user: FirebaseUser?, connectedTo: String?) {
    val context = LocalContext.current  // Get the current Compose context

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Connected to $connectedTo",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            // Optional additional UI elements here
        }

        // FloatingActionButton positioned at the bottom
        FloatingActionButton(
            onClick = {
                // Intent to start BrowseActivity
                val intent = Intent(context, BrowseActivity::class.java)
                context.startActivity(intent)
            },
            containerColor = Color.Red, // Correct parameter to set the background color
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
        ) {
            Text("Start Browsing", color = Color.White)
        }
    }
}


@Composable
fun DrawerContent(scope: CoroutineScope, drawerState: DrawerState) {
    // Remember the selected item state
    var selectedItem by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.padding(PaddingValues(16.dp))) {
        // Define a list of items for demonstration
        val items = listOf("Drawer Item 1", "Drawer Item 2", "Drawer Item 3")

        items.forEach { item ->
            val backgroundColor = if (item == selectedItem) Color.LightGray else Color.Transparent
            Text(
                text = item,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedItem = item
                        println(selectedItem)
                        scope.launch { drawerState.close() }
                    }
                    .background(backgroundColor)
                    .padding(16.dp),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectCodeScreen(currentUser: FirebaseUser?) {
    val code = remember { (100000..999999).random().toString() }
    var enteredCode by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "This is your code",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            code,
            style = MaterialTheme.typography.displayLarge,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(
            value = enteredCode,
            onValueChange = { enteredCode = it },
            label = { Text("Enter your code") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color.Red,
                unfocusedBorderColor = Color.Red
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                db.collection("users")
                    .whereEqualTo("connectionCode", enteredCode)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (documents.documents.size == 0) {
                            Toast.makeText(
                                context,
                                "User with provided code does not exist",
                                Toast.LENGTH_LONG,
                            ).show()
                        } else {
                            val document = documents.documents[0]
                            db.collection("users").document(document.id).update("connectedTo", currentUser?.email)
                            db.collection("users").document(document.id).update("connetionCode", null)
                            db.collection("users").document(currentUser!!.email.toString()).update("connectedTo", document.id)
                            db.collection("users").document(currentUser.email.toString()).update("connetionCode", null)
                            context.startActivity(Intent(context, MainActivity::class.java))
                        }
                }
                    .addOnFailureListener { exception ->
                        Log.w(TAG, "Error getting documents: ", exception)
                    }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Connect", color = Color.White)
        }
    }
}