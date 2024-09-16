package com.mjakopec.letswatch

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Movie
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import coil.compose.rememberImagePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import okhttp3.OkHttpClient
import okhttp3.Request


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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(user: FirebaseUser?) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // State to manage the connection status
    var connectedTo by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // State to manage the current screen
    var currentScreen by remember { mutableStateOf("mainScreen") }

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
                DrawerContent(scope, drawerState) { selectedScreen ->
                    currentScreen = selectedScreen
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Movie,
                                contentDescription = "App Logo",
                                tint = Color.Red,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Let's Watch",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Toggle Navigation Drawer",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Black
                    )
                )
            },
            content = { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .padding(innerPadding)
                ) {
                    when {
                        isLoading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = Color.Red
                            )
                        }
                        else -> {
                            when (currentScreen) {
                                "mainScreen" -> {
                                    if (connectedTo.isNullOrEmpty()) {
                                        ConnectCodeScreen(user)
                                    } else {
                                        ConnectedWelcomeScreen(user, connectedTo)
                                    }
                                }
                                "likedMovies" -> {
                                    MoviesListScreen(user, "Liked Movies", connectedTo)
                                }
                                "dislikedMovies" -> {
                                    MoviesListScreen(user, "Disliked Movies", connectedTo) // Handle Disliked Movies
                                }
                                "watchedMovies" -> {
                                    MoviesListScreen(user, "Watched Movies", connectedTo) // Handle Watched Movies
                                }
                                "likedByBoth" -> {
                                    MoviesListScreen(user, "Liked by Both", connectedTo)
                                }
                                "likedByMe" -> {
                                    MoviesListScreen(user, "Liked by Me", connectedTo)
                                }
                                else -> {
                                    // Handle unknown screen
                                    Text(
                                        "Unknown screen",
                                        color = Color.White,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}


@Composable
fun ConnectedWelcomeScreen(user: FirebaseUser?, connectedTo: String?) {
    val context = LocalContext.current
    var mostRecentLikedMovie by remember { mutableStateOf<Movie?>(null) }
    var mostRecentWatchedMovie by remember { mutableStateOf<Movie?>(null) }
    var isLoadingMovie by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var likedMovieError by remember { mutableStateOf<String?>(null) }
    var watchedMovieError by remember { mutableStateOf<String?>(null) }

    // Fetch the most recent liked and watched movies
    LaunchedEffect(Unit) {
        if (user != null) {
            val userData = db.collection("users").document(user.email.toString())
            try {
                val document = userData.get().await()
                if (document.exists()) {
                    // Fetch liked movie IDs
                    val likedMovieIds = (document.get("likedMovies") as? List<Long>)?.map { it.toInt() } ?: emptyList()

                    // Fetch watched movie IDs
                    val watchedMovieIds = (document.get("watchedMovies") as? List<Long>)?.map { it.toInt() } ?: emptyList()

                    // Fetch the most recent liked movie
                    if (likedMovieIds.isNotEmpty()) {
                        val mostRecentLikedMovieId = likedMovieIds.last()
                        mostRecentLikedMovie = fetchMovieById(mostRecentLikedMovieId)
                    } else {
                        likedMovieError = "You have not liked any movies yet."
                    }

                    // Fetch the most recent watched movie
                    if (watchedMovieIds.isNotEmpty()) {
                        val mostRecentWatchedMovieId = watchedMovieIds.last()
                        mostRecentWatchedMovie = fetchMovieById(mostRecentWatchedMovieId)
                    } else {
                        watchedMovieError = "You have not watched any movies yet."
                    }
                }
            } catch (e: Exception) {
                errorMessage = e.message
                Log.e("Firebase", "Error fetching movies", e)
            } finally {
                isLoadingMovie = false
            }
        } else {
            errorMessage = "User not logged in"
            isLoadingMovie = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(top = 56.dp), // Add padding to leave space at the top
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoadingMovie) {
            CircularProgressIndicator(color = Color.Red)
        } else if (errorMessage != null) {
            Text("Error: $errorMessage", color = Color.White)
        } else {
            if (user != null) {
                Text(
                    "welcome back ${user.email}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "You are connected to to $connectedTo",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(32.dp))
            Divider(
                color = Color.Red,
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 8.dp).width(200.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Display Most Recent Liked Movie
            if (mostRecentLikedMovie != null) {
                Text(
                    "Most Recent Liked Movie:",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                MovieItem(
                    movie = mostRecentLikedMovie!!,
                    movieType = "MainScreen",
                    onWatchedClicked = {} // Empty lambda since no action is needed here
                )
            } else if (likedMovieError != null) {
                Text(likedMovieError!!, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(
                color = Color.Red,
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 8.dp).width(200.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Display Most Recent Watched Movie
            if (mostRecentWatchedMovie != null) {
                Text(
                    "Most Recent Watched Movie:",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                MovieItem(
                    movie = mostRecentWatchedMovie!!,
                    movieType = "MainScreen",
                    onWatchedClicked = {},
                    imageOnLeft = false
                )
            } else if (watchedMovieError != null) {
                Text(watchedMovieError!!, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // FloatingActionButton positioned at the bottom
        FloatingActionButton(
            onClick = {
                // Intent to start BrowseActivity
                val intent = Intent(context, BrowseActivity::class.java)
                context.startActivity(intent)
            },
            containerColor = Color.Red,
            shape = RoundedCornerShape(50), // Set the shape of the button
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 35.dp)
                .width(150.dp)
                .height(56.dp) // Set a height to define the button size
                .clip(RoundedCornerShape(50)) // Clip the content to the rounded shape
                .border(3.dp, Color.White, RoundedCornerShape(50)) // Apply the rounded border
        ) {
            Text("Start Browsing", color = Color.White)
        }
    }
}

@Composable
fun MoviesListScreen(user: FirebaseUser?, movieType: String, connectedTo: String?) {
    val context = LocalContext.current
    var movies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var isLoadingMovies by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Fetch movies based on type
    LaunchedEffect(Unit) {
        if (user != null) {
            val userData = db.collection("users").document(user.email.toString())

            try {
                val document = userData.get().await()
                if (document.exists()) {
                    // Handle movie fetching based on type
                    val movieIds = when (movieType) {
                        "Liked Movies" -> (document.get("likedMovies") as? List<Long>)?.map { it.toInt() } ?: emptyList()
                        "Disliked Movies" -> (document.get("dislikedMovies") as? List<Long>)?.map { it.toInt() } ?: emptyList()
                        "Watched Movies" -> (document.get("watchedMovies") as? List<Long>)?.map { it.toInt() } ?: emptyList()
                        "Liked by Both" -> {
                            val connectedUserData = db.collection("users").document(connectedTo ?: "")
                            val connectedDoc = connectedUserData.get().await()

                            if (connectedDoc.exists()) {
                                val connectedLikedMovies = (connectedDoc.get("likedMovies") as? List<Long>)?.map { it.toInt() } ?: emptyList()
                                val userLikedMovies = (document.get("likedMovies") as? List<Long>)?.map { it.toInt() } ?: emptyList()

                                // Fetch intersection of both users' liked movies
                                userLikedMovies.intersect(connectedLikedMovies).toList()
                            } else emptyList()
                        }
                        "Liked by Me" -> {
                            val connectedUserData = db.collection("users").document(connectedTo ?: "")
                            val connectedDoc = connectedUserData.get().await()

                            if (connectedDoc.exists()) {
                                val connectedLikedMovies = (connectedDoc.get("likedMovies") as? List<Long>)?.map { it.toInt() } ?: emptyList()
                                val userLikedMovies = (document.get("likedMovies") as? List<Long>)?.map { it.toInt() } ?: emptyList()

                                // Fetch movies liked by me but not by the connected user
                                userLikedMovies.subtract(connectedLikedMovies).toList()
                            } else emptyList()
                        }
                        else -> emptyList()
                    }

                    // Fetch movie details concurrently
                    movies = fetchMoviesByIds(movieIds)
                }
            } catch (e: Exception) {
                errorMessage = e.message
                Log.e("Firebase", "Error fetching movies", e)
            } finally {
                isLoadingMovies = false
            }
        } else {
            errorMessage = "User not logged in"
            isLoadingMovies = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Loading and error handling
        if (isLoadingMovies) {
            CircularProgressIndicator(color = Color.Red)
        } else if (errorMessage != null) {
            Text("Error: $errorMessage", color = Color.White)
        } else if (movies.isNotEmpty()) {
            LazyColumn {
                itemsIndexed(movies) { index, movie ->
                    val imageOnLeft = index % 2 == 0
                    MovieItem(
                        movie = movie,
                        movieType = movieType,
                        onWatchedClicked = { movieId ->
                            moveMovieToWatched(user, movieId) {
                                // Update UI after moving the movie
                                movies = movies.filter { it.id != movieId }
                                Toast.makeText(
                                    context,
                                    "Moved to Watched Movies",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        imageOnLeft = imageOnLeft
                    )
                    if (index < movies.size - 1) {
                        Divider(
                            color = Color.Gray,
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        } else {
            Text("No movies available.", color = Color.White)
        }
    }
}

@Composable
fun MovieItem(
    movie: Movie,
    movieType: String = "",
    onWatchedClicked: ((Int) -> Unit)? = null,
    imageOnLeft: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (imageOnLeft) {
            // **Image on the left**
            Image(
                painter = rememberImagePainter(
                    data = "https://image.tmdb.org/t/p/w200${movie.poster_path}",
                    builder = {
                        crossfade(true)
                    }
                ),
                contentDescription = null,
                modifier = Modifier
                    .height(125.dp)
                    .width(175.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally // **Center content horizontally**
            ) {
                Text(
                    text = movie.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center // **Center text within Text**
                )
                Text(
                    text = "Release Date: ${movie.release_date}",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center // **Center text within Text**
                )
                if ((movieType == "Liked Movies" || movieType == "Liked by Me" || movieType == "Liked by Both") && onWatchedClicked != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    // **Center the button horizontally**
                    OutlinedButton(
                        onClick = { onWatchedClicked(movie.id) },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.DarkGray,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 12.dp),
                        border = BorderStroke(2.dp, Color.Red),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text("Watched it")
                    }
                }
            }
        } else {
            // **Image on the right**
            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally // **Center content horizontally**
            ) {
                Text(
                    text = movie.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center // **Center text within Text**
                )
                Text(
                    text = "Release Date: ${movie.release_date}",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center // **Center text within Text**
                )
                if ((movieType == "Liked Movies" || movieType == "Liked by Me" || movieType == "Liked by Both") && onWatchedClicked != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    // **Center the button horizontally**
                    OutlinedButton(
                        onClick = { onWatchedClicked(movie.id) },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.DarkGray,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 12.dp),
                        border = BorderStroke(2.dp, Color.Red),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text("Watched it")
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Image(
                painter = rememberImagePainter(
                    data = "https://image.tmdb.org/t/p/w200${movie.poster_path}",
                    builder = {
                        crossfade(true)
                    }
                ),
                contentDescription = null,
                modifier = Modifier
                    .height(125.dp)
                    .width(175.dp),
                contentScale = ContentScale.Crop
            )
        }
    }
}


fun moveMovieToWatched(
    user: FirebaseUser?,
    movieId: Int,
    onMovieMoved: () -> Unit
) {
    val userEmail = user?.email ?: return
    val userDocRef = db.collection("users").document(userEmail)

    db.runTransaction { transaction ->
        val snapshot = transaction.get(userDocRef)

        // Adjust the type casting based on how IDs are stored
        val likedMovies = snapshot.get("likedMovies") as? MutableList<Long> ?: mutableListOf()
        val watchedMovies = snapshot.get("watchedMovies") as? MutableList<Long> ?: mutableListOf()

        // Remove from likedMovies if present
        if (likedMovies.contains(movieId.toLong())) {
            likedMovies.remove(movieId.toLong())
            transaction.update(userDocRef, "likedMovies", likedMovies)
        }

        // Add to watchedMovies if not already present
        if (!watchedMovies.contains(movieId.toLong())) {
            watchedMovies.add(movieId.toLong())
            transaction.update(userDocRef, "watchedMovies", watchedMovies)
        }
    }.addOnSuccessListener {
        Log.d("Firebase", "Movie moved to watchedMovies successfully.")
        onMovieMoved() // Call the callback to update UI
    }.addOnFailureListener { e ->
        Log.w("Firebase", "Error moving movie to watchedMovies", e)
    }
}
@Composable
fun MoviesList(movies: List<Movie>) {
    LazyColumn {
        items(movies) { movie ->
            MovieItem(movie)
        }
    }
}

suspend fun fetchMoviesByIds(movieIds: List<Int>): List<Movie> = coroutineScope {
    val deferredMovies = movieIds.map { id ->
        async {
            fetchMovieById(id)
        }
    }
    deferredMovies.awaitAll().filterNotNull()
}

@OptIn(ExperimentalSerializationApi::class)
suspend fun fetchMovieById(movieId: Int): Movie? {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("https://api.themoviedb.org/3/movie/$movieId")
        .get()
        .addHeader("accept", "application/json")
        .addHeader("Authorization", "Bearer ${API.globalKey}")
        .build()

    return withContext(Dispatchers.IO) {
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val responseString = response.body?.string()
                responseString?.let {
                    json.decodeFromString<Movie>(it)
                }
            } else {
                Log.e("API", "Error fetching movie with ID $movieId: ${response.message}")
                null
            }
        }
    }
}

@Composable
fun MoviesListDisplay(movies: List<Movie>, isLoading: Boolean, errorMessage: String?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(color = Color.Red)
            }
            errorMessage != null -> {
                Text("Error: $errorMessage", color = Color.White)
            }
            movies.isNotEmpty() -> {
                LazyColumn {
                    items(movies) { movie ->
                        MovieItem(movie = movie, movieType = "Liked by Me", onWatchedClicked = { /* Handle watched */ })
                    }
                }
            }
            else -> {
                Text("No movies available.", color = Color.White)
            }
        }
    }
}


@Composable
fun DrawerContent(
    scope: CoroutineScope,
    drawerState: DrawerState,
    onScreenSelected: (String) -> Unit
) {
    val context = LocalContext.current
    // Remember the selected item state
    var selectedItem by remember { mutableStateOf("mainScreen") }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(PaddingValues(16.dp)),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top section with navigation items
        Column {
            // Main Screen
            val mainItems = listOf("Main Screen", "Disliked Movies", "Watched Movies")
            val mainKeys = listOf("mainScreen", "dislikedMovies", "watchedMovies")

            // Define the list of drawer items excluding liked movies
            mainItems.forEachIndexed { index, item ->
                val key = mainKeys[index]
                val backgroundColor = if (key == selectedItem) Color.DarkGray else Color.Transparent
                Text(
                    text = item,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedItem = key
                            scope.launch { drawerState.close() }
                            onScreenSelected(key)
                        }
                        .background(backgroundColor)
                        .padding(16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }

            // Liked Movies section with sub-options
            val likedMoviesItems = listOf("Liked by Both", "Liked only by Me")
            val likedMoviesKeys = listOf("likedByBoth", "likedByMe")

            Text(
                text = "Liked Movies",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )

            likedMoviesItems.forEachIndexed { index, item ->
                val key = likedMoviesKeys[index]
                val backgroundColor = if (key == selectedItem) Color.DarkGray else Color.Transparent
                Text(
                    text = item,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedItem = key
                            scope.launch { drawerState.close() }
                            onScreenSelected(key)
                        }
                        .background(backgroundColor)
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
            }
        }

        // Bottom section with the "Log Out" option
        Column {
            Divider(color = Color.Gray, thickness = 1.dp)
            Text(
                text = "Log Out",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // Sign out the user
                        Firebase.auth.signOut()
                        // Close the drawer
                        scope.launch { drawerState.close() }
                        // Navigate back to the login screen
                        val intent = Intent(context, LoginRegisterActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(intent)
                    }
                    .padding(16.dp),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectCodeScreen(currentUser: FirebaseUser?) {
    // Initial state to handle generated or fetched code
    var connectionCode by remember { mutableStateOf<String?>(null) }
    var enteredCode by remember { mutableStateOf("") }
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    // State to handle polling for connectedTo field
    var isCheckingConnection by remember { mutableStateOf(true) }
    var hasConnected by remember { mutableStateOf(false) }

    // Fetch or generate the connection code
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            val userDocRef = db.collection("users").document(currentUser.email.toString())
            userDocRef.get().addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Check if connectionCode already exists
                    val codeInFirestore = document.getString("connectionCode")
                    if (codeInFirestore.isNullOrEmpty()) {
                        // If no code exists, generate a new one
                        val newCode = (100000..999999).random().toString()
                        userDocRef.update("connectionCode", newCode)
                            .addOnSuccessListener {
                                connectionCode = newCode
                            }
                    } else {
                        // If code exists, display it
                        connectionCode = codeInFirestore
                    }
                }
            }.addOnFailureListener { exception ->
                Log.e("Firebase", "Error fetching connectionCode: ", exception)
            }
        }
    }

    // Periodic check for the "connectedTo" field every 5 seconds
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            val userDocRef = db.collection("users").document(currentUser.email.toString())
            while (isCheckingConnection && !hasConnected) {
                userDocRef.get().addOnSuccessListener { document ->
                    val connectedTo = document.getString("connectedTo")
                    if (!connectedTo.isNullOrEmpty()) {
                        hasConnected = true
                        isCheckingConnection = false
                        // Navigate to MainActivity when connectedTo is updated
                        context.startActivity(Intent(context, MainActivity::class.java))
                    }
                }
                delay(5000) // Check every 5 seconds
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .padding(top = 56.dp), // Leave space at the top
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Display appropriate message based on whether the code is fetched/generated
        Text(
            "This is your code",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Show the generated or fetched connection code
        if (connectionCode != null) {
            Text(
                connectionCode ?: "",
                style = MaterialTheme.typography.displayMedium,
                color = Color.White
            )
        } else {
            CircularProgressIndicator(color = Color.Red) // Show loading while fetching code
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Input field for entering the connection code
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

        Spacer(modifier = Modifier.height(48.dp))

        // Button to submit the entered code
        Button(
            onClick = {
                db.collection("users")
                    .whereEqualTo("connectionCode", enteredCode)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (documents.documents.isEmpty()) {
                            Toast.makeText(
                                context,
                                "User with provided code does not exist",
                                Toast.LENGTH_LONG,
                            ).show()
                        } else {
                            val document = documents.documents[0]
                            db.collection("users").document(document.id)
                                .update("connectedTo", currentUser?.email)
                            db.collection("users").document(currentUser!!.email.toString())
                                .update("connectedTo", document.id)
                            db.collection("users").document(currentUser.email.toString())
                                .update("connectionCode", null)
                            db.collection("users").document(document.id)
                                .update("connectionCode", null)
                            context.startActivity(Intent(context, MainActivity::class.java))
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.w("Firebase", "Error getting documents: ", exception)
                    }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(50))
                .border(2.dp, Color.White, RoundedCornerShape(50))
        ) {
            Text("Connect", color = Color.White)
        }
    }
}
