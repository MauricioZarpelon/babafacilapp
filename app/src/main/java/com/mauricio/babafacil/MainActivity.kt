package com.mauricio.babafacil

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.delay
import java.net.URLEncoder
import java.util.UUID
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.material.snackbar.Snackbar
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.google.firebase.firestore.DocumentSnapshot
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.lightColorScheme

class MainActivity : ComponentActivity() {

    private lateinit var appUpdateManager: AppUpdateManager

    private val listener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            popupSnackbarForCompleteUpdate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        appUpdateManager = AppUpdateManagerFactory.create(this)

        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() ==
                UpdateAvailability.UPDATE_AVAILABLE &&
                appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.FLEXIBLE,
                    this,
                    1001
                )
            }
        }

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme()
            ) {
                App()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        appUpdateManager.registerListener(listener)
    }

    override fun onPause() {
        super.onPause()
        appUpdateManager.unregisterListener(listener)
    }

    private fun popupSnackbarForCompleteUpdate() {
        Snackbar.make(
            findViewById(android.R.id.content),
            "Atualização pronta!",
            Snackbar.LENGTH_INDEFINITE
        ).setAction("Reiniciar") {
            appUpdateManager.completeUpdate()
        }.show()
    }
    }
@Composable
fun App() {
    var telaAtual by remember { mutableStateOf("splash") }
    val auth = FirebaseAuth.getInstance()

    LaunchedEffect(auth.currentUser) {
        auth.currentUser?.let { user ->
            verificarTipoUsuario(
                uid = user.uid,
                onPai = { telaAtual = "buscar" },
                onBaba = { telaAtual = "cadastro" },
                onError = { _: String -> telaAtual = "home" }
            )
        }
    }
    when (telaAtual) {
        "splash" -> SplashScreen {

            val user = auth.currentUser

            if (user == null) {
                telaAtual = "home"
            } else {

                verificarTipoUsuario(
                    uid = user.uid,
                    onPai = { telaAtual = "buscar" },
                    onBaba = { telaAtual = "cadastro" },
                    onError = { _: String -> telaAtual = "home" }
                )

            }
        }
        "perfilBaba" -> MeuPerfilBabaScreen(
            onEditar = {
                telaAtual = "cadastro"
            }
        )
        "verificando" -> Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        "home" -> HomeScreen(
            onCadastroSucesso = { tipo ->
                telaAtual = if (tipo == "pai") "buscar" else "cadastro"
            },
            onLoginSucesso = { tipo ->
                telaAtual = if (tipo == "pai") "buscar" else "cadastro"
            }
        )

        "cadastro" -> CadastroBabaScreen(
            onVoltar = {
                FirebaseAuth.getInstance().signOut()
                telaAtual = "home"
            },
            irParaPerfil = {
                telaAtual = "perfilBaba"
            }
        )

        "buscar" -> ListaBabasScreen(
            onVoltar = {
                FirebaseAuth.getInstance().signOut()
                telaAtual = "home"

            }
        )
    }
}

@Composable
fun SplashScreen(onAnimacaoTerminou: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2000)
        onAnimacaoTerminou()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF6A1B9A)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "BabáFácil",
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator(color = Color.White)
        }
    }
}

