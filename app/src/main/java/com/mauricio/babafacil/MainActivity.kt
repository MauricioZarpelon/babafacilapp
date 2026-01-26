package com.mauricio.babafacil

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContent { App() }
    }
}

/* ---------------- APP ---------------- */

@Composable
fun App() {
    var telaAtual by remember { mutableStateOf("home") }

    when (telaAtual) {
        "home" -> HomeScreen(
            onCadastrar = { telaAtual = "cadastro" },
            onBuscar = { telaAtual = "lista" }
        )

        "cadastro" -> CadastroBabaScreen(
            onVoltar = { telaAtual = "home" }
        )

        "lista" -> ListaBabasScreen(
            onVoltar = { telaAtual = "home" }
        )
    }
}

/* ---------------- HOME ---------------- */

@Composable
fun HomeScreen(
    onCadastrar: () -> Unit,
    onBuscar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onCadastrar, modifier = Modifier.fillMaxWidth()) {
            Text("Cadastrar babá")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onBuscar, modifier = Modifier.fillMaxWidth()) {
            Text("Encontrar babá")
        }
    }
}

/* ---------------- CADASTRO ---------------- */

@Composable
fun CadastroBabaScreen(
    onVoltar: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val snackbarHostState = remember { SnackbarHostState() }

    var nome by remember { mutableStateOf("") }
    var idade by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }
    var cidade by remember { mutableStateOf("") }
    var experiencia by remember { mutableStateOf("") }
    var fotoUri by remember { mutableStateOf<Uri?>(null) }
    var salvando by remember { mutableStateOf(false) }
    var sucessoCadastro by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        fotoUri = uri
    }

    LaunchedEffect(sucessoCadastro) {
        if (sucessoCadastro) {
            snackbarHostState.showSnackbar("Babá cadastrada com sucesso!")
            delay(800)
            onVoltar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text("Cadastro de Babá", style = MaterialTheme.typography.titleLarge)

            if (fotoUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(fotoUri),
                    contentDescription = "Foto da babá",
                    modifier = Modifier
                        .size(140.dp)
                        .clickable { launcher.launch("image/*") },
                    contentScale = ContentScale.Crop
                )
            } else {
                OutlinedButton(onClick = { launcher.launch("image/*") }) {
                    Text("Selecionar foto")
                }
            }

            OutlinedTextField(nome, { nome = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(idade, { idade = it }, label = { Text("Idade") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(telefone, { telefone = it }, label = { Text("Telefone") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(cidade, { cidade = it }, label = { Text("Cidade") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(experiencia, { experiencia = it }, label = { Text("Experiência") }, modifier = Modifier.fillMaxWidth())

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !salvando,
                onClick = {
                    salvando = true

                    fun salvarNoFirestore(fotoUrl: String?) {
                        val babaData = hashMapOf(
                            "nome" to nome,
                            "idade" to idade,
                            "telefone" to telefone,
                            "cidade" to cidade.trim().lowercase(),
                            "experiencia" to experiencia,
                            "fotoUrl" to fotoUrl
                        )

                        db.collection("babas")
                            .document(telefone)
                            .set(babaData)
                            .addOnSuccessListener {
                                salvando = false
                                sucessoCadastro = true
                            }
                            .addOnFailureListener {
                                salvando = false
                            }
                    }

                    if (fotoUri != null) {
                        val ref = storage.reference.child("babas/${UUID.randomUUID()}.jpg")
                        ref.putFile(fotoUri!!)
                            .continueWithTask { ref.downloadUrl }
                            .addOnSuccessListener { salvarNoFirestore(it.toString()) }
                            .addOnFailureListener { salvando = false }
                    } else {
                        salvarNoFirestore(null)
                    }
                }
            ) {
                if (salvando) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Cadastrar")
                }
            }

            TextButton(onClick = onVoltar) {
                Text("Voltar")
            }
        }
    }
}

/* ---------------- LISTA ---------------- */

@Composable
fun ListaBabasScreen(
    onVoltar: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    var cidadeBusca by remember { mutableStateOf("") }
    var babas by remember { mutableStateOf(listOf<Map<String, Any>>()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {

        OutlinedTextField(
            value = cidadeBusca,
            onValueChange = { cidadeBusca = it },
            label = { Text("Buscar por cidade") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                db.collection("babas")
                    .whereEqualTo("cidade", cidadeBusca.trim().lowercase())
                    .get()
                    .addOnSuccessListener { result ->
                        babas = result.documents.mapNotNull { it.data }
                    }
            }
        ) {
            Text("Buscar")
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn {
            items(babas) { baba ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Column {

                        val fotoUrl = baba["fotoUrl"] as? String

                        if (!fotoUrl.isNullOrEmpty()) {
                            Image(
                                painter = rememberAsyncImagePainter(fotoUrl),
                                contentDescription = "Foto da babá",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Column(Modifier.padding(12.dp)) {
                            Text("Nome: ${baba["nome"]}")
                            Text("Idade: ${baba["idade"]}")
                            Text("Telefone: ${baba["telefone"]}")
                            Text("Experiência: ${baba["experiencia"]}")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onVoltar) {
            Text("Voltar")
        }
    }
}
