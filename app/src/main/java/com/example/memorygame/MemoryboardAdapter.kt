package com.example.memorygame

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.memorygame.models.BoardSize
import com.example.memorygame.models.MemoryCard
import kotlin.math.min

class MemoryboardAdapter(
    private val context: Context,
    private val boardSize: BoardSize,
    private val cards: List<MemoryCard>,
    private val cardClickListener: CardClickListener
) : RecyclerView.Adapter<MemoryboardAdapter.ViewHolder>() {

    companion object {
        val MARGIN_SIZE = 10
        val TAG = "MemoryboardAdapter"
    }

    interface CardClickListener{
        fun onClicked(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val cardWidth = parent.width/boardSize.getWidth()-(2*MARGIN_SIZE)
        val cardHeight = parent.height/boardSize.getHeight()-(2*MARGIN_SIZE)
        val cardSideLength = min(cardHeight,cardWidth)
        val view = LayoutInflater.from(context).inflate(R.layout.memory_card,parent,false)
        val lytParams = view.findViewById<CardView>(R.id.cardView).layoutParams as ViewGroup.MarginLayoutParams
        lytParams.width = cardSideLength
        lytParams.height = cardSideLength
        lytParams.setMargins(MARGIN_SIZE,MARGIN_SIZE,MARGIN_SIZE,MARGIN_SIZE)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount() = boardSize.numCards

    inner class ViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){

        private val imgButton = itemView.findViewById<ImageButton>(R.id.imageButton)

        fun bind(position: Int) {
            imgButton.setImageResource(if (cards[position].isFaceUp) cards[position].identifier else R.drawable.ic_launcher_background)
            imgButton.alpha = if(cards[position].isMatced) .4f else 1.0f
            val colorStateList = if(cards[position].isMatced) ContextCompat.getColorStateList(context,R.color.color_gray) else null
            ViewCompat.setBackgroundTintList(imgButton,colorStateList)
            imgButton.setOnClickListener{
                Log.i(TAG,"Clicked on position: $position")
                cardClickListener.onClicked(position)
            }
        }
    }
}
