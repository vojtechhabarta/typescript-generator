# documentation: http://www.appveyor.com/docs/api/samples/download-artifacts-ps
$apiUrl = 'https://ci.appveyor.com/api'

Write-Host -ForegroundColor DarkCyan "Getting appveyor project..."
$project = Invoke-RestMethod -Method Get -Uri "$apiUrl/projects/vojtechhabarta/typescript-generator"
$jobId = $project.build.jobs[0].jobId

$artifactsUri = "$apiUrl/buildjobs/$jobId/artifacts/target/artifacts.zip"
Write-Host -ForegroundColor DarkCyan "Downloading '$artifactsUri'..."
$zipFilePath = "target\artifacts.zip"
Invoke-RestMethod -Method Get -Uri $artifactsUri -OutFile $zipFilePath
$zipFile = Get-Item $zipFilePath
$zipFile
