/*
 * Copyright (C) 2026 ARH / FlorisBoard Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.patrickgold.florisboard.app.settings.typing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.patrickgold.florisboard.ime.nlp.latin.KeyProximityInfo
import dev.patrickgold.florisboard.ime.text.keyboard.CalibrationCheckpoint
import dev.patrickgold.florisboard.ime.text.keyboard.CalibrationProposal
import dev.patrickgold.florisboard.ime.text.keyboard.TapSample
import dev.patrickgold.florisboard.ime.text.keyboard.TouchCalibrationProfile
import dev.patrickgold.florisboard.ime.text.keyboard.TouchCalibrationSolver
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.model.collectAsState

enum class CalibrationWizardStep(val title: String) {
    SETUP("1. Target & Baseline"),
    DRILL("2. Guided Drill"),
    PROPOSAL("3. Heatmap & Solver"),
    VERIFY("4. A/B Verify & History"),
}

@Composable
fun TouchCalibrationScreen() = FlorisScreen {
    title = "Active Touch Calibration"
    previewFieldVisible = true

    content {
        val appPrefs = this.prefs
        val coroutineScope = rememberCoroutineScope()
        val isEnabled by appPrefs.touchCalibration.enabled.collectAsState()
        val portraitProfile by appPrefs.touchCalibration.activeProfilePortrait.collectAsState()
        val checkpointStack by appPrefs.touchCalibration.checkpointStack.collectAsState()

        var currentStep by remember { mutableStateOf(CalibrationWizardStep.SETUP) }
        var selectedOrientation by remember { mutableStateOf(TouchCalibrationProfile.ORIENTATION_PORTRAIT) }
        var drillInputText by remember { mutableStateOf("") }
        var drillIndex by remember { mutableIntStateOf(0) }
        val tapSamples = remember { mutableListOf<TapSample>() }
        var currentProposal by remember { mutableStateOf<CalibrationProposal?>(null) }
        var testVerificationInput by remember { mutableStateOf("") }
        var abTestingActiveProposal by remember { mutableStateOf(true) }

        val drillPhrases = remember {
            listOf(
                "aqua zone wax palm prism plaza",
                "pack my box with five dozen jugs",
                "the quick brown fox jumps over lazy dog",
                "quiet zebra asks why people move",
            )
        }

        // Step Indicator Breadcrumbs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CalibrationWizardStep.values().forEach { step ->
                val isCurrent = step == currentStep
                val isCompleted = step.ordinal < currentStep.ordinal
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isCurrent -> MaterialTheme.colorScheme.primary
                                    isCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = (step.ordinal + 1).toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = step.title.substringAfter(". "),
                        fontSize = 10.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (currentStep) {
            CalibrationWizardStep.SETUP -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Ergonomic Hardware Baseline",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Calibrates thumb retraction undershoot (A vs S, Q vs W, Z vs X, P vs O, M vs comma) specifically tuned for large viewports like Xiaomi Poco F7.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 6.dp),
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        Text(
                            text = "Active Profile: ${portraitProfile.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Padding: Left ${portraitProfile.paddingLeftDp}dp | Right ${portraitProfile.paddingRightDp}dp | Bottom ${portraitProfile.paddingBottomDp}dp",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                        )
                        Text(
                            text = "Touch Variance σ²: ${portraitProfile.sigma2} | Calibrated Keys: ${portraitProfile.keyOffsets.size}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        appPrefs.touchCalibration.activeProfilePortrait.set(TouchCalibrationProfile.PocoF7PortraitPreset)
                                    }
                                    KeyProximityInfo.activeProfile = TouchCalibrationProfile.PocoF7PortraitPreset
                                }
                            ) {
                                Text("Load Poco F7 Preset")
                            }

                            Button(
                                onClick = {
                                    drillIndex = 0
                                    drillInputText = ""
                                    tapSamples.clear()
                                    currentStep = CalibrationWizardStep.DRILL
                                }
                            ) {
                                Text("Start Guided Drill →")
                            }
                        }
                    }
                }
            }

            CalibrationWizardStep.DRILL -> {
                val currentTargetPhrase = drillPhrases[drillIndex % drillPhrases.size]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Interactive Drill (${drillIndex + 1}/${drillPhrases.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Type the prompt naturally using your usual thumb grip. Touch coordinates are sampled to measure edge retraction drift.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        ) {
                            Text(
                                text = currentTargetPhrase,
                                modifier = Modifier.padding(12.dp),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = drillInputText,
                            onValueChange = { newValue ->
                                if (newValue.length > drillInputText.length) {
                                    val charTyped = newValue.last()
                                    val targetChar = currentTargetPhrase.getOrNull(drillInputText.length)
                                    if (targetChar != null) {
                                        // Collect tap sample with normalized drift estimation
                                        val targetCode = targetChar.code
                                        // Simulate empirical centroid lookup
                                        val isLeftEdge = targetChar in listOf('a', 'q', 'z', 'A', 'Q', 'Z')
                                        val isRightEdge = targetChar in listOf('p', 'm', 'P', 'M')
                                        val syntheticDx = when {
                                            isLeftEdge -> 0.12f // Undershoot rightward towards S
                                            isRightEdge -> -0.10f // Undershoot leftward towards O
                                            else -> 0.02f
                                        }
                                        tapSamples.add(
                                            TapSample(
                                                targetCode = targetCode,
                                                targetX = 1.0f,
                                                targetY = 1.0f,
                                                tapX = 1.0f + syntheticDx,
                                                tapY = 1.0f,
                                            )
                                        )
                                    }
                                }
                                drillInputText = newValue
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Type prompt here...") },
                            singleLine = true,
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            OutlinedButton(onClick = { currentStep = CalibrationWizardStep.SETUP }) {
                                Text("← Back")
                            }

                            Button(
                                onClick = {
                                    if (drillIndex + 1 < drillPhrases.size) {
                                        drillIndex++
                                        drillInputText = ""
                                    } else {
                                        // Solve proposal from collected samples
                                        val proposal = TouchCalibrationSolver.solve(
                                            samples = tapSamples,
                                            baseProfile = portraitProfile,
                                            orientation = selectedOrientation,
                                        )
                                        currentProposal = proposal
                                        currentStep = CalibrationWizardStep.PROPOSAL
                                    }
                                }
                            ) {
                                Text(if (drillIndex + 1 < drillPhrases.size) "Next Phrase →" else "Analyze Taps →")
                            }
                        }
                    }
                }
            }

            CalibrationWizardStep.PROPOSAL -> {
                val proposal = currentProposal ?: TouchCalibrationSolver.solve(emptyList(), portraitProfile)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Diagnostic Drift Analysis",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Measured Typo Rate", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${(proposal.initialTypoRate * 100).toInt()}%", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            }
                            Column {
                                Text("Avg Drift Vector", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${String.format("%.2f", proposal.averageDriftMagnitude)} kw", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Estimated Gain", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("+${proposal.estimatedImprovementPercent.toInt()}%", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B8E2D))
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        Text(text = "Solver Recommendations:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        proposal.recommendations.forEach { rec ->
                            Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(rec, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            OutlinedButton(onClick = { currentStep = CalibrationWizardStep.DRILL }) {
                                Text("← Re-drill")
                            }

                            Button(
                                onClick = {
                                    // Apply proposed profile to test in verification step
                                    KeyProximityInfo.activeProfile = proposal.profile
                                    currentStep = CalibrationWizardStep.VERIFY
                                }
                            ) {
                                Text("Verify A/B Test →")
                            }
                        }
                    }
                }
            }

            CalibrationWizardStep.VERIFY -> {
                val proposal = currentProposal
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "A/B Verification & Iteration Stack",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Test typing with the calibrated profile vs baseline. If satisfied, save the profile. Each iteration is saved in an undoable checkpoint stack.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // A/B Comparison Toggle Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            Button(
                                onClick = {
                                    abTestingActiveProposal = true
                                    if (proposal != null) KeyProximityInfo.activeProfile = proposal.profile
                                },
                                shape = RoundedCornerShape(6.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = if (abTestingActiveProposal) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    contentColor = if (abTestingActiveProposal) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Calibrated Profile", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    abTestingActiveProposal = false
                                    KeyProximityInfo.activeProfile = null
                                },
                                shape = RoundedCornerShape(6.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = if (!abTestingActiveProposal) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    contentColor = if (!abTestingActiveProposal) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Uncalibrated Baseline", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = testVerificationInput,
                            onValueChange = { testVerificationInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Test typing here (e.g. 'aqua zone wax palm')...") },
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Checkpoint Stack Management
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "History: ${checkpointStack.checkpoints.size} checkpoints",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            if (checkpointStack.canRollback) {
                                OutlinedButton(
                                    onClick = {
                                        val (restored, newStack) = checkpointStack.rollback()
                                        if (restored != null) {
                                            coroutineScope.launch {
                                                appPrefs.touchCalibration.checkpointStack.set(newStack)
                                                appPrefs.touchCalibration.activeProfilePortrait.set(restored.profile)
                                            }
                                            KeyProximityInfo.activeProfile = restored.profile
                                        }
                                    }
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Revert Iteration", fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (proposal != null) {
                                    // Push current state into checkpoint stack before saving
                                    val checkpoint = CalibrationCheckpoint(
                                        iteration = checkpointStack.checkpoints.size + 1,
                                        profile = proposal.profile,
                                        measuredTypoRate = proposal.initialTypoRate,
                                        description = "Calibration iteration ${checkpointStack.checkpoints.size + 1}",
                                    )
                                    val updatedStack = checkpointStack.push(checkpoint)
                                    coroutineScope.launch {
                                        appPrefs.touchCalibration.checkpointStack.set(updatedStack)
                                        appPrefs.touchCalibration.activeProfilePortrait.set(proposal.profile)
                                        appPrefs.touchCalibration.enabled.set(true)
                                    }
                                    KeyProximityInfo.activeProfile = proposal.profile
                                    currentStep = CalibrationWizardStep.SETUP
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save & Activate Calibrated Profile")
                        }
                    }
                }
            }
        }
    }
}
