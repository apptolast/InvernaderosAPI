# API Error Handling Guide for Frontend

**Version**: 2.0
**Last Updated**: 2026-01-15
**API URL (DEV)**: https://inverapi-dev.apptolast.com
**API URL (PROD)**: https://inverapi.apptolast.com

---

## Overview

This guide explains how the API returns errors and how the frontend should handle them. All API errors follow the **RFC 7807 Problem Details** specification.

**IMPORTANT**: The API NEVER exposes technical details (SQL errors, stack traces, internal messages) to the frontend. All error messages are user-friendly and safe to display.

---

## Error Response Structure

Every error response follows this JSON structure:

```json
{
    "type": "about:blank",
    "title": "Human-readable error title",
    "status": 400,
    "detail": "Detailed explanation of the error (safe to show to user)",
    "instance": "/api/v1/endpoint/path",
    "timestamp": "2026-01-15T21:40:01.772437173Z",
    "errorCode": "ERROR_CODE",
    "errorId": "uuid-for-support-correlation",
    "errors": {
        "field1": "Error message for field1",
        "field2": "Error message for field2"
    }
}
```

### Field Descriptions

| Field | Type | Always Present | Description |
|-------|------|----------------|-------------|
| `type` | string | Yes | URI reference for error type (usually "about:blank") |
| `title` | string | Yes | Human-readable summary of the error |
| `status` | number | Yes | HTTP status code (400, 401, 403, 404, 409, 500, 504) |
| `detail` | string | Yes | **User-friendly** explanation - safe to display directly |
| `instance` | string | Yes | URI of the request that caused the error |
| `timestamp` | string | Yes | ISO 8601 timestamp when error occurred |
| `errorCode` | string | Yes | Machine-readable error code for frontend logic |
| `errorId` | string | On 500 errors | Unique ID for correlating with backend logs |
| `errors` | object | On validation | Field-specific validation errors (field → message) |

---

## Complete Error Codes Reference

Use these codes for frontend logic (switch statements, error categorization, etc.):

### Client Errors (4xx)

| Error Code | HTTP Status | Description | User Action |
|------------|-------------|-------------|-------------|
| `VALIDATION_ERROR` | 400 | Request body validation failed (@Valid) | Check `errors` field for specific fields |
| `BAD_REQUEST` | 400 | Malformed request (invalid JSON, wrong types, missing params) | Fix request format |
| `AUTHENTICATION_ERROR` | 401 | Invalid credentials, expired token, or not authenticated | Redirect to login |
| `AUTHORIZATION_ERROR` | 403 | User lacks permission for this action | Show "access denied" |
| `ENTITY_NOT_FOUND` | 404 | Resource doesn't exist or endpoint not found | Navigate back or show "not found" |
| `CONSTRAINT_VIOLATION` | 409 | Data already exists or violates unique constraint | Check for duplicates |
| `CONFLICT_ERROR` | 409 | Operation conflicts with current state (optimistic/pessimistic lock) | Refresh data and retry |

### Server Errors (5xx)

| Error Code | HTTP Status | Description | User Action |
|------------|-------------|-------------|-------------|
| `DATABASE_ERROR` | 500 | Database operation failed (connection, query, etc.) | Show generic error, retry |
| `TRANSACTION_ERROR` | 500 | Transaction was rolled back | Retry the operation |
| `INTERNAL_ERROR` | 500 | Unexpected server error | Show generic error, contact support with `errorId` |
| `TIMEOUT_ERROR` | 504 | Database query or operation timed out | Retry later |

---

## All Handled Exception Types

The API handles the following exception types with user-friendly messages:

### Database Exceptions (NEVER expose SQL details)

