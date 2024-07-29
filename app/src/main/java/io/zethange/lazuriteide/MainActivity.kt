package io.zethange.lazuriteide

import CodeEditor
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import com.kingmang.lazurite.crashHandler.CrashHandler
import com.kingmang.lazurite.crashHandler.reporter.crashReporterImplementation.SimpleCrashReporter
import com.kingmang.lazurite.crashHandler.reporter.output.IReportOutput
import com.kingmang.lazurite.crashHandler.reporter.output.impl.FileReportOutput
import com.kingmang.lazurite.crashHandler.reporter.processors.impl.SourceCodeProcessor
import com.kingmang.lazurite.crashHandler.reporter.processors.impl.TokensProcessor
import com.kingmang.lazurite.exceptions.LzrException
import com.kingmang.lazurite.parser.ILexer
import com.kingmang.lazurite.parser.IParser
import com.kingmang.lazurite.parser.impl.LexerImplementation
import com.kingmang.lazurite.parser.impl.ParserImplementation
import com.kingmang.lazurite.parser.preprocessor.Preprocessor
import com.kingmang.lazurite.patterns.visitor.FunctionAdder
import io.zethange.lazuriteide.ui.theme.LazuriteIDETheme
import java.io.OutputStream
import java.io.PrintStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LazuriteIDETheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    var input by remember {
        mutableStateOf("""
            using "lzr.lang.system"

            // Выводит имя операционной системы и версию lazurite
            println(system.getProperty("os.name"))  
            println(system.getProperty("lzr.version"))
        """.trimIndent())
    }
    var result by remember { mutableStateOf("") }

    class ConsoleReportOutput : IReportOutput {
        override fun out(report: String) {
            result += report
        }
    }

    val output = object : PrintStream(System.out) {
        override fun println(x: Any?) {
            Log.i("demo", x.toString())
            result += x
        }

        override fun print(obj: Any?) {
            Log.i("demo", obj.toString())
            result += obj
        }

        override fun write(b: Int) {
            Log.i("demo", b.toString())
            result += b.toChar().toString()
        }
    }

    System.setOut(output)

    CrashHandler.register(
        SimpleCrashReporter(),
        ConsoleReportOutput(),
        FileReportOutput()
    )

    fun runCode(code: String) {
        result = ""
        val processedCode = Preprocessor.preprocess(code)
        CrashHandler.getCrashReporter().addProcessor(SourceCodeProcessor(processedCode))

        val lexer: ILexer = LexerImplementation(processedCode)

        val tokens = lexer.tokenize()
        CrashHandler.getCrashReporter().addProcessor(TokensProcessor(tokens))

        val parser: IParser =
            ParserImplementation(tokens, "local")
        val parsedProgram = parser.parse()
        if (parser.parseErrors.hasErrors()) {
            println(parser.parseErrors)
            return
        }
        parsedProgram.accept(FunctionAdder())

        try {
            parsedProgram.execute()
        } catch (ex: LzrException) {
            result += ex.toString()
        } catch (throwable: Throwable) {
            CrashHandler.proceed(throwable)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick =  { runCode(input) } ,
            ) {
                Icon(Icons.Filled.PlayArrow, "Запустить")
            }
        }
    ) { innerPadding ->
        Box(modifier = modifier.padding(innerPadding)) {
            Column(modifier = modifier.padding(horizontal = 10.dp)) {
                Text("Код:", fontWeight = FontWeight(500))
                Card {
                    CodeEditor(input, onChange = { input = it }, modifier = modifier.padding(10.dp))
                }
                Text("Результат:", fontWeight = FontWeight(500))
                Card {
                    BasicText(text = result.takeIf { it.isNotEmpty() } ?: "Кажется, код еще не запускали или вывода нет", modifier = modifier.fillMaxWidth().padding(10.dp))
                }
            }
        }
    }
}
