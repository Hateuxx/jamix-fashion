# Jamix Fashion

Tienda online de ropa blanca para sesiones de fotos (San Juan de Lurigancho, Lima).

| Parte | Tecnología | Carpeta |
|---|---|---|
| Catálogo, ventas y web con SSR | Spring Boot 4 · Spring Data JPA · MySQL · Flyway | `platform/catalog-service` |
| Formulario de eventos → WhatsApp | Node.js · Express | `platform/leads-bff` |
| Tienda | React · Vite · Tailwind | `platform/storefront-react` |
| Panel de stock | Vue · Vite · Tailwind | `platform/admin-vue` |

La arquitectura completa está en [platform/README.md](platform/README.md).

## Qué puede hacer el cliente en la tienda

- **Armar el look de toda la familia:** agrega prendas indicando para quién son (Mamá, Papá, Hija…) y la talla, y envía
  el pedido completo en **un solo WhatsApp**, con el total.
- **Buscar, filtrar y ordenar:** por categoría, tipo de prenda, disponibilidad y precio. La búsqueda ignora tildes y los
  filtros quedan en la URL, para compartirlos.
- **Ficha rápida con galería:** fotos de modelo, espalda y detalle. Cada prenda tiene su enlace directo
  (`?producto=vestido-rita`).
- **La bolsa se conserva** si recarga la página, y en el celular aparece una barra fija con el total.

## Jamix Stock (panel del equipo)

Cada trabajador tiene **su propia cuenta** y un rol:

| Rol | Puede |
|---|---|
| Vendedor(a) | Ver el stock y registrar ventas (en tienda o por WhatsApp) |
| Encargado(a) | Además: ingresos de mercadería, devoluciones, mermas, conteos físicos y editar productos |
| Dueño(a) | Además: crear cuentas, cambiar roles, restablecer contraseñas y desactivar personas |

- **Historial:** cada cambio de stock queda registrado con quién, cuándo y por qué, incluidos los pedidos web.
  Se ve por prenda ("Historial") o para toda la tienda ("Actividad").
- **Cuentas nuevas:** una cuenta nueva o una contraseña restablecida es **temporal**, y al ingresar el sistema pide
  cambiarla.
- **Cierre de sesión inmediato:** desactivar a alguien o cambiar su rol cierra su sesión al instante.
- **Primer ingreso:** la primera cuenta de dueño(a) es `admin`, con la contraseña `JAMIX_ADMIN_PASSWORD` del `.env`.
  Solo se usa al crear la base; después, las contraseñas se cambian desde el panel.

## Requisitos

- JDK 21 o superior (no hace falta Maven: el proyecto trae `mvnw`)
- Node.js 22.9 o superior
- MySQL 8 o superior

## 1. Clonar

```bash
git clone https://github.com/<usuario>/<repositorio>.git
cd <repositorio>/platform
npm run setup        # instala las dependencias de los 4 servicios
```

## 2. Crear la base de datos (una sola vez)

Crea una copia local del script, que Git ignora, y cambia las dos contraseñas `CHANGE_ME` por unas seguras:

```bash
cd catalog-service
cp db/setup-mysql.sql db/setup-mysql.local.sql     # Windows: copy db\setup-mysql.sql db\setup-mysql.local.sql
mysql -u root -p -e "source db/setup-mysql.local.sql"
```

Así se crean la base `jamix_fashion` y dos usuarios con permisos mínimos. Las **tablas no se crean a mano**: Flyway
las crea al arrancar la app, a partir de `src/main/resources/db/migration`. Cuando termines, borra
`setup-mysql.local.sql`.

## 3. Configurar el `.env` local

```bash
cp .env.example .env          # Windows: copy .env.example .env
```

Completa `.env` con las mismas contraseñas del paso 2 y un `JAMIX_JWT_SECRET` propio:

```bash
node -e "console.log(require('crypto').randomBytes(48).toString('base64'))"
```

El archivo `.env` **nunca se sube** al repositorio, porque está en `.gitignore`. Para agregar una variable nueva,
ponla en `.env.example` sin su valor.

## 4. Arrancar

```bash
cd ..                 # carpeta platform
npm run dev           # catálogo + BFF + tienda + admin
```

| Servicio | URL |
|---|---|
| Web (SSR) | http://localhost:8080 |
| Tienda (React) | http://localhost:5173 |
| Admin de stock (Vue) | http://localhost:5174 |

¿Sin MySQL a mano? `npm run dev:h2` usa una base de datos en memoria con los mismos datos. Se borra al detener la app.

## Pruebas

```bash
cd platform && npm test
```

GitHub Actions ([.github/workflows/ci.yml](.github/workflows/ci.yml)) corre las mismas pruebas y los builds en cada push
y pull request. No necesita MySQL ni secretos.

## Agregar o cambiar fotos

Las páginas no muestran los JPEG originales sino versiones WebP de 480 y 960 px, unas 3 veces más livianas, que la
tienda sirve desde el CDN de Vercel. Después de agregar o reemplazar una foto en
`catalog-service/src/main/resources/static/img/`:

```bash
cd platform/catalog-service
npm run images      # genera img/opt/<foto>-480.webp y -960.webp (solo las que cambiaron)
```

Sube también la carpeta `img/opt/`. Si te olvidas, las páginas muestran la foto original y el CI lo marca como error.

## Problemas comunes

| Error | Causa y solución |
|---|---|
| `Falta la contraseña de la base de datos (DB_PASSWORD)` | No hay `.env` en `catalog-service`, o `DB_PASSWORD` está vacío. |
| `Access denied for user 'jamix_app'@'localhost'` | La contraseña del `.env` no coincide con la del paso 2. |
| `Connections using insecure transport are prohibited` / error de SSL | Tu MySQL no tiene TLS activo. Actívalo; en desarrollo puedes usar `sslMode=PREFERRED` en `DB_URL`. |
| `Port 8080 was already in use` | Ya hay otra instancia corriendo. `npm run dev` la reutiliza si es de Jamix. |
| `Validate failed: Migration checksum mismatch` | Alguien modificó una migración ya aplicada. No se editan: crea una nueva `V3__...sql`. |

## Convenciones

- Commits con [Conventional Commits](https://www.conventionalcommits.org/es/): `feat:`, `fix:`, `docs:`, `refactor:`, `chore:`…
- Cambios de base de datos, siempre con una migración nueva en `db/migration`.
