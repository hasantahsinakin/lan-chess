package com.example.lanchess

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Move
import com.github.bhlangonijr.chesslib.Square
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val io = Executors.newCachedThreadPool()
    private val board = Board()
    private lateinit var status: TextView
    private lateinit var boardGrid: GridLayout
    private lateinit var ipInput: EditText
    private lateinit var hostButton: Button
    private lateinit var joinButton: Button
    private lateinit var newButton: Button
    private lateinit var controls: LinearLayout
    private var selected: Square? = null
    private var legalTargets: Set<Square> = emptySet()
    private var socket: Socket? = null
    private var serverSocket: ServerSocket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null
    private var isHost = false
    private var myColor = "WHITE"
    private var connected = false
    private var gameStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.setBackgroundColor(Color.rgb(247, 246, 241))
        buildUi()
        renderBoard()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(16, 18, 16, 12) }
        val title = TextView(this).apply {
            text = "♟  YEREL SATRANÇ"; textSize = 24f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(23, 58, 43)); gravity = Gravity.CENTER; setPadding(0, 4, 0, 12)
        }
        root.addView(title)
        status = TextView(this).apply {
            text = "Aynı Wi-Fi ağına bağlanın. Bir telefon oyunu kursun."; textSize = 15f
            setTextColor(Color.DKGRAY); gravity = Gravity.CENTER; setPadding(4, 6, 4, 12)
        }
        root.addView(status)
        controls = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        hostButton = Button(this).apply { text = "Oyunu Kur"; setOnClickListener { startHost() } }
        joinButton = Button(this).apply { text = "Bağlan"; setOnClickListener { joinGame() } }
        row.addView(hostButton, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(joinButton, LinearLayout.LayoutParams(0, -2, 1f))
        controls.addView(row)
        ipInput = EditText(this).apply {
            hint = "Kurucu telefonun IP adresi"; setSingleLine(true); textSize = 16f
            setTextColor(Color.BLACK); setPadding(12, 4, 12, 4)
        }
        controls.addView(ipInput)
        newButton = Button(this).apply { text = "Yeni Oyun"; isEnabled = false; setOnClickListener { resetBoard() } }
        controls.addView(newButton)
        root.addView(controls)
        boardGrid = GridLayout(this).apply { rowCount = 8; columnCount = 8; useDefaultMargins = false }
        val boardParams = LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = 10; bottomMargin = 10 }
        root.addView(boardGrid, boardParams)
        val footer = TextView(this).apply {
            text = "İnternet gerekmez • Rok, terfi ve diğer kurallar desteklenir"; textSize = 12f
            setTextColor(Color.GRAY); gravity = Gravity.CENTER; setPadding(0, 4, 0, 4)
        }
        root.addView(footer)
        setContentView(root)
    }

    private fun startHost() {
        if (connected) return
        hostButton.isEnabled = false
        status.text = "Yerel ağda oyun açılıyor…"
        io.execute {
            try {
                val server = ServerSocket(47821)
                serverSocket = server
                val ip = localIpAddress() ?: "Wi-Fi IP bulunamadı"
                runOnUiThread { status.text = "Kurucu (Beyaz) • IP: $ip\nDiğer telefonda bu IP'yi girip Bağlan'a basın."; myColor = "WHITE"; gameStarted = true; newButton.isEnabled = true }
                val client = server.accept()
                attachSocket(client, true)
            } catch (e: Exception) {
                runOnUiThread { status.text = "Sunucu açılamadı: ${e.localizedMessage}"; hostButton.isEnabled = true }
            }
        }
    }

    private fun joinGame() {
        if (connected) return
        val host = ipInput.text.toString().trim()
        if (host.isBlank()) { status.text = "Önce kurucu telefonun IP adresini yazın."; return }
        joinButton.isEnabled = false
        status.text = "$host adresine bağlanılıyor…"
        io.execute {
            try {
                val s = Socket(host, 47821)
                attachSocket(s, false)
            } catch (e: Exception) {
                runOnUiThread { status.text = "Bağlanılamadı: ${e.localizedMessage}\nAynı Wi-Fi ağında olduğunuzu kontrol edin."; joinButton.isEnabled = true }
            }
        }
    }

    private fun attachSocket(s: Socket, host: Boolean) {
        socket = s; isHost = host; connected = true
        writer = PrintWriter(s.getOutputStream(), true)
        reader = BufferedReader(InputStreamReader(s.getInputStream()))
        if (!host) { myColor = "BLACK"; gameStarted = true }
        runOnUiThread {
            status.text = if (host) "Rakip bağlandı. Sen Beyazsın; ilk hamle sende." else "Bağlandın. Sen Siyahsın; Beyaz'ın hamlesini bekle."
            hostButton.isEnabled = false; joinButton.isEnabled = false; newButton.isEnabled = true; renderBoard()
        }
        io.execute {
            try {
                while (true) {
                    val line = reader?.readLine() ?: break
                    if (line.startsWith("MOVE ")) {
                        val uci = line.substringAfter("MOVE ").trim()
                        runOnUiThread { applyMove(uci, false) }
                    } else if (line == "RESET") {
                        runOnUiThread { board.loadFromFen(Board().fen); selected = null; renderBoard(); status.text = "Yeni oyun başladı." }
                    }
                }
            } catch (_: Exception) { }
            runOnUiThread { connected = false; status.text = "Bağlantı kesildi. Aynı ağda olduğunuzu kontrol edin."; hostButton.isEnabled = true; joinButton.isEnabled = true }
        }
    }

    private fun resetBoard() {
        board.loadFromFen(Board().fen); selected = null; legalTargets = emptySet()
        writer?.println("RESET")
        status.text = if (connected) "Yeni oyun başladı. Beyaz başlar." else "Yeni oyun hazır. Bir rakip bağlayın."
        renderBoard()
    }

    private fun renderBoard() {
        if (!::boardGrid.isInitialized) return
        boardGrid.removeAllViews()
        val side = (resources.displayMetrics.widthPixels - 32 - 32) / 8
        val cellSize = side.coerceAtLeast(30)
        for (r in 0..7) for (c in 0..7) {
            val rank = 8 - r; val file = 'a' + c
            val sq = Square.valueOf("${file.uppercaseChar()}$rank")
            val piece = board.getPiece(sq).name
            val light = (r + c) % 2 == 0
            val bg = when {
                selected == sq -> Color.rgb(246, 210, 75)
                sq in legalTargets -> Color.rgb(144, 190, 132)
                light -> Color.rgb(238, 228, 205)
                else -> Color.rgb(91, 132, 104)
            }
            val cell = TextView(this).apply {
                text = pieceGlyph(piece); textSize = (cellSize * 0.56f).coerceAtMost(42f)
                gravity = Gravity.CENTER; setBackgroundColor(bg)
                setTextColor(if (piece.startsWith("BLACK")) Color.rgb(28, 30, 29) else Color.WHITE)
                if (piece.startsWith("WHITE")) setShadowLayer(1.2f, 0f, 1f, Color.DKGRAY)
                setOnClickListener { onSquareTap(sq) }
            }
            boardGrid.addView(cell, GridLayout.LayoutParams().apply { rowSpec = GridLayout.spec(r, 1f); columnSpec = GridLayout.spec(c, 1f); width = 0; height = 0 })
        }
        boardGrid.requestLayout()
    }

    private fun onSquareTap(sq: Square) {
        if (!gameStarted) { status.text = "Önce Oyunu Kur veya Bağlan'a bas."; return }
        val turn = board.sideToMove.name
        if (turn != myColor) { status.text = "Sıra rakibinde."; return }
        val piece = board.getPiece(sq).name
        if (selected == null) {
            if (piece == "NONE" || !piece.startsWith(myColor)) return
            selected = sq
            legalTargets = board.legalMoves().filter { it.from == sq }.map { it.to }.toSet()
            renderBoard(); return
        }
        if (selected == sq) { selected = null; legalTargets = emptySet(); renderBoard(); return }
        val from = selected!!
        var move = "$from$sq".lowercase()
        val movingPiece = board.getPiece(from).name
        if (movingPiece.endsWith("PAWN") && (sq.name.endsWith("8") || sq.name.endsWith("1"))) move += "q"
        selected = null; legalTargets = emptySet()
        applyMove(move, true)
    }

    private fun applyMove(uci: String, send: Boolean) {
        try {
            val candidate = Move.fromValue(uci)
            if (!board.legalMoves().contains(candidate)) {
                if (send) status.text = "Geçersiz hamle." 
                renderBoard(); return
            }
            board.doMove(candidate)
            if (send) writer?.println("MOVE $uci")
            selected = null; legalTargets = emptySet()
            val outcome = when {
                board.isMated -> "Şah mat! ${if (board.sideToMove.name == "WHITE") "Siyah" else "Beyaz"} kazandı."
                board.isStaleMate -> "Pat! Oyun berabere."
                board.isDraw -> "Berabere."
                board.isKingAttacked -> "Şah! Sıra: ${if (board.sideToMove.name == "WHITE") "Beyaz" else "Siyah"}."
                else -> "Sıra: ${if (board.sideToMove.name == "WHITE") "Beyaz" else "Siyah"}."
            }
            status.text = outcome
            renderBoard()
        } catch (e: Exception) { status.text = "Hamle işlenemedi: ${e.localizedMessage}"; renderBoard() }
    }

    private fun pieceGlyph(piece: String): String = when (piece) {
        "WHITE_KING" -> "♔"; "WHITE_QUEEN" -> "♕"; "WHITE_ROOK" -> "♖"; "WHITE_BISHOP" -> "♗"; "WHITE_KNIGHT" -> "♘"; "WHITE_PAWN" -> "♙"
        "BLACK_KING" -> "♚"; "BLACK_QUEEN" -> "♛"; "BLACK_ROOK" -> "♜"; "BLACK_BISHOP" -> "♝"; "BLACK_KNIGHT" -> "♞"; "BLACK_PAWN" -> "♟"
        else -> ""
    }

    private fun localIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val ni = interfaces.nextElement()
                val addresses = ni.inetAddresses
                while (addresses.hasMoreElements()) {
                    val a = addresses.nextElement()
                    if (!a.isLoopbackAddress && a is Inet4Address && ni.isUp) return a.hostAddress
                }
            }
        } catch (_: Exception) { }
        return null
    }

    override fun onDestroy() {
        try { socket?.close() } catch (_: Exception) { }
        try { serverSocket?.close() } catch (_: Exception) { }
        io.shutdownNow()
        super.onDestroy()
    }
}
