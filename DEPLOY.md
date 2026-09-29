# Despliegue de Jamix Fashion

Cada parte va donde mejor funciona:

| Parte | Dónde | Por qué |
|---|---|---|
| Tienda React y admin Vue | **Vercel** | Son sitios estáticos: CDN global y despliegues gratuitos |
| Catálogo (Spring Boot, Java) | **Render** (Docker, plan free) | Vercel no ejecuta servidores Java de larga duración |
| Formulario de eventos (Node/Express) | **Render** (Node, plan free) | Servidor Express siempre activo |
| Base de datos MySQL | **Aiven** (plan free) | Render no ofrece MySQL gratuito; Aiven sí |

**Limitaciones de los planes gratuitos:**
- **Render free** duerme el servicio tras ~15 min sin visitas. La primera visita después tarda ~1 min en despertar Spring Boot. Para una tienda real conviene el plan pagado más barato.
- **Railway** solo da un crédito inicial y luego es de pago.
- **Fly.io** ya no tiene plan gratuito para cuentas nuevas; cobra por uso, en montos pequeños.

El orden importa: **1. base de datos → 2. backend → 3. frontend → 4. conectar las URLs.**

## 1. Base de datos: Aiven MySQL (free)

1. Crea una cuenta en https://aiven.io → **Create service** → **MySQL** → plan **Free**.
2. En **Overview** copia *Host*, *Port*, *User* (`avnadmin`) y *Password*.
3. Crea la base (la exige Flyway) desde tu equipo:
   ```powershell
   & "C:\Program Files\MySQL\MySQL Server 26.7\bin\mysql.exe" -h HOST -P PORT -u avnadmin -p --ssl-mode=REQUIRED -e "CREATE DATABASE jamix_fashion CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
   ```
   Para usar dos cuentas separadas (app y migraciones), adapta `db/setup-mysql.sql` a este servidor. Con una sola
   cuenta basta para empezar.

## 2. Backend: Render

1. Sube el repositorio a GitHub (ver README).
2. Render → **New → Blueprint** → elige el repositorio. `render.yaml` crea `jamix-catalog` (Docker) y `jamix-leads` (Node).
3. Completa las variables que Render te pide:

   | Variable | Valor |
   |---|---|
   | `DB_URL` | `jdbc:mysql://HOST:PORT/jamix_fashion?sslMode=REQUIRED&connectionTimeZone=UTC&characterEncoding=UTF-8` |
   | `DB_USERNAME` / `DB_PASSWORD` | Los de Aiven |
   | `DB_MIGRATION_*` | Vacías: Flyway usará la misma cuenta |
   | `JAMIX_ADMIN_PASSWORD` | La primera contraseña de `admin` (luego se cambia en el panel) |
   | `JAMIX_CUSTOMER_PASSWORD` | Contraseña del usuario de prueba `cliente` |
   | `JAMIX_JWT_SECRET` | Render la genera sola |
   | Las URLs públicas | Ya vienen en `render.yaml` (paso 4) |

4. Al primer arranque, Flyway crea las tablas y carga el catálogo. Prueba:
   `https://jamix-catalog.onrender.com/api/v1/products`

## 3. Frontend: Vercel

Con la CLI (requiere Node):

```powershell
npm i -g vercel
vercel login

cd platform/storefront-react
vercel link                      # crea el proyecto "jamix-tienda"
vercel env add VITE_CATALOG_API_URL production    # pega: https://jamix-catalog.onrender.com
vercel --prod

cd ../admin-vue
vercel link                      # otro proyecto: "jamix-stock"
vercel env add VITE_CATALOG_API_URL production
vercel --prod
```

Desde la web: **Vercel → Add New → Project → importa el repo**. Pon **Root Directory** en
`platform/storefront-react` (y en otro proyecto, `platform/admin-vue`) y agrega la variable
`VITE_CATALOG_API_URL`. `vercel.json` ya define la compilación, el *fallback* de la SPA y la caché.

`VITE_CATALOG_API_URL` se incrusta al **compilar**. Si la cambias, vuelve a desplegar.

## 4. Conectar las URLs

Las URLs públicas están en `render.yaml`, así que el Blueprint las aplica solo. Si cambias un dominio (por ejemplo,
un dominio propio para la tienda), edítalo ahí y haz push: Render sincroniza el Blueprint y reinicia los servicios.

**jamix-catalog**:

| Variable | Ejemplo |
|---|---|
| `JAMIX_CORS_ORIGINS` | `https://jamix-tienda.vercel.app,https://jamix-stock.vercel.app` |
| `JAMIX_SITE_URL` | `https://jamix-catalog.onrender.com` (la web con SSR, la que indexa Google) |
| `JAMIX_SHOP_URL` | `https://jamix-tienda.vercel.app` |
| `JAMIX_LEADS_URL` | `https://jamix-leads.onrender.com/api/leads` |

**jamix-leads**:

| Variable | Ejemplo |
|---|---|
| `SITE_URL` | `https://jamix-catalog.onrender.com` |
| `CORS_ORIGINS` | `https://jamix-catalog.onrender.com,https://jamix-tienda.vercel.app` |

Si la tienda muestra "No pudimos cargar el catálogo", casi siempre
falta su dominio en `JAMIX_CORS_ORIGINS`.

## Probar la imagen Docker en tu equipo (opcional)

```powershell
cd platform/catalog-service
docker build -t jamix-catalog .
docker run -p 8080:8080 --env-file .env -e DB_URL="jdbc:mysql://host.docker.internal:3306/jamix_fashion?sslMode=REQUIRED&connectionTimeZone=UTC&characterEncoding=UTF-8" jamix-catalog
```

Dentro del contenedor, `localhost` es el propio contenedor. Para llegar al MySQL de tu PC se usa
`host.docker.internal`, y tu MySQL debe aceptar conexiones de ese origen.
