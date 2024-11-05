package one.beyond.android.test

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import one.beyond.android.test.ui.theme.AndroidTestTheme
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.Reader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MainActivity : ComponentActivity() {
    private val pokemons: MutableLiveData<List<Pokemon>> = MutableLiveData()
    private val isLoading: MutableLiveData<Boolean> = MutableLiveData()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Content(pokemons, isLoading)
        }
        loadCharacters()
    }


    private fun loadCharacters() {
        isLoading.value = true
        CoroutineScope(Dispatchers.IO).launch {
            val response = makeRequest()
            withContext(Dispatchers.Main) {
                isLoading.value = false
                try {
                    Moshi.Builder().build().adapter(PokemonResponseModel::class.java)
                        .fromJson(response)?.let { data ->
                            pokemons.value = data.results
                        }
                } catch (e: Exception) {
                    showError()
                }
            }
        }
    }

    private fun showError() {
        AlertDialog.Builder(this).setTitle("Error").setMessage("Something went wrong")
            .setPositiveButton("Ok") { _, _ -> }
    }

    private suspend fun makeRequest(): String {
        return suspendCancellableCoroutine { continuation ->
            try {
                val reader: BufferedReader
                val url = URL("https://pokeapi.co/api/v2/pokemon/?offset=0&limit=50")

                with(url.openConnection() as HttpURLConnection) {
                    requestMethod = "GET"
                    reader = BufferedReader(InputStreamReader(inputStream) as Reader?)

                    val response = StringBuffer()
                    var inputLine = reader.readLine()
                    while (inputLine != null) {
                        response.append(inputLine)
                        inputLine = reader.readLine()
                    }
                    reader.close()

                    if (continuation.isActive) {
                        continuation.resume(response.toString())
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }
        }
    }
}


@Composable
private fun Content(
    pokemons: MutableLiveData<List<Pokemon>>,
    isLoading: MutableLiveData<Boolean>
) {
    AndroidTestTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
            val pokemonData by pokemons.observeAsState()
            val loading by isLoading.observeAsState(true)
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Column(
                    Modifier.verticalScroll(rememberScrollState())
                ) {
                    pokemonData?.forEach {
                        Text(
                            text = it.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
                if (loading == true) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ContentPreview() {
    AndroidTestTheme {
        Content(
            MutableLiveData(),
            MutableLiveData(true)
        )
    }
}