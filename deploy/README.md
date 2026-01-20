# 🐳 Despliegue con Docker

Este directorio contiene la configuración para desplegar BiblioTech Pro utilizando contenedores Docker.

## Arquitectura de Contenedores

```
┌─────────────────────────────────────────────────────────────┐
│                    Docker Compose                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────┐         ┌─────────────────────┐       │
│  │   nginx:alpine  │         │ gvenzl/oracle-xe:21 │       │
│  │   Puerto: 8000  │         │    Puerto: 1521     │       │
│  │   (Frontend)    │         │   (Base de Datos)   │       │
│  └────────┬────────┘         └──────────┬──────────┘       │
│           │                             │                   │
│           └──────────┬──────────────────┘                   │
│                      │                                      │
│              bibliotech-network                             │
└──────────────────────┼──────────────────────────────────────┘
                       │
            ┌──────────┴──────────┐
            │  Spring Boot :9091  │
            │     (Backend)       │
            │    (Host local)     │
            └─────────────────────┘
```

## Inicio Rápido

### 1. Prerequisitos

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) instalado
- Docker Compose (incluido en Docker Desktop)

### 2. Configurar Variables de Entorno

```bash
# Copiar plantilla
cp .env.example .env

# Editar con tus valores
notepad .env
```

### 3. Iniciar Servicios

```bash
# Desde la raíz del proyecto
docker-compose up -d
```

### 4. Verificar Estado

```bash
# Ver logs en tiempo real
docker-compose logs -f

# Ver estado de contenedores
docker-compose ps

# Health check de Nginx
curl http://localhost:8000/health
```

### 5. Iniciar Backend (en el host)

```bash
cd backend
./start.ps1
```

## Comandos Útiles

| Comando | Descripción |
|---------|-------------|
| `docker-compose up -d` | Iniciar servicios en background |
| `docker-compose down` | Detener servicios |
| `docker-compose down -v` | Detener y borrar volúmenes (¡BORRA DATOS!) |
| `docker-compose logs -f nginx` | Ver logs de Nginx |
| `docker-compose logs -f oracle-db` | Ver logs de Oracle |
| `docker-compose restart nginx` | Reiniciar solo Nginx |
| `docker-compose exec oracle-db sqlplus` | Conectar a Oracle |

## Estructura de Archivos

```
deploy/
└── nginx/
    └── default.conf    # Configuración de Nginx para Docker

docker-compose.yml      # Orquestación de servicios
.env.example            # Plantilla de variables
.env                    # Variables reales (NO COMMITEAR)
```

## Notas Importantes

### Base de Datos Oracle

- **Primera ejecución**: Oracle tarda ~3-5 minutos en inicializarse
- Los scripts en `db/` se ejecutan automáticamente al crear el contenedor
- Los datos persisten en el volumen `bibliotech-oracle-data`

### Nginx

- Sirve los archivos de `frontend/` en el puerto 8000
- Proxy reverso hacia el backend en `host.docker.internal:9091`
- Compresión Gzip habilitada
- Headers de seguridad configurados

### Backend

- **NO está containerizado** (se ejecuta en el host)
- Esto facilita el desarrollo y debugging
- Para producción, añadir al docker-compose.yml

## Troubleshooting

### Error: "Cannot connect to backend"
```bash
# Verificar que el backend está corriendo
curl http://localhost:9091/actuator/health

# En Docker Desktop, habilitar host.docker.internal
# (habilitado por defecto en Windows/Mac)
```

### Error: "Oracle connection refused"
```bash
# Esperar a que Oracle termine de inicializarse
docker-compose logs -f oracle-db

# Buscar: "DATABASE IS READY TO USE!"
```

### Reiniciar desde cero
```bash
docker-compose down -v
docker-compose up -d
```
