package com.example.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import android.util.Log
import com.example.game.ui.theme.GameTheme
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.unit.times
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


enum class Difficulty{
    EASY,
    MEDIUM,
    HARD
}

// Determines difficulty
var difficulty:Difficulty = Difficulty.MEDIUM


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GameTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        val gameViewModel = GameViewModel()
                        GridScreen(gameViewModel)
                    }
                }
            }
        }
    }
}

class GameViewModel : ViewModel() {
    private val _gameField = MutableLiveData(GameField(9))
    val gameField: LiveData<GameField> = _gameField
    private val _nextBalls = MutableLiveData<MutableList<Ball>>(mutableListOf())
    val nextBalls: LiveData<MutableList<Ball>> = _nextBalls
    val maxNexBallAmount = 3
    private var firstClick: Pair<Int, Int>? = null

    init {
        _gameField.value?.apply {}
    }

    fun onCellClick(x: Int, y: Int) {
        val gameFieldValue = _gameField.value ?: return

        if (firstClick == null) {
            if (gameFieldValue.getPoint(x, y) != '0') {
                firstClick = x to y
            }
        } else {
            val (prevX, prevY) = firstClick!!
            val moveScore = gameFieldValue.moveBall(prevX, prevY, x, y)

            Log.d("GridScreen", "Move Score: $moveScore")
            if (moveScore != -1) {
                if(moveScore == 0){
                    placeNextBalls()
                    Log.d("GridScreen", "count of empty points: ${gameField.value?.getAmountOfEmptyPoints()}")
                }
            }
            _gameField.value = gameFieldValue.copy()
            firstClick = null
        }
        updateNextBalls(maxNexBallAmount)
    }
    fun placeNextBalls(){
        val gameFieldValue = _gameField.value ?: return
        val nextBallsValue = _nextBalls.value ?:return
        for (i in nextBallsValue.size - 1 downTo 0) {
            if(gameFieldValue.getPoint(nextBallsValue[i].x,
                    nextBallsValue[i].y) == '0') {
                gameFieldValue.setPoint(
                    nextBallsValue[i].x,
                    nextBallsValue[i].y,
                    nextBallsValue[i].color
                )

            } else{
                val newBall = gameFieldValue.getRandomBall();
                if(newBall != null){
                    gameFieldValue.setPoint(
                        newBall.x,
                        newBall.y,
                        newBall.color
                    )
                }
            }
            nextBallsValue.removeAt(i)
        }
    }
    fun updateNextBalls(maxBalls:Int){
        val gameFieldValue = _gameField.value ?: return
        val nextBallsValue = _nextBalls.value ?:return
        // Remove balls, if on their place ball already exist
        for (i in nextBallsValue.size - 1 downTo 0) {
            if (gameFieldValue.getPoint(nextBallsValue[i].x, nextBallsValue[i].y) != '0') {
                nextBallsValue.removeAt(i)
            }
        }

        while(gameFieldValue.getAmountOfEmptyPoints() > 0 && nextBallsValue.size < maxBalls) {
            val newBall = gameFieldValue.getRandomBall()
            if (newBall != null) {
                nextBallsValue.add(newBall)
            }
        }
        _nextBalls.value = nextBallsValue.toMutableList()

    }

}

@Composable
fun GridScreen(gameViewModel: GameViewModel) {
    val gameField by gameViewModel.gameField.observeAsState(initial = GameField(9))
    val nextBalls by gameViewModel.nextBalls.observeAsState(initial = mutableListOf<Ball>())
    gameViewModel.updateNextBalls(gameViewModel.maxNexBallAmount)
    gameViewModel.placeNextBalls()
    gameViewModel.updateNextBalls(gameViewModel.maxNexBallAmount)

    val size = 9
    val cellSize = 50.dp
    val radiusCoef = 0.75f

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
                    .size(cellSize)
                    .padding(0.dp)
                    .background(Color.Gray)
                    .border(1.dp, Color.Black)
                    .clickable {
                        gameViewModel.onCellClick(x, y)
                        Log.d("GridScreen", "Cell clicked at: x=$x, y=$y")
                    }
            ) {
                if (symbol != '0') {
                    // Draw the balls of the current field point
                    when (symbol) {
                        'r' -> DrawCircle(radiusCoefficient = radiusCoef, color = Color.Red)
                        'b' -> DrawCircle(radiusCoefficient = radiusCoef, color = Color.Black)
                        'B' -> DrawCircle(radiusCoefficient = radiusCoef, color = Color.Blue)
                        'y' -> DrawCircle(radiusCoefficient = radiusCoef, color = Color.Yellow)
                        'g' -> DrawCircle(radiusCoefficient = radiusCoef, color = Color.Green)
                        'm' -> DrawCircle(radiusCoefficient = radiusCoef, color = Color.Magenta)
                        'c' -> DrawCircle(radiusCoefficient = radiusCoef, color = Color.Cyan)
                    }
                } else {
                    // If difficulty is hard, do not show future balls
                    if(difficulty != Difficulty.HARD){
                        // check whether there is a ball in nextBalls for a given cell
                        nextBalls.find { it.x == x && it.y == y }?.let { ball ->
                            var color:Color = Color.Black
                            // if difficulty is easy, show and color and position of future balls. if medium - only position
                            if (difficulty == Difficulty.EASY){
                                when(ball.color){
                                    'r' -> color = Color.Red
                                    'b' -> color = Color.Black
                                    'B' -> color = Color.Blue
                                    'y' -> color = Color.Yellow
                                    'g' -> color = Color.Green
                                    'm' -> color = Color.Magenta
                                    'c' -> color = Color.Cyan

                                }
                            }
                            DrawCircle(radiusCoefficient = radiusCoef/2.5f, color = color)
                        }
                    }
                }
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
