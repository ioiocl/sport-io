terraform {
  required_version = ">= 1.0"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.0"
    }
  }

  backend "gcs" {
    bucket = "ioio-finbot-sportbot-tf-state"
    prefix = "terraform/state"
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
}

# Enable required APIs
resource "google_project_service" "required_apis" {
  for_each = toset([
    "run.googleapis.com",
    "vpcaccess.googleapis.com",
    "redis.googleapis.com",
    "cloudbuild.googleapis.com",
    "artifactregistry.googleapis.com",
    "compute.googleapis.com",
    "servicenetworking.googleapis.com",
  ])

  service            = each.value
  disable_on_destroy = false
}

# VPC Network
resource "google_compute_network" "sportbot_vpc" {
  name                    = "sportbot-vpc"
  auto_create_subnetworks = false

  depends_on = [google_project_service.required_apis]
}

# Subnet for Redis and VPC Connector
resource "google_compute_subnetwork" "sportbot_subnet" {
  name          = "sportbot-subnet"
  ip_cidr_range = "10.0.0.0/24"
  region        = var.region
  network       = google_compute_network.sportbot_vpc.id
}

# Reserve IP range for Private Service Access
resource "google_compute_global_address" "private_ip_address" {
  name          = "sportbot-private-ip"
  purpose       = "VPC_PEERING"
  address_type  = "INTERNAL"
  prefix_length = 16
  network       = google_compute_network.sportbot_vpc.id
}

# Create Private Service Connection
resource "google_service_networking_connection" "private_vpc_connection" {
  network                 = google_compute_network.sportbot_vpc.id
  service                 = "servicenetworking.googleapis.com"
  reserved_peering_ranges = [google_compute_global_address.private_ip_address.name]
  
  depends_on = [google_project_service.required_apis]
}

# VPC Access Connector for Cloud Run to Redis
resource "google_vpc_access_connector" "connector" {
  name          = "sportbot-connector"
  region        = var.region
  network       = google_compute_network.sportbot_vpc.name
  ip_cidr_range = "10.8.0.0/28"

  depends_on = [google_project_service.required_apis]
}

# Redis Instance (Memorystore)
resource "google_redis_instance" "sportbot_redis" {
  name           = "sportbot-redis"
  tier           = var.redis_tier
  memory_size_gb = var.redis_memory_gb
  region         = var.region
  redis_version  = "REDIS_7_0"

  authorized_network = google_compute_network.sportbot_vpc.id
  connect_mode       = "PRIVATE_SERVICE_ACCESS"

  display_name = "Sportbot Redis"

  depends_on = [
    google_project_service.required_apis,
    google_service_networking_connection.private_vpc_connection
  ]
}

# Artifact Registry for Docker images
resource "google_artifact_registry_repository" "sportbot_repo" {
  location      = var.region
  repository_id = "sportbot"
  description   = "Sportbot Docker images"
  format        = "DOCKER"

  depends_on = [google_project_service.required_apis]
}

# Service Account for Cloud Run services
resource "google_service_account" "sportbot_sa" {
  account_id   = "sportbot-services"
  display_name = "Sportbot Services"
  description  = "Service account for Sportbot Cloud Run services"
}

# Grant permissions to Service Account
resource "google_project_iam_member" "sportbot_sa_roles" {
  for_each = toset([
    "roles/redis.editor",
    "roles/logging.logWriter",
    "roles/cloudtrace.agent",
  ])

  project = var.project_id
  role    = each.value
  member  = "serviceAccount:${google_service_account.sportbot_sa.email}"
}

# Secret Manager for API Key
resource "google_secret_manager_secret" "football_api_key" {
  secret_id = "football-api-key"

  replication {
    auto {}
  }

  depends_on = [google_project_service.required_apis]
}

resource "google_secret_manager_secret_version" "football_api_key_version" {
  secret      = google_secret_manager_secret.football_api_key.id
  secret_data = var.football_api_key
}

