package com.muuu.unshort

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.Window
import android.widget.EditText
import android.widget.LinearLayout
import com.google.android.material.button.MaterialButton

/**
 * Dialog for selecting disable type: permanent or temporary
 */
class DisableTypeDialog(
    context: Context,
    private val onPermanentDisable: () -> Unit,
    private val onTemporaryDisable: (durationMinutes: Int) -> Unit,
    private val onCancel: () -> Unit
) : Dialog(context) {

    private enum class DisableType { PERMANENT, TEMPORARY }
    private enum class TimeUnit { MINUTE, HOUR }

    private var selectedType: DisableType = DisableType.PERMANENT
    private var selectedTimeUnit: TimeUnit = TimeUnit.HOUR
    private var customTimeValue: Int = 1

    // Views
    private lateinit var optionPermanent: LinearLayout
    private lateinit var optionTemporary: LinearLayout
    private lateinit var radioPermanentOuter: View
    private lateinit var radioPermanentInner: View
    private lateinit var radioTemporaryOuter: View
    private lateinit var radioTemporaryInner: View
    private lateinit var timeSelectorContainer: LinearLayout
    private lateinit var preset30min: MaterialButton
    private lateinit var preset1hour: MaterialButton
    private lateinit var preset2hour: MaterialButton
    private lateinit var customTimeInput: EditText
    private lateinit var unitMinute: MaterialButton
    private lateinit var unitHour: MaterialButton
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnConfirm: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_disable_type)

        // Set dialog size
        val displayMetrics = context.resources.displayMetrics
        window?.setLayout(
            (displayMetrics.widthPixels * 0.9).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        window?.setDimAmount(0.5f)

        setCancelable(true)
        setCanceledOnTouchOutside(false)

        setOnCancelListener {
            onCancel()
        }

        initViews()
        setupListeners()
        updateUI()
    }

    private fun initViews() {
        optionPermanent = findViewById(R.id.optionPermanent)
        optionTemporary = findViewById(R.id.optionTemporary)
        radioPermanentOuter = findViewById(R.id.radioPermanentOuter)
        radioPermanentInner = findViewById(R.id.radioPermanentInner)
        radioTemporaryOuter = findViewById(R.id.radioTemporaryOuter)
        radioTemporaryInner = findViewById(R.id.radioTemporaryInner)
        timeSelectorContainer = findViewById(R.id.timeSelectorContainer)
        preset30min = findViewById(R.id.preset30min)
        preset1hour = findViewById(R.id.preset1hour)
        preset2hour = findViewById(R.id.preset2hour)
        customTimeInput = findViewById(R.id.customTimeInput)
        unitMinute = findViewById(R.id.unitMinute)
        unitHour = findViewById(R.id.unitHour)
        btnCancel = findViewById(R.id.btnCancel)
        btnConfirm = findViewById(R.id.btnConfirm)
    }

    private fun setupListeners() {
        // Option selection
        optionPermanent.setOnClickListener {
            selectedType = DisableType.PERMANENT
            updateUI()
        }

        optionTemporary.setOnClickListener {
            selectedType = DisableType.TEMPORARY
            updateUI()
        }

        // Time presets
        preset30min.setOnClickListener {
            customTimeValue = 30
            selectedTimeUnit = TimeUnit.MINUTE
            customTimeInput.setText("30")
            updateTimeUI()
        }

        preset1hour.setOnClickListener {
            customTimeValue = 1
            selectedTimeUnit = TimeUnit.HOUR
            customTimeInput.setText("1")
            updateTimeUI()
        }

        preset2hour.setOnClickListener {
            customTimeValue = 2
            selectedTimeUnit = TimeUnit.HOUR
            customTimeInput.setText("2")
            updateTimeUI()
        }

        // Time unit selection
        unitMinute.setOnClickListener {
            selectedTimeUnit = TimeUnit.MINUTE
            updateTimeUI()
        }

        unitHour.setOnClickListener {
            selectedTimeUnit = TimeUnit.HOUR
            updateTimeUI()
        }

        // Custom time input
        customTimeInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val value = s?.toString()?.toIntOrNull() ?: 0
                customTimeValue = value.coerceIn(1, 999)
                updatePresetButtonStates()
            }
        })

        // Buttons
        btnCancel.setOnClickListener {
            onCancel()
            dismiss()
        }

        btnConfirm.setOnClickListener {
            when (selectedType) {
                DisableType.PERMANENT -> {
                    onPermanentDisable()
                }
                DisableType.TEMPORARY -> {
                    val durationMinutes = when (selectedTimeUnit) {
                        TimeUnit.MINUTE -> customTimeValue
                        TimeUnit.HOUR -> customTimeValue * 60
                    }
                    // Limit to 24 hours max
                    val clampedDuration = durationMinutes.coerceIn(1, 24 * 60)
                    onTemporaryDisable(clampedDuration)
                }
            }
            dismiss()
        }
    }

    private fun updateUI() {
        // Update option card states
        optionPermanent.isSelected = selectedType == DisableType.PERMANENT
        optionTemporary.isSelected = selectedType == DisableType.TEMPORARY

        // Update radio buttons
        updateRadioButton(
            radioPermanentOuter,
            radioPermanentInner,
            selectedType == DisableType.PERMANENT
        )
        updateRadioButton(
            radioTemporaryOuter,
            radioTemporaryInner,
            selectedType == DisableType.TEMPORARY
        )

        // Show/hide time selector
        timeSelectorContainer.visibility = if (selectedType == DisableType.TEMPORARY) {
            View.VISIBLE
        } else {
            View.GONE
        }

        updateTimeUI()
    }

    private fun updateRadioButton(outer: View, inner: View, selected: Boolean) {
        outer.setBackgroundResource(
            if (selected) R.drawable.radio_outer_selected
            else R.drawable.radio_outer_unselected
        )
        inner.visibility = if (selected) View.VISIBLE else View.GONE
    }

    private fun updateTimeUI() {
        // Update unit buttons
        updateUnitButton(unitMinute, selectedTimeUnit == TimeUnit.MINUTE)
        updateUnitButton(unitHour, selectedTimeUnit == TimeUnit.HOUR)

        updatePresetButtonStates()
    }

    private fun updatePresetButtonStates() {
        // Update preset button states
        val is30min = customTimeValue == 30 && selectedTimeUnit == TimeUnit.MINUTE
        val is1hour = customTimeValue == 1 && selectedTimeUnit == TimeUnit.HOUR
        val is2hour = customTimeValue == 2 && selectedTimeUnit == TimeUnit.HOUR

        updatePresetButton(preset30min, is30min)
        updatePresetButton(preset1hour, is1hour)
        updatePresetButton(preset2hour, is2hour)
    }

    private fun updatePresetButton(button: MaterialButton, selected: Boolean) {
        if (selected) {
            button.setBackgroundColor(context.getColor(R.color.primary_dark))
            button.setTextColor(context.getColor(R.color.white))
            button.strokeColor = android.content.res.ColorStateList.valueOf(
                context.getColor(R.color.primary_dark)
            )
        } else {
            button.setBackgroundColor(context.getColor(android.R.color.transparent))
            button.setTextColor(context.getColor(R.color.gray_700))
            button.strokeColor = android.content.res.ColorStateList.valueOf(
                context.getColor(R.color.gray_300)
            )
        }
    }

    private fun updateUnitButton(button: MaterialButton, selected: Boolean) {
        if (selected) {
            button.setBackgroundColor(context.getColor(R.color.primary_dark))
            button.setTextColor(context.getColor(R.color.white))
            button.strokeColor = android.content.res.ColorStateList.valueOf(
                context.getColor(R.color.primary_dark)
            )
        } else {
            button.setBackgroundColor(context.getColor(android.R.color.transparent))
            button.setTextColor(context.getColor(R.color.gray_600))
            button.strokeColor = android.content.res.ColorStateList.valueOf(
                context.getColor(R.color.gray_300)
            )
        }
    }
}
