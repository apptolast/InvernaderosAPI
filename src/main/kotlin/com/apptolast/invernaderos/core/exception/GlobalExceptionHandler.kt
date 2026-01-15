package com.apptolast.invernaderos.core.exception

import com.fasterxml.jackson.databind.exc.InvalidDefinitionException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import jakarta.persistence.EntityNotFoundException
import jakarta.persistence.PersistenceException
import jakarta.validation.ConstraintViolationException
import java.sql.SQLException
import java.time.Instant
import java.util.NoSuchElementException
import java.util.UUID
import org.hibernate.exception.ConstraintViolationException as HibernateConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.dao.PessimisticLockingFailureException
import org.springframework.dao.QueryTimeoutException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.orm.jpa.JpaSystemException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.AuthenticationException
import org.springframework.transaction.TransactionSystemException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingPathVariableException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.NoHandlerFoundException
import org.springframework.web.servlet.resource.NoResourceFoundException

/**
 * Global exception handler usando Spring Boot 3+ RFC 7807 (ProblemDetail)
 *
 * Centraliza el manejo de excepciones para toda la aplicación REST.
 * IMPORTANTE: Este handler devuelve respuestas claras y amigables al frontend,
 * sin exponer detalles técnicos internos (SQL, stack traces, etc.)
 *
 * @see <a href="https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html">Spring Error Responses</a>
 * @see <a href="https://www.rfc-editor.org/rfc/rfc7807">RFC 7807 - Problem Details for HTTP APIs</a>
 */
@RestControllerAdvice
class GlobalExceptionHandler {

        private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

        companion object {
                // Error codes for frontend identification
                const val ERROR_CODE_DATABASE = "DATABASE_ERROR"
                const val ERROR_CODE_TRANSACTION = "TRANSACTION_ERROR"
                const val ERROR_CODE_CONSTRAINT_VIOLATION = "CONSTRAINT_VIOLATION"
                const val ERROR_CODE_ENTITY_NOT_FOUND = "ENTITY_NOT_FOUND"
                const val ERROR_CODE_VALIDATION = "VALIDATION_ERROR"
                const val ERROR_CODE_AUTHENTICATION = "AUTHENTICATION_ERROR"
                const val ERROR_CODE_AUTHORIZATION = "AUTHORIZATION_ERROR"
                const val ERROR_CODE_BAD_REQUEST = "BAD_REQUEST"
                const val ERROR_CODE_INTERNAL = "INTERNAL_ERROR"
                const val ERROR_CODE_TIMEOUT = "TIMEOUT_ERROR"
                const val ERROR_CODE_CONFLICT = "CONFLICT_ERROR"
        }

        // =====================================================================
        // DATABASE EXCEPTIONS - Handlers específicos para errores de BD
        // =====================================================================