| Exception Type | Error Code | HTTP Status | Example User Message |
|----------------|------------|-------------|---------------------|
| `SQLException` | DATABASE_ERROR | 500 | "A database error occurred while processing your request." |
| `DataAccessException` | DATABASE_ERROR | 500 | "A database error occurred while processing your request." |
| `DataIntegrityViolationException` | CONSTRAINT_VIOLATION | 409 | "A record with this email already exists." |
| `EmptyResultDataAccessException` | ENTITY_NOT_FOUND | 404 | "The requested resource was not found in the database." |
| `QueryTimeoutException` | TIMEOUT_ERROR | 504 | "The database query took too long to execute." |
| `OptimisticLockingFailureException` | CONFLICT_ERROR | 409 | "The resource was modified by another user. Please refresh." |
| `PessimisticLockingFailureException` | CONFLICT_ERROR | 409 | "The resource is currently locked by another operation." |
| `TransactionSystemException` | TRANSACTION_ERROR | 500 | "The database operation could not be completed. Please retry." |
| `PersistenceException` | DATABASE_ERROR | 500 | "A database persistence error occurred." |
| `JpaSystemException` | DATABASE_ERROR | 500 | "A database system error occurred." |
| `HibernateConstraintViolationException` | CONSTRAINT_VIOLATION | 409 | "The data violates a database constraint." |
| `EntityNotFoundException` | ENTITY_NOT_FOUND | 404 | "The requested entity was not found." |

### Security Exceptions

| Exception Type | Error Code | HTTP Status | Example User Message |
|----------------|------------|-------------|---------------------|
| `AuthenticationException` | AUTHENTICATION_ERROR | 401 | "Authentication failed. Please check your credentials." |
| `BadCredentialsException` | AUTHENTICATION_ERROR | 401 | "Invalid username or password" |
| `AccessDeniedException` | AUTHORIZATION_ERROR | 403 | "You do not have permission to perform this action." |

### Validation Exceptions

| Exception Type | Error Code | HTTP Status | Example User Message |
|----------------|------------|-------------|---------------------|
| `MethodArgumentNotValidException` | VALIDATION_ERROR | 400 | "Validation failed for one or more fields" + `errors` map |
| `ConstraintViolationException` | VALIDATION_ERROR | 400 | "Validation failed for one or more fields" + `errors` map |

### Request Parameter Exceptions

| Exception Type | Error Code | HTTP Status | Example User Message |
|----------------|------------|-------------|---------------------|
| `MissingServletRequestParameterException` | BAD_REQUEST | 400 | "Required parameter 'name' of type 'String' is missing." |
| `MissingPathVariableException` | BAD_REQUEST | 400 | "Required path variable 'id' is missing from the URL." |
| `MethodArgumentTypeMismatchException` | BAD_REQUEST | 400 | "Invalid parameter type: 'id' should be of type Long" |
| `HttpMessageNotReadableException` | BAD_REQUEST | 400 | "Request body is malformed or missing. Please provide valid JSON." |
| `MismatchedInputException` | BAD_REQUEST | 400 | "Invalid data format for field 'timestamp'." |

### Resource Exceptions

| Exception Type | Error Code | HTTP Status | Example User Message |
|----------------|------------|-------------|---------------------|
| `NoResourceFoundException` | ENTITY_NOT_FOUND | 404 | "The requested resource '/api/v1/xyz' was not found." |
| `NoHandlerFoundException` | ENTITY_NOT_FOUND | 404 | "The requested endpoint '/api/v1/xyz' does not exist." |
| `NoSuchElementException` | ENTITY_NOT_FOUND | 404 | "The requested resource was not found." |

### Business Logic Exceptions

| Exception Type | Error Code | HTTP Status | Example User Message |
|----------------|------------|-------------|---------------------|
| `IllegalArgumentException` | BAD_REQUEST | 400 | Custom message from service layer |
| `IllegalStateException` | CONFLICT_ERROR | 409 | "The operation cannot be performed in the current state." |

### Serialization Exceptions

| Exception Type | Error Code | HTTP Status | Example User Message |
|----------------|------------|-------------|---------------------|
| `InvalidDefinitionException` | INTERNAL_ERROR | 500 | "An error occurred while preparing the response." |

### Catch-All

| Exception Type | Error Code | HTTP Status | Example User Message |
|----------------|------------|-------------|---------------------|
| `Exception` (any unhandled) | INTERNAL_ERROR | 500 | "An unexpected error occurred. Contact support with error ID: xxx" |

