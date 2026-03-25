# CampaignHub

CampaignHub to aplikacja backendowa (Spring Boot) do zarządzania kampaniami reklamowymi i produktami sprzedawców.

## Quick Start

Najkrótsza ścieżka, aby uruchomić aplikację i wykonać pierwszy request do chronionego endpointu.

1. Uruchom aplikację:

```bash
./mvnw spring-boot:run
```

2. Zarejestruj sprzedawcę:

```bash
curl -X POST "http://localhost:8080/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "seller@example.com",
    "displayName": "Seller One",
    "password": "StrongPass123"
  }'
```

3. Zaloguj się i skopiuj `accessToken` z odpowiedzi:

```bash
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "seller@example.com",
    "password": "StrongPass123"
  }'
```

4. Użyj tokena Bearer do wywołania chronionego endpointu:

```bash
curl -X GET "http://localhost:8080/api/products" \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

5. Opcjonalnie odśwież token:

```bash
curl -X POST "http://localhost:8080/api/auth/refresh" \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "<REFRESH_TOKEN>"
  }'
```

## Funkcjonalności

- rejestracja i logowanie sprzedawców
- autoryzacja JWT (access token + refresh token)
- zarządzanie produktami zalogowanego sprzedawcy
- zarządzanie kampaniami zalogowanego sprzedawcy
- słowniki pomocnicze: miasta i słowa kluczowe
- saldo konta Emerald per użytkownik
- wbudowany frontend React (minimalistyczny panel kampanii)

## Frontend (React)

Frontend działa jako statyczne zasoby serwowane przez Spring Boot (bez osobnego builda Node).

### Gdzie jest kod

- `src/main/resources/static/index.html`
- `src/main/resources/static/app.js`
- `src/main/resources/static/styles.css`

### Jak uruchomić

1. Uruchom backend:

```bash
./mvnw spring-boot:run
```

2. Otwórz w przeglądarce:

```text
http://localhost:8080
```

### Co obsługuje frontend

- logowanie i rejestrację (`/api/auth/register`, `/api/auth/login`)
- tworzenie i usuwanie produktów (`/api/products`)
- tworzenie, edycję i usuwanie kampanii (`/api/campaigns`)
- wymagane pola kampanii: nazwa, keywords (typeahead), bid amount, campaign fund, status, town, radius
- podgląd i odświeżanie salda Emerald (`/api/emerald-account/balance`)
- prezentację komunikatów błędów z realną treścią z API (zamiast ogólnego `Unknown API error`)

### Uwagi techniczne

- frontend korzysta z React 18 i ReactDOM 18 z CDN (`unpkg`)
- transpilacja JSX działa w przeglądarce przez `@babel/standalone`
- tokeny (`accessToken`, `refreshToken`) są zapisywane w `localStorage`

## Stack technologiczny

- Java 17
- Spring Boot 4
- Spring Web MVC
- Spring Data JPA
- Spring Security
- JWT (jjwt)
- H2 (in-memory)
- Maven

## Uruchomienie lokalne

1. Zbuduj i uruchom:

```bash
./mvnw spring-boot:run
```

2. Aplikacja domyślnie działa na:

```text
http://localhost:8080
```

3. H2 Console:

```text
http://localhost:8080/h2-console
```

## Konfiguracja

Najważniejsze ustawienia w pliku `src/main/resources/application.properties`:

- `server.port=8080`
- `server.address=0.0.0.0`
- `spring.datasource.url=jdbc:h2:mem:campaignhub;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`
- `jwt.secret=...`
- `jwt.issuer=campaignHub`
- `jwt.access-token-expiration-seconds=900`
- `jwt.refresh-token-expiration-days=14`

Uwaga: dla środowiska produkcyjnego ustaw silny sekret JWT przez zmienne środowiskowe.

## Model bazy danych

Baza tworzona jest automatycznie przez JPA (`ddl-auto=create-drop`).

### Tabele

- `sellers`
  - `id` (UUID, PK)
  - `email` (unique)
  - `display_name`

- `seller_accounts`
  - `id` (UUID, PK)
  - `seller_id` (FK -> sellers.id, unique)
  - `email` (unique)
  - `password_hash`
  - `enabled`
  - `token_version`

- `refresh_tokens`
  - `id` (UUID, PK)
  - `account_id` (FK -> seller_accounts.id)
  - `token_hash` (unique)
  - `created_at`
  - `expires_at`
  - `revoked_at`

- `products`
  - `id` (UUID, PK)
  - `seller_id` (FK -> sellers.id)
  - `name`
  - `description`

- `campaigns`
  - `id` (UUID, PK)
  - `seller_id` (FK -> sellers.id)
  - `product_id` (FK -> products.id)
  - `name`
  - `bid_amount`
  - `campaign_fund`
  - `status` (`ON`/`OFF`)
  - `town`
  - `radius_in_km`

- `campaign_keywords`
  - `campaign_id` (FK -> campaigns.id)
  - `keyword_id` (FK -> keyword_dictionary.id)

- `keyword_dictionary`
  - `id` (UUID, PK)
  - `keyword_value` (unique)

- `towns`
  - `id` (UUID, PK)
  - `name` (unique)

- `emerald_accounts`
  - `id` (UUID, PK)
  - `seller_id` (FK -> sellers.id, unique)
  - `balance`

### Relacje

- `Seller` 1:1 `SellerAccount`
- `Seller` 1:1 `EmeraldAccount`
- `SellerAccount` 1:N `RefreshToken`
- `Seller` 1:N `Product`
- `Seller` 1:N `Campaign`
- `Product` 1:N `Campaign`
- `Campaign` N:M `Keyword` przez `campaign_keywords`

## Bezpieczeństwo i autoryzacja

### Endpointy publiczne

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET /api/towns`
- `GET /api/keywords`

