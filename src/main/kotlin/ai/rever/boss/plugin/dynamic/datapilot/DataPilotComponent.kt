package ai.rever.boss.plugin.dynamic.datapilot

import ai.rever.boss.plugin.api.PanelComponentWithUI
import ai.rever.boss.plugin.api.PanelInfo
import ai.rever.boss.plugin.ui.BossTheme
import ai.rever.boss.plugin.ui.BossThemeColors
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ComponentContext
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

class DataPilotComponent(
    ctx: ComponentContext,
    override val panelInfo: PanelInfo,
) : PanelComponentWithUI, ComponentContext by ctx {

    @Composable
    override fun Content() {

        var profile by remember {
            mutableStateOf<DatasetProfile?>(null)
        }

        var error by remember {
            mutableStateOf<String?>(null)
        }

        BossTheme {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(
                            rememberScrollState()
                        ),
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    // --------------------------------------------------
                    // HEADER
                    // --------------------------------------------------

                    Text(
                        text = "🚀 DataPilot",
                        color = BossThemeColors.TextPrimary
                    )

                    Text(
                        text = "Dataset Intelligence for BOSS",
                        color = BossThemeColors.TextSecondary
                    )

                    Divider()

                    // --------------------------------------------------
                    // FILE SELECTOR
                    // --------------------------------------------------

                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {

                        Button(
                            onClick = {

                                error = null

                                try {

                                    val chooser =
                                        JFileChooser()

                                    chooser.dialogTitle =
                                        "Select a CSV Dataset"

                                    chooser.fileFilter =
                                        FileNameExtensionFilter(
                                            "CSV files (*.csv)",
                                            "csv"
                                        )

                                    val result =
                                        chooser.showOpenDialog(
                                            null
                                        )

                                    if (
                                        result ==
                                        JFileChooser.APPROVE_OPTION
                                    ) {

                                        val selectedFile =
                                            chooser.selectedFile

                                        profile =
                                            DataPilotAnalyzer
                                                .analyze(
                                                    selectedFile
                                                )
                                    }

                                } catch (e: Exception) {

                                    error =
                                        e.message
                                            ?: "Unable to analyze CSV"
                                }
                            }
                        ) {

                            Text(
                                text =
                                    "📂 Select & Analyze CSV"
                            )
                        }
                    }

                    // --------------------------------------------------
                    // ERROR
                    // --------------------------------------------------

                    error?.let {

                        Text(
                            text =
                                "⚠ Error: $it",
                            color =
                                BossThemeColors.TextPrimary
                        )
                    }

                    // --------------------------------------------------
                    // DATASET RESULTS
                    // --------------------------------------------------

                    profile?.let { data ->

                        Divider()

                        Text(
                            text =
                                "📊 ${data.fileName}",
                            color =
                                BossThemeColors.TextPrimary
                        )

                        // --------------------------------------------------
                        // DATASET OVERVIEW
                        // --------------------------------------------------

                        Text(
                            text =
                                "Dataset Overview",
                            color =
                                BossThemeColors.TextPrimary
                        )

                        Row(
                            horizontalArrangement =
                                Arrangement.spacedBy(60.dp)
                        ) {

                            Column {

                                Text(
                                    text =
                                        "${data.rows}",
                                    color =
                                        BossThemeColors.TextPrimary
                                )

                                Text(
                                    text =
                                        "Rows",
                                    color =
                                        BossThemeColors.TextSecondary
                                )
                            }

                            Column {

                                Text(
                                    text =
                                        "${data.columns}",
                                    color =
                                        BossThemeColors.TextPrimary
                                )

                                Text(
                                    text =
                                        "Columns",
                                    color =
                                        BossThemeColors.TextSecondary
                                )
                            }

                            Column {

                                Text(
                                    text =
                                        "${data.missingValues}",
                                    color =
                                        BossThemeColors.TextPrimary
                                )

                                Text(
                                    text =
                                        "Missing",
                                    color =
                                        BossThemeColors.TextSecondary
                                )
                            }

                            Column {

                                Text(
                                    text =
                                        "${data.duplicateRows}",
                                    color =
                                        BossThemeColors.TextPrimary
                                )

                                Text(
                                    text =
                                        "Duplicates",
                                    color =
                                        BossThemeColors.TextSecondary
                                )
                            }
                        }

                        // --------------------------------------------------
                        // EXPLAINABLE DATA QUALITY
                        // --------------------------------------------------

                        Divider()

                        Text(
                            text =
                                "⭐ Data Quality",
                            color =
                                BossThemeColors.TextPrimary
                        )

                        Text(
                            text =
                                "${data.qualityScore}/100",
                            color =
                                BossThemeColors.TextPrimary
                        )

                        Text(
                            text =
                                qualityMessage(
                                    data.qualityScore
                                ),
                            color =
                                BossThemeColors.TextSecondary
                        )

                        Text(
                            text =
                                "Quality Breakdown",
                            color =
                                BossThemeColors.TextPrimary
                        )

                        Text(
                            text =
                                "Completeness: " +
                                    "${data.completenessScore}/100",
                            color =
                                BossThemeColors.TextSecondary
                        )

                        Text(
                            text =
                                "Duplicate Control: " +
                                    "${data.duplicateScore}/100",
                            color =
                                BossThemeColors.TextSecondary
                        )

                        Text(
                            text =
                                "Outlier Safety: " +
                                    "${data.outlierScore}/100",
                            color =
                                BossThemeColors.TextSecondary
                        )

                        // --------------------------------------------------
                        // ISSUES
                        // --------------------------------------------------

                        Divider()

                        Text(
                            text =
                                "⚠ Data Quality Issues",
                            color =
                                BossThemeColors.TextPrimary
                        )

                        data.issues.forEach { issue ->

                            Text(
                                text =
                                    "• $issue",
                                color =
                                    BossThemeColors.TextSecondary
                            )
                        }

                        // --------------------------------------------------
                        // POTENTIAL OUTLIERS
                        // --------------------------------------------------

                        if (
                            data.suspiciousValues.isNotEmpty()
                        ) {

                            Divider()

                            Text(
                                text =
                                    "🔎 Potential Outliers",
                                color =
                                    BossThemeColors.TextPrimary
                            )

                            data.suspiciousValues
                                .forEach { suspicious ->

                                    Text(
                                        text =
                                            "• $suspicious",
                                        color =
                                            BossThemeColors.TextSecondary
                                    )
                                }
                        }

                        // --------------------------------------------------
                        // TARGET INTELLIGENCE
                        // --------------------------------------------------

                        if (
                            data.potentialTarget != null
                        ) {

                            Divider()

                            Text(
                                text =
                                    "🎯 Target Intelligence",
                                color =
                                    BossThemeColors.TextPrimary
                            )

                            Text(
                                text =
                                    "Detected Target: " +
                                        data.potentialTarget,
                                color =
                                    BossThemeColors.TextPrimary
                            )

                            if (
                                data.targetDistribution.isNotEmpty()
                            ) {

                                Text(
                                    text =
                                        "Target Distribution",
                                    color =
                                        BossThemeColors.TextPrimary
                                )

                                data.targetDistribution
                                    .forEach { distribution ->

                                        Text(
                                            text =
                                                "• $distribution",
                                            color =
                                                BossThemeColors
                                                    .TextSecondary
                                        )
                                    }
                            }

                            Text(
                                text =
                                    "Numeric Features: " +
                                        "${data.numericFeatureCount}",
                                color =
                                    BossThemeColors.TextSecondary
                            )

                            Text(
                                text =
                                    "Text Features: " +
                                        "${data.textFeatureCount}",
                                color =
                                    BossThemeColors.TextSecondary
                            )
                        }

                        // --------------------------------------------------
                        // ML READINESS
                        // --------------------------------------------------

                        Divider()

                        Text(
                            text =
                                "🤖 ML Readiness",
                            color =
                                BossThemeColors.TextPrimary
                        )

                        Text(
                            text =
                                if (data.mlReady) {
                                    "✅ Ready for ML"
                                } else {
                                    "⚠ Needs Cleaning"
                                },
                            color =
                                BossThemeColors.TextPrimary
                        )

                        // --------------------------------------------------
                        // RECOMMENDATIONS
                        // --------------------------------------------------

                        Divider()

                        Text(
                            text =
                                "💡 Recommendations",
                            color =
                                BossThemeColors.TextPrimary
                        )

                        data.recommendations
                            .forEach { recommendation ->

                                Text(
                                    text =
                                        "• $recommendation",
                                    color =
                                        BossThemeColors.TextSecondary
                                )
                            }

                        // --------------------------------------------------
                        // COLUMN ANALYSIS
                        // --------------------------------------------------

                        Divider()

                        Text(
                            text =
                                "📋 Column Analysis",
                            color =
                                BossThemeColors.TextPrimary
                        )

                        data.columnProfiles
                            .forEach { column ->

                                Divider()

                                Text(
                                    text =
                                        "${column.name}  •  ${column.type}",
                                    color =
                                        BossThemeColors.TextPrimary
                                )

                                Text(
                                    text =
                                        "Completeness: " +
                                            "${column.completeness}%",
                                    color =
                                        BossThemeColors.TextSecondary
                                )

                                Text(
                                    text =
                                        "Missing: ${column.missing}" +
                                            "  |  Unique: ${column.unique}",
                                    color =
                                        BossThemeColors.TextSecondary
                                )

                                if (
                                    column.samples.isNotEmpty()
                                ) {

                                    Text(
                                        text =
                                            "Examples: " +
                                                column.samples
                                                    .joinToString(", "),
                                        color =
                                        BossThemeColors.TextSecondary
                                    )
                                }
                            }
                    }

                    // --------------------------------------------------
                    // EMPTY STATE
                    // --------------------------------------------------

                    if (
                        profile == null &&
                        error == null
                    ) {

                        Text(
                            text =
                                "No dataset loaded.",
                            color =
                                BossThemeColors.TextPrimary
                        )

                        Text(
                            text =
                                "Select a CSV file to generate " +
                                    "a dataset intelligence profile.",
                            color =
                                BossThemeColors.TextSecondary
                        )
                    }
                }
            }
        }
    }

    // --------------------------------------------------------------
    // QUALITY MESSAGE
    // --------------------------------------------------------------

    private fun qualityMessage(score: Int): String {
    return when {
        score >= 98 -> "Excellent dataset quality"
        score >= 90 -> "High-quality dataset — minor issues detected"
        score >= 75 -> "Good dataset quality — cleaning recommended"
        score >= 60 -> "Moderate dataset quality — cleaning required"
        else -> "Poor dataset quality — significant cleaning required"
        }
    }
}