---

## Common Error Scenarios with Examples

### 1. Validation Errors (400) - Form Fields

When request body fails `@Valid` validation:

```json
{
    "type": "about:blank",
    "title": "Validation Error",
    "status": 400,
    "detail": "Validation failed for one or more fields",
    "instance": "/api/v1/tenants/1/users",
    "timestamp": "2026-01-15T21:40:01Z",
    "errorCode": "VALIDATION_ERROR",
    "errors": {
        "email": "must be a valid email address",
        "passwordRaw": "size must be between 6 and 100",
        "name": "must not be blank"
    }
}
```

**Frontend handling:**
```typescript
if (error.errorCode === 'VALIDATION_ERROR' && error.errors) {
    // Show field-specific errors on the form
    Object.entries(error.errors).forEach(([field, message]) => {
        setFieldError(field, message);
    });
}
```

### 2. Authentication Errors (401) - Login Failed

```json
{
    "type": "about:blank",
    "title": "Authentication Failed",
    "status": 401,
    "detail": "Invalid username or password",
    "instance": "/api/v1/auth/login",
    "timestamp": "2026-01-15T21:40:01Z",
    "errorCode": "AUTHENTICATION_ERROR"
}
```

**Frontend handling:**
```typescript
if (error.errorCode === 'AUTHENTICATION_ERROR') {
    clearAuth();
    showMessage(error.detail);
    navigateTo('/login');
}
```

### 3. Authorization Errors (403) - Access Denied

```json
{
    "type": "about:blank",
    "title": "Access Denied",
    "status": 403,
    "detail": "You do not have permission to perform this action.",
    "instance": "/api/v1/tenants/1/settings",
    "timestamp": "2026-01-15T21:40:01Z",
    "errorCode": "AUTHORIZATION_ERROR"
}
```

**Frontend handling:**
```typescript
if (error.errorCode === 'AUTHORIZATION_ERROR') {
    showMessage(error.detail);
    navigateTo('/dashboard'); // or show permission denied screen
}
```

### 4. Resource Not Found (404)

```json
{
    "type": "about:blank",
    "title": "Entity Not Found",
    "status": 404,
    "detail": "No existe el invernadero con ID: 999",
    "instance": "/api/v1/tenants/1/greenhouses/999",
    "timestamp": "2026-01-15T21:40:01Z",
    "errorCode": "ENTITY_NOT_FOUND"
}
```

**Frontend handling:**
```typescript
if (error.errorCode === 'ENTITY_NOT_FOUND') {
    showMessage(error.detail);
    navigateTo('/greenhouses'); // back to list
}
```

### 5. Constraint Violation (409) - Duplicate Data

When trying to create duplicate record:

```json
{
    "type": "about:blank",
    "title": "Data Integrity Violation",
    "status": 409,
    "detail": "A record with this email already exists. Please use a different value.",
    "instance": "/api/v1/tenants/1/users",
    "timestamp": "2026-01-15T21:40:01Z",
    "errorCode": "CONSTRAINT_VIOLATION",
    "errorId": "a1b2c3d4-e5f6-7890"
}
```

**Frontend handling:**
```typescript
if (error.errorCode === 'CONSTRAINT_VIOLATION') {
    showMessage(error.detail); // Already user-friendly
}
```

### 6. Conflict Error (409) - Concurrent Modification

When another user modified the same resource:

```json
{
    "type": "about:blank",
    "title": "Concurrent Modification Conflict",
    "status": 409,
    "detail": "The resource was modified by another user. Please refresh and try again.",
    "instance": "/api/v1/tenants/1/greenhouses/5",
    "timestamp": "2026-01-15T21:40:01Z",
    "errorCode": "CONFLICT_ERROR",
    "errorId": "a1b2c3d4-e5f6-7890"
}
```

**Frontend handling:**
```typescript
if (error.errorCode === 'CONFLICT_ERROR') {
    showMessage(error.detail);
    refreshData(); // Reload the current data
}
```

### 7. Database/Transaction Errors (500)

