package com.mjakopec.letswatch

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import okhttp3.OkHttpClient
import okhttp3.Request
import coil.compose.rememberImagePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import androidx.compose.ui.text.font.FontWeight
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.FractionalThreshold
import androidx.wear.compose.material.rememberSwipeableState
import androidx.wear.compose.material.swipeable
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.coroutines.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import androidx.compose.ui.platform.LocalConfiguration
import kotlinx.coroutines.tasks.await

var json: Json = Json { ignoreUnknownKeys = true }
private lateinit var db: FirebaseFirestore

class BrowseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        db = Firebase.firestore
        super.onCreate(savedInstanceState)
        setContent {
            BrowserScreen()
        }
    }
}

@OptIn(ExperimentalWearMaterialApi::class)
@Composable
fun BrowserScreen() {
    var movies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var currentMovieIndex by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var likedMoviesIds by remember { mutableStateOf<List<Int>>(emptyList()) }
    var dislikedMoviesIds by remember { mutableStateOf<List<Int>>(emptyList()) }

    LaunchedEffect(Unit) {
        val user = Firebase.auth.currentUser
        if (user != null) {
            val userData = db.collection("users").document(user.email.toString())

            try {
                val document = userData.get().await()
                if (document != null && document.exists()) {
                    likedMoviesIds = document.get("likedMovies") as? List<Int> ?: emptyList()
                    dislikedMoviesIds = document.get("dislikedMovies") as? List<Int> ?: emptyList()
                    Log.d("liked", likedMoviesIds.toString())
                    Log.d("disliked", dislikedMoviesIds.toString())
                }
            } catch (e: Exception) {
                Log.w("Firebase", "Error fetching liked/disliked movies", e)
            }
        }

        var page = 1
        val maxPages = 20
        var fetchedMovies: List<Movie>

        do {
            val newMovies = fetchMovies(page)
            val filteredMovies = mutableListOf<Movie>()
            for (movie in newMovies) {
                if (!likedMoviesIds.contains(movie.id) && !dislikedMoviesIds.contains(movie.id)) {
                    filteredMovies.add(movie)
                }
            }
            fetchedMovies = filteredMovies

            movies += fetchedMovies
            page++
        } while (fetchedMovies.isNotEmpty() && page <= maxPages)

        isLoading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.Red
            )
        } else if (movies.isNotEmpty()) {
            MovieCarousel(movies, currentMovieIndex) { newIndex ->
                currentMovieIndex = newIndex
            }
        } else {
            Text(
                "No more movies to browse",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

suspend fun fetchLikedAndDislikedMovies(): Pair<List<Int>, List<Int>> = withContext(Dispatchers.IO) {
    val user = Firebase.auth.currentUser
    val userData = db.collection("users").document(user?.email.toString())
    val likedMovies = mutableListOf<Int>()
    val dislikedMovies = mutableListOf<Int>()

    try {
        val documentSnapshot = userData.get().await()
        likedMovies.addAll(documentSnapshot.get("likedMovies") as? List<Int> ?: emptyList())
        dislikedMovies.addAll(documentSnapshot.get("dislikedMovies") as? List<Int> ?: emptyList())
    } catch (e: Exception) {
        Log.e("Firebase", "Error fetching liked/disliked movies", e)
    }

    return@withContext Pair(likedMovies, dislikedMovies)
}

@OptIn(ExperimentalWearMaterialApi::class)
@Composable
fun MovieCarousel(movies: List<Movie>, currentIndex: Int, updateIndex: (Int) -> Unit) {
    val context = LocalContext.current
    var currentMovieIndex by remember { mutableStateOf(currentIndex) }
    val swipeableState = rememberSwipeableState(initialValue = 0)
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val screenWidth = with(LocalDensity.current) { configuration.screenWidthDp.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .swipeable(
                state = swipeableState,
                anchors = mapOf(
                    -screenWidth to -1,
                    0f to 0,
                    screenWidth to 1
                ),
                thresholds = { _, _ -> FractionalThreshold(0.3f) },
                orientation = Orientation.Horizontal
            )
    ) {
        val movie = movies[currentMovieIndex]

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            Image(
                painter = rememberImagePainter(
                    "https://image.tmdb.org/t/p/original${movie.backdrop_path}",
                    builder = {
                        crossfade(true)
                    }
                ),
                contentDescription = "Movie Backdrop",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(20.dp))
            MovieDetails(movie)
        }

        LaunchedEffect(swipeableState.currentValue) {
            val movie = movies[currentMovieIndex]
            if (swipeableState.currentValue != 0) {
                val direction = swipeableState.currentValue
                currentMovieIndex = (currentMovieIndex + 1) % movies.size

                if (direction == 1) {
                    addToFirebase(movie, liked = false, context)
                } else if (direction == -1) {
                    addToFirebase(movie, liked = true, context)
                }

                swipeableState.snapTo(0)
                updateIndex(currentMovieIndex)
            }
        }
    }
}

fun addToFirebase(movie: Movie, liked: Boolean, context: Context) {
    val user = Firebase.auth.currentUser
    val userData = db.collection("users").document(user?.email.toString())

    if (liked) {
        Toast.makeText(
            context,
            "Movie liked",
            Toast.LENGTH_SHORT,
        ).show()

        userData.update("likedMovies", FieldValue.arrayUnion(movie.id))
            .addOnSuccessListener {
                Log.d("Firebase", "Movie added to likedMovies successfully.")
            }
            .addOnFailureListener { e ->
                Log.w("Firebase", "Error adding movie to likedMovies", e)
            }

    } else {
        Toast.makeText(
            context,
            "Movie disliked",
            Toast.LENGTH_SHORT,
        ).show()

        userData.update("dislikedMovies", FieldValue.arrayUnion(movie.id))
            .addOnSuccessListener {
                Log.d("Firebase", "Movie added to dislikedMovies successfully.")
            }
            .addOnFailureListener { e ->
                Log.w("Firebase", "Error adding movie to dislikedMovies", e)
            }
    }
}

@Composable
fun MovieDetails(movie: Movie) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Row {
            Text(
                text = "Title: ",
                color = Color.Gray,
                fontSize = 24.sp
            )
            Text(
                text = movie.title,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 24.sp
            )
        }
        Spacer(Modifier.height(10.dp))

        Row {
            Text(
                text = "Date of Release: ",
                color = Color.Gray,
                fontSize = 18.sp
            )
            Text(
                text = movie.release_date,
                color = Color.White,
                fontSize = 18.sp
            )
        }
        Spacer(Modifier.height(10.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Average Vote: ",
                color = Color.Gray,
                fontSize = 18.sp
            )
            Text(
                text = "${movie.vote_average}",
                color = Color.White,
                fontSize = 18.sp
            )
            Text(
                text = " ★",
                color = Color.White,
                fontSize = 18.sp
            )
        }
        Spacer(Modifier.height(10.dp))

        Text(
            text = "Description:",
            color = Color.Gray,
            fontSize = 16.sp
        )
        Text(
            text = movie.overview,
            color = Color.White,
            fontSize = 16.sp
        )
    }
}

@OptIn(ExperimentalSerializationApi::class)
suspend fun fetchMovies(page: Int = 1): List<Movie> {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("https://api.themoviedb.org/3/discover/movie?include_adult=false&include_video=false&language=en-US&page=$page&sort_by=popularity.desc")
        .get()
        .addHeader("accept", "application/json")
        .addHeader("Authorization", "Bearer ${API.globalKey}")
        .build()

    return withContext(Dispatchers.IO) {
        client.newCall(request).execute().use { response ->
            val responseString = response.body?.string()
            responseString?.let {
                json.decodeFromString<MovieApiResponse>(it).results
            } ?: emptyList()
        }
    }
}
