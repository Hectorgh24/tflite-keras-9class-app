package com.empresa.aplicaciontensorflowliteandkeras

import android.content.Context
import org.json.JSONObject
import android.util.Log // Asegúrate de importar esto
class DataPreprocessor(context: Context) {

    private var means: FloatArray = FloatArray(453)
    private var stds: FloatArray = FloatArray(453)

    companion object {
        private const val SCALER_FILE = "scaler_9_clases.json"
        private const val EXPECTED_FEATURES = 453
    }

    init {
        loadScalerConfig(context)
    }

    private fun loadScalerConfig(context: Context) {
        try {
            // Leer el archivo desde assets
            val jsonString = context.assets.open(SCALER_FILE).bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val scaler = jsonObject.getJSONObject("scaler") // Acceder al objeto 'scaler'

            val jsonMeans = scaler.getJSONArray("mean")
            val jsonScales = scaler.getJSONArray("scale")

            for (i in 0 until EXPECTED_FEATURES) {
                means[i] = jsonMeans.getDouble(i).toFloat()
                stds[i] = jsonScales.getDouble(i).toFloat()
            }
        } catch (e: Exception) {
            // El tercer parámetro 'e' imprime toda la traza del error para copiar y pegar
            Log.e("DataPreprocessor", "Error crítico al leer scaler_9_clases.json desde assets", e)
        }
    }

    fun standardizeInPlace(rawData: FloatArray) {
        if (rawData.size != EXPECTED_FEATURES) return

        for (i in rawData.indices) {
            val divisor = if (stds[i] == 0f) 1f else stds[i]
            rawData[i] = (rawData[i] - means[i]) / divisor
        }
    }
}