package com.taskflow.task

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface StatusCount {
    val status: TaskStatus
    val count: Long
}

interface TaskRepository : JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    fun findByIdAndProjectOwnerId(id: Long, ownerId: Long): Task?

    @Query("select t.status as status, count(t) as count from Task t where t.project.id = :projectId group by t.status")
    fun countByStatus(@Param("projectId") projectId: Long): List<StatusCount>

    fun countByProjectIdAndDueDateBeforeAndStatusNot(projectId: Long, date: LocalDate, status: TaskStatus): Long
}
