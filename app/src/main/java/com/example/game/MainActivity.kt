package com.example.game

import android.content.Context
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.File


enum class Difficulty{
    EASY,
    MEDIUM,
    HARD
}

// Determines difficulty
var difficulty:Difficulty = Difficulty.EASY

class MainActivity : ComponentActivity() {
    private lateinit var gameViewModel: GameViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        gameViewModel = GameViewModel()
        val fieldFileName = "gameField.txt"
        val file = File(filesDir, fieldFileName)
        if (file.exists()) {
            gameViewModel.initGameFieldFromFile(this@MainActivity, fieldFileName)
        }

        setContent {
            GameTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        MessageAndGrid(gameViewModel)
                        GameScreen(gameViewModel)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val fieldFileName = "gameField.txt"
        gameViewModel.writeGameFieldToFile(this@MainActivity, fieldFileName)
    }
    override fun onStop() {
        super.onStop()
        val fieldFileName = "gameField.txt"
        gameViewModel.writeGameFieldToFile(this@MainActivity, fieldFileName)
    }
}



class GameViewModel : ViewModel() {
    private var _isGameEnd = MutableLiveData(false)
    var isGameEnd:LiveData<Boolean> = _isGameEnd
    private val _gameField = MutableLiveData(GameField(9))
    val gameField: LiveData<GameField> = _gameField
    private val _nextBalls = MutableLiveData<MutableList<Ball>>(mutableListOf())
    val nextBalls: LiveData<MutableList<Ball>> = _nextBalls
    private val _score = MutableLiveData(0)
    val score: LiveData<Int> = _score
    val maxNexBallAmount = 3

    private var firstClick: Pair<Int, Int>? = null
    init {
        _gameField.value?.apply {}
    }

    fun initGameFieldFromFile(context: Context, fileName: String){
        val gameFieldValue = _gameField.value ?: return
        gameFieldValue.readFromFile(context,fileName)
        _gameField.value = gameFieldValue.copy()
        _score.value = gameFieldValue.score
    }
    fun writeGameFieldToFile(context: Context, fileName: String){
        _gameField.value?.score = _score.value!!
        Log.d("last score", "${_score.value}")
        _gameField.value?.writeToFile(context,fileName)
    }
    fun addScore(score:Int){
        val curScore = _score.value
        if (curScore != null){
            _score.value = curScore + score
        }
    }
    fun resetGame(){
        _isGameEnd.value = false
        _gameField.value?.clear()
        _nextBalls.value?.clear()
        updateNextBalls(maxNexBallAmount)
        addScore(placeNextBalls())
        updateNextBalls(maxNexBallAmount)
        _score.value = 0
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
                if (moveScore == 0) {
                    addScore(placeNextBalls())
                    Log.d("GridScreen", "count of empty points: ${gameField.value?.getAmountOfEmptyPoints()}")
                }
                val curScore = _score.value
                if(curScore != null)
                    _score.value = curScore + moveScore
                Log.d("cur score", "${_score.value}")
            }
            _gameField.value = gameFieldValue.copy()
            firstClick = null
        }
        updateNextBalls(maxNexBallAmount)
    }
    fun placeNextBalls():Int {
        var computerScore = 0
        val gameFieldValue = _gameField.value ?: return 0
        val nextBallsValue = _nextBalls.value ?: return 0
        for (i in nextBallsValue.size - 1 downTo 0) {
            if (gameFieldValue.getPoint(nextBallsValue[i].x, nextBallsValue[i].y) == '0') {
                val addScore = gameFieldValue.setPointAndCheckScore(nextBallsValue[i].x,
                    nextBallsValue[i].y, nextBallsValue[i].color)
                if(addScore > 0)
                    computerScore += addScore
            } else {
                val newBall = gameFieldValue.getRandomBall()
                if (newBall != null) {
                    val addScore = gameFieldValue.setPointAndCheckScore(newBall.x,
                        newBall.y, newBall.color)
                    if(addScore > 0)
                        computerScore += addScore                }
            }
            nextBallsValue.removeAt(i)
        }
        _gameField.value = gameFieldValue.copy()
        Log.d("ttt", "${gameFieldValue.getAmountOfEmptyPoints()}")
        if (gameFieldValue.getAmountOfEmptyPoints() == 0) {
            _isGameEnd.value = true
        }
        Log.d("ttt", "$_isGameEnd")

        return computerScore
    }

    fun updateNextBalls(maxBalls: Int) {
        val gameFieldValue = _gameField.value ?: return
        val nextBallsValue = _nextBalls.value ?: return
        // Remove balls, if on their place ball already exist
        for (i in nextBallsValue.size - 1 downTo 0) {
            if (gameFieldValue.getPoint(nextBallsValue[i].x, nextBallsValue[i].y) != '0') {
                nextBallsValue.removeAt(i)
            }
        }

        while (gameFieldValue.getAmountOfEmptyPoints() > 0 && nextBallsValue.size < maxBalls) {
            val newBall = gameFieldValue.getRandomBall()
            if (newBall != null) {
                nextBallsValue.add(newBall)
            }
        }
        _nextBalls.value = nextBallsValue.toMutableList()
    }

}

