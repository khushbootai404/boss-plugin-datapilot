package ai.rever.boss.plugin.dynamic.datapilot

import ai.rever.boss.plugin.api.McpToolDefinition
import ai.rever.boss.plugin.api.McpToolHandler
import ai.rever.boss.plugin.api.McpToolProvider
import ai.rever.boss.plugin.api.McpToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DataPilotMcpTools(
    override val providerId: String
) : McpToolProvider {

    override fun tools(): List<McpToolDefinition> = listOf(
        McpToolDefinition(
            name = "datapilot_profile",

            description = """
                Analyze a CSV dataset and return structured dataset intelligence,
                including dimensions, missing values, duplicates, inferred column types,
                completeness, data quality score, detected issues, recommendations,
                ML readiness, potential target column, and sample values.
            """.trimIndent(),

            inputSchema = """
                {
                  "type": "object",
                  "properties": {
                    "path": {
                      "type": "string",
                      "description": "Absolute path to the CSV file to analyze."
                    }
                  },
                  "required": ["path"]
                }
            """.trimIndent(),

            handler = McpToolHandler { args ->

                val path = args.string("path")
                    ?: return@McpToolHandler McpToolResult(
                        "Missing required argument: path",
                        isError = true
                    )

                try {

                    val profile = withContext(Dispatchers.IO) {
                        DataPilotAnalyzer.analyze(File(path))
                    }

                    val columns = profile.columnProfiles.joinToString("\n") {
                        """
                        - ${it.name}
                          Type: ${it.type}
                          Completeness: ${it.completeness}%
                          Missing: ${it.missing}
                          Unique: ${it.unique}
                          Samples: ${it.samples.joinToString(", ")}
                        """.trimIndent()
                    }

                    val issues = profile.issues.joinToString("\n") {
                        "- $it"
                    }

                    val recommendations =
                        profile.recommendations.joinToString("\n") {
                            "- $it"
                        }

                    val target =
                        profile.potentialTarget
                            ?: "No obvious target column detected."

                    McpToolResult(
                        """
                        DataPilot Dataset Intelligence

                        Dataset
                        -------
                        File: ${profile.fileName}
                        Rows: ${profile.rows}
                        Columns: ${profile.columns}

                        Data Quality
                        ------------
                        Quality Score: ${profile.qualityScore}/100
                        Missing Values: ${profile.missingValues}
                        Duplicate Rows: ${profile.duplicateRows}

                        Data Quality Issues
                        --------------------
                        $issues

                        ML Readiness
                        ------------
                        ML Ready: ${if (profile.mlReady) "Yes" else "No"}
                        Potential Target Column: $target

                        Recommendations
                        ---------------
                        $recommendations

                        Column Analysis
                        ---------------
                        $columns
                        """.trimIndent()
                    )

                } catch (e: Exception) {

                    McpToolResult(
                        "DataPilot analysis failed: ${e.message}",
                        isError = true
                    )
                }
            }
        )
    )
}