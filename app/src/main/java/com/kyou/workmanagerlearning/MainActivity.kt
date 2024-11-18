package com.kyou.workmanagerlearning

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.work.Constraints
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import coil.compose.AsyncImage
import com.kyou.workmanagerlearning.ui.theme.WorkManagerLearningTheme

class MainActivity : ComponentActivity() {
    public lateinit var workManager: WorkManager
    private val viewModel by viewModels<PhotoViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        workManager = WorkManager.getInstance(applicationContext)
        setContent {
            WorkManagerLearningTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val workResult = viewModel.workId?.let { id ->
                        workManager.getWorkInfoByIdLiveData(id).observeAsState().value
                    }
                    LaunchedEffect(key1 = workResult?.outputData) {
                        if (workResult?.outputData != null) {
                            val filePath = workResult.outputData.getString(
                                PhotoCompressionWorker.KEY_RESUTL_PATH
                            )
                            filePath?.let {
                                val bitmap = BitmapFactory.decodeFile(it)
                                viewModel.updateCompressedBitmap(bitmap)
                            }
                        }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        viewModel.uncompressedUri?.let {
                            Text("Uncompressed photo: ")
                            AsyncImage(
                                model = it,
                                contentDescription = null
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        viewModel.compressedBitmap?.let {
                            Text("Compressed photo: ")
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(Intent.EXTRA_STREAM, android.net.Uri::class.java)
        } else {
            intent?.getParcelableExtra(Intent.EXTRA_STREAM)
        } ?: return
        viewModel.updateUncompressUri(uri)
        val request = OneTimeWorkRequestBuilder<PhotoCompressionWorker>().setInputData(
            workDataOf(
                PhotoCompressionWorker.KEY_CONTENT_URI to uri.toString(),
                PhotoCompressionWorker.KEY_COMPRESSION_THRESHOLD to 1024 * 20L
            )
        ).setConstraints(Constraints(requiresStorageNotLow = true)).build()
        viewModel.updateWorkId(request.id)
        workManager.enqueue(request)
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WorkManagerLearningTheme {
        Greeting("Android")
    }
}