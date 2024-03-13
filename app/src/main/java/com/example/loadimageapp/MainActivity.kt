package com.example.loadimageapp

import android.Manifest
import android.app.AlertDialog
import android.content.ContentUris
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.loadimageapp.ui.theme.LoadImageAppTheme
import java.util.Calendar

class MainActivity : ComponentActivity() {

    private val viewModel by viewModels<ImageViewModel>()

    private val readMedia = Manifest.permission.READ_MEDIA_IMAGES

    private val readMediaImagesPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
//                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
                getContent()
            } else {
                Toast.makeText(this, "Read media images permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermission()

        setContent {
            //display the images on UI
            LoadImageAppTheme {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(viewModel.images) { image ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AsyncImage(model = image.uri, contentDescription = null)
                            Text(text = image.name)
                        }
                    }
                }
            }
        }
    }

    private fun requestPermission() {
        //request permission to read media images
        if (ContextCompat.checkSelfPermission(this, readMedia) == PackageManager.PERMISSION_GRANTED) {
//            Toast.makeText(this, "Read media images permission granted", Toast.LENGTH_SHORT).show()
            getContent()
        } else {
            if (shouldShowRequestPermissionRationale(readMedia)) {
                AlertDialog.Builder(this)
                    .setTitle("Storage permisison")
                    .setMessage("Storage permission is needed to load images")
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setPositiveButton("OK") { _, _ ->
                        readMediaImagesPermission.launch(readMedia)
                    }.show()
            } else {
                readMediaImagesPermission.launch(readMedia)
            }
        }
    }

    private fun getContent() {
        //define projection i.e. image's metadata
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
        )

        //define a date
        val millisYesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }.timeInMillis

        //define a range : to request a limited data
        val selection = "${MediaStore.Images.Media.DATE_TAKEN} >= ?"
        val selectionArgs = arrayOf(millisYesterday.toString())

        //sorting order
        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        //define the content resolver to access images content provider
        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndex(MediaStore.Images.Media._ID)
            val displayNameColumn = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)

            val images = mutableListOf<Image>()

            while (
                cursor.moveToNext()
            ) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(displayNameColumn)
                val uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                images.add(Image(id, name, uri))
            }

            //update the list of images to the viewModel state variable
            viewModel.updateImages(images)
        }
    }
}