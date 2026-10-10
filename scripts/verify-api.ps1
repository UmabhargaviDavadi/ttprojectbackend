$ErrorActionPreference = 'Stop'
$base = 'http://localhost:8080/api'
$email = "verify-$([guid]::NewGuid().ToString('N'))@example.com"
$password = 'VerifyPass123'

function Assert($condition, $message) {
    if (-not $condition) { throw $message }
}

$null = Invoke-RestMethod -Method Post -Uri "$base/auth/register" -ContentType 'application/json' -Body (@{ name = 'Verify User'; email = $email; password = $password } | ConvertTo-Json)
$login = Invoke-RestMethod -Method Post -Uri "$base/auth/login" -ContentType 'application/json' -Body (@{ email = $email; password = $password } | ConvertTo-Json)
$headers = @{ Authorization = "Bearer $($login.token)" }
$today = Get-Date -Format 'yyyy-MM-dd'
$month = (Get-Date).Month
$year = (Get-Date).Year

$ride = Invoke-RestMethod -Method Post -Uri "$base/transactions" -Headers $headers -ContentType 'application/json' -Body (@{ amount = 10; description = 'Uber ride'; type = 'expense'; date = $today } | ConvertTo-Json)
Assert ($ride.category -eq 'Travel' -and $ride.type -eq 'expense') 'Auto-categorization failed.'

foreach ($category in @('Food', 'Overall')) {
    $null = Invoke-RestMethod -Method Post -Uri "$base/budgets" -Headers $headers -ContentType 'application/json' -Body (@{ category = $category; limit = 100; month = $month; year = $year } | ConvertTo-Json)
}
$null = Invoke-RestMethod -Method Post -Uri "$base/transactions" -Headers $headers -ContentType 'application/json' -Body (@{ amount = 80; description = 'Grocery market'; type = 'expense'; date = $today } | ConvertTo-Json)
$null = Invoke-RestMethod -Method Post -Uri "$base/transactions" -Headers $headers -ContentType 'application/json' -Body (@{ amount = 25; description = 'Restaurant lunch'; type = 'expense'; date = $today } | ConvertTo-Json)

$page = Invoke-RestMethod -Method Get -Uri "$base/transactions?page=0&size=1&type=expense&category=Food&from=$today&to=$today&minAmount=20&maxAmount=100&sort=amount,desc" -Headers $headers
Assert ($page.content.Count -eq 1 -and $page.content[0].amount -eq 80 -and $page.size -eq 1) 'Transaction filtering or pagination failed.'
$budgets = Invoke-RestMethod -Method Get -Uri "$base/budgets?month=$month&year=$year" -Headers $headers
Assert ($budgets.Count -eq 2 -and $budgets[0].PSObject.Properties.Name -contains 'spent') 'Budget response shape failed.'

$monthly = Invoke-RestMethod -Method Get -Uri "$base/reports/monthly?month=$month&year=$year" -Headers $headers
$categorySummary = Invoke-RestMethod -Method Get -Uri "$base/reports/category-summary?month=$month&year=$year" -Headers $headers
$trend = Invoke-RestMethod -Method Get -Uri "$base/reports/trend?months=6" -Headers $headers
$savings = Invoke-RestMethod -Method Get -Uri "$base/suggestions/savings" -Headers $headers
$investments = Invoke-RestMethod -Method Get -Uri "$base/suggestions/investments" -Headers $headers
Assert ($trend.items.Count -eq 6 -and $null -ne $categorySummary.items -and $savings.items.Count -gt 0 -and $investments.items.Count -eq 4) 'Report or suggestion response shape failed.'

