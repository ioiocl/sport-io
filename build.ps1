# Enable Docker BuildKit for better caching and performance
$env:DOCKER_BUILDKIT = 1
$env:COMPOSE_DOCKER_CLI_BUILD = 1

Write-Host "Building Sportbot services with BuildKit enabled..." -ForegroundColor Green
Write-Host "This will use Google's Maven mirror and enable dependency caching" -ForegroundColor Cyan

# Build with docker-compose
docker-compose build --progress=plain

if ($LASTEXITCODE -eq 0) {
    Write-Host "`nBuild completed successfully!" -ForegroundColor Green
    Write-Host "Run 'docker-compose up -d' to start the services" -ForegroundColor Cyan
} else {
    Write-Host "`nBuild failed. Check the error messages above." -ForegroundColor Red
    exit $LASTEXITCODE
}
