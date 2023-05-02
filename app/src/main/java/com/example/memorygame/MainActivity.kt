package com.example.memorygame

import android.animation.ArgbEvaluator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.memorygame.models.BoardSize
import com.example.memorygame.models.MemoryGame
import com.example.memorygame.models.UserImageList
import com.example.memorygame.models.utils.EXTRA_BOARD_SIZE
import com.example.memorygame.models.utils.EXTRA_GAME_NAME
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: MemoryboardAdapter
    private lateinit var memoryGame: MemoryGame
    private lateinit var clRoot: ConstraintLayout
    private lateinit var rvBoard: RecyclerView
    private lateinit var tvNumMoves: TextView
    private lateinit var tvNumPairs: TextView

    private val db = Firebase.firestore
    private var gameName: String? = null
    private var boardSize:BoardSize=BoardSize.HARD

    companion object{
        const val TAG = "CreateActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        clRoot = findViewById(R.id.clRoot)
        rvBoard = findViewById(R.id.rvBoard)
        tvNumMoves = findViewById(R.id.moves)
        tvNumPairs = findViewById(R.id.pairs)
        tvNumPairs.setTextColor(ContextCompat.getColor(this,R.color.color_progress_none))

       setupBoard()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.mi_refresh->{
                if(memoryGame.getNumMoves()>0 && memoryGame.haveWonGame()){
                    showAlertDialog("Quit your current game?",null) {
                        setupBoard()
                    }
                }
                else {setupBoard()}
                return true
            }

            R.id.mi_new_size->{
                showNewSizeDialog()
                return true
            }

            R.id.mi_custom->{
                showCreationDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


    var resultLauncher2 = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // There are no request codes
            val gameName = result.data?.getStringExtra(EXTRA_GAME_NAME)
            if(gameName==null){
                return@registerForActivityResult
            }
            downloadGame(gameName)
        }
    }

    private fun downloadGame(gameName: String) {
        db.collection("games").document(gameName).get().addOnSuccessListener { document->
            val userImageList = document.toObject(UserImageList::class.java)
            if(userImageList?.images==null){
                Log.e(TAG,"invalid custom game data from firestore")
                Snackbar.make(clRoot,"Sorry, we couldnt find any such game, $gameName",Snackbar.LENGTH_LONG).show()
            }
            val numPairs = userImageList!!.images!!.size * 2
        }.addOnFailureListener { exception -> Log.i(TAG,"EXCEPTION in downloadGame") }
    }


    private fun showCreationDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size,null)
        val radioGroup = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)

        when(boardSize){
            BoardSize.EASY -> radioGroup.check(R.id.rbEasy)
            BoardSize.MEDIUM -> radioGroup.check(R.id.rbMedium)
            BoardSize.HARD -> radioGroup.check(R.id.rbHard)
        }

        showAlertDialog("Create your own game", boardSizeView){
            val desiredBoardSize = when(radioGroup.checkedRadioButtonId){
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                R.id.rbHard-> BoardSize.HARD
                else -> {BoardSize.HARD}
            }

            val intent = Intent(this, CreateActivity::class.java)
            intent.putExtra(EXTRA_BOARD_SIZE,desiredBoardSize)
            resultLauncher.launch(intent)
        }
    }
    //TODO burayı anlamadim bak tekrar
    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // There are no request codes
            val data: Intent? = result.data
        }
    }

    private fun showNewSizeDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size,null)
        val radioGroup = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)

        when(boardSize){
            BoardSize.EASY -> radioGroup.check(R.id.rbEasy)
            BoardSize.MEDIUM ->radioGroup.check(R.id.rbMedium)
            BoardSize.HARD ->radioGroup.check(R.id.rbHard)
        }

        showAlertDialog("Choose new size",boardSizeView) {
            boardSize = when (radioGroup.checkedRadioButtonId) {
                R.id.rbEasy-> BoardSize.EASY
                R.id.rbMedium-> BoardSize.MEDIUM
                R.id.rbHard-> BoardSize.HARD
                else -> BoardSize.HARD
            }
            setupBoard()
        }
    }

    private fun showAlertDialog(title: String, view: View?, positiveClickListener: View.OnClickListener) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(view)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("OK") { _, _ ->
                positiveClickListener.onClick(null)
            }.show()
    }


    private fun setupBoard() {
        when (boardSize) {
            BoardSize.EASY -> {
                tvNumPairs.text = "Pairs: 0/4"
            }
            BoardSize.MEDIUM -> {
                tvNumPairs.text = "Pairs: 0/9"
            }
            BoardSize.HARD -> {
                tvNumPairs.text = "Pairs: 0/12"
            }
        }
        tvNumMoves.text="Moves: 0"

        memoryGame = MemoryGame(boardSize)

        adapter = MemoryboardAdapter(this,boardSize,memoryGame.cards, object: MemoryboardAdapter.CardClickListener{
            override fun onClicked(position: Int) {
                updateGameWithFlip(position)
            }
        })
        rvBoard.adapter=adapter
        rvBoard.setHasFixedSize(true)
        rvBoard.layoutManager = GridLayoutManager(this,boardSize.getWidth())
    }

    private fun updateGameWithFlip(position: Int) {

        if(memoryGame.haveWonGame()){
            Snackbar.make(clRoot,"YOU WON!",Snackbar.LENGTH_LONG).show()
            return
        }

        if(!memoryGame.cards[position].isFaceUp ){
            memoryGame.flipCard(position)
            val color = ArgbEvaluator().evaluate(
                memoryGame.numPairs.toFloat()/boardSize.getPairs(),
                ContextCompat.getColor(this,R.color.color_progress_none),
                ContextCompat.getColor(this,R.color.color_progress_full)
                ) as Int
            tvNumPairs.setTextColor(color)
            tvNumMoves.text="Moves: ${memoryGame.getNumMoves()}"
            tvNumPairs.text = "Pairs: ${memoryGame.numPairs}/${boardSize.getPairs()}"
            adapter.notifyDataSetChanged()
        }
    }
}