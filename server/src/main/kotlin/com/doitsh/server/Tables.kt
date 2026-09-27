package com.doitsh.server

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.java.time.datetime

object UsersTable : Table("users") {
    val id = long("id").autoIncrement()
    val username = varchar("username", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)
}

object ProjectsTable : Table("projects") {
    val id = long("id").autoIncrement()
    val userId = long("user_id").references(UsersTable.id)
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val color = integer("color").nullable()
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object TasksTable : Table("tasks") {
    val id = long("id").autoIncrement()
    val userId = long("user_id").references(UsersTable.id)
    val projectId = long("project_id").nullable().references(ProjectsTable.id)
    val title = varchar("title", 500)
    val description = text("description").nullable()
    val isCompleted = bool("is_completed").default(false)
    val priority = integer("priority").default(0)
    val dueDate = datetime("due_date").nullable()
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")

    override val primaryKey = PrimaryKey(id)
}