# Grant access to secret
resource "google_secret_manager_secret_iam_member" "sportbot_sa_secret_access" {
  secret_id = google_secret_manager_secret.football_api_key.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.sportbot_sa.email}"
}

# Cloud Run Service - Ingestion
resource "google_cloud_run_v2_service" "ingestion_service" {
  name     = "sportbot-ingestion"
  location = var.region

  template {
    service_account = google_service_account.sportbot_sa.email

    vpc_access {
      connector = google_vpc_access_connector.connector.id
      egress    = "PRIVATE_RANGES_ONLY"
    }

    scaling {
      min_instance_count = var.min_instances
      max_instance_count = var.max_instances
    }

    containers {
      image = "${var.region}-docker.pkg.dev/${var.project_id}/sportbot/ingestion-service:latest"

      resources {
        limits = {
          cpu    = "1"
          memory = "512Mi"
        }
      }

      startup_probe {
        initial_delay_seconds = 0
        timeout_seconds       = 240
        period_seconds        = 240
        failure_threshold     = 1
        http_get {
          path = "/health"
          port = 8080
        }
      }

      env {
        name  = "FOOTBALL_API_HOST"
        value = "v3.football.api-sports.io"
      }

      env {
        name  = "FOOTBALL_BASE_URL"
        value = "https://v3.football.api-sports.io"
      }

      env {
        name = "FOOTBALL_API_KEY"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.football_api_key.secret_id
            version = "latest"
          }
        }
      }

      env {
        name  = "AUTO_DISCOVER_LIVE"
        value = "true"
      }

      env {
        name  = "POLL_INTERVAL"
        value = var.poll_interval
      }

      env {
        name  = "REDIS_HOST"
        value = google_redis_instance.sportbot_redis.host
      }

      env {
        name  = "REDIS_PORT"
        value = tostring(google_redis_instance.sportbot_redis.port)
      }

      env {
        name  = "QUARKUS_REDIS_HOSTS"
        value = "redis://${google_redis_instance.sportbot_redis.host}:${google_redis_instance.sportbot_redis.port}"
      }
    }
  }

  traffic {
    type    = "TRAFFIC_TARGET_ALLOCATION_TYPE_LATEST"
    percent = 100
  }

  depends_on = [
    google_vpc_access_connector.connector,
    google_redis_instance.sportbot_redis,
    google_secret_manager_secret_version.football_api_key_version
  ]
}

# Cloud Run Service - Analytics
resource "google_cloud_run_v2_service" "analytics_service" {
  name     = "sportbot-analytics"
  location = var.region

  template {
    service_account = google_service_account.sportbot_sa.email

    vpc_access {
      connector = google_vpc_access_connector.connector.id
      egress    = "PRIVATE_RANGES_ONLY"
    }

    scaling {
      min_instance_count = var.min_instances
      max_instance_count = var.max_instances
    }

    containers {
      image = "${var.region}-docker.pkg.dev/${var.project_id}/sportbot/analytics-service:latest"

      resources {
        limits = {
          cpu    = "2"
          memory = "1Gi"
        }
      }

      startup_probe {
        initial_delay_seconds = 0
        timeout_seconds       = 240
        period_seconds        = 240
        failure_threshold     = 1
        http_get {
          path = "/health"
          port = 8080
        }
      }

      env {
        name  = "ANALYTICS_MATCHES"
        value = var.football_matches
      }

      env {
        name  = "SNAPSHOT_INTERVAL"
        value = var.snapshot_interval
      }

      env {
        name  = "MONTE_CARLO_HORIZON_MINUTES"
        value = var.monte_carlo_horizon
      }

      env {
        name  = "MONTE_CARLO_SIMULATIONS"
        value = var.monte_carlo_simulations
      }

      env {
        name  = "ARIMA_HORIZON_MINUTES"
        value = var.arima_horizon
      }

      env {
        name  = "REDIS_HOST"
        value = google_redis_instance.sportbot_redis.host
      }

      env {
        name  = "REDIS_PORT"
        value = tostring(google_redis_instance.sportbot_redis.port)
      }

      env {
        name  = "QUARKUS_REDIS_HOSTS"
        value = "redis://${google_redis_instance.sportbot_redis.host}:${google_redis_instance.sportbot_redis.port}"
      }
    }
  }

  traffic {
    type    = "TRAFFIC_TARGET_ALLOCATION_TYPE_LATEST"
    percent = 100
  }

  depends_on = [
    google_vpc_access_connector.connector,
    google_redis_instance.sportbot_redis
  ]
}

