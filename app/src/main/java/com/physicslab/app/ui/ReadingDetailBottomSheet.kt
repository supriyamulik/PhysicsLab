package com.physicslab.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.physicslab.app.R
import com.physicslab.app.data.Quantity
import com.physicslab.app.data.SensorReading
import android.widget.TextView
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/**
 * A bottom sheet displaying full details of a [SensorReading], including:
 * - Name and full date/time
 * - Hero value with unit
 * - X/Y/Z components (if applicable)
 * - Sensor name, accuracy, sample count
 * - A short physical interpretation line (conservative, factual)
 *
 * The caller provides callbacks for Rename and Delete actions so that
 * all data mutations stay in [QuantityViewModel].
 */
class ReadingDetailBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_READING_ID = "reading_id"
        private const val ARG_NAME = "name"
        private const val ARG_TIMESTAMP = "timestamp"
        private const val ARG_QUANTITY = "quantity"
        private const val ARG_VALUE = "value"
        private const val ARG_X = "x"
        private const val ARG_Y = "y"
        private const val ARG_Z = "z"
        private const val ARG_SENSOR_NAME = "sensor_name"
        private const val ARG_ACCURACY = "accuracy"
        private const val ARG_SAMPLE_COUNT = "sample_count"
        private const val TAG = "reading_detail"

        fun newInstance(reading: SensorReading): ReadingDetailBottomSheet {
            return ReadingDetailBottomSheet().apply {
                arguments = Bundle().apply {
                    putLong(ARG_READING_ID, reading.id)
                    putString(ARG_NAME, reading.name)
                    putLong(ARG_TIMESTAMP, reading.timestamp)
                    putString(ARG_QUANTITY, reading.quantity)
                    putDouble(ARG_VALUE, reading.value)
                    reading.x?.let { putDouble(ARG_X, it) }
                    reading.y?.let { putDouble(ARG_Y, it) }
                    reading.z?.let { putDouble(ARG_Z, it) }
                    putString(ARG_SENSOR_NAME, reading.sensorName)
                    reading.accuracy?.let { putInt(ARG_ACCURACY, it) }
                    putInt(ARG_SAMPLE_COUNT, reading.sampleCount)
                }
            }
        }

        fun show(
            reading: SensorReading,
            fragmentManager: androidx.fragment.app.FragmentManager,
            onRename: (SensorReading) -> Unit,
            onDelete: (SensorReading) -> Unit
        ): ReadingDetailBottomSheet {
            val sheet = newInstance(reading)
            sheet.onRenameCallback = onRename
            sheet.onDeleteCallback = onDelete
            sheet.show(fragmentManager, TAG)
            return sheet
        }
    }

    var onRenameCallback: ((SensorReading) -> Unit)? = null
    var onDeleteCallback: ((SensorReading) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_reading_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = requireArguments()
        val id = args.getLong(ARG_READING_ID)
        val name = args.getString(ARG_NAME, "")
        val timestamp = args.getLong(ARG_TIMESTAMP)
        val quantityStr = args.getString(ARG_QUANTITY, "")
        val value = args.getDouble(ARG_VALUE)
        val x = if (args.containsKey(ARG_X)) args.getDouble(ARG_X) else null
        val y = if (args.containsKey(ARG_Y)) args.getDouble(ARG_Y) else null
        val z = if (args.containsKey(ARG_Z)) args.getDouble(ARG_Z) else null
        val sensorName = args.getString(ARG_SENSOR_NAME, "Unknown sensor")
        val accuracy = if (args.containsKey(ARG_ACCURACY)) args.getInt(ARG_ACCURACY) else null
        val sampleCount = args.getInt(ARG_SAMPLE_COUNT, 0)

        val quantity = try { Quantity.valueOf(quantityStr) } catch (e: Exception) { null }
        val unit = quantity?.unit ?: ""

        // Reconstruct the reading object for callbacks
        val reading = SensorReading(
            id = id,
            quantity = quantityStr,
            name = name,
            timestamp = timestamp,
            x = x, y = y, z = z,
            value = value,
            sampleCount = sampleCount,
            sensorName = sensorName,
            accuracy = accuracy,
            updatedAt = timestamp
        )

        // Name
        view.findViewById<TextView>(R.id.tvDetailName).text = name

        // Full date/time using device locale
        val fullDateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT, Locale.getDefault())
        view.findViewById<TextView>(R.id.tvDetailTimestamp).text = fullDateFormat.format(Date(timestamp))

        // Hero value
        val formattedValue = when (quantity) {
            Quantity.LIGHT -> formatLight(value)
            Quantity.MAGNETIC_FIELD -> String.format(Locale.US, "%.1f", value)
            Quantity.ACCELERATION -> String.format(Locale.US, "%.2f", value)
            null -> String.format(Locale.US, "%.2f", value)
        }
        view.findViewById<TextView>(R.id.tvDetailHeroValue).text = "$formattedValue $unit"

        // X/Y/Z components
        val componentsLayout = view.findViewById<View>(R.id.layoutDetailComponents)
        if (x != null && y != null && z != null) {
            componentsLayout.visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.tvDetailX).text = String.format(Locale.US, "X: %.2f %s", x, unit)
            view.findViewById<TextView>(R.id.tvDetailY).text = String.format(Locale.US, "Y: %.2f %s", y, unit)
            view.findViewById<TextView>(R.id.tvDetailZ).text = String.format(Locale.US, "Z: %.2f %s", z, unit)
        } else {
            componentsLayout.visibility = View.GONE
        }

        // Sensor name
        view.findViewById<TextView>(R.id.tvDetailSensorName).text = sensorName

        // Accuracy
        val accuracyLayout = view.findViewById<View>(R.id.layoutDetailAccuracy)
        if (accuracy != null) {
            accuracyLayout.visibility = View.VISIBLE
            val accLabel = when (accuracy) {
                3 -> "High"
                2 -> "Medium"
                1 -> "Low"
                0 -> "Unreliable"
                else -> "Unknown"
            }
            view.findViewById<TextView>(R.id.tvDetailAccuracy).text = accLabel
        } else {
            accuracyLayout.visibility = View.GONE
        }

        // Sample count
        view.findViewById<TextView>(R.id.tvDetailSampleCount).text =
            if (sampleCount == 1) "1 sample" else "$sampleCount samples"

        // Physical interpretation
        val interpretation = buildInterpretation(quantity, value, x, y, z)
        view.findViewById<TextView>(R.id.tvDetailInterpretation).text = interpretation

        // Action buttons
        view.findViewById<View>(R.id.btnDetailRename).setOnClickListener {
            dismiss()
            onRenameCallback?.invoke(reading)
        }
        view.findViewById<View>(R.id.btnDetailDelete).setOnClickListener {
            dismiss()
            onDeleteCallback?.invoke(reading)
        }
    }

    private fun formatLight(value: Double): String {
        return when {
            value < 10.0   -> String.format(Locale.US, "%.2f", value)
            value < 1000.0 -> String.format(Locale.US, "%.1f", value)
            else           -> String.format(Locale.US, "%,d", value.toLong())
        }
    }

    /**
     * Returns a short, conservative, factual physical context string for this reading.
     * Does not make definitive claims; uses "consistent with", "typical for", etc.
     */
    private fun buildInterpretation(
        quantity: Quantity?,
        value: Double,
        x: Double?, y: Double?, z: Double?
    ): String {
        return when (quantity) {
            Quantity.MAGNETIC_FIELD -> buildMagneticInterpretation(value)
            Quantity.ACCELERATION   -> buildAccelerationInterpretation(value, x, y, z)
            Quantity.LIGHT          -> buildLightInterpretation(value)
            null                    -> "No interpretation available."
        }
    }

    private fun buildMagneticInterpretation(value: Double): String {
        return when {
            value < 10.0 ->
                "Very low field strength. Possible nearby magnetic shielding, or sensor not yet calibrated. " +
                "Earth's field is typically 25–65 µT at the surface."
            value in 10.0..65.0 ->
                "Within the typical range for Earth's magnetic field (25–65 µT). " +
                "Consistent with a normal indoor environment without strong local magnets."
            value in 65.0..200.0 ->
                "Moderately elevated above Earth's natural background. " +
                "Likely influenced by nearby electronic devices, metal structures, or appliances."
            else ->
                "Significantly above Earth's ambient field. A strong local magnetic source " +
                "(e.g., a speaker, motor, or magnet) is likely nearby."
        }
    }

    private fun buildAccelerationInterpretation(value: Double, x: Double?, y: Double?, z: Double?): String {
        val note = "Note: these readings include gravity (~9.81 m/s²). "
        val orientationHint = if (x != null && y != null && z != null) {
            when {
                kotlin.math.abs(z - 9.81) < 2.0 -> " Device orientation: resting flat face-up."
                kotlin.math.abs(z + 9.81) < 2.0 -> " Device orientation: resting face-down."
                kotlin.math.abs(y - 9.81) < 2.0 -> " Device orientation: held upright (portrait)."
                kotlin.math.abs(x - 9.81) < 2.0 -> " Device orientation: tilted on side."
                else -> ""
            }
        } else ""

        val magnitudeDesc = when {
            value < 1.0 ->
                "${note}Near-zero resultant suggests the phone may be in free fall, " +
                "or gravity is cancelling along all axes simultaneously."
            value in 8.0..12.0 ->
                "${note}Close to 9.81 m/s². Consistent with the device being held still or " +
                "lying flat — predominantly showing gravitational acceleration."
            value in 12.0..25.0 ->
                "${note}Above resting gravity. Indicates active motion or vibration " +
                "in addition to gravitational acceleration."
            else ->
                "${note}High resultant value. Likely caused by vigorous motion, " +
                "an impact, or being placed near a vibrating source."
        }
        return magnitudeDesc + orientationHint
    }

    private fun buildLightInterpretation(value: Double): String {
        val disclaimer = "Phone light sensors are uncalibrated and indicative only. "
        return when {
            value < 1.0   -> "${disclaimer}Very dim: consistent with a dark room or sensor covered."
            value < 50.0  -> "${disclaimer}Low light: consistent with indoor dim lighting or night conditions."
            value < 500.0 -> "${disclaimer}Moderate light: consistent with typical indoor office or room lighting."
            value < 5000.0 -> "${disclaimer}Bright: consistent with daylight near a window or bright artificial lighting."
            value < 30000.0 -> "${disclaimer}Very bright: consistent with direct sunlight or outdoor conditions."
            else            -> "${disclaimer}Extreme brightness: consistent with direct solar exposure or a very bright artificial source."
        }
    }
}
