package com.example.game

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.example.game.ui.theme.GameTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.File
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size



enum class Difficulty {
    EASY,
    MEDIUM,
    HARD
}

// Determines current difficulty level
var difficulty: Difficulty = Difficulty.EASY

class MainActivity : ComponentActivity() {
    private lateinit var gameViewModel: GameViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Read the saved difficulty level from the file
        readDifficulty()

        // Initialize the GameViewModel
        gameViewModel = GameViewModel()

        val fieldFileName = "gameField.txt"
        val file = File(filesDir, fieldFileName)

        // If the game field file exists, initialize the game state from the file
        if (file.exists()) {
            gameViewModel.initGameFieldFromFile(this@MainActivity, fieldFileName)
        }

        // Set the content view using Jetpack Compose
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

        // Save the current difficulty level to the file
        writeDifficulty()

        // Save the current game state to the file
        val fieldFileName = "gameField.txt"
        gameViewModel.writeGameFieldToFile(this@MainActivity, fieldFileName)
    }

    override fun onStop() {
        super.onStop()

        // Save the current difficulty level to the file
        writeDifficulty()

        // Save the current game state to the file
        val fieldFileName = "gameField.txt"
        gameViewModel.writeGameFieldToFile(this@MainActivity, fieldFileName)
    }

    private fun writeDifficulty() {
        val difficultyFileName = "currentDifficulty.txt"

        // Convert the current difficulty level to a single character representation
        val dif: Char = when (difficulty) {
            Difficulty.EASY -> 'e'
            Difficulty.MEDIUM -> 'm'
            Difficulty.HARD -> 'h'
        }

        val fileWriter = FileWriter()

        // Write the difficulty level to the file
        fileWriter.writeToFile(this@MainActivity, difficultyFileName, dif.toString())
    }

    private fun readDifficulty() {
        val difficultyFileName = "currentDifficulty.txt"
        val file = File(this@MainActivity.filesDir, difficultyFileName)

        // If the file does not exist, set the difficulty to EASY
        if (!file.exists()) {
            difficulty = Difficulty.EASY
        } else {
            val fileWriter = FileWriter()

            // Read the difficulty level from the file and set it accordingly
            when (fileWriter.readFromFile(this@MainActivity, difficultyFileName)[0]) {
                'e' -> difficulty = Difficulty.EASY
                'm' -> difficulty = Difficulty.MEDIUM
                'h' -> difficulty = Difficulty.HARD
            }
        }
    }
}



class GameViewModel : ViewModel() {

    // LiveData to observe the end of the game
    private var _isGameEnd = MutableLiveData(false)
    var isGameEnd: LiveData<Boolean> = _isGameEnd

    // LiveData to observe the game field
    private val _gameField = MutableLiveData(GameField(9))
    val gameField: LiveData<GameField> = _gameField

    // LiveData to observe the next balls to be placed
    private val _nextBalls = MutableLiveData<MutableList<Ball>>(mutableListOf())
    val nextBalls: LiveData<MutableList<Ball>> = _nextBalls

    // LiveData to observe the current score
    private val _score = MutableLiveData(0)
    val score: LiveData<Int> = _score

    // Maximum number of next balls to be displayed
    val maxNexBallAmount = 3

    // Stores the coordinates of the first click in a move
    private var firstClick: Pair<Int, Int>? = null

    init {
        // Optional initialization logic
        _gameField.value?.apply {}
    }

    // Initialize the game field from a file
    fun initGameFieldFromFile(context: Context, fileName: String) {
        val gameFieldValue = _gameField.value ?: return
        gameFieldValue.readFromFile(context, fileName)
        _gameField.value = gameFieldValue.copy()
        _score.value = gameFieldValue.score
    }

    // Write the current game field to a file
    fun writeGameFieldToFile(context: Context, fileName: String) {
        _gameField.value?.score = _score.value!!
        Log.d("last score", "${_score.value}")
        _gameField.value?.writeToFile(context, fileName)
    }

    // Add to the current score
    fun addScore(score: Int) {
        val curScore = _score.value
        if (curScore != null) {
            _score.value = curScore + score
        }
    }

