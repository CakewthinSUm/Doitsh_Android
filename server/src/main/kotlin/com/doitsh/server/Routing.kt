package com.doitsh.server

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.time.LocalDateTime

@Serializable
data class HealthResponse(val status: String = "ok", val version: String = "1.0.0")

@Serializable
data class AuthRequest(val username: String, val password: String)

@Serializable
data class AuthResponse(val token: String, val userId: Long)

@Serializable
data class TaskRecord(
    val id: Long, val title: String, val description: String? = null,
    val isCompleted: Boolean, val projectId: Long?, val priority: Int,
    val dueDate: String?, val createdAt: String, val updatedAt: String
)

@Serializable
data class CreateTaskRequest(
    val title: String, val description: String? = null,
    val projectId: Long? = null, val priority: Int = 0, val dueDate: String? = null
)

@Serializable
data class ProjectRecord(
    val id: Long, val name: String, val description: String? = null,
    val color: Int?, val createdAt: String, val updatedAt: String
)

@Serializable
data class CreateProjectRequest(val name: String, val description: String? = null, val color: Int? = null)

fun Application.configureRouting() {
    routing {
        get("/api/health") {
            call.respond(HealthResponse())
        }

        post("/api/auth/login") {
            val request = call.receive<AuthRequest>()
            val user = transaction {
                UsersTable.select { UsersTable.username eq request.username }.singleOrNull()
            }
            if (user == null || !BCrypt.checkpw(request.password, user[UsersTable.passwordHash])) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid credentials"))
                return@post
            }
            val token = generateToken(user[UsersTable.id])
            call.respond(AuthResponse(token = token, userId = user[UsersTable.id]))
        }

        post("/api/auth/register") {
            val request = call.receive<AuthRequest>()
            val existing = transaction {
                UsersTable.select { UsersTable.username eq request.username }.count() > 0
            }
            if (existing) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "Username taken"))
                return@post
            }
            val hash = BCrypt.hashpw(request.password, BCrypt.gensalt())
            val userId = transaction {
                UsersTable.insert {
                    it[username] = request.username
                    it[passwordHash] = hash
                    it[createdAt] = LocalDateTime.now()
                } get UsersTable.id
            }
            val token = generateToken(userId)
            call.respond(AuthResponse(token = token, userId = userId))
        }

        authenticate("auth-jwt") {
            route("/api/tasks") {
                get {
                    val userId = call.principal<JWTPrincipal>()?.payload
                        ?.getClaim("user_id")?.asLong()
                        ?: return@get call.respond(HttpStatusCode.Unauthorized)
                    val tasks = transaction {
                        TasksTable.select { TasksTable.userId eq userId }
                            .map { it.toTaskRecord() }
                    }
                    call.respond(tasks)
                }

                post {
                    val userId = call.principal<JWTPrincipal>()?.payload
                        ?.getClaim("user_id")?.asLong()
                        ?: return@post call.respond(HttpStatusCode.Unauthorized)
                    val request = call.receive<CreateTaskRequest>()
                    val now = LocalDateTime.now()
                    val id = transaction {
                        TasksTable.insert {
                            it[TasksTable.userId] = userId
                            it[title] = request.title
                            it[description] = request.description
                            it[projectId] = request.projectId
                            it[priority] = request.priority
                            it[dueDate] = request.dueDate?.let { d -> LocalDateTime.parse(d) }
                            it[createdAt] = now
                            it[updatedAt] = now
                        } get TasksTable.id
                    }
                    call.respond(HttpStatusCode.Created, mapOf("id" to id))
                }

                delete("{id}") {
                    val userId = call.principal<JWTPrincipal>()?.payload
                        ?.getClaim("user_id")?.asLong()
                        ?: return@delete call.respond(HttpStatusCode.Unauthorized)
                    val id = call.parameters["id"]?.toLongOrNull()
                        ?: return@delete call.respond(HttpStatusCode.BadRequest)
                    transaction {
                        TasksTable.deleteWhere {
                            (TasksTable.id eq id) and (TasksTable.userId eq userId)
                        }
                    }
                    call.respond(HttpStatusCode.NoContent)
                }
            }

            route("/api/projects") {
                get {
                    val userId = call.principal<JWTPrincipal>()?.payload
                        ?.getClaim("user_id")?.asLong()
                        ?: return@get call.respond(HttpStatusCode.Unauthorized)
                    val projects = transaction {
                        ProjectsTable.select { ProjectsTable.userId eq userId }
                            .map { it.toProjectRecord() }
                    }
                    call.respond(projects)
                }

                post {
                    val userId = call.principal<JWTPrincipal>()?.payload
                        ?.getClaim("user_id")?.asLong()
                        ?: return@post call.respond(HttpStatusCode.Unauthorized)
                    val request = call.receive<CreateProjectRequest>()
                    val now = LocalDateTime.now()
                    val id = transaction {
                        ProjectsTable.insert {
                            it[ProjectsTable.userId] = userId
                            it[name] = request.name
                            it[description] = request.description
                            it[color] = request.color
                            it[createdAt] = now
                            it[updatedAt] = now
                        } get ProjectsTable.id
                    }
                    call.respond(HttpStatusCode.Created, mapOf("id" to id))
                }
            }
        }
    }
}

private fun generateToken(userId: Long): String {
    val secret = "doitsh-dev-secret-change-in-production"
    return JWT.create()
        .withAudience("doitsh-client")
        .withIssuer("doitsh-server")
        .withClaim("user_id", userId)
        .sign(Algorithm.HMAC256(secret))
}

private fun ResultRow.toTaskRecord() = TaskRecord(
    id = this[TasksTable.id],
    title = this[TasksTable.title],
    description = this[TasksTable.description],
    isCompleted = this[TasksTable.isCompleted],
    projectId = this[TasksTable.projectId],
    priority = this[TasksTable.priority],
    dueDate = this[TasksTable.dueDate]?.toString(),
    createdAt = this[TasksTable.createdAt].toString(),
    updatedAt = this[TasksTable.updatedAt].toString()
)

private fun ResultRow.toProjectRecord() = ProjectRecord(
    id = this[ProjectsTable.id],
    name = this[ProjectsTable.name],
    description = this[ProjectsTable.description],
    color = this[ProjectsTable.color],
    createdAt = this[ProjectsTable.createdAt].toString(),
    updatedAt = this[ProjectsTable.updatedAt].toString()
)
