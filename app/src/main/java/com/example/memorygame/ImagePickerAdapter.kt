package com.example.memorygame

import android.content.Context
import android.media.Image
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.memorygame.models.BoardSize
import kotlin.math.min

class ImagePickerAdapter(private val context: Context,
                         private val chosenImgUris: MutableList<Uri>,
                         private val boardSize: BoardSize ,
                         private val imageClickListener: ImageClickListener): RecyclerView.Adapter<ImagePickerAdapter.ViewHolder>(){

    interface ImageClickListener{
        fun onPlaceHolderClicked()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.card_image,parent,false)
        val cardHeight = parent.height/boardSize.getHeight()
        val cardWidth = parent.width/boardSize.getWidth()
        val cardSideLength = min(cardHeight,cardWidth)
        val lytParams = view.findViewById<ImageView>(R.id.ivCustomImg).layoutParams as ViewGroup.MarginLayoutParams
        lytParams.width = cardSideLength
        lytParams.height = cardSideLength
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if(position<chosenImgUris.size){
            holder.bind(chosenImgUris[position])
        }else{
            holder.bind()
        }
    }

    override fun getItemCount() = boardSize.getPairs()

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){

        private val ivCustomImg = itemView.findViewById<ImageView>(R.id.ivCustomImg)

        fun bind(uri: Uri) {
            ivCustomImg.setImageURI(uri)
            ivCustomImg.setOnClickListener(null)
        }

        fun bind() {
            ivCustomImg.setOnClickListener {
                imageClickListener.onPlaceHolderClicked()
            }
        }
    }
}
