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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

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
    private val nextBalls = MutableLiveData(mutableListOf<Ball>())
    private var firstClick: Pair<Int, Int>? = null

    init {
        _gameField.value?.apply {
            setPoint(0, 0, 'c')
            setPoint(1, 1, 'b')
            setPoint(2, 2, 'c')
            setPoint(3, 3, 'b')
            setPoint(4, 4, 'c')
            setPoint(5, 5, 'c')
            setPoint(6, 6, 'c')
            setPoint(6, 7, 'c')
        }
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
                    val isGameEnd: Int = placeRandomBalls(3)
                    Log.d("GridScreen", "$isGameEnd, count of balls: ${countNotNullPoints()}")
                }
            }
            _gameField.value = gameFieldValue.copy()
            firstClick = null
        }
    }
    fun placeRandomBalls(n:Int): Int{
        var i = n
        val gameFieldValue = _gameField.value ?: return -1
        while (i>0 && gameFieldValue.placeRandomBall()){
            i--
        }
        _gameField.value = gameFieldValue.copy()
        if(i != 0){
            return -1
        }
        return 0
    }
    private fun countNotNullPoints():Int{
        var count = 0
        for (i in 0 until (_gameField.value?.getSize() ?: 9)) {
            for (j in 0 until (_gameField.value?.getSize() ?: 9)) {
                if (_gameField.value?.getPoint(i, j) != '0') {
                    count++
                }
            }
        }

        return count
    }
}

@Composable
fun GridScreen(gameViewModel: GameViewModel) {
    val gameField by gameViewModel.gameField.observeAsState(initial = GameField(9))
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
                when (symbol) {
                    'r' -> DrawCircle(radiusCoefficient = radiusCoef, color = Color.Red)
                    'b' -> DrawCircle(radiusCoefficient = radiusCoef, color = Color.Black)
                    'B' -> DrawCircle(radiusCoefficient = radiusCoef, color = Color.Blue)
                    'y' -> DrawCircle(radiusCoefficient = radiusCoef, color = Color.Yellow)
                    'g' -> DrawCircle(radiusCoefficient = radiusCoef, color = Color.Green)
                    'm' -> DrawCircle(radiusCoefficient = radiusCoef, color = Color.Magenta)
                    'c' -> DrawCircle(radiusCoefficient = radiusCoef, color = Color.Cyan)
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

