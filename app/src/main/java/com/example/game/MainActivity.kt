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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import android.util.Log
import androidx.compose.ui.Alignment
import com.example.game.ui.theme.GameTheme
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize

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
    val size: Int = 6
    val gameField = GameField(size)
    gameField.setPoint(0, 0, 'A')
    gameField.setPoint(1, 1, 'B')
    gameField.setPoint(2, 2, 'C')
    gameField.setPoint(3, 5, 'D')

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
                if (symbol != '0') {
                    DrawCircle(radiusCoefficient = 0.8f, color = Color.Blue)
                }
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
fun DrawCircle(radiusCoefficient: Float, color: Color) {
    var adjustedRadiusCoefficient = radiusCoefficient
    if (adjustedRadiusCoefficient > 1) {
        adjustedRadiusCoefficient = 1f
    }
    if (adjustedRadiusCoefficient < 0) {
        adjustedRadiusCoefficient = 0f
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            color = color,
            radius = size.minDimension / 2 * adjustedRadiusCoefficient,
            center = center
        )
    }
}
