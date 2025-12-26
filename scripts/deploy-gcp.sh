#!/bin/bash

# Sportbot GCP Deployment Script
# This script automates the deployment of Sportbot to Google Cloud Platform

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
PROJECT_ID=""
REGION="us-central1"
TERRAFORM_BUCKET=""

# Functions
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_prerequisites() {
    print_info "Checking prerequisites..."
    
    # Check gcloud
    if ! command -v gcloud &> /dev/null; then
        print_error "gcloud CLI not found. Install from: https://cloud.google.com/sdk/docs/install"
        exit 1
    fi
    
    # Check terraform
    if ! command -v terraform &> /dev/null; then
        print_error "Terraform not found. Install from: https://www.terraform.io/downloads"
        exit 1
    fi
    
    # Check docker
    if ! command -v docker &> /dev/null; then
        print_error "Docker not found. Install from: https://docs.docker.com/get-docker/"
        exit 1
    fi
    
    print_info "All prerequisites met!"
}

configure_gcp() {
    print_info "Configuring GCP..."
    
    # Get project ID
    if [ -z "$PROJECT_ID" ]; then
        read -p "Enter your GCP Project ID: " PROJECT_ID
    fi
    
    # Set project
    gcloud config set project "$PROJECT_ID"
    
    # Enable required APIs
    print_info "Enabling required APIs..."
    gcloud services enable \
        run.googleapis.com \
        vpcaccess.googleapis.com \
        redis.googleapis.com \
        cloudbuild.googleapis.com \
        artifactregistry.googleapis.com \
        compute.googleapis.com \
        secretmanager.googleapis.com
    
    print_info "GCP configured successfully!"
}

create_terraform_backend() {
    print_info "Creating Terraform backend..."
    
    TERRAFORM_BUCKET="${PROJECT_ID}-terraform-state"
    
    # Check if bucket exists
    if gsutil ls -b "gs://${TERRAFORM_BUCKET}" &> /dev/null; then
        print_warning "Terraform state bucket already exists"
    else
        print_info "Creating Terraform state bucket..."
        gsutil mb -p "$PROJECT_ID" -l "$REGION" "gs://${TERRAFORM_BUCKET}"
        gsutil versioning set on "gs://${TERRAFORM_BUCKET}"
        print_info "Terraform state bucket created!"
    fi
}

build_and_push_images() {
    print_info "Building and pushing Docker images..."
    
    # Configure Docker for GCP
    gcloud auth configure-docker "${REGION}-docker.pkg.dev"
    
    # Create Artifact Registry repository
    print_info "Creating Artifact Registry repository..."
    gcloud artifacts repositories create sportbot \
        --repository-format=docker \
        --location="$REGION" \
        --description="Sportbot Docker images" || true
    
    # Build and push using Cloud Build
    print_info "Building images with Cloud Build..."
    gcloud builds submit --config=cloudbuild.yaml \
        --substitutions=_REGION="$REGION"
    
    print_info "Images built and pushed successfully!"
}

deploy_infrastructure() {
    print_info "Deploying infrastructure with Terraform..."
    
    cd terraform
    
    # Initialize Terraform
    print_info "Initializing Terraform..."
    terraform init \
        -backend-config="bucket=${TERRAFORM_BUCKET}" \
        -backend-config="prefix=terraform/state"
    
    # Plan
    print_info "Planning infrastructure changes..."
    terraform plan -out=tfplan
    
    # Apply
    read -p "Apply these changes? (yes/no): " confirm
    if [ "$confirm" = "yes" ]; then
        print_info "Applying infrastructure changes..."
        terraform apply tfplan
        print_info "Infrastructure deployed successfully!"
    else
        print_warning "Deployment cancelled"
        exit 0
    fi
    
    cd ..
}

get_outputs() {
    print_info "Getting deployment outputs..."
    
    cd terraform
    
    echo ""
    echo "=========================================="
    echo "Deployment Complete!"
    echo "=========================================="
    echo ""
    
    DASHBOARD_URL=$(terraform output -raw dashboard_url)
    WEBSOCKET_URL=$(terraform output -raw websocket_service_url)
    
    echo "Dashboard URL: $DASHBOARD_URL"
    echo "WebSocket URL: $WEBSOCKET_URL"
    echo ""
    echo "To view logs:"
    echo "  gcloud run services logs read sportbot-ingestion --region=$REGION"
    echo "  gcloud run services logs read sportbot-analytics --region=$REGION"
    echo ""
    echo "To update match IDs:"
    echo "  cd terraform"
    echo "  terraform apply -var='football_matches=NEW_MATCH_IDS'"
    echo ""
    
    cd ..
}

# Main execution
main() {
    print_info "Starting Sportbot GCP Deployment"
    echo ""
    
    check_prerequisites
    configure_gcp
    create_terraform_backend
    build_and_push_images
    deploy_infrastructure
    get_outputs
    
    print_info "Deployment complete! 🎉"
}

# Run main function
main
