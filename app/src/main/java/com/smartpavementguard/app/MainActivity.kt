package com.smartpavementguard.app

import com.smartpavementguard.app.ui.TripScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import kotlin.concurrent.thread
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.FileProvider
import java.io.File
import androidx.activity.compose.BackHandler
import com.smartpavementguard.app.ui.AquaBg
import com.smartpavementguard.app.ui.AquaBorder
import com.smartpavementguard.app.ui.AquaCard
import com.smartpavementguard.app.ui.AquaPrimary
import com.smartpavementguard.app.ui.AquaText
import com.smartpavementguard.app.ui.AquaMuted
import com.smartpavementguard.app.ui.AquaYellow
import com.smartpavementguard.app.ui.AquaRed
import coil.compose.rememberAsyncImagePainter
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import okhttp3.MultipartBody
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.runBlocking
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import kotlin.concurrent.thread

const val BASE_URL = "http://10.0.255.133:3000"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartPavementApp(this)
        }
    }
}

@Composable
fun SmartPavementApp(activity: ComponentActivity) {
    var screen by remember {
        mutableStateOf("splash")
    }

    BackHandler(enabled = screen != "role") {
        screen = when (screen) {
            "login" -> "role"
            "register" -> "login"
            "home" -> "role"
            "manual" -> "home"
            "trip" -> "home"
            else -> "role"
        }
    }

    when (screen) {

        "splash" -> SplashScreen {
            screen = "role"
        }

        "role" -> RoleSelectionScreen(
            onCitizen = { screen = "home" },
            onGovernment = { screen = "login" }
        )

        "login" -> LoginScreen(
            onLogin = { screen = "home" },
            onRegister = { screen = "register" }
        )

        "register" -> RegisterScreen(
            onRegister = { screen = "home" },
            onBack = { screen = "login" }
        )

        "home" -> HomeScreen(
            onManualReport = { screen = "manual" },
            onStartTrip = { screen = "trip" }
        )

        "manual" -> ManualReportScreen(
            activity = activity,
            onBack = { screen = "home" }
        )

        "trip" -> TripScreen(
            activity = activity,
            onBack = { screen = "home" }
        )
    }
}

@Composable
fun RoleSelectionScreen(
    onCitizen: () -> Unit,
    onGovernment: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AquaBg)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "BacheTrack",
            color = AquaPrimary,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Selecciona tu tipo de acceso",
            color = AquaMuted
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AquaCard)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                Button(
                    onClick = onCitizen,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AquaPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Text("Soy ciudadano")
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onGovernment,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AquaText
                    )
                ) {
                    Text("Soy trabajador de gobierno")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Monitoreo ciudadano e institucional de infraestructura vial",
            color = AquaMuted
        )
    }
}

@Composable
fun LoginScreen(onLogin: () -> Unit, onRegister: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AquaBg)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "BacheTrack",
            color = AquaPrimary,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Acceso institucional",
            color = AquaMuted
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AquaCard)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                Text(
                    text = "Inicio de sesión",
                    color = AquaText,
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo institucional") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AquaText,
                        unfocusedTextColor = AquaText,
                        focusedBorderColor = AquaPrimary,
                        unfocusedBorderColor = AquaBorder,
                        focusedLabelColor = AquaPrimary,
                        unfocusedLabelColor = AquaMuted
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AquaText,
                        unfocusedTextColor = AquaText,
                        focusedBorderColor = AquaPrimary,
                        unfocusedBorderColor = AquaBorder,
                        focusedLabelColor = AquaPrimary,
                        unfocusedLabelColor = AquaMuted
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            message = "Ingrese correo y contraseña"
                            return@Button
                        }

                        message = "Validando acceso..."

                        thread {
                            try {
                                val client = OkHttpClient()

                                val json = JSONObject()
                                json.put("email", email)
                                json.put("password", password)

                                val body = json.toString()
                                    .toRequestBody("application/json".toMediaType())

                                val request = Request.Builder()
                                    .url("$BASE_URL/login")
                                    .post(body)
                                    .build()

                                client.newCall(request).execute().use { response ->
                                    if (response.isSuccessful) {
                                        message = "Acceso autorizado"
                                        onLogin()
                                    } else {
                                        message = "Credenciales incorrectas"
                                    }
                                }

                            } catch (e: Exception) {
                                e.printStackTrace()
                                message = "No se pudo conectar al servidor"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AquaPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Text("Iniciar sesión")
                }

                TextButton(onClick = onRegister) {
                    Text("Crear cuenta", color = AquaPrimary)
                }

                if (message.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(message, color = AquaYellow)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Uso exclusivo para personal autorizado",
            color = AquaMuted
        )
    }
}