# Cloud Run Service - WebSocket API
resource "google_cloud_run_v2_service" "websocket_service" {
  name     = "sportbot-websocket"
  location = var.region

  template {
    service_account = google_service_account.sportbot_sa.email

    vpc_access {
      connector = google_vpc_access_connector.connector.id
      egress    = "PRIVATE_RANGES_ONLY"
    }

    scaling {
      min_instance_count = var.min_instances
      max_instance_count = var.max_instances
    }

    containers {
      image = "${var.region}-docker.pkg.dev/${var.project_id}/sportbot/websocket-api:latest"

      resources {
        limits = {
          cpu    = "1"
          memory = "512Mi"
        }
      }

      startup_probe {
        initial_delay_seconds = 0
        timeout_seconds       = 240
        period_seconds        = 240
        failure_threshold     = 1
        http_get {
          path = "/health"
          port = 8080
        }
      }

      ports {
        container_port = 8083
      }

      env {
        name  = "BROADCAST_MATCHES"
        value = var.football_matches
      }

      env {
        name  = "BROADCAST_INTERVAL"
        value = var.broadcast_interval
      }

      env {
        name  = "REDIS_HOST"
        value = google_redis_instance.sportbot_redis.host
      }

      env {
        name  = "REDIS_PORT"
        value = tostring(google_redis_instance.sportbot_redis.port)
      }

      env {
        name  = "QUARKUS_REDIS_HOSTS"
        value = "redis://${google_redis_instance.sportbot_redis.host}:${google_redis_instance.sportbot_redis.port}"
      }
    }
  }

  traffic {
    type    = "TRAFFIC_TARGET_ALLOCATION_TYPE_LATEST"
    percent = 100
  }

  depends_on = [
    google_vpc_access_connector.connector,
    google_redis_instance.sportbot_redis
  ]
}

# Allow public access to WebSocket API
resource "google_cloud_run_v2_service_iam_member" "websocket_public" {
  location = google_cloud_run_v2_service.websocket_service.location
  name     = google_cloud_run_v2_service.websocket_service.name
  role     = "roles/run.invoker"
  member   = "allUsers"
}

# Cloud Run Service - Dashboard
resource "google_cloud_run_v2_service" "dashboard_service" {
  name     = "sportbot-dashboard"
  location = var.region

  template {
    containers {
      image = "${var.region}-docker.pkg.dev/${var.project_id}/sportbot/dashboard:latest"

      resources {
        limits = {
          cpu    = "1"
          memory = "512Mi"
        }
      }

      ports {
        container_port = 80
      }

      env {
        name  = "WEBSOCKET_URL"
        value = google_cloud_run_v2_service.websocket_service.uri
      }
    }

    scaling {
      min_instance_count = 0
      max_instance_count = 10
    }
  }

  traffic {
    type    = "TRAFFIC_TARGET_ALLOCATION_TYPE_LATEST"
    percent = 100
  }

  depends_on = [google_cloud_run_v2_service.websocket_service]
}

# Allow public access to Dashboard
resource "google_cloud_run_v2_service_iam_member" "dashboard_public" {
  location = google_cloud_run_v2_service.dashboard_service.location
  name     = google_cloud_run_v2_service.dashboard_service.name
  role     = "roles/run.invoker"
  member   = "allUsers"
}