Server-side database issues (user-friendly, never exposes SQL):

```json
{
    "type": "about:blank",
    "title": "Transaction Failed",
    "status": 500,
    "detail": "The database operation could not be completed due to a previous error. Please retry the operation.",
    "instance": "/api/v1/tenants/1/settings",
    "timestamp": "2026-01-15T21:40:01Z",
    "errorCode": "TRANSACTION_ERROR",
    "errorId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

**Frontend handling:**
```typescript
if (error.status >= 500) {
    showMessage(error.detail);

    // Log errorId for support tickets
    if (error.errorId) {
        console.error(`Server error. Support reference: ${error.errorId}`);
    }
}
```

### 8. Timeout Errors (504)

```json
{
    "type": "about:blank",
    "title": "Query Timeout",
    "status": 504,
    "detail": "The database query took too long to execute. Please try again.",
    "instance": "/api/v1/tenants/1/reports/generate",
    "timestamp": "2026-01-15T21:40:01Z",
    "errorCode": "TIMEOUT_ERROR",
    "errorId": "a1b2c3d4-e5f6-7890"
}
```

**Frontend handling:**
```typescript
if (error.errorCode === 'TIMEOUT_ERROR') {
    showMessage('The operation is taking longer than expected. Please try again.');
}
```

### 9. Bad Request (400) - Missing Parameters

```json
{
    "type": "about:blank",
    "title": "Missing Required Parameter",
    "status": 400,
    "detail": "Required parameter 'startDate' of type 'LocalDate' is missing.",
    "instance": "/api/v1/tenants/1/reports",
    "timestamp": "2026-01-15T21:40:01Z",
    "errorCode": "BAD_REQUEST",
    "parameterName": "startDate",
    "parameterType": "LocalDate"
}
```

### 10. Bad Request (400) - Invalid JSON

```json
{
    "type": "about:blank",
    "title": "Invalid Request Body",
    "status": 400,
    "detail": "Request body is malformed or missing. Please provide valid JSON.",
    "instance": "/api/v1/tenants/1/greenhouses",
    "timestamp": "2026-01-15T21:40:01Z",
    "errorCode": "BAD_REQUEST"
}
```

---

## Complete Error Handler Implementation

### TypeScript (React Native / Web)

```typescript
// types/api-error.ts
interface ApiError {
    type: string;
    title: string;
    status: number;
    detail: string;
    instance: string;
    timestamp: string;
    errorCode: string;
    errorId?: string;
    errors?: Record<string, string>;
    // Additional fields for specific errors
    parameterName?: string;
    parameterType?: string;
    field?: string;
    path?: string;
    method?: string;
}

// Constants matching backend
const ErrorCodes = {
    VALIDATION_ERROR: 'VALIDATION_ERROR',
    BAD_REQUEST: 'BAD_REQUEST',
    AUTHENTICATION_ERROR: 'AUTHENTICATION_ERROR',
    AUTHORIZATION_ERROR: 'AUTHORIZATION_ERROR',
    ENTITY_NOT_FOUND: 'ENTITY_NOT_FOUND',
    CONSTRAINT_VIOLATION: 'CONSTRAINT_VIOLATION',
    CONFLICT_ERROR: 'CONFLICT_ERROR',
    DATABASE_ERROR: 'DATABASE_ERROR',
    TRANSACTION_ERROR: 'TRANSACTION_ERROR',
    INTERNAL_ERROR: 'INTERNAL_ERROR',
    TIMEOUT_ERROR: 'TIMEOUT_ERROR',
} as const;

// Result type for error handling
type ErrorResult =
    | { type: 'validation'; errors: Record<string, string> }
    | { type: 'auth'; action: 'logout' }
    | { type: 'forbidden'; message: string }
    | { type: 'notFound'; message: string }
    | { type: 'conflict'; message: string; shouldRefresh: boolean }
    | { type: 'timeout'; message: string }
    | { type: 'error'; message: string; supportRef?: string };

