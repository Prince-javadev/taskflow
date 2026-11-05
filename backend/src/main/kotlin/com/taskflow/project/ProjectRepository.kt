package com.taskflow.project

import org.springframework.data.jpa.repository.JpaRepository

interface ProjectRepository : JpaRepository<Project, Long> {
    fun findAllByOwnerIdOrderByCreatedAtDesc(ownerId: Long): List<Project>
    fun findByIdAndOwnerId(id: Long, ownerId: Long): Project?
}
