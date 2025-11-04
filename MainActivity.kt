package com.example.chess

import android.R
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.tooling.preview.Preview
import com.example.chess.ui.theme.ChessTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.Piece
import com.example.chess.PieceType
import com.example.chess.PieceColor
import com.example.chess.initialBoard
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.*
import androidx.compose.foundation.clickable
import kotlinx.coroutines.delay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*

val initialBoard = listOf(
    Piece(PieceType.ROOK, PieceColor.WHITE, 0 to 0),
    Piece(PieceType.KNIGHT, PieceColor.WHITE, 0 to 1),
    Piece(PieceType.BISHOP, PieceColor.WHITE, 0 to 2),
    Piece(PieceType.QUEEN, PieceColor.WHITE, 0 to 3),
    Piece(PieceType.KING, PieceColor.WHITE, 0 to 4),
    Piece(PieceType.BISHOP, PieceColor.WHITE, 0 to 5),
    Piece(PieceType.KNIGHT, PieceColor.WHITE, 0 to 6),
    Piece(PieceType.ROOK, PieceColor.WHITE, 0 to 7),
    *List(8) { col -> Piece(PieceType.PAWN, PieceColor.WHITE, 1 to col) }.toTypedArray(),
    *List(8) { col -> Piece(PieceType.PAWN, PieceColor.BLACK, 6 to col) }.toTypedArray(),
    Piece(PieceType.ROOK, PieceColor.BLACK, 7 to 0),
    Piece(PieceType.KNIGHT, PieceColor.BLACK, 7 to 1),
    Piece(PieceType.BISHOP, PieceColor.BLACK, 7 to 2),
    Piece(PieceType.QUEEN, PieceColor.BLACK, 7 to 3),
    Piece(PieceType.KING, PieceColor.BLACK, 7 to 4),
    Piece(PieceType.BISHOP, PieceColor.BLACK, 7 to 5),
    Piece(PieceType.KNIGHT, PieceColor.BLACK, 7 to 6),
    Piece(PieceType.ROOK, PieceColor.BLACK, 7 to 7)
)

data class BotMove(val piece: Piece, val target: Pair<Int, Int>)

fun getPieceSymbol(piece: Piece): String {
    return when (piece.type) {
        PieceType.KING -> if (piece.color == PieceColor.WHITE) "♚" else "♚"
        PieceType.QUEEN -> if (piece.color == PieceColor.WHITE) "♛" else "♛"
        PieceType.ROOK -> if (piece.color == PieceColor.WHITE) "♜" else "♜"
        PieceType.BISHOP -> if (piece.color == PieceColor.WHITE) "♝" else "♝"
        PieceType.KNIGHT -> if (piece.color == PieceColor.WHITE) "♞" else "♞"
        PieceType.PAWN -> if (piece.color == PieceColor.WHITE) "♟" else "♟"
    }
}

enum class PieceType { KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN }
enum class PieceColor { WHITE, BLACK }

data class Piece(
    val type: PieceType,
    val color: PieceColor,
    val position: Pair<Int, Int>
)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF1E1E1E)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Chessboard()
                }
            }
        }
    }
}