@Composable
fun RegisterScreen(onRegister: () -> Unit, onBack: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AquaBg)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "BacheTrack",
            color = AquaPrimary,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Registro institucional",
            color = AquaMuted
        )

        Spacer(modifier = Modifier.height(28.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AquaCard)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                Text(
                    text = "Crear cuenta de trabajador",
                    color = AquaText,
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo institucional") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AquaText,
                        unfocusedTextColor = AquaText,
                        focusedBorderColor = AquaPrimary,
                        unfocusedBorderColor = AquaBorder,
                        focusedLabelColor = AquaPrimary,
                        unfocusedLabelColor = AquaMuted
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AquaText,
                        unfocusedTextColor = AquaText,
                        focusedBorderColor = AquaPrimary,
                        unfocusedBorderColor = AquaBorder,
                        focusedLabelColor = AquaPrimary,
                        unfocusedLabelColor = AquaMuted
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirmar contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AquaText,
                        unfocusedTextColor = AquaText,
                        focusedBorderColor = AquaPrimary,
                        unfocusedBorderColor = AquaBorder,
                        focusedLabelColor = AquaPrimary,
                        unfocusedLabelColor = AquaMuted
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                            message = "Complete todos los campos"
                            return@Button
                        }

                        if (password != confirmPassword) {
                            message = "Las contraseñas no coinciden"
                            return@Button
                        }

                        message = "Registrando trabajador..."

                        thread {
                            try {
                                val client = OkHttpClient()

                                val json = JSONObject()
                                json.put("email", email)
                                json.put("password", password)

                                val body = json.toString()
                                    .toRequestBody("application/json".toMediaType())

                                val request = Request.Builder()
                                    .url("$BASE_URL/register")
                                    .post(body)
                                    .build()

                                client.newCall(request).execute().use { response ->
                                    message = if (response.isSuccessful) {
                                        "Registro exitoso"
                                    } else {
                                        "No se pudo registrar. El correo puede existir."
                                    }

                                    if (response.isSuccessful) {
                                        onRegister()
                                    }
                                }

                            } catch (e: Exception) {
                                e.printStackTrace()
                                message = "No se pudo conectar al servidor"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AquaPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Text("Registrar trabajador")
                }

                TextButton(onClick = onBack) {
                    Text("Volver al inicio de sesión", color = AquaPrimary)
                }

                if (message.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(message, color = AquaYellow)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Uso exclusivo para personal autorizado del municipio",
            color = AquaMuted
        )
    }
}

