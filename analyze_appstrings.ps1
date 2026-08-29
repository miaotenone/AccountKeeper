$file = 'C:\Users\DiRJ\Documents\VSCODEF\AccountKeeperDev\app\src\main\java\com\example\accountkeeper\ui\theme\AppStrings.kt'
$lines = Get-Content $file

# --- Collect data class fields ---
$dcFields = @()
$inDC = $false
$depth = 0
foreach ($line in $lines) {
    $trimmed = $line.Trim()
    if ($trimmed -match 'data class AppStrings\(') {
        $inDC = $true
        $opens = ([regex]::Matches($trimmed, '\(')).Count
        $closes = ([regex]::Matches($trimmed, '\)')).Count
        $depth = $opens - $closes
        continue
    }
    if ($inDC) {
        $opens = ([regex]::Matches($trimmed, '\(')).Count
        $closes = ([regex]::Matches($trimmed, '\)')).Count
        $depth += $opens - $closes
        if ($trimmed -match '^\s*val (\w+): String') {
            $dcFields += $Matches[1]
        }
        if ($depth -le 0) { break }
    }
}
Write-Host "=== Data class field count ==="
Write-Host "$($dcFields.Count)"

# --- Collect EnStrings fields ---
$enFields = @()
$inEn = $false
$depth = 0
foreach ($line in $lines) {
    $trimmed = $line.Trim()
    if ($trimmed -match 'val EnStrings = AppStrings\(') {
        $inEn = $true
        $opens = ([regex]::Matches($trimmed, '\(')).Count
        $closes = ([regex]::Matches($trimmed, '\)')).Count
        $depth = $opens - $closes
        continue
    }
    if ($inEn) {
        $opens = ([regex]::Matches($trimmed, '\(')).Count
        $closes = ([regex]::Matches($trimmed, '\)')).Count
        $depth += $opens - $closes
        # EnStrings lines look like: home = "Home",  (no val keyword)
        if ($trimmed -match '^\s*(\w+) =') {
            $fieldName = $Matches[1]
            if ($fieldName -ne 'val') {
                $enFields += $fieldName
            }
        }
        if ($depth -le 0) { break }
    }
}
Write-Host ""
Write-Host "=== EnStrings field count ==="
Write-Host "$($enFields.Count)"

# --- Collect ZhStrings fields ---
$zhFields = @()
$inZh = $false
$depth = 0
foreach ($line in $lines) {
    $trimmed = $line.Trim()
    if ($trimmed -match 'val ZhStrings = AppStrings\(') {
        $inZh = $true
        $opens = ([regex]::Matches($trimmed, '\(')).Count
        $closes = ([regex]::Matches($trimmed, '\)')).Count
        $depth = $opens - $closes
        continue
    }
    if ($inZh) {
        $opens = ([regex]::Matches($trimmed, '\(')).Count
        $closes = ([regex]::Matches($trimmed, '\)')).Count
        $depth += $opens - $closes
        if ($trimmed -match '^\s*(\w+) =') {
            $fieldName = $Matches[1]
            if ($fieldName -ne 'val') {
                $zhFields += $fieldName
            }
        }
        if ($depth -le 0) { break }
    }
}
Write-Host ""
Write-Host "=== ZhStrings field count ==="
Write-Host "$($zhFields.Count)"

# --- Compare ---
Write-Host ""
$enMissing = $dcFields | Where-Object { $_ -notin $enFields }
Write-Host "=== Fields in data class but NOT in EnStrings ($($enMissing.Count)) ==="
if ($enMissing.Count -eq 0) { Write-Host "None" } else { $enMissing | ForEach-Object { Write-Host "  $_" } }

Write-Host ""
$zhMissing = $dcFields | Where-Object { $_ -notin $zhFields }
Write-Host "=== Fields in data class but NOT in ZhStrings ($($zhMissing.Count)) ==="
if ($zhMissing.Count -eq 0) { Write-Host "None" } else { $zhMissing | ForEach-Object { Write-Host "  $_" } }

Write-Host ""
$enExtra = $enFields | Where-Object { $_ -notin $dcFields }
Write-Host "=== Fields in EnStrings but NOT in data class ($($enExtra.Count)) ==="
if ($enExtra.Count -eq 0) { Write-Host "None" } else { $enExtra | ForEach-Object { Write-Host "  $_" } }

Write-Host ""
$zhExtra = $zhFields | Where-Object { $_ -notin $dcFields }
Write-Host "=== Fields in ZhStrings but NOT in data class ($($zhExtra.Count)) ==="
if ($zhExtra.Count -eq 0) { Write-Host "None" } else { $zhExtra | ForEach-Object { Write-Host "  $_" } }

Write-Host ""
$enOnly = $enFields | Where-Object { $_ -notin $zhFields }
Write-Host "=== Fields in EnStrings but NOT in ZhStrings ($($enOnly.Count)) ==="
if ($enOnly.Count -eq 0) { Write-Host "None" } else { $enOnly | ForEach-Object { Write-Host "  $_" } }

Write-Host ""
$zhOnly = $zhFields | Where-Object { $_ -notin $enFields }
Write-Host "=== Fields in ZhStrings but NOT in EnStrings ($($zhOnly.Count)) ==="
if ($zhOnly.Count -eq 0) { Write-Host "None" } else { $zhOnly | ForEach-Object { Write-Host "  $_" } }

# --- Default values ---
Write-Host ""
$dcDefaults = @()
$inDC2 = $false
$depth = 0
foreach ($line in $lines) {
    $trimmed = $line.Trim()
    if ($trimmed -match 'data class AppStrings\(') {
        $inDC2 = $true
        $opens = ([regex]::Matches($trimmed, '\(')).Count
        $closes = ([regex]::Matches($trimmed, '\)')).Count
        $depth = $opens - $closes
        continue
    }
    if ($inDC2) {
        $opens = ([regex]::Matches($trimmed, '\(')).Count
        $closes = ([regex]::Matches($trimmed, '\)')).Count
        $depth += $opens - $closes
        if ($trimmed -match '^\s*val (\w+): String = ') {
            $dcDefaults += $Matches[1]
        }
        if ($depth -le 0) { break }
    }
}
Write-Host "=== Fields with default values in data class ($($dcDefaults.Count)) ==="
if ($dcDefaults.Count -eq 0) { Write-Host "None - all fields have no default values" } else { $dcDefaults | ForEach-Object { Write-Host "  $_" } }
