package ai.rever.boss.plugin.dynamic.datapilot

import java.io.File

data class ColumnProfile(
    val name: String,
    val type: String,
    val missing: Int,
    val unique: Int,
    val samples: List<String>,
    val completeness: Int
)

data class DatasetProfile(
    val fileName: String,
    val rows: Int,
    val columns: Int,
    val duplicateRows: Int,
    val missingValues: Int,
    val qualityScore: Int,
    val columnProfiles: List<ColumnProfile>,
    val issues: List<String>,
    val recommendations: List<String>,
    val mlReady: Boolean,
    val potentialTarget: String?,
    val suspiciousValues: List<String>,

    // Target intelligence
    val targetDistribution: List<String>,
    val numericFeatureCount: Int,
    val textFeatureCount: Int
)

object DataPilotAnalyzer {

    fun analyze(file: File): DatasetProfile {

        require(file.exists()) {
            "File does not exist."
        }

        val lines = file.readLines()
            .filter { it.isNotBlank() }

        require(lines.size >= 2) {
            "CSV must contain a header and at least one data row."
        }

        val headers = parseCsvLine(lines.first())

        val dataRows = lines.drop(1).map { line ->

            val row = parseCsvLine(line)

            headers.indices.map { index ->
                row.getOrNull(index)?.trim() ?: ""
            }
        }

        val rows = dataRows.size
        val columns = headers.size

        // ------------------------------------------------------------
        // DUPLICATES
        // ------------------------------------------------------------

        val duplicateRows =
            rows - dataRows.distinct().size

        // ------------------------------------------------------------
        // COLUMN PROFILES
        // ------------------------------------------------------------

        val columnProfiles =
            headers.mapIndexed { index, header ->

                val values =
                    dataRows.map { it[index] }

                val missing =
                    values.count {
                        isMissing(it)
                    }

                val validValues =
                    values.filterNot {
                        isMissing(it)
                    }

                val unique =
                    validValues.distinct().size

                val completeness =
                    if (rows == 0) {
                        0
                    } else {
                        (
                            (rows - missing)
                                .toDouble() /
                                rows *
                                100
                            ).toInt()
                    }

                ColumnProfile(
                    name =
                        header.ifBlank {
                            "Column ${index + 1}"
                        },

                    type =
                        inferType(validValues),

                    missing =
                        missing,

                    unique =
                        unique,

                    samples =
                        validValues
                            .distinct()
                            .take(3),

                    completeness =
                        completeness
                )
            }

        // ------------------------------------------------------------
        // MISSING VALUES
        // ------------------------------------------------------------

        val missingValues =
            columnProfiles.sumOf {
                it.missing
            }

        val totalCells =
            rows * columns

        val missingRatio =
            if (totalCells == 0) {
                0.0
            } else {
                missingValues.toDouble() /
                    totalCells
            }

        val duplicateRatio =
            if (rows == 0) {
                0.0
            } else {
                duplicateRows.toDouble() /
                    rows
            }

        // ------------------------------------------------------------
        // QUALITY SCORE
        // ------------------------------------------------------------

        val qualityScore =
            (
                100 -
                    (missingRatio * 50) -
                    (duplicateRatio * 30)
                )
                .coerceIn(
                    0.0,
                    100.0
                )
                .toInt()

        // ------------------------------------------------------------
        // TARGET DETECTION
        // ------------------------------------------------------------

        val targetCandidates =
            listOf(
                "target",
                "label",
                "class",
                "outcome",
                "churn",
                "y"
            )

        val potentialTarget =
            columnProfiles
                .firstOrNull {

                    targetCandidates.contains(
                        it.name.lowercase()
                    )
                }
                ?.name

        // ------------------------------------------------------------
        // TARGET INTELLIGENCE
        // ------------------------------------------------------------

        val targetDistribution =
            if (potentialTarget != null) {

                val targetIndex =
                    headers.indexOfFirst {
                        it.equals(
                            potentialTarget,
                            ignoreCase = true
                        )
                    }

                if (targetIndex >= 0) {

                    val targetValues =
                        dataRows
                            .mapNotNull { row ->

                                row.getOrNull(
                                    targetIndex
                                )
                                    ?.trim()
                                    ?.takeUnless {
                                        isMissing(it)
                                    }
                            }

                    val totalTargetValues =
                        targetValues.size

                    targetValues
                        .groupingBy {
                            it
                        }
                        .eachCount()
                        .entries
                        .sortedByDescending {
                            it.value
                        }
                        .map { entry ->

                            val percentage =
                                if (
                                    totalTargetValues == 0
                                ) {
                                    0.0
                                } else {
                                    entry.value
                                        .toDouble() /
                                        totalTargetValues *
                                        100
                                }

                            "${entry.key}: " +
                                "${entry.value} " +
                                "(${formatPercentage(percentage)}%)"
                        }

                } else {
                    emptyList()
                }

            } else {
                emptyList()
            }

        // ------------------------------------------------------------
        // FEATURE COUNTS
        // ------------------------------------------------------------

        val numericFeatureCount =
            columnProfiles.count {
                it.type == "Numeric" &&
                    it.name != potentialTarget
            }

        val textFeatureCount =
            columnProfiles.count {
                it.type == "Text" &&
                    it.name != potentialTarget
            }

        // ------------------------------------------------------------
        // ISSUES
        // ------------------------------------------------------------

        val issues =
            mutableListOf<String>()

        if (missingValues > 0) {

            issues.add(
                "$missingValues missing value(s) detected."
            )
        }

        if (duplicateRows > 0) {

            issues.add(
                "$duplicateRows duplicate row(s) detected."
            )
        }

        val emptyColumns =
            columnProfiles.filter {
                it.completeness == 0
            }

        if (emptyColumns.isNotEmpty()) {

            issues.add(
                "${emptyColumns.size} completely empty column(s) detected."
            )
        }

        // ------------------------------------------------------------
        // OUTLIER DETECTION
        // ------------------------------------------------------------

        val suspiciousValues =
            detectNumericOutliers(
                headers,
                dataRows
            )

        if (suspiciousValues.isNotEmpty()) {

            issues.add(
                "${suspiciousValues.size} potential outlier(s) detected."
            )
        }

        if (issues.isEmpty()) {

            issues.add(
                "No major data quality issues detected."
            )
        }

        // ------------------------------------------------------------
        // RECOMMENDATIONS
        // ------------------------------------------------------------

        val recommendations =
            mutableListOf<String>()

        if (missingValues > 0) {

            recommendations.add(
                "Handle missing values before analysis or ML training."
            )
        }

        if (duplicateRows > 0) {

            recommendations.add(
                "Remove duplicate records to avoid biased analysis."
            )
        }

        if (emptyColumns.isNotEmpty()) {

            recommendations.add(
                "Remove or investigate completely empty columns."
            )
        }

        if (suspiciousValues.isNotEmpty()) {

            recommendations.add(
                "Review potential numeric outliers before ML training."
            )
        }

        if (potentialTarget != null) {

            recommendations.add(
                "Target column '$potentialTarget' detected for supervised ML."
            )
        }

        if (qualityScore >= 95) {

            recommendations.add(
                "Dataset is mostly clean and suitable for exploratory analysis."
            )

        } else if (qualityScore >= 80) {

            recommendations.add(
                "Perform basic cleaning before using this dataset for ML."
            )

        } else {

            recommendations.add(
                "Significant cleaning is recommended before ML use."
            )
        }

        // ------------------------------------------------------------
        // ML READINESS
        // ------------------------------------------------------------

        val mlReady =
            qualityScore >= 90 &&
                missingValues == 0 &&
                duplicateRows == 0 &&
                emptyColumns.isEmpty() &&
                suspiciousValues.isEmpty()

        return DatasetProfile(

            fileName =
                file.name,

            rows =
                rows,

            columns =
                columns,

            duplicateRows =
                duplicateRows,

            missingValues =
                missingValues,

            qualityScore =
                qualityScore,

            columnProfiles =
                columnProfiles,

            issues =
                issues,

            recommendations =
                recommendations,

            mlReady =
                mlReady,

            potentialTarget =
                potentialTarget,

            suspiciousValues =
                suspiciousValues,

            targetDistribution =
                targetDistribution,

            numericFeatureCount =
                numericFeatureCount,

            textFeatureCount =
                textFeatureCount
        )
    }

