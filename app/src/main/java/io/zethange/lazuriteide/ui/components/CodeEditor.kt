import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun CodeEditor(input: String, onChange: (args: String) -> Unit, modifier: Modifier = Modifier) {
    BasicTextField(
        modifier = modifier,
        value = input,
        onValueChange = { onChange(it) }
    )
}