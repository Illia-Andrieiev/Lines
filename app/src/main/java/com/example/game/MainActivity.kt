package com.example.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import android.util.Log
import androidx.compose.ui.Alignment
import com.example.game.ui.theme.GameTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GameTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        GridScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun GridScreen() {
    val size: Int = 9
    val gameField = GameField(size)
    gameField.setPoint(0, 0, 'A')
    gameField.setPoint(1, 1, 'B')
    gameField.setPoint(2, 2, 'C')
    gameField.setPoint(3, 5, 'C')

    LazyVerticalGrid(
        columns = GridCells.Fixed(size),
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        items(gameField.getSize() * gameField.getSize()) { index ->
            val x = index % gameField.getSize()
            val y = index / gameField.getSize()
            val symbol = gameField.getPoint(x, y)
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .padding(0.dp)
                    .background(Color.Gray)
                    .border(1.dp, Color.Black)
                    .clickable {
                        Log.d("GridScreen", "Cell clicked at: x=$x, y=$y")
                    }
            ) {
                Text(
                    text = symbol.toString(),
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GameTheme {
        Greeting("Android")
    }
}

@Preview(showBackground = true)
@Composable
fun GridScreenPreview() {
    GameTheme {
        GridScreen()
    }
}
