package com.mjakopec.letswatch


import android.os.Bundle
import android.util.Log
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
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.coroutines.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp

private var json: Json = Json { ignoreUnknownKeys = true }

class BrowseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
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

    LaunchedEffect(Unit) {
        movies = fetchMovies()
        isLoading = false
        // Load additional movies in background
        launch {
            val moreMovies = fetchMovies(page = 2)
            movies += moreMovies
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (movies.isNotEmpty()) {
            MovieCarousel(movies, currentMovieIndex) { newIndex ->
                currentMovieIndex = newIndex  // Update index on swipe
            }
        } else {
            Text("Failed to load movie data", color = Color.White, modifier = Modifier.align(Alignment.Center))
        }
    }
}
@OptIn(ExperimentalWearMaterialApi::class)
@Composable
fun MovieCarousel(movies: List<Movie>, currentIndex: Int, updateIndex: (Int) -> Unit) {
    val swipeableState = rememberSwipeableState(initialValue = currentIndex)
    val scope = rememberCoroutineScope()

    BoxWithConstraints {
        val screenWidth = maxWidth
        val anchors = mapOf(
            0f to currentIndex,  // Current index
            -screenWidth.value to (currentIndex + 1) % movies.size,  // Next movie index (swipe right)
            screenWidth.value to if (currentIndex - 1 < 0) movies.size - 1 else (currentIndex - 1)  // Previous movie index (swipe left)
        )

        Column(modifier = Modifier
            .swipeable(
                state = swipeableState,
                anchors = anchors,
                thresholds = { _, _ -> FractionalThreshold(0.3f) },
                orientation = Orientation.Horizontal
            )
        ) {
            val movie = movies[currentIndex]
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
                    .height(200.dp),  // Adjusted for example
                contentScale = ContentScale.Crop
            )
            MovieDetails(movie)
        }

        LaunchedEffect(swipeableState.currentValue) {
            updateIndex(swipeableState.currentValue)
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
        Text("Title: ${movie.title}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 20.sp)
        Text("Date of Release: ${movie.release_date}", color = Color.White, fontSize = 18.sp)
        Text("Average Vote: ${movie.vote_average}", color = Color.White, fontSize = 18.sp)
        Text("Description: ${movie.overview}", color = Color.White, fontSize = 16.sp)
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