function handleApiError(error: ApiError): ErrorResult {
    switch (error.errorCode) {
        case ErrorCodes.VALIDATION_ERROR:
            return {
                type: 'validation',
                errors: error.errors || {}
            };

        case ErrorCodes.AUTHENTICATION_ERROR:
            return { type: 'auth', action: 'logout' };

        case ErrorCodes.AUTHORIZATION_ERROR:
            return { type: 'forbidden', message: error.detail };

        case ErrorCodes.ENTITY_NOT_FOUND:
            return { type: 'notFound', message: error.detail };

        case ErrorCodes.CONSTRAINT_VIOLATION:
            return {
                type: 'conflict',
                message: error.detail,
                shouldRefresh: false
            };

        case ErrorCodes.CONFLICT_ERROR:
            return {
                type: 'conflict',
                message: error.detail,
                shouldRefresh: true  // Optimistic/pessimistic lock
            };

        case ErrorCodes.TIMEOUT_ERROR:
            return {
                type: 'timeout',
                message: error.detail
            };

        case ErrorCodes.BAD_REQUEST:
            return {
                type: 'error',
                message: error.detail
            };

        case ErrorCodes.DATABASE_ERROR:
        case ErrorCodes.TRANSACTION_ERROR:
        case ErrorCodes.INTERNAL_ERROR:
        default:
            return {
                type: 'error',
                message: error.detail,
                supportRef: error.errorId
            };
    }
}

// Axios interceptor example
axios.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.data?.errorCode) {
            const result = handleApiError(error.response.data);

            switch (result.type) {
                case 'auth':
                    authStore.logout();
                    router.push('/login');
                    break;
                case 'validation':
                    // Handle in component
                    break;
                case 'error':
                    if (result.supportRef) {
                        console.error(`Support ref: ${result.supportRef}`);
                    }
                    toast.error(result.message);
                    break;
                // ... handle other cases
            }
        }
        return Promise.reject(error);
    }
);
```

### Kotlin (Android / KMM)

```kotlin
// data/api/ApiError.kt
@Serializable
data class ProblemDetail(
    val type: String,
    val title: String,
    val status: Int,
    val detail: String,
    val instance: String,
    val timestamp: String,
    val errorCode: String,
    val errorId: String? = null,
    val errors: Map<String, String>? = null,
    val parameterName: String? = null,
    val parameterType: String? = null,
    val field: String? = null,
    val path: String? = null,
    val method: String? = null
)

// Error codes matching backend
object ErrorCodes {
    const val VALIDATION_ERROR = "VALIDATION_ERROR"
    const val BAD_REQUEST = "BAD_REQUEST"
    const val AUTHENTICATION_ERROR = "AUTHENTICATION_ERROR"
    const val AUTHORIZATION_ERROR = "AUTHORIZATION_ERROR"
    const val ENTITY_NOT_FOUND = "ENTITY_NOT_FOUND"
    const val CONSTRAINT_VIOLATION = "CONSTRAINT_VIOLATION"
    const val CONFLICT_ERROR = "CONFLICT_ERROR"
    const val DATABASE_ERROR = "DATABASE_ERROR"
    const val TRANSACTION_ERROR = "TRANSACTION_ERROR"
    const val INTERNAL_ERROR = "INTERNAL_ERROR"
    const val TIMEOUT_ERROR = "TIMEOUT_ERROR"
}

// Sealed class for type-safe error handling
sealed class ApiError {
    data class Validation(val errors: Map<String, String>) : ApiError()
    data class Authentication(val message: String) : ApiError()
    data class Forbidden(val message: String) : ApiError()
    data class NotFound(val message: String) : ApiError()
    data class Conflict(val message: String, val shouldRefresh: Boolean) : ApiError()
    data class Timeout(val message: String) : ApiError()
    data class Server(val message: String, val errorId: String?) : ApiError()
    data class BadRequest(val message: String) : ApiError()
}

