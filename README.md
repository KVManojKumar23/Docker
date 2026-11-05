# Comprehensive Docker Guide: Architecture, Core Concepts & Flowcharts

A complete, production-oriented reference guide explaining Docker's underlying architecture, lifecycle, networking, storage engines, and operational commands with ASCII flowcharts.

---

## Table of Contents
1. [What is Docker?](#1-what-is-docker)
2. [Docker Architecture](#2-docker-architecture)
3. [Core Concepts Breakdown](#3-core-concepts-breakdown)
4. [Image Build & Layer Caching Flowchart](#4-image-build--layer-caching-flowchart)
5. [Container Lifecycle & State Machine](#5-container-lifecycle--state-machine)
6. [Storage Architecture: Volumes vs. Bind Mounts](#6-storage-architecture-volumes-vs-bind-mounts)
7. [Docker Networking Engine](#7-docker-networking-engine)
8. [Multi-Stage Builds & Optimization](#8-multi-stage-builds--optimization)
9. [Docker Compose Multi-Container Orchestration](#9-docker-compose-multi-container-orchestration)
10. [Essential Command Reference](#10-essential-command-reference)

---

## 1. What is Docker?

Docker is an open-source platform designed to build, package, distribute, and run applications in isolated environments called **containers**. 

Unlike virtual machines (VMs) that emulate complete hardware stacks and run guest operating systems via a hypervisor, containers share the host machine's Linux kernel and isolate processes using two core Linux kernel primitives:
* **Namespaces:** Provide isolation (Process IDs, Mount points, Network interfaces, User IDs, Inter-process Communication).
* **Control Groups (`cgroups`):** Provide resource metering and restriction (CPU, Memory, Disk I/O, Network bandwidth).

---

## 2. Docker Architecture

Docker operates on a **Client-Server** model:
* **Docker Client (`docker` CLI):** The primary user interface that accepts commands and communicates with the daemon via UNIX sockets or a REST API.
* **Docker Host (`dockerd` Daemon):** The background service managing containers, images, networks, and storage volumes. It internally interfaces with `containerd` and `runc`.
* **Docker Registry:** A centralized or distributed image catalog (Docker Hub, AWS ECR, GCP Artifact Registry, GitHub Container Registry).

```
+-----------------------------------------------------------------------------------+
|                                  Docker Client                                    |
|         (CLI: docker build, docker pull, docker run, docker-compose)              |
+-----------------------------------------+-----------------------------------------+
                                          |
                                          | REST API / UNIX Socket (/var/run/docker.sock)
                                          v
+-----------------------------------------------------------------------------------+
|                                   Docker Host                                     |
|                                                                                   |
|   +---------------------------------------------------------------------------+   |
|   |                           Docker Engine (dockerd)                         |   |
|   +-------------------------------------+-------------------------------------+   |
|                                         |                                         |
|                                         v                                         |
|   +---------------------------------------------------------------------------+   |
|   |                            containerd & runc                              |   |
|   +------------------+------------------+------------------+------------------+   |
|                      |                  |                  |                      |
|                      v                  v                  v                      |
|                 [ Images ]        [ Containers ]      [ Volumes ]                 |
|                - Read-only        - Read/Write Layer  - Named Volumes             |
|                - Layered          - Namespaces        - Bind Mounts               |
|                - Content-hash     - cgroups limits    - tmpfs                     |
|                                                                                   |
|   +---------------------------------------------------------------------------+   |
|   | Virtual Networks: bridge (default), host, overlay, macvlan, none         |   |
|   +---------------------------------------------------------------------------+   |
+-----------------------------------------+-----------------------------------------+
                                          |
                                          | TLS Encrypted Push / Pull
                                          v
+-----------------------------------------------------------------------------------+
|                                 Docker Registry                                   |
|                (Docker Hub / GitHub Packages / AWS ECR / Private)                 |
+-----------------------------------------------------------------------------------+
```

---

## 3. Core Concepts Breakdown

| Concept | Description | Mutability | Underlying Technology |
| :--- | :--- | :--- | :--- |
| **Dockerfile** | Plaintext script containing declarative instructions to assemble an image. | Editable source | Text / Configuration |
| **Docker Image** | Packaged, portable snapshot containing system libraries, runtime, dependencies, and application binaries. | **Immutable (Read-Only)** | OverlayFS / UnionFS layers |
| **Container** | Active, running runtime instance of a Docker image. | **Mutable (Ephemeral Read/Write Layer)** | Linux Namespaces + cgroups |
| **Volume** | Host filesystem storage managed completely by Docker, isolated from container lifecycle. | Persistent | Host filesystem `/var/lib/docker/volumes` |
| **Bind Mount** | Direct file or directory link from host OS to container filesystem. | Persistent | Host absolute path mapping |
| **Network** | Software-defined virtual bridge routing packets between containers and external networks. | Configurable | Linux iptables, veth pairs, IPVS |

---

## 4. Image Build & Layer Caching Flowchart

Docker builds images sequentially using an **Union File System (UnionFS)**. Each command in a `Dockerfile` generates a distinct, read-only cache layer. Unchanged instructions are reused directly from cache.

```
                  [ Dockerfile Execution ]
                             |
                             v
               +---------------------------+
               |    FROM node:18-alpine    |  ---> Layer 1: Base Alpine OS + Node (Cached)
               +-------------+-------------+
                             |
                             v
               +---------------------------+
               |     WORKDIR /usr/app      |  ---> Layer 2: Working Directory metadata
               +-------------+-------------+
                             |
                             v
               +---------------------------+
               | COPY package*.json ./     |  ---> Layer 3: Dependency manifests
               +-------------+-------------+
                             |
                             v
               +---------------------------+
               |     RUN npm install       |  ---> Layer 4: Installed node_modules
               +-------------+-------------+
                             |
                             v
               +---------------------------+
               |        COPY . .           |  ---> Layer 5: Application source code
               +-------------+-------------+       (Invalidates cache if code changed)
                             |
                             v
               +---------------------------+
               |        EXPOSE 3000        |  ---> Layer 6: Port binding metadata
               +-------------+-------------+
                             |
                             v
               +---------------------------+
               |  CMD ["npm", "start"]     |  ---> Layer 7: Execution instruction
               +-------------+-------------+
                             |
                             v
          =======================================
          [ Immutable Final Docker Image Artifact ]
          =======================================
```

> **Performance Tip:** Order steps from least frequently changed (e.g., base images, package managers) to most frequently changed (e.g., local source code) to maximize layer cache hits.

---

## 5. Container Lifecycle & State Machine

A container transitions through deterministic operational states from creation to deletion:

```
                            +--------------------+
                            |    Docker Image    |
                            +---------+----------+
                                      |
                        docker create | (docker run = create + start)
                                      v
                            +--------------------+
             +------------->|      Created       |
             |              +---------+----------+
             |                        |
             |           docker start |
             |                        v
             |              +--------------------+   docker pause    +--------------------+
             |  +---------->|      Running       | ----------------> |       Paused       |
             |  |           |  (Active Process)  | <---------------- |  (SIGSTOP frozen)  |
             |  |           +---------+----------+   docker unpause  +--------------------+
             |  |                     |
             |  |         docker stop | (SIGTERM sent -> 10s grace -> SIGKILL)
             |  |                  or |
             |  |         Main PID Exits (Code 0 or error code > 0)
             |  |                  or |
             |  |         docker kill | (Immediate SIGKILL)
             |  |                     v
             |  |           +--------------------+
             |  +-----------+      Stopped       |
             |  docker start|      (Exited)      |
             |              +---------+----------+
             |                        |
             |              docker rm | (or --rm on run)
             |                        v
             +------------------ [ Destroyed ]
```

---

## 6. Storage Architecture: Volumes vs. Bind Mounts

Any data written directly inside a container's default filesystem is written to a thin, ephemeral **Copy-on-Write (CoW)** layer. When the container is removed, that layer is discarded. Docker provides persistent storage strategies:

```
+---------------------------------------------------------------------------------+
|                                   Host Machine                                  |
|                                                                                 |
|   +--------------------------+                     +------------------------+   |
|   |       Named Volume       |                     |    Host Bind Mount     |   |
|   |   /var/lib/docker/       |                     |   /home/user/project/  |   |
|   |   volumes/db_data/_data  |                     |   config.yaml          |   |
|   +-------------+------------+                     +-----------+------------+   |
|                 |                                              |                |
|                 | Docker managed                               | Direct link    |
|                 v                                              v                |
+-----------------|----------------------------------------------|----------------+
| Container FS    v                                              v                |
|          /var/lib/postgresql/data                       /app/config.yaml        |
|                                                                                 |
|   +-------------------------------------------------------------------------+   |
|   |                   Container Ephemeral Read/Write Layer                  |   |
|   +-------------------------------------------------------------------------+   |
+---------------------------------------------------------------------------------+
```

### Storage Type Comparison

* **Named Volumes (`-v db_data:/var/lib/mysql`)**:
  * Stored in `/var/lib/docker/volumes/` managed directly by Docker.
  * Optimized for performance, database integrity, and easy backups.
  * Highly recommended for production environments.
* **Bind Mounts (`-v /local/path:/container/path`)**:
  * Direct map between an arbitrary host directory/file and the container.
  * High dependency on host system directory structures and file permissions.
  * Ideal for local development live-reloading.
* **tmpfs Mounts (`--tmpfs /tmp`)**:
  * Stored strictly in host memory. Never written to disk.
  * Ideal for storing sensitive keys, temporary tokens, or high-throughput scratchpads.

---

## 7. Docker Networking Engine

Docker isolates containers on virtual networks using Linux software bridges and iptables packet manipulation.

```
       [ Host Physical Network Interface (eth0: 192.168.1.50) ]
                                  |
                                  v
+-------------------------------------------------------------------+
|                        Docker Host Gateway                        |
|                                                                   |
|   [ Bridge Network (docker0: 172.17.0.1) ]                        |
|           |                                 |                     |
|           | vethA                           | vethB               |
|           v                                 v                     |
|   +------------------+             +------------------+           |
|   | Container A      |             | Container B      |           |
|   | IP: 172.17.0.2   |             | IP: 172.17.0.3   |           |
|   | Port: 3000       |             | Port: 5432       |           |
|   +--------+---------+             +--------+---------+           |
|            |                                |                     |
+------------|--------------------------------|---------------------+
             | Port Forwarding                | (Internal only)
             v (-p 8080:3000)                 |
     Host Port 8080 --------------------------+
```

### Network Driver Types

1. **Bridge (`bridge`)**: The default driver. Allocates an internal private subnet. Containers on user-defined custom bridge networks resolve each other automatically via container names through Docker's internal DNS.
2. **Host (`host`)**: Disables network namespace isolation. The container directly uses the host's network stack (no port forwarding required, minimal latency).
3. **Overlay (`overlay`)**: Enables communication across multiple distinct Docker hosts (Swarm mode or distributed setups) without OS-level routing.
4. **Macvlan (`macvlan`)**: Assigns a real MAC address to the container, making it appear as a physical device on the local network router.
5. **None (`none`)**: Completely detaches the container from any external or internal network interfaces (isolated compute).

---

## 8. Multi-Stage Builds & Optimization

Multi-stage builds eliminate compiler toolchains, package manager caches, and documentation from final deployment images, dramatically shrinking payload size and attack surface.

```dockerfile
# ==========================================
# Stage 1: Build & Compile
# ==========================================
FROM golang:1.22-alpine AS builder

WORKDIR /src
COPY go.mod go.sum ./
RUN go mod download

COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -ldflags="-w -s" -o /bin/app .

# ==========================================
# Stage 2: Minimal Production Runtime
# ==========================================
FROM scratch

# Import certificates from builder for HTTPS requests
COPY --from=builder /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/
COPY --from=builder /bin/app /usr/local/bin/app

USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["/usr/local/bin/app"]
```

---

## 9. Docker Compose Multi-Container Orchestration

`docker-compose` coordinates multi-service architectures including shared virtual networks, health dependencies, and volumes in declarative YAML syntax.

```yaml
version: '3.8'

services:
  web:
    build:
      context: .
      dockerfile: Dockerfile
    ports:
      - "80:8000"
    environment:
      - DATABASE_URL=postgres://appuser:secret@db:5432/appdb
    depends_on:
      db:
        condition: service_healthy
    networks:
      - backend-net
    restart: unless-stopped

  db:
    image: postgres:15-alpine
    environment:
      POSTGRES_USER: appuser
      POSTGRES_PASSWORD: secret
      POSTGRES_DB: appdb
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - backend-net
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U appuser -d appdb"]
      interval: 5s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
    driver: local

networks:
  backend-net:
    driver: bridge
```

---

## 10. Essential Command Reference

### Image Management
```bash
# Build an image with a specific tag using current context
docker build -t my-app:1.0.0 .

# List local images with sizes and IDs
docker images

# Remove an image from local store
docker rmi my-app:1.0.0

# Authenticate and push image to remote repository
docker login
docker tag my-app:1.0.0 registry.example.com/org/my-app:1.0.0
docker push registry.example.com/org/my-app:1.0.0
```

### Container Operations
```bash
# Run container detached with port-mapping, restart policy, and environment variables
docker run -d \
  --name web-service \
  -p 8080:80 \
  --restart unless-stopped \
  -e ENV=production \
  -v my-volume:/data \
  nginx:alpine

# Inspect real-time execution logs
docker logs -f --tail 100 web-service

# Open an interactive shell inside a running container
docker exec -it web-service /bin/sh

# List running containers (add -a to show stopped containers)
docker ps -a

# Stop, start, and delete containers
docker stop web-service
docker start web-service
docker rm -f web-service
```

### Networking & Storage
```bash
# Create and inspect user-defined bridge network
docker network create my-custom-net
docker network inspect my-custom-net

# Create and list independent persistent volumes
docker volume create app-storage
docker volume ls
```

### Diagnostics & Hygiene
```bash
# Monitor container resource metrics (CPU, RAM, Network I/O)
docker stats

# Clean up dangling images, unused networks, and exited containers
docker system prune -f

# Total aggressive cleanup including unused volumes
docker system prune -a --volumes
```
