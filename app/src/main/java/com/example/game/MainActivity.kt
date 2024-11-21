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
import androidx.compose.ui.platform.LocalContext
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
        readDifficulty()
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
                        EndGameScreen(gameViewModel)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        writeDifficulty()
        val fieldFileName = "gameField.txt"
        gameViewModel.writeGameFieldToFile(this@MainActivity, fieldFileName)
    }
    override fun onStop() {
        super.onStop()
        writeDifficulty()
        val fieldFileName = "gameField.txt"
        gameViewModel.writeGameFieldToFile(this@MainActivity, fieldFileName)
    }
    private fun writeDifficulty(){
        val difficultyFileName = "currentDifficulty.txt"
        val dif:Char = when(difficulty){
            Difficulty.EASY -> 'e'
            Difficulty.MEDIUM -> 'm'
            Difficulty.HARD -> 'h'
        }
        val fileWriter = FileWriter()
        fileWriter.writeToFile(this@MainActivity, difficultyFileName, dif.toString())
    }
    private fun readDifficulty(){
        val difficultyFileName = "currentDifficulty.txt"
        val file = File(this@MainActivity.filesDir, difficultyFileName)
        if (!file.exists())
            difficulty = Difficulty.EASY
        else{
            val fileWriter = FileWriter()
            when(fileWriter.readFromFile(this@MainActivity, difficultyFileName)[0]){
                'e' -> difficulty = Difficulty.EASY
                'm' -> difficulty = Difficulty.MEDIUM
                'h' -> difficulty = Difficulty.HARD
            }
        }
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
fun EndGameScreen(gameViewModel: GameViewModel) {
    var playerName by remember { mutableStateOf("") }
    val isGameEnd by gameViewModel.isGameEnd.observeAsState(initial = false)
    val context =  LocalContext.current
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
    ) {
        if (isGameEnd) {
            EndGameDialog(
                showDialog = true,
                onDismiss = { gameViewModel.resetGame() },
                onConfirm = { name ->
                    playerName = name
                    Log.d("GameScreen", "Player Name: $playerName")
                    gameViewModel.score.value?.let {
                        updateRecordsFile(
                           context, playerName ,
                            it
                        )
                    }
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
            gameViewModel = gameViewModel,
            onNewGameClick = { Log.d("GameScreen", "New Game button clicked") }
        )
    }
}

@Composable
fun ActionButtons(gameViewModel: GameViewModel, onNewGameClick: () -> Unit) {
    var showNewGameDialog by remember { mutableStateOf(false) }
    var showRecordsDialog by remember { mutableStateOf(false) }
    var showDifficultyDialog by remember { mutableStateOf(false) }
    var showConfirmDifficultyChangeDialog by remember { mutableStateOf(false) }
    var selectedDifficulty by remember { mutableStateOf(difficulty) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = { showDifficultyDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
        ) {
            Text(text = "Difficulty", color = Color.White)
        }
        Button(
            onClick = { showRecordsDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
        ) {
            Text(text = "Records", color = Color.White)
        }
        Button(
            onClick = { showNewGameDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
        ) {
            Text(text = "New game", color = Color.White)
        }
    }

    ConfirmNewGameDialog(
        showDialog = showNewGameDialog,
        onDismiss = { showNewGameDialog = false },
        onConfirm = {
            gameViewModel.resetGame()
            onNewGameClick()
        }
    )

    RecordsDialog(
        showDialog = showRecordsDialog,
        onDismiss = { showRecordsDialog = false }
    )

    DifficultyDialog(
        showDialog = showDifficultyDialog,
        onDismiss = { showDifficultyDialog = false },
        onConfirm = { newDifficulty ->
            if (difficulty != newDifficulty) {
                selectedDifficulty = newDifficulty
                showConfirmDifficultyChangeDialog = true
            }
        }
    )

    ConfirmDifficultyChangeDialog(
        showDialog = showConfirmDifficultyChangeDialog,
        onDismiss = { showConfirmDifficultyChangeDialog = false },
        onConfirm = {
            difficulty = selectedDifficulty
            gameViewModel.resetGame()
            Log.d("ActionButtons", "Difficulty set to: $difficulty")
        }
    )
}


@Composable
fun ConfirmDifficultyChangeDialog(showDialog: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = "Change Difficulty") },
            text = { Text("Are you sure you want to change the difficulty? This will reset your current progress.") },
            confirmButton = {
                Button(onClick = {
                    onConfirm()
                    onDismiss()
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                Button(onClick = onDismiss) {
                    Text("No")
                }
            }
        )
    }
}

@Composable
fun DifficultyDialog(showDialog: Boolean, onDismiss: () -> Unit, onConfirm: (Difficulty) -> Unit) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = "Select new Difficulty.") },
            text = {
                Column {
                    Button(onClick = { onConfirm(Difficulty.EASY); onDismiss() }) {
                        Text(text = "Easy")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { onConfirm(Difficulty.MEDIUM); onDismiss() }) {
                        Text(text = "Medium")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { onConfirm(Difficulty.HARD); onDismiss() }) {
                        Text(text = "Hard")
                    }
                }
            },
            confirmButton = {
                Button(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}


fun readRecordsFromFile(context: Context, fileName: String): List<Pair<String, Int>> {
    val file = File(context.filesDir, fileName)
    if (!file.exists()) return emptyList()
    val fileWriter = FileWriter()
    return fileWriter.readFromFile(context, fileName).split("\n").filter { it.isNotEmpty() }
        .map { line ->
            val parts = line.split(" - ")
            parts[0] to parts[1].toInt()
        }
}

@Composable
fun RecordsDialog(showDialog: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val records = readRecordsFromFile(context, "records.txt")

    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = "High Scores") },
            text = {
                Column {
                    if (records.isEmpty()) {
                        Text(text = "No records at the moment.")
                    } else {
                        records.forEach { record ->
                            Text(text = "${record.first}: ${record.second}")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = onDismiss) {
                    Text("OK")
                }
            }
        )
    }
}


@Composable
fun ConfirmNewGameDialog(showDialog: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = "New Game") },
            text = { Text("Are you sure you want to start a new game? This will reset your current progress.") },
            confirmButton = {
                Button(onClick = {
                    onConfirm()
                    onDismiss()
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                Button(onClick = onDismiss) {
                    Text("No")
                }
            }
        )
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
fun EndGameDialog(
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




fun updateRecordsFile(context: Context, playerName: String, playerScore: Int) {
    val fileName = "records.txt"
    val file = File(context.filesDir, fileName)
    val fileWriter = FileWriter()
    val records = if (file.exists()) {
        fileWriter.readFromFile(context, fileName).split("\n").filter { it.isNotEmpty() }
            .map { line ->
                val parts = line.split(" - ")
                parts[0] to parts[1].toInt()
            }.toMutableList()
    } else {
        mutableListOf()
    }

    records.add(playerName to playerScore)
    records.sortByDescending { it.second }

    if (records.size > 10) {
        records.removeAt(records.size - 1)
    }

    val updatedRecords = records.joinToString("\n") { "${it.first} - ${it.second}" }
    fileWriter.writeToFile(context, fileName, updatedRecords)
}