@Composable
fun HomeScreen(
    onStartTrip: () -> Unit,
    onManualReport: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AquaBg)
            .padding(24.dp)
    ) {

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "BacheTrack",
            color = AquaPrimary,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Panel Ciudadano",
            color = AquaMuted
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = AquaCard
            ),
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                Text(
                    text = "Estado del Sistema",
                    color = AquaText
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "● Conectado",
                    color = AquaPrimary
                )

            }

        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onStartTrip,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = AquaPrimary,
                contentColor = Color.White
            )
        ) {
            Text("Iniciar Monitoreo")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onManualReport,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = AquaText
            )
        ) {
            Text("Reportar Incidencia")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = AquaCard
            ),
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                Text(
                    "Sistema BacheTrack",
                    color = AquaText
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Detección ciudadana y monitoreo inteligente de infraestructura urbana.",
                    color = AquaMuted
                )

            }

        }
    }
}
@Composable
fun ManualReportScreen(
    activity: ComponentActivity,
    onBack: () -> Unit
) {
    var description by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(activity)
    }

    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoUri = tempPhotoUri
            message = "Foto capturada correctamente"
        } else {
            message = "No se capturó la foto"
        }
    }

    LaunchedEffect(Unit) {
        if (
            ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                100
            )
            message = "Permiso de ubicación requerido"
        } else {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    latitude = location.latitude
                    longitude = location.longitude
                    message = "Ubicación detectada automáticamente"
                } else {
                    message = "Buscando ubicación..."
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AquaBg)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Reporte Manual",
            color = AquaPrimary,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {

                if (
                    ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(Manifest.permission.CAMERA),
                        200
                    )
                    message = "Permiso de cámara requerido"
                    return@Button
                }

                val photoFile = File(
                    context.cacheDir,
                    "reporte_${System.currentTimeMillis()}.jpg"
                )

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    photoFile
                )

                tempPhotoUri = uri
                cameraLauncher.launch(uri)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = AquaPrimary,
                contentColor = Color.White
            )
        ) {
            Text("Tomar Foto")
        }

        Spacer(modifier = Modifier.height(12.dp))

        photoUri?.let { uri ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                colors = CardDefaults.cardColors(containerColor = AquaCard)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = uri
                    ),
                    contentDescription = "Foto del reporte",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AquaCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Ubicación", color = AquaText)
                Text("Latitud: ${latitude ?: "Detectando..."}", color = AquaMuted)
                Text("Longitud: ${longitude ?: "Detectando..."}", color = AquaMuted)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Descripción") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = AquaText,
                unfocusedTextColor = AquaText,
                focusedBorderColor = AquaPrimary,
                unfocusedBorderColor = AquaBorder,
                focusedLabelColor = AquaPrimary,
                unfocusedLabelColor = AquaMuted
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                message = "Enviando reporte..."

                thread {
                    try {
                        runBlocking {

                            // 1. Subir foto a Supabase Storage
                            var imageUrl: String? = null

                            photoUri?.let { uri ->

                                val inputStream =
                                    context.contentResolver.openInputStream(uri)

                                val bytes = inputStream?.readBytes()

                                inputStream?.close()

                                if (bytes != null) {

                                    val fileName =
                                        "reporte_${System.currentTimeMillis()}.jpg"

                                    SupabaseManager.client.storage
                                        .from("report-images")
                                        .upload(fileName, bytes)

                                    imageUrl = SupabaseManager.client.storage
                                        .from("report-images")
                                        .publicUrl(fileName)

                                    android.util.Log.d(
                                        "SUPABASE_MANUAL",
                                        "FOTO SUBIDA: $imageUrl"
                                    )
                                }
                            }

                            // 2. Crear reporte
                            val data = Report(
                                type = "manual",
                                description = if (description.isBlank()) {
                                    "Reporte ciudadano"
                                } else {
                                    description
                                },
                                latitude = latitude ?: 18.8467431,
                                longitude = longitude ?: -97.1305888,
                                impact = 0f,
                                speed = 0,
                                priority = 50,
                                status = "reportado",
                                confirmations = 1,
                                image_url = imageUrl
                            )

                            // 3. Guardarlo en Supabase
                            SupabaseManager.client
                                .from("reports")
                                .insert(data)

                            android.util.Log.d(
                                "SUPABASE_MANUAL",
                                "REPORTE MANUAL GUARDADO"
                            )
                        }

                        message = "Reporte enviado correctamente"
                        description = ""
                        photoUri = null

                    } catch (e: Exception) {

                        android.util.Log.e(
                            "SUPABASE_MANUAL",
                            "ERROR EN REPORTE MANUAL",
                            e
                        )

                        message = "Error al enviar reporte"
                    }
                }


            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = AquaYellow,
                contentColor = Color.White
            )
        ) {
            Text("Enviar Reporte")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(message, color = AquaMuted)

        TextButton(onClick = onBack) {
            Text("Volver", color = AquaPrimary)
        }
    }
}