    // ------------------------------------------------------------
    // NUMERIC OUTLIER DETECTION
    // ------------------------------------------------------------

    private fun detectNumericOutliers(
        headers: List<String>,
        dataRows: List<List<String>>
    ): List<String> {

        val results =
            mutableListOf<String>()

        if (dataRows.size < 4) {
            return results
        }

        headers.forEachIndexed { index, header ->

            val numericValues =
                dataRows.mapNotNull { row ->

                    row.getOrNull(index)
                        ?.takeUnless {
                            isMissing(it)
                        }
                        ?.toDoubleOrNull()
                }

            if (numericValues.size < 4) {
                return@forEachIndexed
            }

            val sorted =
                numericValues.sorted()

            val q1 =
                percentile(
                    sorted,
                    0.25
                )

            val q3 =
                percentile(
                    sorted,
                    0.75
                )

            val iqr =
                q3 - q1

            if (iqr == 0.0) {
                return@forEachIndexed
            }

            val lowerBound =
                q1 - (1.5 * iqr)

            val upperBound =
                q3 + (1.5 * iqr)

            val outlierCount =
                numericValues.count {
                    it < lowerBound ||
                        it > upperBound
                }

            if (outlierCount > 0) {

                results.add(
                    "$header: $outlierCount potential outlier(s) detected."
                )
            }
        }

        return results
    }

