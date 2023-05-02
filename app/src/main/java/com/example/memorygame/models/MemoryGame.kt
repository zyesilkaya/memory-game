package com.example.memorygame.models

import com.example.memorygame.models.utils.DEFAULT_ICONS

class MemoryGame(private val boardSize: BoardSize) {
    private var indexOfSingleSelectedCard: Int?=null
    var numPairs = 0
    private var numOfFlips=0
    val cards: List<MemoryCard>

    init {
        val chosenImages = DEFAULT_ICONS.shuffled().take(boardSize.getPairs())
        val randomizedImages = (chosenImages+chosenImages).shuffled()
        cards = randomizedImages.map { MemoryCard(it) }
    }

    fun flipCard(position: Int):Boolean {
        numOfFlips++
        val card = cards[position]
        var foundMatch=false

        if(indexOfSingleSelectedCard==null){
            restoreCards()
            indexOfSingleSelectedCard=position
        }
        else{
            foundMatch= checkMatch(indexOfSingleSelectedCard as Int,position)
            indexOfSingleSelectedCard=null
        }

        card.isFaceUp = !card.isFaceUp
        return foundMatch
    }

    private fun checkMatch(position1: Int, position2: Int) : Boolean{

        if(cards[position1].identifier!=cards[position2].identifier){
            return false
        }
        cards[position1].isMatced = true
        cards[position2].isMatced = true
        numPairs++
        return true
    }

    private fun restoreCards() {
        for(card in cards){
            if(!card.isMatced){
                card.isFaceUp =false
            }
        }
    }

    fun getNumMoves(): Int{
        return numOfFlips/2
    }

    fun haveWonGame(): Boolean {
        return numPairs == boardSize.getPairs()
    }
}