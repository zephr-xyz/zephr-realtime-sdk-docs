package xyz.zephr.sampleclient

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.zephr.sampleclient.ui.theme.ZephrSampleClientAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ZephrSampleClientAppTheme {
                MenuScreen(
                    onBackgroundClick = { startActivity(Intent(this, BackgroundActivity::class.java)) },
                    onEmbeddedSolverClick = { startActivity(Intent(this, EmbeddedSolverActivity::class.java)) }
                )
            }
        }
    }
}

@Composable
fun MenuScreen(onBackgroundClick: () -> Unit, onEmbeddedSolverClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onBackgroundClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        ) {
            Text("Background activity")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onEmbeddedSolverClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        ) {
            Text("Embedded solver")
        }
    }
}
