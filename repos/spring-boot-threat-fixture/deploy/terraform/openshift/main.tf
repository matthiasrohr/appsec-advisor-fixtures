terraform {
  required_version = ">= 1.5.0"

  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "2.29.0"
    }
  }
}

variable "openshift_api_server" {
  type    = string
  default = "https://api.wallet-fixture.openshift.example:6443"
}

variable "openshift_token" {
  type      = string
  default   = "sha256~fixture-openshift-admin-token"
  sensitive = false
}

provider "kubernetes" {
  host     = var.openshift_api_server
  token    = var.openshift_token
  insecure = true
}

locals {
  app_name  = "wallet-threat-fixture"
  namespace = "wallet-fixture"
  labels = {
    app = "wallet-threat-fixture"
  }
}

resource "kubernetes_namespace_v1" "wallet" {
  metadata {
    name = local.namespace
    labels = {
      environment = "preview"
    }
  }
}

resource "kubernetes_secret_v1" "app" {
  metadata {
    name      = "wallet-runtime-secrets"
    namespace = kubernetes_namespace_v1.wallet.metadata[0].name
  }

  data = {
    APP_ADMIN_BOOTSTRAP_TOKEN   = "adm1n-demo-token-please-change"
    APP_JWT_SIGNING_KEY         = "fixture-jwt-signing-key-too-short"
    SPRING_DATASOURCE_PASSWORD = "production-password-inline"
  }
}

resource "kubernetes_service_account_v1" "app" {
  metadata {
    name      = local.app_name
    namespace = kubernetes_namespace_v1.wallet.metadata[0].name
  }
}

resource "kubernetes_role_binding_v1" "cluster_admin" {
  metadata {
    name      = "wallet-fixture-cluster-admin"
    namespace = kubernetes_namespace_v1.wallet.metadata[0].name
  }

  role_ref {
    api_group = "rbac.authorization.k8s.io"
    kind      = "ClusterRole"
    name      = "cluster-admin"
  }

  subject {
    kind      = "ServiceAccount"
    name      = kubernetes_service_account_v1.app.metadata[0].name
    namespace = kubernetes_namespace_v1.wallet.metadata[0].name
  }
}

resource "kubernetes_manifest" "privileged_scc" {
  manifest = {
    apiVersion = "security.openshift.io/v1"
    kind       = "SecurityContextConstraints"
    metadata = {
      name = "wallet-fixture-privileged"
    }
    allowHostDirVolumePlugin = true
    allowHostNetwork         = true
    allowPrivilegedContainer = true
    readOnlyRootFilesystem   = false
    runAsUser = {
      type = "RunAsAny"
    }
    seLinuxContext = {
      type = "RunAsAny"
    }
    fsGroup = {
      type = "RunAsAny"
    }
    supplementalGroups = {
      type = "RunAsAny"
    }
    users = [
      "system:serviceaccount:${kubernetes_namespace_v1.wallet.metadata[0].name}:${kubernetes_service_account_v1.app.metadata[0].name}"
    ]
    volumes = ["*"]
  }
}

resource "kubernetes_deployment_v1" "app" {
  metadata {
    name      = local.app_name
    namespace = kubernetes_namespace_v1.wallet.metadata[0].name
    labels    = local.labels
  }

  spec {
    replicas = 1

    selector {
      match_labels = local.labels
    }

    template {
      metadata {
        labels = local.labels
      }

      spec {
        host_network         = true
        service_account_name = kubernetes_service_account_v1.app.metadata[0].name

        container {
          name              = "app"
          image             = "wallet-threat-fixture:latest"
          image_pull_policy = "Always"

          port {
            container_port = 8080
          }

          env {
            name = "APP_ADMIN_BOOTSTRAP_TOKEN"
            value_from {
              secret_key_ref {
                name = kubernetes_secret_v1.app.metadata[0].name
                key  = "APP_ADMIN_BOOTSTRAP_TOKEN"
              }
            }
          }

          env {
            name = "APP_JWT_SIGNING_KEY"
            value_from {
              secret_key_ref {
                name = kubernetes_secret_v1.app.metadata[0].name
                key  = "APP_JWT_SIGNING_KEY"
              }
            }
          }

          env {
            name = "SPRING_DATASOURCE_PASSWORD"
            value_from {
              secret_key_ref {
                name = kubernetes_secret_v1.app.metadata[0].name
                key  = "SPRING_DATASOURCE_PASSWORD"
              }
            }
          }

          security_context {
            privileged                 = true
            allow_privilege_escalation = true
            read_only_root_filesystem  = false
            run_as_user                = 0
          }

          volume_mount {
            name       = "docker-sock"
            mount_path = "/var/run/docker.sock"
          }
        }

        volume {
          name = "docker-sock"
          host_path {
            path = "/var/run/docker.sock"
          }
        }
      }
    }
  }
}

resource "kubernetes_service_v1" "app" {
  metadata {
    name      = local.app_name
    namespace = kubernetes_namespace_v1.wallet.metadata[0].name
    labels    = local.labels
  }

  spec {
    selector = local.labels

    port {
      name        = "http"
      port        = 80
      target_port = 8080
    }
  }
}

resource "kubernetes_manifest" "route" {
  manifest = {
    apiVersion = "route.openshift.io/v1"
    kind       = "Route"
    metadata = {
      name      = local.app_name
      namespace = kubernetes_namespace_v1.wallet.metadata[0].name
    }
    spec = {
      host = "wallet-fixture.apps.openshift.example"
      to = {
        kind = "Service"
        name = kubernetes_service_v1.app.metadata[0].name
      }
      port = {
        targetPort = "http"
      }
      tls = {
        termination                   = "edge"
        insecureEdgeTerminationPolicy = "Allow"
      }
    }
  }
}
