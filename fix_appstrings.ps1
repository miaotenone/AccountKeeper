$file = "app\src\main\java\com\example\accountkeeper\ui\theme\AppStrings.kt"
$content = Get-Content $file -Raw -Encoding UTF8

# Step 1: Add = "" default to every field in the AppStrings data class
# Match lines like "    val home: String," and change to "    val home: String = \"\","
$content = $content -replace '(    val \w+: String),(\r?\n)', '$1 = "",$2'

# Step 2: For each language init block, we need to transform:
#   val EnStrings: AppStrings by lazy { AppStrings(
#       field = "value",
#       ...
#   ) }
# Into:
#   val EnStrings: AppStrings by lazy { AppStrings().copy(
#       field = "value",
#       ... (first ~147 args)
#   ).copy(
#       ... (next ~147 args)
#   ).copy(
#       ... (remaining args)
#   ) }

# We'll process each language block separately
$lines = $content -split "`r?`n"
$newLines = [System.Collections.Generic.List[string]]::new()

$i = 0
while ($i -lt $lines.Count) {
    $line = $lines[$i]
    
    # Detect start of language initialization
    if ($line -match '^\s*val (En|Zh)Strings: AppStrings by lazy \{ AppStrings\($') {
        $lang = $Matches[1]
        $newLines.Add($line -replace 'AppStrings\($', 'AppStrings().copy(')
        
        # Collect all argument lines until we find the closing
        $i++
        $argsList = [System.Collections.Generic.List[string]]::new()
        while ($i -lt $lines.Count) {
            $argLine = $lines[$i]
            if ($argLine -match '^\s*\) \}$') {
                # End of block
                break
            }
            $argsList.Add($argLine.TrimEnd())
            $i++
        }
        
        # Split args into 3 groups of ~147
        $totalArgs = $argsList.Count
        $groupSize = [Math]::Ceiling($totalArgs / 3)
        
        for ($g = 0; $g -lt 3; $g++) {
            $start = $g * $groupSize
            $end = [Math]::Min(($g + 1) * $groupSize, $totalArgs)
            if ($start -ge $totalArgs) { break }
            
            $group = $argsList[$start..($end - 1)]
            
            if ($g -eq 0) {
                # First group already has .copy( from above
                foreach ($arg in $group) {
                    $newLines.Add($arg)
                }
            } else {
                # Close previous .copy() and start new one
                $newLines.Add("    ).copy(")
                foreach ($arg in $group) {
                    $newLines.Add($arg)
                }
            }
        }
        
        # Close the last copy and the lazy block
        $newLines.Add("    ) }")
        $i++ # skip closing line
    } else {
        $newLines.Add($line)
    }
    $i++
}

$output = $newLines -join "`r`n"
[System.IO.File]::WriteAllText((Resolve-Path $file).Path, $output, [System.Text.UTF8Encoding]::new($false))
Write-Host "Transformation complete!"
Write-Host "Total lines: $($newLines.Count)"