@Composable
fun HomeScreen(
    onCadastroSucesso: (String) -> Unit,
    onLoginSucesso: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var tipoUsuario by remember { mutableStateOf("pai") }
    var erro by remember { mutableStateOf("") }
    var modoLogin by remember { mutableStateOf(false) }
    var senhaVisivel by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = if (modoLogin) "Entrar" else "Criar Conta",
            style = MaterialTheme.typography.headlineMedium,
            color = Color(0xFF6A1B9A)
        )

        Spacer(Modifier.height(32.dp))

        if (!modoLogin) {
            Text("Quem é você?", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { tipoUsuario = "pai" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (tipoUsuario == "pai") Color(0xFF6A1B9A) else Color.Gray
                    )
                ) {
                    Text("Procuro Babá")
                }

                Button(
                    onClick = { tipoUsuario = "baba" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (tipoUsuario == "baba") Color(0xFF6A1B9A) else Color.Gray
                    )
                ) {
                    Text("Sou Babá")
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        // EMAIL
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        // SENHA
        OutlinedTextField(
            value = senha,
            onValueChange = { senha = it },
            label = { Text("Senha") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (senhaVisivel)
                VisualTransformation.None
            else
                PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            trailingIcon = {
                IconButton(onClick = { senhaVisivel = !senhaVisivel }) {
                    Icon(
                        imageVector = if (senhaVisivel)
                            Icons.Default.Visibility
                        else
                            Icons.Default.VisibilityOff,
                        contentDescription = null
                    )
                }
            }
        )

        if (erro.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                erro,
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (modoLogin) {
                    fazerLogin(
                        email = email,
                        senha = senha,
                        onSucesso = { uid ->
                            verificarTipoUsuario(
                                uid = uid,
                                onPai = { onLoginSucesso("pai") },
                                onBaba = { onLoginSucesso("baba") },
                                onError = { errorMessage ->
                                    erro = errorMessage
                                }
                            )
                        },
                        onErro = { errorMessage ->
                            erro = errorMessage
                        }
                    )
                } else {
                    cadastrarUsuario(
                        email = email,
                        senha = senha,
                        tipo = tipoUsuario,
                        onSucesso = { onCadastroSucesso(tipoUsuario) },
                        onErro = { errorMessage ->
                            erro = errorMessage
                        }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A))
        ) {
            Text(if (modoLogin) "Entrar" else "Criar conta")
        }

        TextButton(
            onClick = { modoLogin = !modoLogin },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (modoLogin)
                    "Não tenho conta? Criar agora"
                else
                    "Já tenho conta? Entrar"
            )
        }
    }
}
@Composable
fun CadastroBabaScreen(
    onVoltar: () -> Unit,
    irParaPerfil: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val auth = FirebaseAuth.getInstance()
    val user = FirebaseAuth.getInstance().currentUser

    var nome by remember { mutableStateOf("") }
    var idade by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }
    var cidade by remember { mutableStateOf("") }
    var experiencia by remember { mutableStateOf("") }
    var fotoUri by remember { mutableStateOf<Uri?>(null) }
    var salvando by remember { mutableStateOf(false) }

    var perfilExiste by remember { mutableStateOf(false) }


    LaunchedEffect(Unit) {

        user?.uid?.let { uid ->

            db.collection("babas")
                .document(uid)
                .get()
                .addOnSuccessListener { doc ->

                    if (doc.exists()) {
                        perfilExiste = true
                        nome = doc.getString("nome") ?: ""
                        idade = doc.getString("idade") ?: ""
                        telefone = doc.getString("whatsapp") ?: ""
                        cidade = doc.getString("cidade") ?: ""
                        experiencia = doc.getString("experiencia") ?: ""

                    }

                }

        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val cropLauncher = rememberLauncherForActivityResult(
            contract = CropImageContract()
        ) { result ->

            if (result.isSuccessful) {
                val croppedUri = result.uriContent
                if (croppedUri != null) {
                    fotoUri = croppedUri
                }
            } else {
                result.error?.printStackTrace()
            }
        }

        val context = LocalContext.current

        val galleryLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            if (uri != null) {

                val options = CropImageOptions().apply {
                    guidelines = CropImageView.Guidelines.ON
                    aspectRatioX = 1
                    aspectRatioY = 1
                    fixAspectRatio = true

                    cropMenuCropButtonTitle = "Confirmar"
                    cropMenuCropButtonIcon = android.R.drawable.ic_menu_save
                    activityBackgroundColor = android.graphics.Color.WHITE

                    outputCompressQuality = 80

                    showCropOverlay = true
                    showProgressBar = true
                    allowRotation = false
                    allowFlipping = false
                }

                val contractOptions = CropImageContractOptions(uri, options)
                cropLauncher.launch(contractOptions)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Meu Perfil de Babá", style = MaterialTheme.typography.titleLarge)

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(Color.LightGray)
                    .clickable { galleryLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (fotoUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(fotoUri),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("Add Foto", color = Color.DarkGray)
                }
            }

            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text("Nome Completo") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = idade,
                onValueChange = { idade = it },
                label = { Text("Idade") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = telefone,
                onValueChange = { telefone = it },
                label = { Text("WhatsApp (com DDD)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = cidade,
                onValueChange = { cidade = it },
                label = { Text("Cidade") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = experiencia,
                onValueChange = { experiencia = it },
                label = { Text("Resumo da Experiência") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !salvando,
                onClick = {
                    val currentUser = auth.currentUser

                    if (currentUser != null) {

                        salvando = true
                        val uid = currentUser.uid

                        fun salvarFirestore(url: String?) {
                            val dados = hashMapOf(
                                "uid" to uid,
                                "nome" to nome,
                                "telefone" to telefone,
                                "cidade" to cidade.trim().lowercase(),
                                "experiencia" to experiencia,
                                "fotoUrl" to url,
                                "idade" to idade
                            )

                            db.collection("babas").document(uid).set(dados)
                                .addOnSuccessListener {

                                    salvando = false

                                    android.widget.Toast
                                        .makeText(
                                            context,
                                            "Perfil salvo com sucesso!",
                                            android.widget.Toast.LENGTH_SHORT
                                        )
                                        .show()

                                    irParaPerfil()
                                }
                                .addOnFailureListener {

                                    salvando = false

                                    android.widget.Toast
                                        .makeText(
                                            context,
                                            "Erro ao salvar perfil",
                                            android.widget.Toast.LENGTH_SHORT
                                        )
                                        .show()
                                }
                        }

                        fotoUri?.let { uri ->

                            val ref = storage.reference
                                .child("fotos_babas/${UUID.randomUUID()}.jpg")

                            ref.putFile(uri)
                                .continueWithTask { task ->
                                    if (!task.isSuccessful) {
                                        task.exception?.let { throw it }
                                    }
                                    ref.downloadUrl
                                }
                                .addOnSuccessListener { downloadUri ->
                                    salvarFirestore(downloadUri.toString())
                                }
                                .addOnFailureListener {
                                    salvando = false
                                    salvarFirestore(null)
                                }

                        } ?: run {
                            salvarFirestore(null)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A))
            )
            {
                if (salvando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        if (perfilExiste) "Atualizar Perfil"
                        else "Salvar Perfil"
                    )
                }
            }

            TextButton(
                onClick = {
                    FirebaseAuth.getInstance().signOut()
                    onVoltar()
                }
            ) {
                Text("Voltar", color = Color.Red)
            }
        }
    }
}
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ListaBabasScreen(onVoltar: () -> Unit) {
        val db = FirebaseFirestore.getInstance()
        var cidadeBusca by remember { mutableStateOf("") }
        var babas by remember { mutableStateOf(listOf<Map<String, Any>>()) }
        var carregando by remember { mutableStateOf(false) }
        var buscaRealizada by remember { mutableStateOf(false) }
        val context = LocalContext.current

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Buscar Babá") },
                    navigationIcon = {
                        IconButton(onClick = onVoltar) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar"
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = cidadeBusca,
                    onValueChange = { cidadeBusca = it },
                    label = { Text("Digite a cidade") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ex: São Paulo") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))
                // -------------------------------------------------------

                Button(
                    onClick = {
                        carregando = true
                        buscaRealizada = true
                        db.collection("babas")
                            .whereEqualTo("cidade", cidadeBusca.trim().lowercase())
                            .get()
                            .addOnSuccessListener { result ->
                                babas = result.documents.mapNotNull { doc ->
                                    doc.data?.toMutableMap()?.apply {
                                        put("id", doc.id)
                                    }
                                }
                                carregando = false
                            }
                            .addOnFailureListener {
                                babas = emptyList()
                                carregando = false
                            }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    enabled = !carregando && cidadeBusca.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A))
                ) {
                    if (carregando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Buscar")
                    }
                }

                if (carregando) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                if (buscaRealizada && !carregando && babas.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Nenhuma babá encontrada em ${cidadeBusca}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(babas) { baba ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .background(Color.LightGray)
                                ) {
                                    val fotoUrl = baba["fotoUrl"] as? String
                                    if (!fotoUrl.isNullOrEmpty()) {
                                        Image(
                                            painter = rememberAsyncImagePainter(fotoUrl),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .padding(start = 12.dp)
                                        .weight(1f)
                                ) {
                                    Text(
                                        text = baba["nome"]?.toString() ?: "Nome não informado",
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text("Cidade: ${baba["cidade"]}")

                                    Text(
                                        text = "Experiência: ${baba["experiencia"]?.toString() ?: ""}",
                                        style = MaterialTheme.typography.bodySmall
                                    )

                                    val telefone = baba["telefone"]?.toString() ?: ""
                                    if (telefone.isNotBlank()) {
                                        Text(
                                            text = buildAnnotatedString {
                                                append("WhatsApp: ")
                                                withStyle(
                                                    style = SpanStyle(
                                                        color = Color.Blue,
                                                        textDecoration = TextDecoration.Underline
                                                    )
                                                ) {
                                                    append(telefone)
                                                }
                                            },
                                            modifier = Modifier.clickable {
                                                abrirWhatsApp(context, telefone)
                                            }
                                        )
                                            }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }


    // FUNÇÕES AUXILIARES
    fun cadastrarUsuario(
        email: String,
        senha: String,
        tipo: String,
        onSucesso: () -> Unit,
        onErro: (String) -> Unit
    ) {
        val auth = FirebaseAuth.getInstance()

        auth.createUserWithEmailAndPassword(email, senha)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid != null) {
                    val dados = hashMapOf(
                        "email" to email,
                        "tipo" to tipo
                    )

                    FirebaseFirestore.getInstance()
                        .collection("usuarios")
                        .document(uid)
                        .set(dados)
                        .addOnSuccessListener { onSucesso() }
                        .addOnFailureListener { onErro("Erro ao salvar usuário") }
                } else {
                    onErro("Erro ao criar usuário")
                }
            }
            .addOnFailureListener {
                onErro(it.message ?: "Erro ao criar conta")
            }
    }

    fun fazerLogin(
        email: String,
        senha: String,
        onSucesso: (String) -> Unit,
        onErro: (String) -> Unit
    ) {
        val auth = FirebaseAuth.getInstance()

        auth.signInWithEmailAndPassword(email, senha)
            .addOnSuccessListener { result ->
                result.user?.uid?.let { uid ->
                    onSucesso(uid)
                } ?: onErro("Erro ao fazer login")
            }
            .addOnFailureListener {
                onErro(it.message ?: "Erro ao fazer login")
            }
    }

    fun verificarTipoUsuario(
        uid: String,
        onPai: () -> Unit,
        onBaba: () -> Unit,
        onError: (String) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()

        db.collection("usuarios")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->

                if (doc.exists()) {

                    val tipo = doc.getString("tipo")

                    when (tipo) {
                        "pai" -> onPai()
                        "baba" -> onBaba()
                        else -> onError("Tipo desconhecido")
                    }

                } else {
                    onError("Usuário não encontrado")
                }

            }
            .addOnFailureListener {
                onError("Erro ao verificar usuário")
            }
    }

    @Composable
    fun MeuPerfilBabaScreen(onEditar: () -> Unit) {

        val db = FirebaseFirestore.getInstance()
        val user = FirebaseAuth.getInstance().currentUser

        var nome by remember { mutableStateOf("") }
        var idade by remember { mutableStateOf("") }
        var cidade by remember { mutableStateOf("") }
        var telefone by remember { mutableStateOf("") }
        var experiencia by remember { mutableStateOf("") }
        var fotoUrl by remember { mutableStateOf("") }

        LaunchedEffect(Unit) {

            user?.uid?.let { uid ->

                db.collection("babas")
                    .document(uid)
                    .get()
                    .addOnSuccessListener { doc ->

                        nome = doc.getString("nome") ?: ""
                        idade = doc.getString("idade") ?: ""
                        cidade = doc.getString("cidade") ?: ""
                        telefone = doc.getString("telefone") ?: ""
                        experiencia = doc.getString("experiencia") ?: ""
                        fotoUrl = doc.getString("fotoUrl") ?: ""
                    }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {

            Text(
                "Meu Perfil",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(Modifier.height(20.dp))
            if (fotoUrl.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(fotoUrl),
                    contentDescription = null,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(Modifier.height(20.dp))
            Text("Nome: $nome")
            Text("Idade: $idade")
            Text("Cidade: $cidade")
            Text("WhatsApp: $telefone")

            Spacer(Modifier.height(12.dp))

            Text(
                "Experiência:",
                fontWeight = FontWeight.Bold
            )

            Text(experiencia)

            Spacer(Modifier.height(24.dp))

            Button(onClick = { onEditar() }) {
                Text("Editar Perfil")
            }
        }
    }

    fun abrirWhatsApp(context: android.content.Context, telefone: String) {
        val digits = telefone.filter { it.isDigit() }
        val fullNumber = if (digits.startsWith("55")) digits else "55$digits"
        val message = "Olá, vi seu perfil no BabáFácil!"
        val encodedMessage = URLEncoder.encode(message, "UTF-8")
        val uri = Uri.parse("https://wa.me/$fullNumber?text=$encodedMessage")

        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: Exception) {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }

    fun whatsappLinkWithMessage(phone: String, message: String): String {
        val digits = phone.filter { it.isDigit() }
        val full = if (digits.startsWith("55")) digits else "55$digits"
        val encodedMessage = URLEncoder.encode(message, "UTF-8")
        return "https://wa.me/$full?text=$encodedMessage"
    }
