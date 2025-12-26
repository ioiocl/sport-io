# Sportbot GCP Deployment Script (PowerShell)
# This script automates the deployment of Sportbot to Google Cloud Platform

param(
    [string]$ProjectId = "",
    [string]$Region = "us-central1"
)

$ErrorActionPreference = "Stop"

# Functions
function Write-Info {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor Green
}

function Write-Warning {
    param([string]$Message)
    Write-Host "[WARNING] $Message" -ForegroundColor Yellow
}

function Write-Error {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor Red
}

function Test-Prerequisites {
    Write-Info "Checking prerequisites..."
    
    # Check gcloud
    if (!(Get-Command gcloud -ErrorAction SilentlyContinue)) {
        Write-Error "gcloud CLI not found. Install from: https://cloud.google.com/sdk/docs/install"
        exit 1
    }
    
    # Check terraform
    if (!(Get-Command terraform -ErrorAction SilentlyContinue)) {
        Write-Error "Terraform not found. Install from: https://www.terraform.io/downloads"
        exit 1
    }
    
    # Check docker
    if (!(Get-Command docker -ErrorAction SilentlyContinue)) {
        Write-Error "Docker not found. Install from: https://docs.docker.com/get-docker/"
        exit 1
    }
    
    Write-Info "All prerequisites met!"
}

function Initialize-GCP {
    Write-Info "Configuring GCP..."
    
    # Get project ID
    if ([string]::IsNullOrEmpty($script:ProjectId)) {
        $script:ProjectId = Read-Host "Enter your GCP Project ID"
    }
    
    # Set project
    gcloud config set project $script:ProjectId
    
    # Enable required APIs
    Write-Info "Enabling required APIs..."
    $apis = @(
        "run.googleapis.com",
        "vpcaccess.googleapis.com",
        "redis.googleapis.com",
        "cloudbuild.googleapis.com",
        "artifactregistry.googleapis.com",
        "compute.googleapis.com",
        "secretmanager.googleapis.com"
    )
    
    foreach ($api in $apis) {
        Write-Info "Enabling $api..."
        gcloud services enable $api --project=$script:ProjectId
    }
    
    Write-Info "GCP configured successfully!"
}

function New-TerraformBackend {
    Write-Info "Creating Terraform backend..."
    
    $script:TerraformBucket = "$script:ProjectId-terraform-state"
    
    # Check if bucket exists
    $bucketExists = gsutil ls -b "gs://$script:TerraformBucket" 2>$null
    
    if ($LASTEXITCODE -eq 0) {
        Write-Warning "Terraform state bucket already exists"
    } else {
        Write-Info "Creating Terraform state bucket..."
        gsutil mb -p $script:ProjectId -l $script:Region "gs://$script:TerraformBucket"
        gsutil versioning set on "gs://$script:TerraformBucket"
        Write-Info "Terraform state bucket created!"
    }
}

function Build-AndPushImages {
    Write-Info "Building and pushing Docker images..."
    
    # Configure Docker for GCP
    gcloud auth configure-docker "$script:Region-docker.pkg.dev"
    
    # Create Artifact Registry repository
    Write-Info "Creating Artifact Registry repository..."
    gcloud artifacts repositories create sportbot `
        --repository-format=docker `
        --location=$script:Region `
        --description="Sportbot Docker images" `
        --project=$script:ProjectId 2>$null
    
    # Build and push using Cloud Build
    Write-Info "Building images with Cloud Build..."
    Write-Info "This may take 15-20 minutes..."
    gcloud builds submit --config=cloudbuild.yaml `
        --substitutions=_REGION=$script:Region `
        --project=$script:ProjectId
    
    Write-Info "Images built and pushed successfully!"
}

function Deploy-Infrastructure {
    Write-Info "Deploying infrastructure with Terraform..."
    
    Push-Location terraform
    
    try {
        # Initialize Terraform
        Write-Info "Initializing Terraform..."
        terraform init `
            -backend-config="bucket=$script:TerraformBucket" `
            -backend-config="prefix=terraform/state"
        
        # Plan
        Write-Info "Planning infrastructure changes..."
        terraform plan -out=tfplan
        
        # Apply
        $confirm = Read-Host "Apply these changes? (yes/no)"
        if ($confirm -eq "yes") {
            Write-Info "Applying infrastructure changes..."
            terraform apply tfplan
            Write-Info "Infrastructure deployed successfully!"
        } else {
            Write-Warning "Deployment cancelled"
            exit 0
        }
    }
    finally {
        Pop-Location
    }
}

function Get-DeploymentOutputs {
    Write-Info "Getting deployment outputs..."
    
    Push-Location terraform
    
    try {
        Write-Host ""
        Write-Host "==========================================" -ForegroundColor Cyan
        Write-Host "Deployment Complete!" -ForegroundColor Cyan
        Write-Host "==========================================" -ForegroundColor Cyan
        Write-Host ""
        
        $dashboardUrl = terraform output -raw dashboard_url
        $websocketUrl = terraform output -raw websocket_service_url
        
        Write-Host "Dashboard URL: " -NoNewline
        Write-Host $dashboardUrl -ForegroundColor Yellow
        Write-Host "WebSocket URL: " -NoNewline
        Write-Host $websocketUrl -ForegroundColor Yellow
        Write-Host ""
        Write-Host "To view logs:" -ForegroundColor Cyan
        Write-Host "  gcloud run services logs read sportbot-ingestion --region=$script:Region"
        Write-Host "  gcloud run services logs read sportbot-analytics --region=$script:Region"
        Write-Host ""
        Write-Host "To update match IDs:" -ForegroundColor Cyan
        Write-Host "  cd terraform"
        Write-Host "  terraform apply -var='football_matches=NEW_MATCH_IDS'"
        Write-Host ""
    }
    finally {
        Pop-Location
    }
}

# Main execution
function Main {
    Write-Info "Starting Sportbot GCP Deployment"
    Write-Host ""
    
    Test-Prerequisites
    Initialize-GCP
    New-TerraformBackend
    Build-AndPushImages
    Deploy-Infrastructure
    Get-DeploymentOutputs
    
    Write-Info "Deployment complete! 🎉"
}

# Run main function
Main
