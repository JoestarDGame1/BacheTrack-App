package com.smartpavementguard.app.ui

import com.smartpavementguard.app.LocalReport
import com.smartpavementguard.app.DatabaseProvider
import com.smartpavementguard.app.NetworkUtils
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import androidx.activity.ComponentActivity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt
import kotlin.concurrent.thread
import com.smartpavementguard.app.SupabaseManager
import kotlinx.coroutines.runBlocking
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.from
import com.smartpavementguard.app.Report

fun distanceMeters(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double
): Double {

    val r = 6371000.0

    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)

    val a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) *
                Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) *
                Math.sin(dLon / 2)

    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

    return r * c
}

@Composable
fun TripScreen(
    activity: ComponentActivity,
    onBack: () -> Unit
){
    val context = LocalContext.current

    var showDialog by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Monitoreando vibraciones") }
    var lastIntensity by remember { mutableStateOf(0f) }
    var lastImpactTime by remember { mutableStateOf(0L) }

    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(activity)
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
            status = "Permiso de ubicación requerido"
        } else {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    latitude = location.latitude
                    longitude = location.longitude
                    status = "GPS activo - monitoreando"
                } else {
                    status = "GPS buscando ubicación"
                }
            }
        }
    }


    val anomalies = remember { mutableStateListOf<Float>() }

    DisposableEffect(Unit) {
        val sensorManager =
            context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        val accelerometer =
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {

            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                val magnitude = sqrt(x * x + y * y + z * z)
                lastIntensity = magnitude

                val now = System.currentTimeMillis()

                if (magnitude > 22f && now - lastImpactTime > 2500) {
                    lastImpactTime = now
                    anomalies.add(magnitude)
                    status = "Anomalía detectada"
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(
            listener,
            accelerometer,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        onDispose {
            sensorManager.unregisterListener(listener)
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
            text = "Monitoreo Inteligente",
            color = AquaPrimary,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AquaCard)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                Text("Estado", color = AquaText)
                Text("● $status", color = AquaPrimary)

                Spacer(modifier = Modifier.height(12.dp))

                Text("Ubicación actual", color = AquaText)
                Text("Latitud: ${latitude ?: "Detectando..."}", color = AquaMuted)
                Text("Longitud: ${longitude ?: "Detectando..."}", color = AquaMuted)

                Spacer(modifier = Modifier.height(16.dp))

                Text("Intensidad actual", color = AquaText)
                Text(
                    "%.2f".format(lastIntensity),
                    color = AquaMuted
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Anomalías detectadas", color = AquaText)
                Text(
                    anomalies.size.toString(),
                    color = AquaYellow,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Simula un tope o vibración fuerte moviendo el teléfono.",
            color = AquaMuted
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (anomalies.isNotEmpty()) {
                    showDialog = true
                } else {
                    onBack()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = AquaPrimary,
                contentColor = Color.Black
            )
        ) {
            Text("Finalizar Viaje")
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text("Anomalías detectadas")
            },
            text = {
                Text(
                    "Durante el recorrido se detectaron ${anomalies.size} posibles anomalías.\n\n¿Deseas enviarlas como reportes?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false

                        thread {
                            try {
                                android.util.Log.d("SUPABASE", "ACTIVO")

                                runBlocking {
                                    val db = DatabaseProvider.getDatabase(context)
                                    val dao = db.reportDao()

                                    android.util.Log.d(
                                        "DEBUG_AQUAROAD",
                                        "ENTRO AL FOREACH"
                                    )

                                    anomalies.forEach { impactValue ->

                                        android.util.Log.d(
                                            "DEBUG_AQUAROAD",
                                            "INTERNET = ${NetworkUtils.isInternetAvailable(context)}"
                                        )

                                        if (!NetworkUtils.isInternetAvailable(context)) {

                                            val data = Report(
                                                type = "automatico",
                                                description = "Bache detectado automáticamente",
                                                latitude = latitude ?: 18.8467431,
                                                longitude = longitude ?: -97.1305888,
                                                impact = impactValue,
                                                speed = 0,
                                                priority = 70,
                                                status = "reportado",
                                                confirmations = 1
                                            )

                                            dao.insert(
                                                LocalReport(
                                                    type = data.type,
                                                    description = data.description,
                                                    latitude = data.latitude,
                                                    longitude = data.longitude,
                                                    impact = data.impact ?: 0f,
                                                    speed = data.speed ?: 0,
                                                    priority = data.priority,
                                                    status = data.status
                                                )
                                            )

                                            android.util.Log.d(
                                                "OFFLINE",
                                                "REPORTE GUARDADO LOCALMENTE"
                                            )

                                            return@forEach
                                        }

                                        val reports = SupabaseManager.client
                                            .from("reports")
                                            .select()
                                            .decodeList<Report>()

                                        val existingReport = reports.find {
                                            distanceMeters(
                                                latitude ?: 0.0,
                                                longitude ?: 0.0,
                                                it.latitude,
                                                it.longitude
                                            ) < 20
                                        }

                                        if (existingReport != null && existingReport.id != null) {

                                            val newConfirmations =
                                                (existingReport.confirmations ?: 1) + 1

                                            var verified = false
                                            var category = "bache"

                                            if (newConfirmations >= 3) {
                                                verified = true
                                            }

                                            if (newConfirmations >= 5) {
                                                category = "tope"
                                            }

                                            SupabaseManager.client
                                                .from("reports")
                                                .update(
                                                    {
                                                        set("confirmations", newConfirmations)
                                                        set("verified", verified)
                                                        set("category", category)
                                                    }
                                                ) {
                                                    filter {
                                                        eq("id", existingReport.id)
                                                    }
                                                }

                                            android.util.Log.d(
                                                "SUPABASE",
                                                "CONFIRMACION AGREGADA"
                                            )

                                        } else {

                                            val data = Report(
                                                type = "automatico",
                                                description = "Bache detectado automáticamente",
                                                latitude = latitude ?: 18.8467431,
                                                longitude = longitude ?: -97.1305888,
                                                impact = impactValue,
                                                speed = 0,
                                                priority = 70,
                                                status = "reportado",
                                                confirmations = 1
                                            )

                                            if (NetworkUtils.isInternetAvailable(context)) {

                                                SupabaseManager.client
                                                    .from("reports")
                                                    .insert(data)

                                                android.util.Log.d(
                                                    "SUPABASE",
                                                    "REPORTE NUEVO"
                                                )

                                            } else {

                                                dao.insert(
                                                    LocalReport(
                                                        type = data.type,
                                                        description = data.description,
                                                        latitude = data.latitude,
                                                        longitude = data.longitude,
                                                        impact = data.impact ?: 0f,
                                                        speed = data.speed ?: 0,
                                                        priority = data.priority,
                                                        status = data.status
                                                    )
                                                )

                                                android.util.Log.d(
                                                    "OFFLINE",
                                                    "REPORTE GUARDADO LOCALMENTE"
                                                )
                                            }
                                        }
                                    }
                                }

                            } catch (e: Exception) {
                                android.util.Log.e(
                                    "SUPABASE",
                                    "ERROR INSERTANDO",
                                    e
                                )
                            }
                        }

                        onBack()
                    }
                ) {
                    Text("Sí, enviar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        anomalies.clear()
                        showDialog = false
                        onBack()
                    }
                ) {
                    Text("No, descartar")
                }
            }
        )
    }
}