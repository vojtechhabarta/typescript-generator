
# Releases latest build to Maven Central Repository
# Prerequisites:
# - gpg with signing key without passphrase
# - OSSRH_USERNAME
# - OSSRH_PASSWORD

if (-not $env:OSSRH_USERNAME -or -not $env:OSSRH_PASSWORD) {
    Write-Error "Please set OSSRH_USERNAME and OSSRH_PASSWORD environment variables"
    exit 1
}

$ErrorActionPreference = "Stop"

# download
$zipFile = ./build/download-appveyor-artifacts.ps1

# unzip
Write-Host -ForegroundColor DarkCyan "Unzipping..."
$unzipDirectoryPath = "target/gpg-sign"
Remove-Item -Recurse -Force $unzipDirectoryPath -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force $unzipDirectoryPath -ErrorAction SilentlyContinue
$unzipDirectory = New-Item -ItemType directory -Path $unzipDirectoryPath -Force
[System.Reflection.Assembly]::LoadWithPartialName("System.IO.Compression.FileSystem") | Out-Null
[System.IO.Compression.ZipFile]::ExtractToDirectory($zipFile, $unzipDirectory.FullName)
$basePath = (Resolve-Path $unzipDirectory).Path

# sign
foreach ($file in Get-ChildItem -Recurse -File $basePath -Exclude *.md5,*.sha1) {
    $path = $file.FullName.Substring($basePath.Length + 1)
    Write-Host -ForegroundColor DarkCyan "Signing $path..."
    gpg --detach-sign --armor $file.FullName
    Get-FileHash -Algorithm MD5 $file.FullName | ForEach-Object { [IO.File]::WriteAllText($file.FullName + ".md5", $_.Hash) }
    Get-FileHash -Algorithm SHA1 $file.FullName | ForEach-Object { [IO.File]::WriteAllText($file.FullName + ".sha1", $_.Hash) }
}

# upload
$repoUri = "https://oss.sonatype.org/service/local/staging/deploy/maven2"
$credential = New-Object -TypeName System.Net.NetworkCredential -ArgumentList $env:OSSRH_USERNAME, $env:OSSRH_PASSWORD
$headers = @{"Authorization" = "Basic " + [System.Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($credential.UserName + ":" + $credential.Password))}
foreach ($file in Get-ChildItem -Recurse -File $basePath) {
    $path = $file.FullName.Substring($basePath.Length + 1)
    Write-Host -ForegroundColor DarkCyan "Uploading $path..."
    $pom = (Get-ChildItem $file.Directory -Filter *.pom).FullName
    [xml]$pomXml = Get-Content $pom
    $groupId = if ($pomXml.project.groupId) { $pomXml.project.groupId } else { $pomXml.project.parent.groupId }
    $uri = $groupId.Replace(".", "/") + $path.Substring($groupId.Length).Replace("\", "/")
    Invoke-WebRequest -InFile $file.FullName -Method Put "$repoUri/$uri" -Headers $headers -UseBasicParsing | Out-Null
}

Write-Host -ForegroundColor Cyan "Build successfully uploaded. Go to https://oss.sonatype.org and promote the release."
