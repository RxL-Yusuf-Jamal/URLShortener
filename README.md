# SnapURL

A full-stack URL shortener with click analytics, link expiration, Redis caching, and rate limiting.

- **Frontend**: React (Vite) + Tailwind, deployed on Vercel
- **Backend**: Spring Boot 3.5 (Java 21) REST API, deployed on Render
- **Database**: PostgreSQL, schema managed by Flyway
- **Cache**: Redis, cache-aside in front of the redirect lookup
- **Docs**: OpenAPI/Swagger UI at `/swagger-ui.html`

For backend setup and the full API reference, see [`Url-Shortner-sb/README.md`](Url-Shortner-sb/README.md).

## Architecture

```mermaid
flowchart LR
    Browser["User's Browser"]

    subgraph Vercel["Vercel"]
        FE["React SPA<br/>Vite + Tailwind"]
    end

    subgraph Render["Render"]
        BE["Spring Boot 3.5 API<br/>JWT auth · rate limiting"]
        PG[("PostgreSQL<br/>Flyway-managed schema")]
        Redis[("Redis<br/>cache-aside")]
    end

    Browser -- "loads app" --> FE
    FE -- "REST/JSON + Bearer JWT" --> BE
    Browser -- "GET /{shortUrl} (public)" --> BE
    BE -- "JPA" --> PG
    BE -- "get / set / evict" --> Redis
```

Two very different traffic shapes hit the same service: authenticated management calls from the SPA (`/api/auth/**`, `/api/urls/**`), and high-volume, unauthenticated redirects (`GET /{shortUrl}`) — which is why the redirect path gets its own cache and its own, more generous rate limit.

## Auth flow

Registration, login, and how the issued JWT is validated on every subsequent authenticated request.

```mermaid
sequenceDiagram
    actor User
    participant FE as React SPA
    participant Auth as AuthController
    participant US as UserService
    participant DB as PostgreSQL
    participant Filter as JwtAuthenticationFilter
    participant API as UrlMappingController

    User->>FE: Fill registration form
    FE->>Auth: POST /api/auth/public/register
    Auth->>US: registerUser(user)
    US->>DB: existsByUsername / existsByEmail
    alt username or email already taken
        US-->>Auth: throw DuplicateResourceException
        Auth-->>FE: 409 Conflict
    else available
        US->>US: BCrypt.encode(password)
        US->>DB: save(user)
        Auth-->>FE: 200 OK
    end

    User->>FE: Fill login form
    FE->>Auth: POST /api/auth/public/login
    Auth->>US: authenticateUser(credentials)
    US->>DB: load user, verify BCrypt hash
    US->>US: JwtUtils.generateToken() (HS512, 48h expiry)
    US-->>Auth: signed JWT
    Auth-->>FE: 200 OK { token }
    FE->>FE: store token

    Note over FE,API: every authenticated request after this
    FE->>Filter: request + Authorization: Bearer <token>
    Filter->>Filter: validateToken() + read subject
    Filter->>DB: load UserDetails by username
    Filter->>Filter: populate SecurityContext
    Filter->>API: forward request (now authenticated)
    API-->>FE: 200 OK
```

## Redirect flow

The hot path: rate-limited per IP, cache-aside in front of the database, and click tracking that runs on every request regardless of whether it was a cache hit.

```mermaid
sequenceDiagram
    actor User
    participant Browser
    participant RC as RedirectController
    participant RL as RateLimiterService
    participant Svc as UrlMappingService
    participant Redis
    participant DB as PostgreSQL

    Browser->>RC: GET /{shortUrl}
    RC->>RL: checkRedirectLimit(clientIp)
    alt limit exceeded (20/min per IP)
        RL-->>RC: throw RateLimitExceededException
        RC-->>Browser: 429 Too Many Requests
    else within limit
        RC->>Svc: getOriginalUrl(shortUrl)
        Svc->>Redis: GET urlmapping:{shortUrl}

        alt cache hit
            Redis-->>Svc: cached { id, originalUrl, expiresAt }
        else cache miss
            Svc->>DB: findByShortUrl(shortUrl)
            DB-->>Svc: UrlMapping (or none)
            Svc->>Redis: SET urlmapping:{shortUrl} (10m TTL)
        end

        alt not found or expired
            Svc-->>RC: null
            RC-->>Browser: 404 Not Found
        else valid
            Svc->>DB: atomic UPDATE click_count = click_count + 1
            Svc->>DB: INSERT click_event
            Svc-->>RC: UrlMapping
            RC-->>Browser: 302 Found, Location: originalUrl
        end
    end
```

Client IP is read from `X-Forwarded-For` first, falling back to the raw connection address — necessary because Render's reverse proxy sits in front of the app, so the raw connection address alone would just be the proxy's IP.