        /**
         * Maneja excepciones de SQL directas.
         * NUNCA expone el mensaje técnico de SQL al frontend.
         */
        @ExceptionHandler(SQLException::class)
        fun handleSqlException(ex: SQLException): ResponseEntity<ProblemDetail> {
                logger.error("SQL Exception - SQLState: {}, ErrorCode: {}, Message: {}",
                        ex.sqlState, ex.errorCode, ex.message, ex)

                val userMessage = determineDatabaseErrorMessage(ex.sqlState, ex.message)

                val problemDetail = ProblemDetail.forStatusAndDetail(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        userMessage
                )
                problemDetail.title = "Database Operation Failed"
                problemDetail.setProperty("timestamp", Instant.now())
                problemDetail.setProperty("errorCode", ERROR_CODE_DATABASE)
                problemDetail.setProperty("errorId", UUID.randomUUID().toString())

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail)
        }

        /**
         * Maneja excepciones generales de acceso a datos (Spring Data).
         * Captura la mayoría de errores de base de datos de Spring.
         */
        @ExceptionHandler(DataAccessException::class)
        fun handleDataAccessException(ex: DataAccessException): ResponseEntity<ProblemDetail> {
                logger.error("Data Access Exception: {} - Root cause: {}",
                        ex.javaClass.simpleName, ex.rootCause?.message, ex)

                val userMessage = when (ex) {
                        is DataIntegrityViolationException -> {
                                extractConstraintViolationMessage(ex)
                        }
                        is EmptyResultDataAccessException -> {
                                "The requested resource was not found in the database."
                        }
                        is QueryTimeoutException -> {
                                "The database query took too long to execute. Please try again."
                        }
                        is OptimisticLockingFailureException -> {
                                "The resource was modified by another user. Please refresh and try again."
                        }
                        is PessimisticLockingFailureException -> {
                                "The resource is currently locked by another operation. Please try again later."
                        }
                        else -> {
                                "A database error occurred while processing your request. Please try again."
                        }
                }

                val status = when (ex) {
                        is EmptyResultDataAccessException -> HttpStatus.NOT_FOUND
                        is DataIntegrityViolationException -> HttpStatus.CONFLICT
                        is OptimisticLockingFailureException, is PessimisticLockingFailureException -> HttpStatus.CONFLICT
                        is QueryTimeoutException -> HttpStatus.GATEWAY_TIMEOUT
                        else -> HttpStatus.INTERNAL_SERVER_ERROR
                }

                val errorCode = when (ex) {
                        is DataIntegrityViolationException -> ERROR_CODE_CONSTRAINT_VIOLATION
                        is EmptyResultDataAccessException -> ERROR_CODE_ENTITY_NOT_FOUND
                        is QueryTimeoutException -> ERROR_CODE_TIMEOUT
                        is OptimisticLockingFailureException, is PessimisticLockingFailureException -> ERROR_CODE_CONFLICT
                        else -> ERROR_CODE_DATABASE
                }

                val problemDetail = ProblemDetail.forStatusAndDetail(status, userMessage)
                problemDetail.title = determineDatabaseErrorTitle(ex)
                problemDetail.setProperty("timestamp", Instant.now())
                problemDetail.setProperty("errorCode", errorCode)
                problemDetail.setProperty("errorId", UUID.randomUUID().toString())

                return ResponseEntity.status(status).body(problemDetail)
        }

        /**
         * Maneja errores de transacción abortada.
         * Este es el error específico que el usuario reportó.
         */
        @ExceptionHandler(TransactionSystemException::class)
        fun handleTransactionSystemException(ex: TransactionSystemException): ResponseEntity<ProblemDetail> {
                logger.error("Transaction System Exception: {} - Root cause: {}",
                        ex.message, ex.rootCause?.message, ex)

                // Buscar la causa raíz para determinar el mensaje apropiado
                val rootCause = ex.rootCause ?: ex.cause
                val userMessage = when {
                        rootCause is ConstraintViolationException -> {
                                "Validation failed: ${formatConstraintViolations(rootCause)}"
                        }
                        rootCause?.message?.contains("aborted", ignoreCase = true) == true -> {
                                "The database operation could not be completed due to a previous error. Please retry the operation."
                        }
                        rootCause?.message?.contains("constraint", ignoreCase = true) == true -> {
                                "The operation violates a data constraint. Please check your input and try again."
                        }
                        else -> {
                                "The database transaction could not be completed. Please try again."
                        }
                }

                val problemDetail = ProblemDetail.forStatusAndDetail(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        userMessage
                )
                problemDetail.title = "Transaction Failed"
                problemDetail.setProperty("timestamp", Instant.now())
                problemDetail.setProperty("errorCode", ERROR_CODE_TRANSACTION)
                problemDetail.setProperty("errorId", UUID.randomUUID().toString())

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail)
        }

        /**
         * Maneja excepciones de persistencia JPA.
         */
        @ExceptionHandler(PersistenceException::class)
        fun handlePersistenceException(ex: PersistenceException): ResponseEntity<ProblemDetail> {
                logger.error("Persistence Exception: {} - Cause: {}", ex.message, ex.cause?.message, ex)

                val userMessage = when {
                        ex.cause is HibernateConstraintViolationException -> {
                                extractHibernateConstraintMessage(ex.cause as HibernateConstraintViolationException)
                        }
                        ex.message?.contains("constraint", ignoreCase = true) == true -> {
                                "The operation violates a data constraint. The data may already exist or references invalid data."
                        }
                        else -> {
                                "A database persistence error occurred. Please check your data and try again."
                        }
                }

                val status = if (ex.cause is HibernateConstraintViolationException) {
                        HttpStatus.CONFLICT
                } else {
                        HttpStatus.INTERNAL_SERVER_ERROR
                }

                val problemDetail = ProblemDetail.forStatusAndDetail(status, userMessage)
                problemDetail.title = "Persistence Error"
                problemDetail.setProperty("timestamp", Instant.now())
                problemDetail.setProperty("errorCode", ERROR_CODE_DATABASE)
                problemDetail.setProperty("errorId", UUID.randomUUID().toString())

                return ResponseEntity.status(status).body(problemDetail)
        }

        /**
         * Maneja violaciones de constraint de Hibernate.
         */
        @ExceptionHandler(HibernateConstraintViolationException::class)
        fun handleHibernateConstraintViolation(ex: HibernateConstraintViolationException): ResponseEntity<ProblemDetail> {
                logger.error("Hibernate Constraint Violation: {} - Constraint: {}",
                        ex.message, ex.constraintName, ex)

                val userMessage = extractHibernateConstraintMessage(ex)

                val problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, userMessage)
                problemDetail.title = "Data Constraint Violation"
                problemDetail.setProperty("timestamp", Instant.now())
                problemDetail.setProperty("errorCode", ERROR_CODE_CONSTRAINT_VIOLATION)
                problemDetail.setProperty("constraintName", ex.constraintName ?: "unknown")
                problemDetail.setProperty("errorId", UUID.randomUUID().toString())

                return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail)
        }

        /**
         * Maneja entidades no encontradas (JPA).
         */
        @ExceptionHandler(EntityNotFoundException::class)
        fun handleEntityNotFound(ex: EntityNotFoundException): ResponseEntity<ProblemDetail> {
                logger.warn("Entity not found: {}", ex.message)

                val problemDetail = ProblemDetail.forStatusAndDetail(
                        HttpStatus.NOT_FOUND,
                        ex.message ?: "The requested entity was not found."
                )
                problemDetail.title = "Entity Not Found"
                problemDetail.setProperty("timestamp", Instant.now())
                problemDetail.setProperty("errorCode", ERROR_CODE_ENTITY_NOT_FOUND)

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail)
        }

        /**
         * Maneja errores del sistema JPA.
         */
        @ExceptionHandler(JpaSystemException::class)
        fun handleJpaSystemException(ex: JpaSystemException): ResponseEntity<ProblemDetail> {
                logger.error("JPA System Exception: {} - Root cause: {}",
                        ex.message, ex.rootCause?.message, ex)

                val userMessage = "A database system error occurred. Please try again later."

                val problemDetail = ProblemDetail.forStatusAndDetail(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        userMessage
                )
                problemDetail.title = "Database System Error"
                problemDetail.setProperty("timestamp", Instant.now())
                problemDetail.setProperty("errorCode", ERROR_CODE_DATABASE)
                problemDetail.setProperty("errorId", UUID.randomUUID().toString())

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail)
        }

        // =====================================================================
        // SECURITY EXCEPTIONS - Autenticación y Autorización
        // =====================================================================

        /**
         * Maneja excepciones de autenticación general.
         */
        @ExceptionHandler(AuthenticationException::class)
        fun handleAuthenticationException(ex: AuthenticationException): ResponseEntity<ProblemDetail> {
                logger.warn("Authentication failed: {}", ex.message)

                val problemDetail = ProblemDetail.forStatusAndDetail(
                        HttpStatus.UNAUTHORIZED,
                        "Authentication failed. Please check your credentials and try again."
                )
                problemDetail.title = "Authentication Failed"
                problemDetail.setProperty("timestamp", Instant.now())
                problemDetail.setProperty("errorCode", ERROR_CODE_AUTHENTICATION)

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problemDetail)
        }

        /**
         * Maneja acceso denegado (falta de permisos).
         */
        @ExceptionHandler(AccessDeniedException::class)
        fun handleAccessDenied(ex: AccessDeniedException): ResponseEntity<ProblemDetail> {
                logger.warn("Access denied: {}", ex.message)

                val problemDetail = ProblemDetail.forStatusAndDetail(
                        HttpStatus.FORBIDDEN,
                        "You do not have permission to perform this action."
                )
                problemDetail.title = "Access Denied"
                problemDetail.setProperty("timestamp", Instant.now())
                problemDetail.setProperty("errorCode", ERROR_CODE_AUTHORIZATION)

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problemDetail)
        }

        // =====================================================================
        // REQUEST PARAMETER EXCEPTIONS - Errores de parámetros de petición
        // =====================================================================

        /**
         * Maneja parámetros de petición faltantes.
         */
        @ExceptionHandler(MissingServletRequestParameterException::class)
        fun handleMissingParameter(ex: MissingServletRequestParameterException): ResponseEntity<ProblemDetail> {
                logger.warn("Missing request parameter: {}", ex.parameterName)

                val problemDetail = ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST,
                        "Required parameter '${ex.parameterName}' of type '${ex.parameterType}' is missing."
                )
                problemDetail.title = "Missing Required Parameter"
                problemDetail.setProperty("timestamp", Instant.now())
                problemDetail.setProperty("errorCode", ERROR_CODE_BAD_REQUEST)
                problemDetail.setProperty("parameterName", ex.parameterName)
                problemDetail.setProperty("parameterType", ex.parameterType)

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail)
        }

        /**
         * Maneja path variables faltantes.
         */
        @ExceptionHandler(MissingPathVariableException::class)
        fun handleMissingPathVariable(ex: MissingPathVariableException): ResponseEntity<ProblemDetail> {
                logger.warn("Missing path variable: {}", ex.variableName)

                val problemDetail = ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST,
                        "Required path variable '${ex.variableName}' is missing from the URL."
                )
                problemDetail.title = "Missing Required Path Variable"
                problemDetail.setProperty("timestamp", Instant.now())
                problemDetail.setProperty("errorCode", ERROR_CODE_BAD_REQUEST)
                problemDetail.setProperty("variableName", ex.variableName)

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail)
        }

        /**
         * Maneja recursos no encontrados (endpoints inexistentes).
         */
        @ExceptionHandler(NoResourceFoundException::class)
        fun handleNoResourceFound(ex: NoResourceFoundException): ResponseEntity<ProblemDetail> {
                logger.warn("Resource not found: {} {}", ex.httpMethod, ex.resourcePath)

                val problemDetail = ProblemDetail.forStatusAndDetail(
                        HttpStatus.NOT_FOUND,
                        "The requested resource '${ex.resourcePath}' was not found."
                )
                problemDetail.title = "Resource Not Found"
                problemDetail.setProperty("timestamp", Instant.now())
                problemDetail.setProperty("errorCode", ERROR_CODE_ENTITY_NOT_FOUND)
                problemDetail.setProperty("path", ex.resourcePath)
                problemDetail.setProperty("method", ex.httpMethod.name())

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail)
        }

        // =====================================================================
        // GENERIC EXCEPTION HANDLER - Fallback para excepciones no manejadas
        // =====================================================================

        /**
         * Maneja excepciones generales no capturadas.
         * IMPORTANTE: NO expone el mensaje técnico de la excepción al frontend.
         */
        @ExceptionHandler(Exception::class)
        fun handleGeneralException(ex: Exception): ResponseEntity<ProblemDetail> {
                // Generar un ID único para correlacionar logs con respuesta
                val errorId = UUID.randomUUID().toString()

                logger.error("Unhandled exception [errorId={}]: {} - {}",
                        errorId, ex.javaClass.simpleName, ex.message, ex)

                // NUNCA exponer el mensaje técnico al frontend
                val problemDetail = ProblemDetail.forStatusAndDetail(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "An unexpected error occurred while processing your request. Please try again later. If the problem persists, contact support with error ID: $errorId"
                )
                problemDetail.title = "Internal Server Error"
                problemDetail.setProperty("timestamp", Instant.now())
                problemDetail.setProperty("errorCode", ERROR_CODE_INTERNAL)
                problemDetail.setProperty("errorId", errorId)

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail)
        }

        /** Maneja excepciones de validación (@Valid en request bodies) */
        @ExceptionHandler(MethodArgumentNotValidException::class)
        fun handleValidationException(
                ex: MethodArgumentNotValidException
        ): ResponseEntity<ProblemDetail> {
                logger.warn("Validation error: {}", ex.message)

                val errors =
                        ex.bindingResult.fieldErrors.associate {
                                it.field to (it.defaultMessage ?: "Invalid value")
                        }

                val problemDetail =
                        ProblemDetail.forStatusAndDetail(
                                HttpStatus.BAD_REQUEST,
                                "Validation failed for one or more fields"
                        )
                problemDetail.title = "Validation Error"
                problemDetail.setProperty("timestamp", Instant.now())
                problemDetail.setProperty("errorCode", ERROR_CODE_VALIDATION)
                problemDetail.setProperty("errors", errors)

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail)
        }

        /** Maneja errores de tipo en path variables o request params */
        @ExceptionHandler(MethodArgumentTypeMismatchException::class)
        fun handleTypeMismatch(
                ex: MethodArgumentTypeMismatchException
        ): ResponseEntity<ProblemDetail> {
                logger.warn("Type mismatch error: {}", ex.message)

                val problemDetail =
                        ProblemDetail.forStatusAndDetail(
                                HttpStatus.BAD_REQUEST,
                                "Invalid parameter type: '${ex.name}' should be of type ${ex.requiredType?.simpleName}"
                        )
                problemDetail.title = "Invalid Parameter Type"
                problemDetail.setProperty("timestamp", Instant.now())
                problemDetail.setProperty("errorCode", ERROR_CODE_BAD_REQUEST)
                problemDetail.setProperty("parameter", ex.name)
                problemDetail.setProperty("expectedType", ex.requiredType?.simpleName)

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail)
        }

        /** Maneja recursos no encontrados (404) */
        @ExceptionHandler(NoHandlerFoundException::class)
        fun handleNotFound(ex: NoHandlerFoundException): ResponseEntity<ProblemDetail> {
                logger.warn("Endpoint not found: {} {}", ex.httpMethod, ex.requestURL)

                val problemDetail =
                        ProblemDetail.forStatusAndDetail(
                                HttpStatus.NOT_FOUND,
                                "The requested endpoint '${ex.requestURL}' does not exist"
                        )
                problemDetail.title = "Endpoint Not Found"
                problemDetail.setProperty("timestamp", Instant.now())
                problemDetail.setProperty("errorCode", ERROR_CODE_ENTITY_NOT_FOUND)
                problemDetail.setProperty("path", ex.requestURL)
                problemDetail.setProperty("method", ex.httpMethod)

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail)
        }

        /** Maneja errores de serialización de Jackson (ej: Instant sin JavaTimeModule) */
        @ExceptionHandler(InvalidDefinitionException::class)
        fun handleJacksonInvalidDefinition(
                ex: InvalidDefinitionException
        ): ResponseEntity<ProblemDetail> {
                val errorId = UUID.randomUUID().toString()
                logger.error("Jackson serialization error [errorId={}]: {}", errorId, ex.message, ex)

                val problemDetail =
                        ProblemDetail.forStatusAndDetail(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "An error occurred while preparing the response. Please try again."
                        )
                problemDetail.title = "Response Serialization Error"
                problemDetail.setProperty("timestamp", Instant.now())
                problemDetail.setProperty("errorCode", ERROR_CODE_INTERNAL)
                problemDetail.setProperty("errorId", errorId)

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail)
        }

        /**
         * Maneja errores de deserialización JSON (MismatchedInputException)
         * Ocurre cuando el JSON no coincide con el tipo esperado (ej: formato incorrecto en BD)
         */
        @ExceptionHandler(MismatchedInputException::class)
        fun handleMismatchedInput(ex: MismatchedInputException): ResponseEntity<ProblemDetail> {
                val errorId = UUID.randomUUID().toString()
                logger.error("JSON deserialization error [errorId={}]: {}", errorId, ex.message, ex)

                val fieldPath = ex.path?.joinToString(".") { it.fieldName ?: "[${it.index}]" }
                val userMessage = if (fieldPath.isNullOrBlank()) {
                        "The data format is invalid. Please check your request and try again."
                } else {
                        "Invalid data format for field '$fieldPath'. Please verify the value type."
                }

                val problemDetail =
                        ProblemDetail.forStatusAndDetail(
                                HttpStatus.BAD_REQUEST,
                                userMessage
                        )
                problemDetail.title = "Invalid Data Format"
                problemDetail.setProperty("timestamp", Instant.now())
                problemDetail.setProperty("errorCode", ERROR_CODE_BAD_REQUEST)
                if (!fieldPath.isNullOrBlank()) {
                        problemDetail.setProperty("field", fieldPath)
                }

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail)
        }

        /**
         * Maneja errores de lectura de mensajes HTTP (envuelve errores de Jackson)
         * Ocurre cuando el request body no puede ser parseado
         */
        @ExceptionHandler(HttpMessageNotReadableException::class)
        fun handleHttpMessageNotReadable(
                ex: HttpMessageNotReadableException
        ): ResponseEntity<ProblemDetail> {
                logger.warn("HTTP message not readable: {}", ex.message)

                val detail = when (val cause = ex.cause) {
                        is MismatchedInputException -> {
                                val fieldPath = cause.path?.joinToString(".") { it.fieldName ?: "[${it.index}]" }
                                if (fieldPath.isNullOrBlank()) {
                                        "Invalid JSON format. Expected type: ${cause.targetType?.simpleName}"
                                } else {
                                        "Invalid JSON format for field '$fieldPath'. Expected type: ${cause.targetType?.simpleName}"
                                }
                        }
                        else ->
                                "Request body is malformed or missing. Please provide valid JSON."
                }

                val problemDetail =
                        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail)
                problemDetail.title = "Invalid Request Body"
                problemDetail.setProperty("timestamp", Instant.now())
                problemDetail.setProperty("errorCode", ERROR_CODE_BAD_REQUEST)

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail)
        }

        /** Maneja IllegalArgumentException (usualmente de validaciones de negocio) */
        @ExceptionHandler(IllegalArgumentException::class)
        fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ProblemDetail> {
                logger.warn("Illegal argument: {}", ex.message)

                val problemDetail =
                        ProblemDetail.forStatusAndDetail(
                                HttpStatus.BAD_REQUEST,
                                ex.message ?: "Invalid argument provided"
                        )
                problemDetail.title = "Invalid Argument"
                problemDetail.setProperty("timestamp", Instant.now())
                problemDetail.setProperty("errorCode", ERROR_CODE_BAD_REQUEST)

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail)
        }

        /** Maneja IllegalStateException (errores de estado de la aplicación) */
        @ExceptionHandler(IllegalStateException::class)
        fun handleIllegalState(ex: IllegalStateException): ResponseEntity<ProblemDetail> {
                logger.error("Illegal state: {}", ex.message, ex)

                val problemDetail =
                        ProblemDetail.forStatusAndDetail(
                                HttpStatus.CONFLICT,
                                ex.message
                                        ?: "The operation cannot be performed in the current state"
                        )
                problemDetail.title = "Operation Conflict"
                problemDetail.setProperty("timestamp", Instant.now())
                problemDetail.setProperty("errorCode", ERROR_CODE_CONFLICT)

                return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail)
        }

        /** Maneja NoSuchElementException (recursos no encontrados) */
        @ExceptionHandler(NoSuchElementException::class)
        fun handleNoSuchElement(ex: NoSuchElementException): ResponseEntity<ProblemDetail> {
                logger.warn("Resource not found: {}", ex.message)

                val problemDetail =
                        ProblemDetail.forStatusAndDetail(
                                HttpStatus.NOT_FOUND,
                                ex.message ?: "The requested resource was not found"
                        )
                problemDetail.title = "Resource Not Found"
                problemDetail.setProperty("timestamp", Instant.now())
                problemDetail.setProperty("errorCode", ERROR_CODE_ENTITY_NOT_FOUND)

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail)
        }
        /**
         * Maneja excepciones de violación de restricciones de validación (Jakarta Bean Validation)
         */
        @ExceptionHandler(ConstraintViolationException::class)
        fun handleConstraintViolation(
                ex: ConstraintViolationException
        ): ResponseEntity<ProblemDetail> {
                logger.warn("Constraint violation: {}", ex.message)

                val errors =
                        ex.constraintViolations.associate {
                                (it.propertyPath.toString()) to (it.message ?: "Invalid value")
                        }

                val problemDetail =
                        ProblemDetail.forStatusAndDetail(
                                HttpStatus.BAD_REQUEST,
                                "Validation failed for one or more fields"
                        )
                problemDetail.title = "Validation Error"
                problemDetail.setProperty("timestamp", Instant.now())
                problemDetail.setProperty("errorCode", ERROR_CODE_VALIDATION)
                problemDetail.setProperty("errors", errors)

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail)
        }

        /** Maneja BadCredentialsException (login fallido) */
        @ExceptionHandler(BadCredentialsException::class)
        fun handleBadCredentials(ex: BadCredentialsException): ResponseEntity<ProblemDetail> {
                logger.warn("Authentication failed: {}", ex.message)

                val problemDetail =
                        ProblemDetail.forStatusAndDetail(
                                HttpStatus.UNAUTHORIZED,
                                "Invalid username or password"
                        )
                problemDetail.title = "Authentication Failed"
                problemDetail.setProperty("timestamp", Instant.now())
                problemDetail.setProperty("errorCode", ERROR_CODE_AUTHENTICATION)

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problemDetail)
        }

        // =====================================================================
        // HELPER METHODS - Métodos auxiliares para formatear mensajes de error
        // =====================================================================

        /**
         * Determina el mensaje de error para el usuario basándose en el código SQL State.
         * Nunca expone el mensaje técnico de SQL al frontend.
         *
         * @see <a href="https://www.postgresql.org/docs/current/errcodes-appendix.html">PostgreSQL Error Codes</a>
         */
        private fun determineDatabaseErrorMessage(sqlState: String?, rawMessage: String?): String {
                return when {
                        // Class 23 - Integrity Constraint Violation
                        sqlState?.startsWith("23") == true -> when (sqlState) {
                                "23505" -> "A record with this identifier already exists. Please use a different value."
                                "23503" -> "Cannot complete operation because it references data that does not exist."
                                "23502" -> "A required field is missing. Please provide all required data."
                                "23514" -> "The provided data violates validation rules. Please check your input."
                                else -> "The operation violates data integrity constraints. Please verify your data."
                        }
                        // Class 25 - Invalid Transaction State (el error del usuario)
                        sqlState?.startsWith("25") == true -> when (sqlState) {
                                "25P02" -> "The database operation failed due to a previous error. Please retry the operation."
                                "25001" -> "The operation cannot be performed in the current transaction state."
                                else -> "A transaction error occurred. Please retry the operation."
                        }
                        // Class 40 - Transaction Rollback
                        sqlState?.startsWith("40") == true -> {
                                "The operation was rolled back due to a conflict. Please retry."
                        }
                        // Class 42 - Syntax Error or Access Rule Violation
                        sqlState?.startsWith("42") == true -> {
                                "The database operation could not be processed. Please contact support."
                        }
                        // Class 53 - Insufficient Resources
                        sqlState?.startsWith("53") == true -> {
                                "The database is temporarily unavailable due to resource constraints. Please try again later."
                        }
                        // Class 57 - Operator Intervention
                        sqlState?.startsWith("57") == true -> {
                                "The database connection was interrupted. Please try again."
                        }
                        // Class 08 - Connection Exception
                        sqlState?.startsWith("08") == true -> {
                                "Unable to connect to the database. Please try again later."
                        }
                        // Analyze raw message for common patterns (without exposing technical details)
                        rawMessage?.contains("unique constraint", ignoreCase = true) == true -> {
                                "A record with this identifier already exists."
                        }
                        rawMessage?.contains("foreign key", ignoreCase = true) == true -> {
                                "The operation references data that does not exist or cannot be deleted."
                        }
                        rawMessage?.contains("not-null", ignoreCase = true) == true -> {
                                "A required field is missing."
                        }
                        rawMessage?.contains("timeout", ignoreCase = true) == true -> {
                                "The database operation timed out. Please try again."
                        }
                        else -> {
                                "A database error occurred while processing your request. Please try again."
                        }
                }
        }

        /**
         * Determina el título del error basándose en el tipo de excepción de DataAccess.
         */
        private fun determineDatabaseErrorTitle(ex: DataAccessException): String {
                return when (ex) {
                        is DataIntegrityViolationException -> "Data Integrity Violation"
                        is EmptyResultDataAccessException -> "Resource Not Found"
                        is QueryTimeoutException -> "Query Timeout"
                        is OptimisticLockingFailureException -> "Concurrent Modification Conflict"
                        is PessimisticLockingFailureException -> "Resource Locked"
                        else -> "Database Error"
                }
        }

        /**
         * Extrae un mensaje amigable de una violación de constraint de integridad.
         */
        private fun extractConstraintViolationMessage(ex: DataIntegrityViolationException): String {
                val rootCause = ex.rootCause?.message ?: ex.message ?: ""

                return when {
                        rootCause.contains("unique", ignoreCase = true) ||
                                rootCause.contains("duplicate", ignoreCase = true) -> {
                                extractDuplicateKeyField(rootCause)?.let { field ->
                                        "A record with this $field already exists. Please use a different value."
                                } ?: "A record with this identifier already exists."
                        }
                        rootCause.contains("foreign key", ignoreCase = true) ||
                                rootCause.contains("fk_", ignoreCase = true) -> {
                                if (rootCause.contains("update", ignoreCase = true) ||
                                        rootCause.contains("delete", ignoreCase = true)) {
                                        "Cannot modify or delete this record because it is referenced by other data."
                                } else {
                                        "The referenced record does not exist. Please verify the related data."
                                }
                        }
                        rootCause.contains("not-null", ignoreCase = true) ||
                                rootCause.contains("null value", ignoreCase = true) -> {
                                extractNullField(rootCause)?.let { field ->
                                        "The field '$field' is required and cannot be empty."
                                } ?: "A required field is missing. Please provide all required data."
                        }
                        rootCause.contains("check", ignoreCase = true) -> {
                                "The provided data violates validation rules. Please check your input."
                        }
                        else -> {
                                "The data violates database constraints. Please verify your input."
                        }
                }
        }

        /**
         * Intenta extraer el nombre del campo duplicado del mensaje de error.
         */
        private fun extractDuplicateKeyField(message: String): String? {
                // Pattern: "Key (field_name)=" or "column \"field_name\""
                val patterns = listOf(
                        Regex("""Key \((\w+)\)=""", RegexOption.IGNORE_CASE),
                        Regex("""column "(\w+)"""", RegexOption.IGNORE_CASE),
                        Regex("""_(\w+)_key""", RegexOption.IGNORE_CASE)
                )
                for (pattern in patterns) {
                        pattern.find(message)?.groups?.get(1)?.value?.let { return it }
                }
                return null
        }

        /**
         * Intenta extraer el nombre del campo nulo del mensaje de error.
         */
        private fun extractNullField(message: String): String? {
                val pattern = Regex("""column "(\w+)"""", RegexOption.IGNORE_CASE)
                return pattern.find(message)?.groups?.get(1)?.value
        }

        /**
         * Extrae un mensaje amigable de una violación de constraint de Hibernate.
         */
        private fun extractHibernateConstraintMessage(ex: HibernateConstraintViolationException): String {
                val constraintName = ex.constraintName ?: ""
                val sqlException = ex.sqlException?.message ?: ""

                return when {
                        constraintName.contains("unique", ignoreCase = true) ||
                                constraintName.contains("_key", ignoreCase = true) ||
                                constraintName.endsWith("_pkey") -> {
                                "A record with this identifier already exists."
                        }
                        constraintName.contains("fk_", ignoreCase = true) ||
                                constraintName.contains("_fkey", ignoreCase = true) -> {
                                "The operation references data that does not exist."
                        }
                        constraintName.contains("_check") ||
                                constraintName.contains("chk_") -> {
                                "The provided value does not meet the required criteria."
                        }
                        sqlException.contains("unique constraint", ignoreCase = true) -> {
                                "A record with this identifier already exists."
                        }
                        else -> {
                                "The data violates a database constraint. Please verify your input."
                        }
                }
        }

        /**
         * Formatea las violaciones de constraint de Jakarta Bean Validation.
         */
        private fun formatConstraintViolations(ex: ConstraintViolationException): String {
                return ex.constraintViolations
                        .take(3) // Limitar a 3 errores para no sobrecargar el mensaje
                        .joinToString("; ") { violation ->
                                "${violation.propertyPath}: ${violation.message}"
                        }
                        .let { message ->
                                if (ex.constraintViolations.size > 3) {
                                        "$message; and ${ex.constraintViolations.size - 3} more error(s)"
                                } else {
                                        message
                                }
                        }
        }
}
