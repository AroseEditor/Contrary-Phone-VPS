package com.contrary.phonevps.python

/**
 * Validates Python scripts before execution.
 * Blocks dangerous patterns that could harm the device or leak data.
 */
object ScriptValidator {

    private val DANGEROUS_PATTERNS = listOf(
        // Shell execution
        Regex("""os\.system\s*\("""),
        Regex("""subprocess\.(?:call|Popen|run|check_output)\s*\(.*shell\s*=\s*True"""),
        // Direct file system attacks
        Regex("""shutil\.rmtree\s*\(\s*['"]/"""),
        Regex("""os\.remove\s*\(\s*['"]/(?:system|data/data|proc)"""),
        // ctypes / native code injection
        Regex("""ctypes\.(?:CDLL|cdll|WinDLL|windll)\s*\("""),
        // Dangerous eval with variable input
        Regex("""exec\s*\(\s*(?:input|open|request)"""),
        // Network binding on privileged ports
        Regex("""\.bind\s*\(\s*\(\s*['"].*['"]\s*,\s*(?:[0-9]|[1-9][0-9]|[1-9][0-9]{2}|10[01][0-9]|102[0-3])\s*\)"""),
    )

    private val WARNING_PATTERNS = listOf(
        Regex("""import\s+subprocess"""),
        Regex("""import\s+ctypes"""),
        Regex("""__import__\s*\("""),
        Regex("""open\s*\(\s*['"][/\\]"""),
    )

    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String>,
        val warnings: List<String>,
    )

    fun validate(scriptContent: String): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val lines = scriptContent.lines()

        for ((index, line) in lines.withIndex()) {
            val lineNum = index + 1
            val trimmed = line.trim()
            if (trimmed.startsWith("#")) continue // skip comments

            for (pattern in DANGEROUS_PATTERNS) {
                if (pattern.containsMatchIn(line)) {
                    errors.add("Line $lineNum: Blocked dangerous pattern — ${pattern.pattern}")
                }
            }

            for (pattern in WARNING_PATTERNS) {
                if (pattern.containsMatchIn(line)) {
                    warnings.add("Line $lineNum: Potentially dangerous — ${pattern.pattern}")
                }
            }
        }

        // Basic syntax check — look for obvious issues
        if (scriptContent.isBlank()) {
            errors.add("Script is empty")
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
        )
    }
}