$deadline = (Get-Date).AddMonths(2).ToString('yyyy-MM-dd')
$goal = Invoke-RestMethod -Method Post -Uri "$base/goals" -Headers $headers -ContentType 'application/json' -Body (@{ name = 'Verification goal'; targetAmount = 1000; currentAmount = 100; deadline = $deadline } | ConvertTo-Json)
$updatedGoal = Invoke-RestMethod -Method Put -Uri "$base/goals/$($goal.id)" -Headers $headers -ContentType 'application/json' -Body (@{ name = 'Verification goal'; targetAmount = 1000; currentAmount = 250; deadline = $deadline } | ConvertTo-Json)
$goals = Invoke-RestMethod -Method Get -Uri "$base/goals" -Headers $headers
Assert ($updatedGoal.percentage -eq 25 -and $goals.Count -eq 1) 'Goal create/update/list failed.'

$notificationRaw = & curl.exe -sS -i -H "Authorization: Bearer $($login.token)" "$base/notifications"
$notificationText = $notificationRaw -join "`n"
$notificationParts = $notificationText -split "`r?`n`r?`n", 2
$notificationHeader = [regex]::Match($notificationParts[0], '(?im)^X-Unread-Count:\s*(\d+)').Groups[1].Value
$notifications = $notificationParts[1] | ConvertFrom-Json
Assert ($notifications.Count -ge 4 -and [int]$notificationHeader -ge 4) 'Budget threshold notifications or unread count failed.'
$marked = Invoke-RestMethod -Method Put -Uri "$base/notifications/$($notifications[0].id)/read" -Headers $headers
Assert ($marked.read) 'Notification read operation failed.'

$profile = Invoke-RestMethod -Method Get -Uri "$base/users/profile" -Headers $headers
$updatedProfile = Invoke-RestMethod -Method Put -Uri "$base/users/profile" -Headers $headers -ContentType 'application/json' -Body (@{ name = 'Verify User Updated'; currency = 'USD' } | ConvertTo-Json)
$null = Invoke-RestMethod -Method Put -Uri "$base/users/profile" -Headers $headers -ContentType 'application/json' -Body (@{ currentPassword = $password; newPassword = 'VerifyPass456' } | ConvertTo-Json)
$relogin = Invoke-RestMethod -Method Post -Uri "$base/auth/login" -ContentType 'application/json' -Body (@{ email = $email; password = 'VerifyPass456' } | ConvertTo-Json)
$null = Invoke-RestMethod -Method Delete -Uri "$base/goals/$($goal.id)" -Headers $headers
Assert ($profile.currency -eq 'INR' -and $updatedProfile.currency -eq 'USD' -and $relogin.token) 'Profile or password change failed.'

$cors = (& curl.exe -sS -i -X OPTIONS "$base/transactions" -H 'Origin: http://localhost:5173' -H 'Access-Control-Request-Method: POST' -H 'Access-Control-Request-Headers: authorization,content-type') -join "`n"
Assert ($cors -match '(?im)^HTTP/[^ ]+ 200' -and $cors -match '(?im)^Access-Control-Allow-Origin:\s*http://localhost:5173') 'CORS preflight failed.'
$swaggerStatus = & curl.exe -sS -o NUL -w '%{http_code}' "$base/swagger-ui/index.html"
Assert ($swaggerStatus -eq '200') 'Swagger UI did not return HTTP 200.'

[pscustomobject]@{
    RegisterAndLogin = 'PASS'
    AutoCategorizedCategory = $ride.category
    TransactionFiltersPagination = 'PASS'
    Budgets = $budgets.Count
    BudgetAlerts = $notifications.Count
    MonthlyReportBalance = $monthly.balance
    CategoryGroups = $categorySummary.items.Count
    TrendMonths = $trend.items.Count
    SavingsSuggestions = $savings.items.Count
    InvestmentSuggestions = $investments.items.Count
    GoalCrud = 'PASS'
    NotificationRead = 'PASS'
    ProfileAndPassword = 'PASS'
    CorsPreflight = 'HTTP 200'
    SwaggerUi = "HTTP $swaggerStatus"
} | ConvertTo-Json -Depth 4