### Endpointy chronione (Bearer token)

- wszystkie pozostałe (`/api/products/**`, `/api/campaigns/**`, `/api/emerald-account/balance`)

Nagłówek autoryzacji:

```text
Authorization: Bearer <access_token>
```

## Dokumentacja API

### Auth

#### POST /api/auth/register

Rejestruje nowego sprzedawcę, tworzy jego konto Emerald i zwraca tokeny.

Request:

```json
{
  "email": "seller@example.com",
  "displayName": "Seller One",
  "password": "StrongPass123"
}
```

Response:

```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "sellerId": "uuid",
  "email": "seller@example.com"
}
```

#### POST /api/auth/login

Request:

```json
{
  "email": "seller@example.com",
  "password": "StrongPass123"
}
```

Response: jak przy register.

#### POST /api/auth/refresh

Request:

```json
{
  "refreshToken": "..."
}
```

Response: nowy `accessToken` i `refreshToken`.

#### POST /api/auth/logout

Request:

```json
{
  "refreshToken": "..."
}
```

Response: `204 No Content`.

### Products (wymaga JWT)

#### GET /api/products

Zwraca produkty zalogowanego sprzedawcy.

#### GET /api/products/{id}

Zwraca produkt zalogowanego sprzedawcy po ID.

#### POST /api/products

Request:

```json
{
  "name": "Gaming Monitor 27",
  "description": "Monitor 27 cale 165Hz"
}
```

#### PUT /api/products/{id}

Request:

```json
{
  "name": "Gaming Monitor 32",
  "description": "Monitor 32 cale 240Hz"
}
```

#### DELETE /api/products/{id}

Usuwa produkt zalogowanego sprzedawcy.

### Campaigns (wymaga JWT)

#### GET /api/campaigns

Zwraca kampanie zalogowanego sprzedawcy.

#### GET /api/campaigns/{id}

Zwraca kampanię zalogowanego sprzedawcy po ID.

#### POST /api/campaigns

Request:

```json
{
  "productId": "uuid",
  "name": "Back to School 2026",
  "keywords": ["books", "electronics"],
  "bidAmount": 2.50,
  "campaignFund": 1500.00,
  "status": "ON",
  "town": "Warsaw",
  "radiusInKm": 25
}
```

#### PUT /api/campaigns/{id}

Request: taki sam jak przy create.

#### DELETE /api/campaigns/{id}

Usuwa kampanię i zwraca środki na saldo.

### Słowniki i saldo

#### GET /api/keywords?query=boo

Publiczny endpoint do wyszukiwania słów kluczowych.

#### GET /api/towns

Publiczna lista miast.

#### GET /api/emerald-account/balance

Wymaga JWT. Zwraca aktualne saldo Emerald.

## Przykład pełnego flow JWT (curl)

### 1. Rejestracja

```bash
curl -X POST "http://localhost:8080/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "seller@example.com",
    "displayName": "Seller One",
    "password": "StrongPass123"
  }'
```

### 2. Logowanie

```bash
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "seller@example.com",
    "password": "StrongPass123"
  }'
```

Z odpowiedzi skopiuj `accessToken`.

### 3. Użycie tokena na chronionym endpoincie

```bash
curl -X GET "http://localhost:8080/api/products" \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

### 4. Odświeżenie tokena

```bash
curl -X POST "http://localhost:8080/api/auth/refresh" \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "<REFRESH_TOKEN>"
  }'
```

### 5. Logout (unieważnienie refresh tokena)

```bash
curl -X POST "http://localhost:8080/api/auth/logout" \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "<REFRESH_TOKEN>"
  }'
```

## Kody błędów

- `400 Bad Request` - walidacja lub błąd biznesowy
- `401 Unauthorized` - brak/nieprawidłowy token
- `403 Forbidden` - brak uprawnień
- `404 Not Found` - zasób nie istnieje
- `409 Conflict` - np. niewystarczające środki