fun isValidMove(piece: Piece, targetRow: Int, targetCol: Int, currentPieces: List<Piece>): Boolean {
    val (currentRow, currentCol) = piece.position

    return when (piece.type) {
        PieceType.PAWN -> {
            val direction = if (piece.color == PieceColor.WHITE) 1 else -1
            val startRow = if (piece.color == PieceColor.WHITE) 1 else 6

            val targetOccupied = currentPieces.any { it.position == Pair(targetRow, targetCol) }

            // Move forward by 1
            if (targetCol == currentCol && targetRow == currentRow + direction && !targetOccupied) true
            // Move forward by 2 from starting row
            else if (targetCol == currentCol && currentRow == startRow && targetRow == currentRow + 2 * direction && !targetOccupied) true
            // Diagonal capture
            else if (Math.abs(targetCol - currentCol) == 1 && targetRow == currentRow + direction && targetOccupied) {
                val targetPiece = currentPieces.find { it.position == Pair(targetRow, targetCol) }
                targetPiece?.color != piece.color
            } else false
        }

        PieceType.ROOK -> {
            if (targetRow != currentRow && targetCol != currentCol) return false // Must be straight

            val path = if (targetRow == currentRow) {
                val range =
                    if (targetCol > currentCol) (currentCol + 1 until targetCol) else (targetCol + 1 until currentCol)
                range.map { col -> Pair(currentRow, col) }
            } else {
                val range =
                    if (targetRow > currentRow) (currentRow + 1 until targetRow) else (targetRow + 1 until currentRow)
                range.map { row -> Pair(row, currentCol) }
            }

            val blocked = path.any { pos -> currentPieces.any { it.position == pos } }
            if (blocked) return false

            val targetPiece = currentPieces.find { it.position == Pair(targetRow, targetCol) }
            targetPiece?.color != piece.color
        }

        PieceType.KNIGHT -> {
            val rowDiff = Math.abs(targetRow - currentRow)
            val colDiff = Math.abs(targetCol - currentCol)

            val isLShape = (rowDiff == 2 && colDiff == 1) || (rowDiff == 1 && colDiff == 2)
            if (!isLShape) return false

            val targetPiece = currentPieces.find { it.position == Pair(targetRow, targetCol) }
            return targetPiece?.color != piece.color
        }

        PieceType.BISHOP -> {
            val rowDiff = Math.abs(targetRow - currentRow)
            val colDiff = Math.abs(targetCol - currentCol)

            if (rowDiff != colDiff) return false // Must be diagonal

            val rowStep = if (targetRow > currentRow) 1 else -1
            val colStep = if (targetCol > currentCol) 1 else -1

            val path = (1 until rowDiff).map { i ->
                Pair(currentRow + i * rowStep, currentCol + i * colStep)
            }

            val blocked = path.any { pos -> currentPieces.any { it.position == pos } }
            if (blocked) return false

            val targetPiece = currentPieces.find { it.position == Pair(targetRow, targetCol) }
            return targetPiece?.color != piece.color
        }

        PieceType.QUEEN -> {
            val rowDiff = Math.abs(targetRow - currentRow)
            val colDiff = Math.abs(targetCol - currentCol)

            val isStraight = targetRow == currentRow || targetCol == currentCol
            val isDiagonal = rowDiff == colDiff

            if (!isStraight && !isDiagonal) return false

            val path = if (isStraight) {
                if (targetRow == currentRow) {
                    val range =
                        if (targetCol > currentCol) (currentCol + 1 until targetCol) else (targetCol + 1 until currentCol)
                    range.map { col -> Pair(currentRow, col) }
                } else {
                    val range =
                        if (targetRow > currentRow) (currentRow + 1 until targetRow) else (targetRow + 1 until currentRow)
                    range.map { row -> Pair(row, currentCol) }
                }
            } else {
                val rowStep = if (targetRow > currentRow) 1 else -1
                val colStep = if (targetCol > currentCol) 1 else -1
                (1 until rowDiff).map { i ->
                    Pair(
                        currentRow + i * rowStep,
                        currentCol + i * colStep
                    )
                }
            }

            val blocked = path.any { pos -> currentPieces.any { it.position == pos } }
            if (blocked) return false

            val targetPiece = currentPieces.find { it.position == Pair(targetRow, targetCol) }
            return targetPiece?.color != piece.color
        }

        PieceType.KING -> {
            val rowDiff = Math.abs(targetRow - currentRow)
            val colDiff = Math.abs(targetCol - currentCol)

            val isOneStep = rowDiff <= 1 && colDiff <= 1
            if (!isOneStep) return false

            val targetPiece = currentPieces.find { it.position == Pair(targetRow, targetCol) }
            return targetPiece?.color != piece.color
        }
    }
}

fun isKingInCheck(color: PieceColor, currentPieces: List<Piece>): Boolean {
    val king = currentPieces.find { it.type == PieceType.KING && it.color == color } ?: return false
    val kingPos = king.position

    return currentPieces.any { enemy ->
        if (enemy.color != color) {
            isValidMove(enemy, kingPos.first, kingPos.second, currentPieces)
        } else false
    }
}

fun hasLegalMoves(color: PieceColor, currentPieces: List<Piece>): Boolean {
    val pieces = currentPieces.filter { it.color == color }

    return pieces.any { piece ->
        (0..7).any { row ->
            (0..7).any { col ->
                val target = Pair(row, col)
                val tempBoard = currentPieces.toMutableList()
                val targetPiece = tempBoard.find { it.position == target }

                if (targetPiece?.color == color) return@any false

                tempBoard.removeAll { it.position == target || it == piece }
                tempBoard.add(piece.copy(position = target))

                isValidMove(piece, row, col, currentPieces) &&
                        !isKingInCheck(color, tempBoard)
            }
        }
    }
}