fun handleApiError(response: ProblemDetail): ApiError {
    return when (response.errorCode) {
        ErrorCodes.VALIDATION_ERROR ->
            ApiError.Validation(response.errors ?: emptyMap())

        ErrorCodes.AUTHENTICATION_ERROR ->
            ApiError.Authentication(response.detail)

        ErrorCodes.AUTHORIZATION_ERROR ->
            ApiError.Forbidden(response.detail)

        ErrorCodes.ENTITY_NOT_FOUND ->
            ApiError.NotFound(response.detail)

        ErrorCodes.CONSTRAINT_VIOLATION ->
            ApiError.Conflict(response.detail, shouldRefresh = false)

        ErrorCodes.CONFLICT_ERROR ->
            ApiError.Conflict(response.detail, shouldRefresh = true)

        ErrorCodes.TIMEOUT_ERROR ->
            ApiError.Timeout(response.detail)

        ErrorCodes.BAD_REQUEST ->
            ApiError.BadRequest(response.detail)

        ErrorCodes.DATABASE_ERROR,
        ErrorCodes.TRANSACTION_ERROR,
        ErrorCodes.INTERNAL_ERROR ->
            ApiError.Server(response.detail, response.errorId)

        else ->
            ApiError.Server(response.detail, response.errorId)
    }
}

// Usage in ViewModel
fun handleError(error: ApiError) {
    when (error) {
        is ApiError.Validation -> {
            _validationErrors.value = error.errors
        }
        is ApiError.Authentication -> {
            authRepository.logout()
            navigator.navigateToLogin()
        }
        is ApiError.Forbidden -> {
            _uiState.value = UiState.Error(error.message)
        }
        is ApiError.NotFound -> {
            navigator.navigateBack()
            showSnackbar(error.message)
        }
        is ApiError.Conflict -> {
            showSnackbar(error.message)
            if (error.shouldRefresh) {
                refreshData()
            }
        }
        is ApiError.Timeout -> {
            showSnackbar(error.message)
            _canRetry.value = true
        }
        is ApiError.Server -> {
            showSnackbar(error.message)
            error.errorId?.let { id ->
                Log.e("API", "Server error. Support ref: $id")
            }
        }
        is ApiError.BadRequest -> {
            showSnackbar(error.message)
        }
    }
}
```

---

## HTTP Response Codes Summary

| Code | Meaning | Error Codes | Common Causes |
|------|---------|-------------|---------------|
| 200 | OK | - | Request successful |
| 201 | Created | - | Resource created successfully |
| 204 | No Content | - | Delete successful |
| 400 | Bad Request | VALIDATION_ERROR, BAD_REQUEST | Validation error, malformed JSON, missing params |
| 401 | Unauthorized | AUTHENTICATION_ERROR | Invalid/expired token, wrong credentials |
| 403 | Forbidden | AUTHORIZATION_ERROR | No permission for this action |
| 404 | Not Found | ENTITY_NOT_FOUND | Resource doesn't exist, endpoint not found |
| 409 | Conflict | CONSTRAINT_VIOLATION, CONFLICT_ERROR | Duplicate data, concurrent modification |
| 500 | Internal Error | DATABASE_ERROR, TRANSACTION_ERROR, INTERNAL_ERROR | Server-side error (use errorId for support) |
| 504 | Gateway Timeout | TIMEOUT_ERROR | Query took too long |

---

## Best Practices for Frontend

1. **Always use `errorCode`** for logic, not `status` alone (multiple error codes can have same status)
2. **Display `detail` directly** to users - it's always user-friendly and never contains technical info
3. **Log `errorId`** for 500 errors - needed for support tickets and debugging
4. **Handle `errors` object** for form validation - shows field-specific messages
5. **Never show raw API responses** - always process through error handler
6. **Implement global error interceptor** in your HTTP client (Axios, Ktor, etc.)
7. **Handle CONFLICT_ERROR with refresh** - the resource may have been modified

---

## Support & Debugging

When reporting issues to the backend team, always include:

1. **errorId** (if present in response) - MOST IMPORTANT for server errors
2. **Full request URL** and method (GET/POST/etc.)
3. **Request body** (if applicable, exclude passwords)
4. **Full error response** (JSON)
5. **Timestamp** when error occurred
6. **User actions** that led to the error

Example support ticket:
```
Error ID: a1b2c3d4-e5f6-7890-abcd-ef1234567890
Timestamp: 2026-01-15T21:40:01Z
Endpoint: POST /api/v1/tenants/1/users
Request body: {"email": "test@example.com", "name": "Test User"}
Response: {"errorCode": "TRANSACTION_ERROR", "detail": "The database operation could not be completed..."}
User action: Clicked "Create User" button after filling the form
```

---

## API Base URLs

| Environment | URL | Use Case |
|-------------|-----|----------|
| Development | `https://inverapi-dev.apptolast.com` | Testing, development |
| Production | `https://inverapi.apptolast.com` | Live users |

