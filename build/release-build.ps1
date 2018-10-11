
$ErrorActionPreference = "Stop"

# download
$zipFile = .\build\download-appveyor-artifacts.ps1

# unzip
Write-Host -ForegroundColor DarkCyan "Unzipping..."
$unzipDirectoryPath = "target\gpg-sign"
rm -Recurse -Force $unzipDirectoryPath -ErrorAction SilentlyContinue
rm -Recurse -Force $unzipDirectoryPath -ErrorAction SilentlyContinue
$unzipDirectory = mkdir $unzipDirectoryPath -Force
[System.Reflection.Assembly]::LoadWithPartialName("System.IO.Compression.FileSystem") | Out-Null
[System.IO.Compression.ZipFile]::ExtractToDirectory($zipFile, $unzipDirectory.FullName)
$basePath = (Resolve-Path $unzipDirectory).Path

# passphrase
$securePassphrase = Read-Host -Prompt "Enter signing key passphrase" -AsSecureString
$passphraseCredential = New-Object System.Management.Automation.PSCredential -ArgumentList "Domain\User", $securePassphrase
$passphrase = $passphraseCredential.GetNetworkCredential().Password
"test" | gpg --detach-sign --armor --passphrase $passphrase | Out-Null
if (! $?) {
    exit
}

# sign
foreach ($file in dir -Recurse -File $basePath -Exclude *.md5,*.sha1) {
    $path = $file.FullName.Substring($basePath.Length + 1)
    Write-Host -ForegroundColor DarkCyan "Signing $path..."
    gpg --detach-sign --armor --passphrase $passphrase $file.FullName
    Get-FileHash -Algorithm MD5 $file.FullName | % { [IO.File]::WriteAllText($file.FullName + ".md5", $_.Hash) }
    Get-FileHash -Algorithm SHA1 $file.FullName | % { [IO.File]::WriteAllText($file.FullName + ".sha1", $_.Hash) }
}

# upload
$repoUri = "https://oss.sonatype.org/service/local/staging/deploy/maven2"
$credential = $host.UI.PromptForCredential("OSS Repository Hosting", "Enter your credentials for Sonatype maven repository https://oss.sonatype.org", "", "")
if (! $credential) {
    exit
}
$headers = @{"Authorization" = "Basic " + [System.Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($credential.UserName + ":" + $credential.GetNetworkCredential().Password))}
foreach ($file in dir -Recurse -File $basePath) {
    $path = $file.FullName.Substring($basePath.Length + 1)
    Write-Host -ForegroundColor DarkCyan "Uploading $path..."
    $pom = (dir $file.Directory -Filter *.pom).FullName
    [xml]$pomXml = Get-Content $pom
    $groupId = if ($pomXml.project.groupId) { $pomXml.project.groupId } else { $pomXml.project.parent.groupId }
    $uri = $groupId.Replace(".", "/") + $path.Substring($groupId.Length).Replace("\", "/")
    $response = Invoke-WebRequest -InFile $file.FullName -Method Put "$repoUri/$uri" -Headers $headers -UseBasicParsing
}

Write-Host -ForegroundColor Cyan "Build successfully uploaded. Go to https://oss.sonatype.org and promote the release."
