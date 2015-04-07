
if (! $env:HUDSON_URL) {
    Write-Error "Please set environment variable 'HUDSON_URL'"
    exit
}

$artifactsUri = "$env:HUDSON_URL/job/typescript-generator/lastSuccessfulBuild/artifact/*zip*/archive.zip"
Write-Host -ForegroundColor DarkCyan "Downloading '$artifactsUri'..."
$zipFilePath = "target\archive.zip"
Invoke-WebRequest $artifactsUri -OutFile $zipFilePath
$zipFile = Get-Item $zipFilePath
$zipFile