---

## Postman Collection

The complete Postman collection is available at:
```
InvernaderosAPI/Invernaderos_API_Collection.postman_collection.json
```

Import this into Postman and set the `baseUrl` variable to the appropriate environment URL.

---

## New API Features (2026-01-15)

### DataType Catalog

New catalog for data types used in Settings. Defines how to interpret setting values.

**Endpoints:** `/api/v1/catalog/data-types`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/catalog/data-types` | Get all data types |
| GET | `/api/v1/catalog/data-types/active` | Get only active data types |
| GET | `/api/v1/catalog/data-types/{id}` | Get data type by ID |
| GET | `/api/v1/catalog/data-types/name/{name}` | Get data type by name |
| POST | `/api/v1/catalog/data-types` | Create new data type |
| PUT | `/api/v1/catalog/data-types/{id}` | Update data type |
| DELETE | `/api/v1/catalog/data-types/{id}` | Delete data type (not system types) |
| GET | `/api/v1/catalog/data-types/{id}/validate?value=X` | Validate value for data type |

**Available Data Types:**

| ID | Name | Description | Example |
|----|------|-------------|---------|
| 1 | INTEGER | 32-bit integer | `25` |
| 2 | LONG | 64-bit integer | `9223372036854775807` |
| 3 | DOUBLE | Decimal number | `25.5` |
| 4 | BOOLEAN | true/false | `true`, `false`, `1`, `0` |
| 5 | STRING | Text string | `"Invernadero Norte"` |
| 6 | DATE | ISO 8601 date | `2026-01-15` |
| 7 | TIME | Time HH:mm:ss | `14:30:00` |
| 8 | DATETIME | ISO 8601 datetime | `2026-01-15T14:30:00` |
| 9 | JSON | JSON object | `{"key": "value"}` |

**DataType Response:**
```json
{
    "id": 1,
    "name": "INTEGER",
    "description": "Numero entero (32 bits)",
    "validationRegex": "^-?\\d+$",
    "exampleValue": "25",
    "displayOrder": 1,
    "isActive": true
}
```

### Settings Model Refactoring

**BREAKING CHANGE:** Settings model has been refactored.

**Old Structure (DEPRECATED):**
```json
{
    "id": 123,
    "greenhouseId": 1,
    "parameterId": 1,
    "periodId": 1,       // REMOVED
    "minValue": 15.00,   // REMOVED
    "maxValue": 30.00    // REMOVED
}
```

**New Structure:**
```json
{
    "id": 123,
    "code": "SET-00001",
    "greenhouseId": 1,
    "tenantId": 1,
    "parameterId": 1,
    "parameterName": "TEMPERATURE",
    "actuatorStateId": 2,      // NEW: FK to actuator_states (1=OFF, 2=ON, 3=AUTO, etc.)
    "actuatorStateName": "ON", // NEW
    "dataTypeId": 1,           // NEW: FK to data_types
    "dataTypeName": "INTEGER", // NEW
    "value": "25",             // NEW: Single value as String (validated by dataType)
    "isActive": true,
    "createdAt": "2026-01-15T10:00:00Z",
    "updatedAt": "2026-01-15T10:00:00Z"
}
```

**Settings Endpoints Updated:**

| Old Endpoint | New Endpoint |
|--------------|--------------|
| `GET .../greenhouse/{id}/period/{periodId}` | `GET .../greenhouse/{id}/actuator-state/{actuatorStateId}` |
| `GET .../greenhouse/{id}/parameter/{pid}/period/{periodId}` | `GET .../greenhouse/{id}/parameter/{pid}/actuator-state/{actuatorStateId}` |

**Settings Create/Update Request:**
```json
{
    "greenhouseId": 1,
    "parameterId": 1,
    "actuatorStateId": 2,  // Optional: FK to actuator_states
    "dataTypeId": 1,       // Optional: FK to data_types
    "value": "25",         // String value (validated against dataType if provided)
    "isActive": true
}
```

**Unique Constraint Changed:**
- Old: `(greenhouse_id, parameter_id, period_id)`
- New: `(greenhouse_id, parameter_id, actuator_state_id)`

### Device Hierarchy Change

**BREAKING CHANGE:** Device now belongs to Sector instead of Greenhouse.

**Old Hierarchy:**
```
Tenant → Greenhouse → Device
```

**New Hierarchy:**
```
Tenant → Greenhouse → Sector → Device
```

**Device Response Updated:**
```json
{
    "id": 123456789,
    "code": "DEV-00001",
    "tenantId": 1,
    "sectorId": 1,           // CHANGED: was greenhouseId
    "sectorCode": "SEC-00001", // NEW
    "name": "Sensor Temperatura",
    "categoryId": 1,
    "categoryName": "SENSOR",
    "typeId": 1,
    "typeName": "TEMPERATURE",
    "unitId": 1,
    "unitSymbol": "°C",
    "isActive": true
}
```

**Device Create Request:**
```json
{
    "sectorId": 1,      // CHANGED: was greenhouseId
    "name": "Sensor Temperatura",
    "categoryId": 1,
    "typeId": 1,
    "unitId": 1
}
```

**Device Endpoints Updated:**

| Old Endpoint | New Endpoint |
|--------------|--------------|
| `GET /api/v1/tenants/{id}/greenhouses/{ghId}/devices` | Use sector-based queries instead |

### Sector Response Updated

Sector now includes `tenantId`:

```json
{
    "id": 123456789,
    "code": "SEC-00001",
    "tenantId": 1,           // NEW
    "greenhouseId": 1,
    "greenhouseCode": "GH-00001",
    "variety": "Tomate Cherry"
}
```

### Catalog Controller Refactored

All catalog endpoints now use Services instead of Repositories directly. This ensures proper business logic and validation.

**No changes to endpoint URLs**, but responses may include additional validation errors.

---

## Changelog

### Version 2.0 (2026-01-15)

#### Error Handling (GlobalExceptionHandler)
- Complete rewrite of GlobalExceptionHandler
- Added 24+ exception type handlers
- Database errors (SQLException, TransactionSystemException) now return user-friendly messages
- Never expose SQL details, stack traces, or technical messages to frontend
- Added `errorId` for all 500 errors for support correlation
- Added PostgreSQL SQLState-based error messages
- Added comprehensive TypeScript and Kotlin examples
- Added all error codes reference table

#### DataType Catalog (NEW)
- New `data_types` table with 9 basic types
- New CRUD endpoints at `/api/v1/catalog/data-types`
- Value validation endpoint: `GET /data-types/{id}/validate?value=X`

#### Settings Refactoring (BREAKING)
- Replaced `periodId` with `actuatorStateId` (FK to actuator_states)
- Replaced `minValue`/`maxValue` with single `value` field (VARCHAR)
- Added `dataTypeId` to link setting values with their type
- Updated unique constraint: `(greenhouse_id, parameter_id, actuator_state_id)`

#### Device Hierarchy (BREAKING)
- Device now belongs to Sector instead of Greenhouse
- New hierarchy: Tenant → Greenhouse → Sector → Device
- `greenhouseId` replaced with `sectorId` in Device
- Added `sectorCode` to Device response

#### Sector Updates
- Added `tenantId` to Sector entity and response
- `code` is now unique per tenant (not globally)

#### Architecture Improvements
- CatalogController refactored to use Services instead of Repositories
- Fixes ArchitectureTest compliance
