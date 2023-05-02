package com.example.memorygame

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.memorygame.models.BoardSize
import com.example.memorygame.models.utils.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.google.firebase.storage.ktx.storage
import java.io.ByteArrayOutputStream

class CreateActivity : AppCompatActivity() {

    private lateinit var rvImagePicker:RecyclerView
    private lateinit var etName:EditText
    private lateinit var btnSave: Button
    private lateinit var adapter: ImagePickerAdapter
    private lateinit var boardSize: BoardSize
    private lateinit var pbUploading: ProgressBar
    private var numOfRequiredImgs = 0
    private var chosenImgUris = mutableListOf<Uri>()
    private val remoteConfig = Firebase.remoteConfig
    private val storage = Firebase.storage
    private val db = Firebase.firestore

    companion object{
        val READ_PHOTOS_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE
        val  READ_EXTERNAL_PHOTOS_CODE=248
        const val TAG = "CreateActivity"
        const val MIN_GAME_NAME_LENGTH = 3
        const val MAX_GAME_NAME_LENGTH = 14
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)

        val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)

        rvImagePicker = findViewById(R.id.rvImagePicker)
        etName = findViewById(R.id.etGameName)
        btnSave = findViewById(R.id.btnSave)
        pbUploading = findViewById(R.id.pbUploading)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        boardSize = intent.getSerializableExtra(EXTRA_BOARD_SIZE) as BoardSize
        numOfRequiredImgs = boardSize.getPairs()
        supportActionBar?.title ="Choose pics (0 / $numOfRequiredImgs)"

        btnSave.setOnClickListener {
            saveDataToFirebase()
        }
        etName.filters = arrayOf(InputFilter.LengthFilter(MAX_GAME_NAME_LENGTH))
        etName.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(p0: Editable?) {
                btnSave.isEnabled = shouldEnabledSaveButton()
            }
        })

        adapter = ImagePickerAdapter(this,chosenImgUris,boardSize, object: ImagePickerAdapter.ImageClickListener{
            override fun onPlaceHolderClicked() {
                if(isPermissionGranted(this@CreateActivity, READ_PHOTOS_PERMISSION)){
                    launchIntentForPhotos()
                }else{
                    requestPermission(this@CreateActivity, READ_PHOTOS_PERMISSION,
                        READ_EXTERNAL_PHOTOS_CODE)
                }
            }
        })
        rvImagePicker.adapter = adapter
        rvImagePicker.setHasFixedSize(true)
        rvImagePicker.layoutManager = GridLayoutManager(this,boardSize.getWidth())
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode== READ_EXTERNAL_PHOTOS_CODE){
            if(grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                launchIntentForPhotos()
            }else{
                Toast.makeText(this," In order to create a custom game, you need to provide access to your photos", Toast.LENGTH_LONG).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId==android.R.id.home){
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun launchIntentForPhotos() {
        val intent = Intent().apply{
            action=Intent.ACTION_PICK
            type="image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true)
        }
        resultLauncher.launch(Intent.createChooser(intent,"Choose pics"))
    }

    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data: Intent? = result.data
        if (result.resultCode != RESULT_OK || data ==null) {
            // There are no request codes
            Log.w(TAG, "Did not get data back from the launched activity, user likely canceled flow")
            return@registerForActivityResult
        }

        val selectedUri: Uri? = data.data
        val clipData: ClipData? = data.clipData

        if(clipData!=null){
            Log.i(TAG,"clipData num of images : ${clipData.itemCount}: $clipData")
            for(i in 0 until clipData.itemCount){
                val clipItem = clipData.getItemAt(i)
                if(chosenImgUris.size<numOfRequiredImgs){
                    chosenImgUris.add(clipItem.uri)
                }
            }
        }else if(selectedUri!=null){
            Log.i(TAG,"data: $selectedUri")
            chosenImgUris.add(selectedUri)
        }
        adapter.notifyDataSetChanged()
        supportActionBar?.title = "Chosen pics: (${chosenImgUris.size}/$numOfRequiredImgs)"
        btnSave.isEnabled = shouldEnabledSaveButton()

    }

    private fun getImageByteArray(photoUri: Uri): ByteArray {
        val originalBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, photoUri)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(contentResolver, photoUri)
        }
        Log.i(TAG, "Original width ${originalBitmap.width} and height ${originalBitmap.height}")
        val scaledBitmap = BitmapScaler.scaleToFitHeight(originalBitmap,250)
        Log.i(TAG, "Scaled width ${scaledBitmap.width} and height ${scaledBitmap.height}")
        val byteOutputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, byteOutputStream)
        return byteOutputStream.toByteArray()
    }

    private fun shouldEnabledSaveButton(): Boolean {
        return chosenImgUris.size==numOfRequiredImgs && isTitleValid()
    }

    private fun isTitleValid(): Boolean {
        if(etName.text.isBlank() || etName.text.length < MIN_GAME_NAME_LENGTH){
            Log.i(TAG,"${etName.text}")
            return false
        }
        return true
    }

    private fun saveDataToFirebase() {
        btnSave.isEnabled=false
        val gameName = etName.text.toString()
        db.collection("games").document(gameName).get().addOnSuccessListener { document ->
            if(document!=null && document.data!=null){
                AlertDialog.Builder(this).setTitle("Name Taken")
                    .setMessage("A game already exists with the same name $gameName. Please choose another one")
                    .setPositiveButton("OK",null).show()
                btnSave.isEnabled=true
            }else{
                handleImageUploading(gameName)
            }
        }.addOnFailureListener { exception-> Log.i(TAG,"EXCEPTION in saveDataToFirebase")
            Toast.makeText(this,"Encountered error while saving the game",Toast.LENGTH_SHORT).show()
            btnSave.isEnabled=true

        }
    }

    private fun handleImageUploading(gameName: String) {
        pbUploading.visibility= View.VISIBLE
        val uploadedImgUrls = mutableListOf<String>()
        for((index,photoUri) in chosenImgUris.withIndex()){
            var didEncounterError = false
            val imgByteArray = getImageByteArray(photoUri)
            val filePath = "images/$gameName/${System.currentTimeMillis()}-$index.jpg"
            val photoReference = storage.reference.child(filePath)
            photoReference.putBytes(imgByteArray)
                .continueWithTask { photoUploadTask ->
                    Log.i(TAG, "UPLOADED: ${photoUploadTask.result?.bytesTransferred}")
                    photoReference.downloadUrl
                }.addOnCompleteListener{ downloadURLTask ->
                    if (!downloadURLTask.isSuccessful){
                        Log.i(TAG,"FAILED")
                        Toast.makeText(this,"failed to upload image",Toast.LENGTH_SHORT).show()
                        didEncounterError=true
                        return@addOnCompleteListener
                    }
                    if(didEncounterError){
                        pbUploading.visibility = View.GONE
                        Log.i(TAG,"BASARAMADİLK ABİ")
                        return@addOnCompleteListener
                    }
                    val downloadUrl = downloadURLTask.result.toString()
                    uploadedImgUrls.add(downloadUrl)
                    pbUploading.progress = uploadedImgUrls.size*100/chosenImgUris.size

                    Log.i(TAG,"FINISHED $photoUri, num uploaded: ${uploadedImgUrls.size} ")
                    if(uploadedImgUrls.size == chosenImgUris.size){
                        handleAllImgsUploaded(gameName,uploadedImgUrls)
                    }
                }
        }
    }

    private fun handleAllImgsUploaded(gameName: String, uploadedImgUrls: MutableList<String>) {
        db.collection("games").document(gameName).set(mapOf("images" to uploadedImgUrls)).addOnCompleteListener{
            gameCreationTask ->
                pbUploading.visibility=View.GONE
                if(!gameCreationTask.isSuccessful){
                Log.i(TAG,"EXCEPTION", gameCreationTask.exception)
                Toast.makeText(this,"FAILED",Toast.LENGTH_SHORT).show()
                return@addOnCompleteListener
            }

            Log.i(TAG,"SUCCESS")
            AlertDialog.Builder(this).setTitle("Upload complete! Lets play your game $gameName").setPositiveButton("OK"){ _,_->
                val resultData = Intent()
                resultData.putExtra(EXTRA_GAME_NAME, gameName)
                setResult(Activity.RESULT_OK,resultData)
                finish()
            }.show()

        }
    }
}
