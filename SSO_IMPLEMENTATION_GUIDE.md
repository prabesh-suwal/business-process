# SSO (Single Sign-On) Implementation Guide

This document provides complete documentation for implementing SSO across all products in our system. Follow this guide when adding a new product (UI + Gateway).

---

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [How SSO Works](#how-sso-works)
3. [Product Authorization (Security)](#product-authorization-security)
4. [CAS Server Endpoints](#cas-server-endpoints)
5. [Gateway Configuration](#gateway-configuration)
6. [Frontend Implementation](#frontend-implementation)
7. [Cookie Configuration](#cookie-configuration)
8. [Checklist for New Product](#checklist-for-new-product)
9. [Troubleshooting](#troubleshooting)

---

## Architecture Overview

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Product UI    │     │ Product Gateway │     │   CAS Server    │
│  (React/Vite)   │────▶│  (Spring Cloud  │────▶│   (Port 9000)   │
│                 │     │   Gateway)      │     │                 │
└─────────────────┘     └─────────────────┘     └─────────────────┘
        │                       │                       │
        │  credentials:include  │  CookiePropagation    │  Set-Cookie
        │  (sends cookies)      │  Filter (forwards     │  CAS_SESSION
        └───────────────────────┴───────────────────────┘
```

### Components
| Component | Purpose | Port (Dev) |
|-----------|---------|------------|
| CAS Server | Central auth, issues tokens, manages SSO sessions | 9000 |
| Product Gateway | Routes requests, JWT validation for protected routes | 8085 (admin), 8086 (lms), etc. |
| Product UI | React frontend | 5173, 5174, etc. |

---

## How SSO Works

### Login Flow (First Product)
```
1. User visits Product-A UI → No token → Shows Login page
2. User submits username/password
3. UI calls POST /auth/login via Gateway → CAS Server
4. CAS validates credentials, creates SSO session in Redis
5. CAS returns: tokens + user info + Set-Cookie: CAS_SESSION=xxx
6. Gateway forwards Set-Cookie to browser
7. Browser stores CAS_SESSION cookie (domain=localhost)
8. UI stores accessToken in localStorage
```

### SSO Auto-Login Flow (Second Product)
```
1. User already logged into Product-A (has CAS_SESSION cookie)
2. User opens Product-B UI → No token → Login page mounts
3. Login page checks sessionStorage for SSO flag
4. If not already checked: calls GET /auth/session via Gateway
5. Gateway forwards Cookie: CAS_SESSION to CAS (via CookiePropagation filter)
6. CAS validates session → Returns {active: true, user: {...}}
7. UI calls POST /auth/token-for-product with productCode
8. CAS returns product-specific tokens
9. UI stores tokens → Redirects to dashboard
10. User is auto-logged in!
```

### Logout Flow (Global)
```
1. User clicks Logout
2. UI calls POST /auth/logout/global via Gateway
3. CAS deletes SSO session from Redis, clears cookie
4. UI clears localStorage (tokens, user)
5. UI clears sessionStorage (SSO check flags)
6. User redirected to Login
7. SSO check will NOT auto-login (session destroyed)
```

---

## Product Authorization (Security)

> ⚠️ **CRITICAL SECURITY**: Users can only access products they have roles for. A valid SSO session alone is NOT enough!

### Three-Layer Security Model

```
┌─────────────────────────────────────────────────────────────────────┐
│                    SECURITY LAYERS                                   │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Layer 1: CAS Server (Login & SSO Token Exchange)                   │
│  ════════════════════════════════════════════════                   │
│  ✓ /auth/login checks: user.getRolesForProduct(productCode)        │
│  ✓ /auth/token-for-product checks: user.getRolesForProduct()       │
│  → If no roles for product → 403 "no_product_access"                │
│                                                                      │
│  Layer 2: Gateway (Token Validation)                                 │
│  ════════════════════════════════════                               │
│  ✓ Validates JWT signature via JWKS                                 │
│  ✓ Validates audience matches gateway's product (e.g., "lms-api")   │
│  → Token for Product-A cannot be used at Product-B gateway          │
│                                                                      │
│  Layer 3: Backend Service (Permission Checks)                        │
│  ═════════════════════════════════════════════                      │
│  ✓ Checks specific permissions from token scope                     │
│  ✓ Applies business rules based on user roles                       │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Layer 1: CAS Server Authorization

**Where**: `AuthenticationService.login()` and `AuthController.tokenForProduct()`

```java
// Check if user has any roles for this product
if (user.getRolesForProduct(productCode).isEmpty()) {
    log.warn("Access denied: user {} has no roles for product {}", 
            user.getUsername(), productCode);
    throw new RuntimeException("You do not have access to this product");
}
```

**When it applies**:
- Direct login via `/auth/login` with `productCode`
- SSO token exchange via `/auth/token-for-product`

**Result if no access**: 
- Login: 400/500 with "You do not have access to this product"
- Token exchange: 403 with `{"error": "no_product_access"}`

### Layer 2: Gateway Token Validation

**Where**: `JwtValidationService.parseToken()` in each gateway

```java
// Validate audience
Set<String> audience = claims.getAudience();
if (audience == null || !audience.contains(gatewayProperties.getAudience())) {
    return Mono.error(new JwtException("Invalid audience"));
}
```

**Configuration** (in gateway's `application.yml`):
```yaml
lms:
  audience: lms-api  # Must match token's audience claim
```

**How it works**:
- CAS issues token with `aud: "lms-api"` for LMS product
- LMS gateway validates `aud` matches its configured audience
- If user somehow gets a CAS_ADMIN token, it won't work at LMS gateway

### Why All Layers Matter

| Attack Scenario | Prevented By |
|-----------------|--------------|
| User with no LMS roles tries to login to LMS | Layer 1: CAS rejects login |
| User with CAS_ADMIN session tries SSO to LMS | Layer 1: CAS rejects token-for-product |
| User steals CAS_ADMIN token, uses at LMS gateway | Layer 2: Gateway rejects wrong audience |
| User has LMS role but not permission X | Layer 3: Backend rejects unauthorized action |

### Frontend Handling of Authorization Errors

When SSO token exchange fails due to no access, the frontend should handle it gracefully:

```javascript
getTokenForProduct: async (productCode) => {
    const response = await fetch(`${GATEWAY_URL}/auth/token-for-product`, {
        method: 'POST',
        credentials: 'include',
        body: JSON.stringify({ productCode })
    });

    // Handle 403 - user has SSO session but no access to this product
    if (response.status === 403) {
        const error = await response.json();
        if (error.error === 'no_product_access') {
            // Clear SSO check flag and show login form
            // User can login with different account that has access
            throw new Error('You do not have access to this product');
        }
    }
    // ... rest of handling
}
```

---

## CAS Server Endpoints

### Authentication Endpoints (All Public)

| Endpoint | Method | Purpose | Request | Response |
|----------|--------|---------|---------|----------|
| `/auth/login` | POST | Login & create SSO session | `{username, password, productCode}` | `{tokens, user}` + Set-Cookie |
| `/auth/session` | GET | Check SSO session | Cookie: CAS_SESSION | `{active: true/false, user}` |
| `/auth/token-for-product` | POST | Get tokens for product via SSO | `{productCode}` + Cookie | `{tokens, user}` |
| `/auth/logout/global` | POST | Destroy SSO session | Cookie: CAS_SESSION | 200 OK + Clear-Cookie |
| `/auth/refresh` | POST | Refresh access token | `{refreshToken, productCode}` | `{tokens}` |

### Token Response Format (snake_case via Jackson)
```json
{
  "tokens": {
    "access_token": "eyJhbG...",
    "refresh_token": "eyJhbG...",
    "token_type": "Bearer",
    "expires_in": 300
  },
  "user": {
    "id": "uuid",
    "username": "admin",
    "email": "admin@example.com",
    "firstName": "Admin",
    "lastName": "User"
  }
}
```

> ⚠️ **IMPORTANT**: Token fields are `access_token` and `refresh_token` (snake_case), NOT camelCase!

---

## Gateway Configuration

### Required Routes

Every product gateway needs these auth routes to proxy SSO requests to CAS:

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          enabled: true
          
          # CORS for credentials (required for cookies)
          globalcors:
            corsConfigurations:
              '[/**]':
                allowedOrigins:
                  - "http://localhost:YOUR_UI_PORT"
                allowedMethods: [GET, POST, PUT, DELETE, OPTIONS]
                allowedHeaders: "*"
                allowCredentials: true  # CRITICAL for cookies!
                maxAge: 3600
          
          routes:
            # Auth routes with cookie forwarding
            - id: auth-routes
              uri: http://localhost:9000
              predicates:
                - Path=/auth/**
              filters:
                - name: CookiePropagation  # CRITICAL!
```

### CookiePropagation Filter

**REQUIRED** for SSO. This filter forwards browser cookies to CAS backend:

```java
package com.PRODUCT.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CookiePropagationGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    public CookiePropagationGatewayFilterFactory() {
        super(Object.class);
    }

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String cookieHeader = request.getHeaders().getFirst("Cookie");
            
            if (cookieHeader != null && !cookieHeader.isEmpty()) {
                log.debug("Propagating Cookie header to backend");
                ServerHttpRequest mutatedRequest = request.mutate()
                    .header("Cookie", cookieHeader)
                    .build();
                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            }
            return chain.filter(exchange);
        };
    }
}
```

### JWT Filter PUBLIC_PATHS

If your gateway has a JWT GlobalFilter, add SSO endpoints to PUBLIC_PATHS:

```java
private static final Set<String> PUBLIC_PATHS = Set.of(
    "/auth/login",
    "/auth/register",
    "/auth/refresh",
    "/auth/session",           // SSO session check
    "/auth/token-for-product", // SSO token exchange
    "/auth/logout",
    "/auth/logout/global",
    "/.well-known/",
    "/actuator/health"
);
```

---

## Frontend Implementation

### API Client Setup

```javascript
// api/index.js

const GATEWAY_URL = import.meta.env.PROD
    ? 'https://YOUR-PRODUCT-api.example.com'
    : 'http://localhost:YOUR_GATEWAY_PORT';

let accessToken = localStorage.getItem('accessToken');

export function setToken(token) {
    accessToken = token;
    localStorage.setItem('accessToken', token);
}

export function getToken() {
    return accessToken;
}

export function clearToken() {
    accessToken = null;
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
}

// Generic request with credentials:include
async function request(endpoint, options = {}) {
    const headers = {
        'Content-Type': 'application/json',
        ...options.headers,
    };

    if (accessToken) {
        headers['Authorization'] = `Bearer ${accessToken}`;
    }

    const response = await fetch(`${GATEWAY_URL}${endpoint}`, {
        ...options,
        headers,
        credentials: 'include', // CRITICAL for SSO cookies!
    });

    // Handle 401...
    return response;
}

// Auth object with SSO functions
export const auth = {
    /**
     * Login with username/password
     */
    login: async (username, password) => {
        const response = await fetch(`${GATEWAY_URL}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({
                username,
                password,
                productCode: 'YOUR_PRODUCT_CODE'  // e.g., 'MEMO', 'LMS'
            })
        });

        if (!response.ok) throw new Error('Login failed');

        const data = await response.json();

        // NOTE: CAS returns snake_case tokens!
        if (data.tokens) {
            setToken(data.tokens.access_token);
            localStorage.setItem('refreshToken', data.tokens.refresh_token);
        }
        if (data.user) {
            localStorage.setItem('user', JSON.stringify(data.user));
        }

        return data;
    },

    /**
     * Check for active SSO session
     */
    checkSSO: async () => {
        try {
            const response = await fetch(`${GATEWAY_URL}/auth/session`, {
                credentials: 'include'
            });
            const data = await response.json();
            return data.active ? data : null;
        } catch {
            return null;
        }
    },

    /**
     * Get product-specific tokens using SSO session
     */
    getTokenForProduct: async (productCode = 'YOUR_PRODUCT_CODE') => {
        const response = await fetch(`${GATEWAY_URL}/auth/token-for-product`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ productCode })
        });

        if (!response.ok) throw new Error('Failed to get token');

        const data = await response.json();

        // NOTE: snake_case tokens!
        if (data.tokens) {
            setToken(data.tokens.access_token);
            localStorage.setItem('refreshToken', data.tokens.refresh_token);
        }
        if (data.user) {
            localStorage.setItem('user', JSON.stringify(data.user));
        }

        return data;
    },

    /**
     * Global logout - clears SSO session everywhere
     */
    globalLogout: async () => {
        try {
            await fetch(`${GATEWAY_URL}/auth/logout/global`, {
                method: 'POST',
                credentials: 'include'
            });
        } catch {}
        
        clearToken();
        sessionStorage.removeItem('sso_check_YOUR_PRODUCT_in_progress');
        window.location.href = '/login';
    }
};
```

### Login Page with SSO Check

```jsx
// pages/Login.jsx
import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { auth, getToken } from '../api';

// Unique key per product to prevent infinite loops
const SSO_CHECK_KEY = 'sso_check_YOUR_PRODUCT_in_progress';

export default function Login() {
    const [checkingSSO, setCheckingSSO] = useState(true);
    const navigate = useNavigate();

    useEffect(() => {
        const checkExistingSession = async () => {
            // Already logged in?
            if (getToken()) {
                navigate('/', { replace: true });
                return;
            }

            // Prevent multiple SSO checks (critical!)
            if (sessionStorage.getItem(SSO_CHECK_KEY) === 'true') {
                setCheckingSSO(false);
                return;
            }
            sessionStorage.setItem(SSO_CHECK_KEY, 'true');

            // Check SSO session
            try {
                const ssoSession = await auth.checkSSO();
                if (ssoSession) {
                    const result = await auth.getTokenForProduct('YOUR_PRODUCT_CODE');
                    if (result?.tokens) {
                        sessionStorage.removeItem(SSO_CHECK_KEY);
                        navigate('/', { replace: true });
                        return;
                    }
                }
            } catch {}

            setCheckingSSO(false);
        };

        checkExistingSession();
    }, []);

    // Show loading while checking
    if (checkingSSO) {
        return <div>Checking for existing session...</div>;
    }

    // Render login form...
}
```

### Logout Handler

```jsx
// In Layout or App component
import { auth } from '../api';

const handleLogout = () => {
    auth.globalLogout();  // This handles everything!
};
```

---

## Cookie Configuration

### CAS Server SessionService

The SSO cookie is set by CAS with these properties:

```java
// CAS_SESSION cookie attributes
Name: CAS_SESSION
Value: UUID session ID
Path: /
Domain: localhost (configurable)
SameSite: Lax (for dev), None (for prod with Secure)
HttpOnly: true
Max-Age: 28800 (8 hours default)
```

### Why SameSite=Lax for Development

- Chrome blocks `SameSite=None` without `Secure` flag
- `Secure` requires HTTPS (not available on localhost by default)
- `SameSite=Lax` works for same-site navigation and top-level POST

### Production Configuration

For production (with HTTPS), use:
```yaml
cas:
  session:
    cookie-secure: true  # Enables HTTPS-only
    # This will set SameSite=None with Secure flag
```

---

## Checklist for New Product

When adding a new product (e.g., "MEMO"), follow this checklist:

### 1. CAS Server Changes
- [ ] Add product code to database (if product-specific scopes needed)
- [ ] No code changes needed for SSO endpoints (already generic)

### 2. Gateway Setup
```
memo-gateway/
├── src/main/java/com/memo/gateway/
│   ├── MemoGatewayApplication.java
│   └── filter/
│       ├── CookiePropagationGatewayFilterFactory.java  ← COPY FROM LMS
│       └── JwtAuthenticationGatewayFilter.java         ← ADD SSO PATHS
└── src/main/resources/
    └── application.yml                                  ← CONFIGURE CORS + ROUTES
```

### 3. Gateway application.yml
```yaml
server:
  port: 8087  # Choose unique port

spring:
  cloud:
    gateway:
      server:
        webflux:
          enabled: true
          globalcors:
            corsConfigurations:
              '[/**]':
                allowedOrigins: ["http://localhost:5175"]  # MEMO UI port
                allowCredentials: true
          routes:
            - id: auth-routes
              uri: http://localhost:9000
              predicates:
                - Path=/auth/**
              filters:
                - name: CookiePropagation
            # Add your product-specific routes...
```

### 4. Frontend Setup
```
memo-ui/
├── src/
│   ├── api/
│   │   └── index.js    ← SET GATEWAY_URL, productCode='MEMO'
│   ├── pages/
│   │   └── Login.jsx   ← COPY SSO CHECK LOGIC
│   └── components/
│       └── Layout.jsx  ← USE auth.globalLogout()
```

### 5. Key Values to Customize
| Item | Example |
|------|---------|
| Product Code | `'MEMO'` |
| Gateway Port | `8087` |
| UI Port | `5175` |
| SSO Check Key | `'sso_check_memo_in_progress'` |
| GATEWAY_URL | `'http://localhost:8087'` |

### 6. Testing
1. Start CAS server, admin-gateway, new gateway, all UIs
2. Login to cas-admin-ui first
3. Open new product UI → Should auto-login via SSO!
4. Test logout → Should stay logged out

---

## Troubleshooting

### Issue: Cookie not sent to gateway
**Symptoms**: `/auth/session` returns `{active: false}` even after login
**Causes**:
- Missing `credentials: 'include'` in fetch
- Gateway CORS missing `allowCredentials: true`
- SameSite cookie blocking

**Fix**: Check all three items above

### Issue: Gateway returns 401 on /auth/session
**Symptoms**: JWT filter blocking public SSO endpoints
**Cause**: `/auth/session` not in `PUBLIC_PATHS`
**Fix**: Add all SSO endpoints to PUBLIC_PATHS in JWT filter

### Issue: Cookie not forwarded to CAS
**Symptoms**: CAS logs show no session cookie in request
**Cause**: Missing CookiePropagation filter on gateway
**Fix**: Add `CookiePropagationGatewayFilterFactory` and configure in routes

### Issue: Infinite SSO check loop
**Symptoms**: Login page keeps calling `/auth/session` repeatedly
**Cause**: sessionStorage flag not set/checked properly
**Fix**: Use unique `SSO_CHECK_KEY` per product and check before SSO call

### Issue: Logout doesn't work (auto-login back)
**Symptoms**: After logout, SSO logs user back in immediately
**Cause**: Not calling global logout / not clearing sessionStorage flag
**Fix**: Use `auth.globalLogout()` which handles all cleanup

---

## Quick Reference

### Ports (Development)
```
CAS Server:      9000
admin-gateway:   8085
lms-gateway:     8086
cas-admin-ui:    5173
lms-ui:          5174
```

### Product Codes
```
CAS_ADMIN → cas-admin-ui (manages CAS)
LMS       → lms-ui (loan management)
```

### Key Files
```
CAS Server:
  - SessionService.java (cookie handling)
  - AuthController.java (login, session, token-for-product)

Gateway:
  - CookiePropagationGatewayFilterFactory.java
  - JwtAuthenticationGatewayFilter.java (PUBLIC_PATHS)
  - application.yml (CORS, routes)

Frontend:
  - api/index.js (auth functions)
  - pages/Login.jsx (SSO check)
  - Layout.jsx (logout handler)
```

---

*Last updated: 2026-01-19*