    // Reset the game to its initial state
    fun resetGame() {
        _isGameEnd.value = false
        _gameField.value?.clear()
        _nextBalls.value?.clear()
        updateNextBalls(maxNexBallAmount)
        addScore(placeNextBalls())
        updateNextBalls(maxNexBallAmount)
        _score.value = 0
    }

    // Handle a cell click event
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
                // If do not built line, place new balls
                if (moveScore == 0) {
                    addScore(placeNextBalls())
                    Log.d("GridScreen", "count of empty points: ${gameField.value?.getAmountOfEmptyPoints()}")
                }
                val curScore = _score.value
                if (curScore != null) {
                    _score.value = curScore + moveScore
                }
                Log.d("cur score", "${_score.value}")
            }
            _gameField.value = gameFieldValue.copy()
            firstClick = null
        }
        updateNextBalls(maxNexBallAmount)
    }

    // Place the next set of balls on the game field
    fun placeNextBalls(): Int {
        var computerScore = 0
        val gameFieldValue = _gameField.value ?: return 0
        val nextBallsValue = _nextBalls.value ?: return 0

        // Place each ball in its position or find a new position if occupied
        for (i in nextBallsValue.size - 1 downTo 0) {
            if (gameFieldValue.getPoint(nextBallsValue[i].x, nextBallsValue[i].y) == '0') {
                val addScore = gameFieldValue.setPointAndCheckScore(nextBallsValue[i].x,
                    nextBallsValue[i].y, nextBallsValue[i].color)
                if (addScore > 0) {
                    computerScore += addScore
                }
            } else {
                val newBall = gameFieldValue.getRandomBall()
                if (newBall != null) {
                    val addScore = gameFieldValue.setPointAndCheckScore(newBall.x,
                        newBall.y, newBall.color)
                    if (addScore > 0) {
                        computerScore += addScore
                    }
                }
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

    // Update the list of next balls to be placed
    fun updateNextBalls(maxBalls: Int) {
        val gameFieldValue = _gameField.value ?: return
        val nextBallsValue = _nextBalls.value ?: return

        // Remove balls if there is already a ball at their position
        for (i in nextBallsValue.size - 1 downTo 0) {
            if (gameFieldValue.getPoint(nextBallsValue[i].x, nextBallsValue[i].y) != '0') {
                nextBallsValue.removeAt(i)
            }
        }

        // Add new random balls until the list is full
        while (gameFieldValue.getAmountOfEmptyPoints() > 0 && nextBallsValue.size < maxBalls) {
            val newBall = gameFieldValue.getRandomBall()
            if (newBall != null) {
                nextBallsValue.add(newBall)
            }
        }
        _nextBalls.value = nextBallsValue.toMutableList()
    }
}


/*
* UI
* */

@Composable
fun EndGameScreen(gameViewModel: GameViewModel) {
    // Mutable state to store the player's name
    var playerName by remember { mutableStateOf("") }
    // Observe the game end state from the ViewModel
    val isGameEnd by gameViewModel.isGameEnd.observeAsState(initial = false)
    // Get the current context
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // If the game has ended, show the end game dialog
        if (isGameEnd) {
            EndGameDialog(
                showDialog = true,
                onDismiss = { gameViewModel.resetGame() },
                onConfirm = { name ->
                    playerName = name
                    Log.d("GameScreen", "Player Name: $playerName")
                    gameViewModel.score.value?.let {
                        // Update the records file with the player's score
                        updateRecordsFile(context, playerName, it)
                    }
                    // Reset the game after updating the records
                    gameViewModel.resetGame()
                }
            )
        }
        // Display the main game interface
        MessageAndGrid(gameViewModel)
    }
}

@Composable
fun MessageAndGrid(gameViewModel: GameViewModel) {
    // Observe the current score from the ViewModel
    val score by gameViewModel.score.observeAsState(initial = 0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Display the current score
        Text(
            text = "Current Score: $score",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Display the game grid
        GridScreen(gameViewModel)

        // Spacer to add some space between the grid and buttons
        Spacer(modifier = Modifier.height(16.dp))

        // Display action buttons
        ActionButtons(
            gameViewModel = gameViewModel,
            onNewGameClick = { Log.d("GameScreen", "New Game button clicked") }
        )
    }
}

@Composable
fun ActionButtons(gameViewModel: GameViewModel, onNewGameClick: () -> Unit) {
    // States to manage the visibility of dialogs
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
        // Button to change the difficulty
        Button(
            onClick = { showDifficultyDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
        ) {
            Text(text = "Difficulty", color = Color.White)
        }

        // Button to view the records
        Button(
            onClick = { showRecordsDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
        ) {
            Text(text = "Records", color = Color.White)
        }

        // Button to start a new game
        Button(
            onClick = { showNewGameDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
        ) {
            Text(text = "New game", color = Color.White)
        }
    }

    // Dialog to confirm starting a new game
    ConfirmNewGameDialog(
        showDialog = showNewGameDialog,
        onDismiss = { showNewGameDialog = false },
        onConfirm = {
            gameViewModel.resetGame()
            onNewGameClick()
        }
    )

    // Dialog to view the records
    RecordsDialog(
        showDialog = showRecordsDialog,
        onDismiss = { showRecordsDialog = false }
    )

    // Dialog to change the difficulty
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

    // Dialog to confirm changing the difficulty
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
                    // Button to select Easy difficulty
                    Button(onClick = { onConfirm(Difficulty.EASY); onDismiss() }) {
                        Text(text = "Easy")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Button to select Medium difficulty
                    Button(onClick = { onConfirm(Difficulty.MEDIUM); onDismiss() }) {
                        Text(text = "Medium")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Button to select Hard difficulty
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

    // Read the file contents and split by new lines
    return fileWriter.readFromFile(context, fileName).split("\n").filter { it.isNotEmpty() }
        .map { line ->
            // Split each line by " - " to separate player name and score, and convert to a pair
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

    // Initialize next balls if the game just started
    gameViewModel.updateNextBalls(gameViewModel.maxNexBallAmount)
    if (gameField.getAmountOfEmptyPoints() ==
        gameField.getSize() * gameField.getSize()) {

        // Place the first set of balls and update the game state
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
                        // Handle cell click
                        gameViewModel.onCellClick(x, y)
                        Log.d("GridScreen", "Cell clicked at: x=$x, y=$y")
                    }
            ) {
                if (symbol != '0') {
                    // Draw the ball with the corresponding color
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
                    // If difficulty is not hard, show future balls
                    if (difficulty != Difficulty.HARD) {
                        // Check if there is a ball in nextBalls for the given cell
                        nextBalls.find { it.x == x && it.y == y }?.let { ball ->
                            var color: Color = Color.Black
                            // If difficulty is easy, show the color and position of future balls, if medium - only position
                            if (difficulty == Difficulty.EASY) {
                                when (ball.color) {
                                    'r' -> color = Color.Red
                                    'b' -> color = Color.Black
                                    'B' -> color = Color.Blue
                                    'y' -> color = Color.Yellow
                                    'g' -> color = Color.Green
                                    'm' -> color = Color.Magenta
                                    'c' -> color = Color.Cyan
                                }
                            }
                            DrawCircle(radiusCoefficient = radiusCoefficient / 2.5f, color = color)
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
    // Ensure radius coefficient is between 0 and 1
    if (adjustedRadiusCoefficient > 1) {
        adjustedRadiusCoefficient = 1f
    }
    if (adjustedRadiusCoefficient < 0) {
        adjustedRadiusCoefficient = 0f
    }

    // Draw a circle with the given color and adjusted radius
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
                // Split each line to separate player name and score, and convert to a pair
                val parts = line.split(" - ")
                parts[0] to parts[1].toInt()
            }.toMutableList()
    } else {
        mutableListOf()
    }

    // Add the new player's score to the list
    records.add(playerName to playerScore)
    // Sort records in descending order of scores
    records.sortByDescending { it.second }

    // If there are more than 10 records, remove the last one
    if (records.size > 10) {
        records.removeAt(records.size - 1)
    }

    // Convert the records to a string for saving
    val updatedRecords = records.joinToString("\n") { "${it.first} - ${it.second}" }
    // Write the updated records to the file
    fileWriter.writeToFile(context, fileName, updatedRecords)
}


