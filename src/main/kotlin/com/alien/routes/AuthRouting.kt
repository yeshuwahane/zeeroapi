package com.alien.routes

import com.alien.models.LoginRequest
import com.alien.models.RegisterRequest
import com.alien.models.User
import com.alien.models.UserRole
import com.alien.plugins.UsersTable
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

fun Route.authRouting() {
    route("/api/auth") {
        post("/login") {
            val request = call.receive<LoginRequest>()
            val user = transaction {
                UsersTable.select {
                    (UsersTable.email eq request.email) and
                    (UsersTable.password eq request.password) and
                    (UsersTable.role eq request.role.name)
                }.map { row ->
                    User(
                        id = row[UsersTable.id],
                        name = row[UsersTable.name],
                        email = row[UsersTable.email],
                        password = row[UsersTable.password],
                        role = UserRole.valueOf(row[UsersTable.role])
                    )
                }.firstOrNull()
            }

            if (user != null) {
                call.respond(HttpStatusCode.OK, user)
            } else {
                call.respond(HttpStatusCode.Unauthorized, "Invalid credentials or role mismatch.")
            }
        }

        post("/register") {
            val request = call.receive<RegisterRequest>()
            
            val existingUser = transaction {
                UsersTable.select { UsersTable.email eq request.email }.count() > 0
            }

            if (existingUser) {
                call.respond(HttpStatusCode.Conflict, "Email is already registered.")
                return@post
            }

            val newId = UUID.randomUUID().toString()
            val newUser = transaction {
                UsersTable.insert {
                    it[id] = newId
                    it[name] = request.name
                    it[email] = request.email
                    it[password] = request.password
                    it[role] = request.role.name
                }
                User(
                    id = newId,
                    name = request.name,
                    email = request.email,
                    password = request.password,
                    role = request.role
                )
            }

            call.respond(HttpStatusCode.Created, newUser)
        }

        get("/users") {
            val users = transaction {
                UsersTable.selectAll().map { row ->
                    User(
                        id = row[UsersTable.id],
                        name = row[UsersTable.name],
                        email = row[UsersTable.email],
                        password = row[UsersTable.password],
                        role = UserRole.valueOf(row[UsersTable.role])
                    )
                }
            }
            call.respond(HttpStatusCode.OK, users)
        }

        delete("/users/{id}") {
            val requesterId = call.request.headers["X-User-Id"]
            if (requesterId == "adm_02") {
                return@delete call.respond(HttpStatusCode.Forbidden, "Operations Manager does not have permission to delete users.")
            }
            val idParam = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing user ID.")
            val success = transaction {
                UsersTable.deleteWhere { UsersTable.id eq idParam } > 0
            }
            if (success) {
                call.respond(HttpStatusCode.OK, "User deleted successfully.")
            } else {
                call.respond(HttpStatusCode.NotFound, "User not found.")
            }
        }

        put("/users/{id}") {
            val requesterId = call.request.headers["X-User-Id"]
            if (requesterId == "adm_02") {
                return@put call.respond(HttpStatusCode.Forbidden, "Operations Manager does not have permission to edit users.")
            }
            val idParam = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest, "Missing user ID.")
            val request = call.receive<RegisterRequest>()
            val success = transaction {
                UsersTable.update({ UsersTable.id eq idParam }) {
                    it[name] = request.name
                    it[email] = request.email
                    it[password] = request.password
                    it[role] = request.role.name
                } > 0
            }
            if (success) {
                call.respond(HttpStatusCode.OK, "User updated successfully.")
            } else {
                call.respond(HttpStatusCode.NotFound, "User not found.")
            }
        }
    }
}