@Composable
fun GameScreen(gameViewModel: GameViewModel) {
    var playerName by remember { mutableStateOf("") }
    val isGameEnd by gameViewModel.isGameEnd.observeAsState(initial = false)

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
    ) {
        if (isGameEnd) {
            SimpleTextInputDialog(
                showDialog = true,
                onDismiss = { gameViewModel.resetGame() },
                onConfirm = { name ->
                    playerName = name
                    Log.d("GameScreen", "Player Name: $playerName")
                    gameViewModel.resetGame()
                }
            )
        }

        MessageAndGrid(gameViewModel)
    }
}

@Composable
fun MessageAndGrid(gameViewModel: GameViewModel) {
    val score by gameViewModel.score.observeAsState(initial = 0)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Current Score: $score",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        GridScreen(gameViewModel)
        Spacer(modifier = Modifier.height(16.dp))
        ActionButtons(
            onDifficultyClick = { Log.d("GameScreen", "Difficulty button clicked") },
            onRecordsClick = { Log.d("GameScreen", "Records button clicked") },
            onNewGameClick = { Log.d("GameScreen", "New Game button clicked") }
        )
    }
}

@Composable
fun ActionButtons(onDifficultyClick: () -> Unit, onRecordsClick: () -> Unit, onNewGameClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onDifficultyClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black) // Явный цвет фона кнопки
        ) {
            Text(text = "Difficulty", color = Color.White) // Явный цвет текста
        }
        Button(
            onClick = onRecordsClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
        ) {
            Text(text = "Records", color = Color.White)
        }
        Button(
            onClick = onNewGameClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
        ) {
            Text(text = "New game", color = Color.White)
        }
    }
}

@Composable
fun GridScreen(gameViewModel: GameViewModel) {
    val gameField by gameViewModel.gameField.observeAsState(initial = GameField(9))
    val nextBalls by gameViewModel.nextBalls.observeAsState(initial = mutableListOf())

    gameViewModel.updateNextBalls(gameViewModel.maxNexBallAmount)
    if(gameField.getAmountOfEmptyPoints() ==
        gameField.getSize()*gameField.getSize()) {

        gameViewModel.addScore(gameViewModel.placeNextBalls())
        gameViewModel.updateNextBalls(gameViewModel.maxNexBallAmount)
    }
    val size = 9
    val cellSize = 50.dp
    val radiusCoefficient = 0.75f

    LazyVerticalGrid(
        columns = GridCells.Fixed(size),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 500.dp)
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
                        'r' -> DrawCircle(radiusCoefficient = radiusCoefficient, color = Color.Red)
                        'b' -> DrawCircle(radiusCoefficient = radiusCoefficient, color = Color.Black)
                        'B' -> DrawCircle(radiusCoefficient = radiusCoefficient, color = Color.Blue)
                        'y' -> DrawCircle(radiusCoefficient = radiusCoefficient, color = Color.Yellow)
                        'g' -> DrawCircle(radiusCoefficient = radiusCoefficient, color = Color.Green)
                        'm' -> DrawCircle(radiusCoefficient = radiusCoefficient, color = Color.Magenta)
                        'c' -> DrawCircle(radiusCoefficient = radiusCoefficient, color = Color.Cyan)
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
                            DrawCircle(radiusCoefficient = radiusCoefficient/2.5f, color = color)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleTextInputDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    if (showDialog) {
        var text by remember { mutableStateOf("") }
        var showError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = "Enter your name") },
            text = {
                Column {
                    Text(text = "Please enter your name below:")
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text(text = "Name") }
                    )
                    if (showError) {
                        Text(text = "Name cannot be empty", color = Color.Red)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (text.isNotBlank()) {
                        onConfirm(text)
                        onDismiss()
                    } else {
                        showError = true
                    }
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                Button(onClick = { onDismiss() }) {
                    Text("Cancel")
                }
            }
        )
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