    // ------------------------------------------------------------
    // PERCENTILE
    // ------------------------------------------------------------

    private fun percentile(
        sortedValues: List<Double>,
        percentile: Double
    ): Double {

        if (sortedValues.isEmpty()) {
            return 0.0
        }

        val position =
            percentile *
                (sortedValues.size - 1)

        val lower =
            position.toInt()

        val upper =
            kotlin.math.ceil(position)
                .toInt()

        if (lower == upper) {
            return sortedValues[lower]
        }

        val weight =
            position - lower

        return sortedValues[lower] *
            (1 - weight) +
            sortedValues[upper] *
            weight
    }

    // ------------------------------------------------------------
    // PERCENTAGE FORMATTING
    // ------------------------------------------------------------

    private fun formatPercentage(
        value: Double
    ): String {

        return if (
            value == value.toInt().toDouble()
        ) {
            value.toInt().toString()
        } else {
            "%.1f".format(value)
        }
    }

    // ------------------------------------------------------------
    // MISSING VALUE DETECTION
    // ------------------------------------------------------------

    private fun isMissing(
        value: String
    ): Boolean {

        return value.isBlank() ||
            value.equals(
                "null",
                ignoreCase = true
            ) ||
            value.equals(
                "na",
                ignoreCase = true
            ) ||
            value.equals(
                "n/a",
                ignoreCase = true
            )
    }

    // ------------------------------------------------------------
    // TYPE INFERENCE
    // ------------------------------------------------------------

    private fun inferType(
        values: List<String>
    ): String {

        if (values.isEmpty()) {
            return "Unknown"
        }

        if (
            values.all {
                it.toDoubleOrNull() != null
            }
        ) {
            return "Numeric"
        }

        if (
            values.all {

                it.equals(
                    "true",
                    true
                ) ||

                it.equals(
                    "false",
                    true
                ) ||

                it.equals(
                    "yes",
                    true
                ) ||

                it.equals(
                    "no",
                    true
                )
            }
        ) {
            return "Boolean"
        }

        return "Text"
    }

    // ------------------------------------------------------------
    // CSV PARSER
    // ------------------------------------------------------------

    private fun parseCsvLine(
        line: String
    ): List<String> {

        val result =
            mutableListOf<String>()

        val current =
            StringBuilder()

        var insideQuotes =
            false

        var i = 0

        while (i < line.length) {

            val char =
                line[i]

            when {

                char == '"' -> {

                    if (
                        insideQuotes &&
                        i + 1 < line.length &&
                        line[i + 1] == '"'
                    ) {

                        current.append('"')
                        i++

                    } else {

                        insideQuotes =
                            !insideQuotes
                    }
                }

                char == ',' &&
                    !insideQuotes -> {

                    result.add(
                        current.toString()
                    )

                    current.clear()
                }

                else -> {

                    current.append(char)
                }
            }

            i++
        }

        result.add(
            current.toString()
        )

        return result
    }
}