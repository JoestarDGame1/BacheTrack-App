package com.smartpavementguard.app.ui

import com.smartpavementguard.app.LocalReport
import com.smartpavementguard.app.DatabaseProvider
import com.smartpavementguard.app.NetworkUtils
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import android.os.Looper
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
    var lastRotationMagnitude by remember { mutableStateOf(0f) }
    var vehicleSpeedKmH by remember { mutableStateOf(0f) }

    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(activity)
    }

    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return
                latitude = location.latitude
                longitude = location.longitude
                if (location.hasSpeed()) {
                    vehicleSpeedKmH = location.speed * 3.6f
                }
                status = if (vehicleSpeedKmH > 5f) {
                    "GPS activo (%.1f km/h)".format(vehicleSpeedKmH)
                } else {
                    "GPS activo - monitoreando"
                }
            }
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
            status = "Permiso de ubicación requerido"
        } else {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    latitude = location.latitude
                    longitude = location.longitude
                    if (location.hasSpeed()) {
                        vehicleSpeedKmH = location.speed * 3.6f
                    }
                    status = "GPS activo - monitoreando"
                } else {
                    status = "GPS buscando ubicación"
                }
            }
        }
    }

    DisposableEffect(Unit) {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setMinUpdateIntervalMillis(500)
                .build()
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    val anomalies = remember { mutableStateListOf<Float>() }

    // Variables para filtro estadístico de terreno irregular / camino rocoso
    val recentAccels = remember { FloatArray(50) }
    var accelIndex by remember { mutableIntStateOf(0) }
    var isBufferFull by remember { mutableStateOf(false) }
    val recentImpactsTimestamps = remember { mutableStateListOf<Long>() }

    DisposableEffect(Unit) {
        val sensorManager =
            context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        val linearAccel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        val listener = object : SensorEventListener {

            override fun onSensorChanged(event: SensorEvent) {
                val now = System.currentTimeMillis()

                if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
                    val gx = event.values[0]
                    val gy = event.values[1]
                    val gz = event.values[2]
                    lastRotationMagnitude = sqrt(gx * gx + gy * gy + gz * gz)
                }

                if (event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION ||
                    (linearAccel == null && event.sensor.type == Sensor.TYPE_ACCELEROMETER)
                ) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    val magnitude = if (event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {
                        sqrt(x * x + y * y + z * z)
                    } else {
                        val raw = sqrt(x * x + y * y + z * z)
                        kotlin.math.abs(raw - 9.81f)
                    }

                    lastIntensity = magnitude

                    // 1. Mantenimiento del buffer circular para promedio y desviación estándar
                    recentAccels[accelIndex] = magnitude
                    accelIndex = (accelIndex + 1) % recentAccels.size
                    if (accelIndex == 0) isBufferFull = true

                    val sampleSize = if (isBufferFull) recentAccels.size else kotlin.math.max(1, accelIndex)
                    var sum = 0f
                    for (i in 0 until sampleSize) sum += recentAccels[i]
                    val mean = sum / sampleSize

                    var varianceSum = 0f
                    for (i in 0 until sampleSize) {
                        val diff = recentAccels[i] - mean
                        varianceSum += diff * diff
                    }
                    val stdDev = sqrt(varianceSum / sampleSize)

                    // 2. Limpieza de marcas de tiempo de impactos antiguos (> 4 segundos)
                    recentImpactsTimestamps.removeAll { now - it > 4000 }

                    // 3. Cálculo de Puntuación Z (Z-Score)
                    val zScore = if (stdDev > 0.1f) (magnitude - mean) / stdDev else 0f

                    // Criterios anti falsos positivos:
                    val isStrongImpact = magnitude > 12.0f
                    val isLowRotation = lastRotationMagnitude < 2.5f
                    val isAnomalousPeak = zScore > 3.0f || magnitude > 22.0f
                    val isCoolingTimePassed = (now - lastImpactTime > 2500)

                    // Si hay 3 o más sacudidas fuertes en los últimos 4 segundos, es un camino rocoso/empedrado
                    val isRockyRoad = recentImpactsTimestamps.size >= 3

                    if (isStrongImpact && isLowRotation) {
                        recentImpactsTimestamps.add(now)

                        if (!isRockyRoad && isAnomalousPeak && isCoolingTimePassed) {
                            lastImpactTime = now
                            anomalies.add(magnitude)
                            status = "Bache detectado (%.1f m/s²)".format(magnitude)
                        } else if (isRockyRoad) {
                            status = "Terreno irregular / Camino rocoso"
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (linearAccel != null) {
            sensorManager.registerListener(
                listener,
                linearAccel,
                SensorManager.SENSOR_DELAY_GAME
            )
        } else if (accelerometer != null) {
            sensorManager.registerListener(
                listener,
                accelerometer,
                SensorManager.SENSOR_DELAY_GAME
            )
        }

        if (gyroscope != null) {
            sensorManager.registerListener(
                listener,
                gyroscope,
                SensorManager.SENSOR_DELAY_GAME
            )
        }

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

                Text("Aceleración Lineal (m/s²)", color = AquaText)
                Text(
                    "%.2f".format(lastIntensity),
                    color = AquaMuted
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Velocidad GPS", color = AquaText)
                Text(
                    "%.1f km/h".format(vehicleSpeedKmH),
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
            text = "Filtro dinámico adaptativo: distingue baches aislados de caminos empedrados o rocosos.",
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

                                        val currentSpeed = vehicleSpeedKmH.toInt()

                                        if (!NetworkUtils.isInternetAvailable(context)) {

                                            val data = Report(
                                                type = "automatico",
                                                description = "Bache detectado automáticamente",
                                                latitude = latitude ?: 18.8467431,
                                                longitude = longitude ?: -97.1305888,
                                                impact = impactValue,
                                                speed = currentSpeed,
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
                                                speed = currentSpeed,
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