@Composable
fun Chessboard() {
    val boardState = remember {
        mutableStateMapOf<Pair<Int, Int>, Piece>().apply {
            initialBoard.forEach { put(it.position, it) }
        }
    }
    var currentTurn by remember { mutableStateOf(PieceColor.WHITE) }
    var selectedPiece by remember { mutableStateOf<Piece?>(null) }
    var gameStatus by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }

    // Check detection
    fun isKingInCheck(color: PieceColor, pieces: List<Piece>): Boolean {
        val king = pieces.find { it.type == PieceType.KING && it.color == color }
        if (king == null) return true
        val kingPos = king.position
        val opponentPieces = pieces.filter { it.color != color }
        return opponentPieces.any { piece ->
            isValidMove(piece, kingPos.first, kingPos.second, pieces)
        }
    }

    // Legal move detection
    fun hasLegalMoves(color: PieceColor, board: Map<Pair<Int, Int>, Piece>): Boolean {
        val pieces = board.values.filter { it.color == color }
        return pieces.any { piece ->
            (0..7).any { row ->
                (0..7).any { col ->
                    val target = Pair(row, col)
                    if (isValidMove(piece, row, col, board.values.toList())) {
                        val simulatedBoard = board.toMutableMap()
                        simulatedBoard.remove(piece.position)
                        simulatedBoard[target] = piece.copy(position = target)
                        !isKingInCheck(color, simulatedBoard.values.toList())
                    } else false
                }
            }
        }
    }

    // Bot move logic
    fun makeBotMove() {
        val botPieces = boardState.values.filter { it.color == PieceColor.BLACK }
        val legalMoves = botPieces.flatMap { piece ->
            (0..7).flatMap { row ->
                (0..7).mapNotNull { col ->
                    val target = Pair(row, col)
                    val tempBoard = boardState.toMutableMap()
                    if (isValidMove(piece, row, col, tempBoard.values.toList())) {
                        val simulatedBoard = tempBoard.toMutableMap()
                        simulatedBoard.remove(piece.position)
                        simulatedBoard[target] = piece.copy(position = target)
                        if (!isKingInCheck(PieceColor.BLACK, simulatedBoard.values.toList())) {
                            BotMove(piece, target)
                        } else null
                    } else null
                }
            }
        }

        if (legalMoves.isNotEmpty()) {
            val move = legalMoves.random()
            boardState.remove(move.piece.position)
            boardState[move.target] = move.piece.copy(position = move.target)
            currentTurn = PieceColor.WHITE
        }
    }

    // Trigger bot move
    LaunchedEffect(currentTurn) {
        if (currentTurn == PieceColor.BLACK) {
            delay(500L)
            makeBotMove()
        }
    }

    //  Determine king in check
    val isCheck = isKingInCheck(currentTurn, boardState.values.toList())
    val kingInCheckPos = if (isCheck) {
        boardState.values.find { it.type == PieceType.KING && it.color == currentTurn }?.position
    } else null

    // Game status dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Game Over") },
            text = { Text(gameStatus) },
            confirmButton = {
                Button(onClick = {
                    boardState.clear()
                    initialBoard.forEach { boardState[it.position] = it }
                    currentTurn = PieceColor.WHITE
                    selectedPiece = null
                    gameStatus = ""
                    showDialog = false
                }) {
                    Text("Restart Game")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
    // Board UI
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(450.dp)
    ) {
        for (row in 7 downTo 0) {
            Row(modifier = Modifier.weight(1f)) {
                for (col in 0 until 8) {
                    val isWhite = (row + col) % 2 == 0
                    val pieceAtSquare = boardState[row to col]

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                when {
                                    selectedPiece?.position == (row to col) -> Color.Gray
                                    kingInCheckPos == row to col -> Color.Red
                                    isWhite -> Color.LightGray
                                    else -> Color.DarkGray
                                }
                            )
                            .clickable {
                                val clickedPiece = boardState[row to col]
                                if (selectedPiece == null && clickedPiece?.color == currentTurn) {
                                    selectedPiece = clickedPiece
                                } else if (selectedPiece != null) {
                                    if (isValidMove(selectedPiece!!, row, col, boardState.values.toList())) {
                                        val from = selectedPiece!!.position
                                        val to = Pair(row, col)
                                        val movedPiece = selectedPiece!!.copy(position = to)

                                        boardState.remove(from)
                                        boardState[to] = movedPiece

                                        // Pawn promotion
                                        if (movedPiece.type == PieceType.PAWN) {
                                            val promotionRow = if (movedPiece.color == PieceColor.WHITE) 0 else 7
                                            if (row == promotionRow) {
                                                boardState[to] = Piece(PieceType.QUEEN, movedPiece.color, to)
                                            }
                                        }

                                        // Switch turn
                                        currentTurn = if (currentTurn == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE

                                        // Check game status
                                        val opponentColor = if (currentTurn == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
                                        val isOpponentCheck = isKingInCheck(opponentColor, boardState.values.toList())
                                        val hasOpponentMoves = hasLegalMoves(opponentColor, boardState)

                                        gameStatus = when {
                                            isOpponentCheck && !hasOpponentMoves -> "Checkmate! ${currentTurn.name} wins."
                                            !isOpponentCheck && !hasOpponentMoves -> "Stalemate! It's a draw."
                                            isOpponentCheck -> "${opponentColor.name} is in check!"
                                            else -> ""
                                        }

                                        if (gameStatus.contains("Checkmate") || gameStatus.contains("Stalemate")) {
                                            showDialog = true
                                        }
                                    }
                                    selectedPiece = null
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (pieceAtSquare != null) {
                            Text(
                                text = getPieceSymbol(pieceAtSquare),
                                fontSize = 50.sp,
                                color = if (pieceAtSquare.color == PieceColor.WHITE) Color.White else Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}
