package com.taskflow.project

import com.taskflow.common.NotFoundException
import com.taskflow.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val userRepository: UserRepository,
) {

    @Transactional(readOnly = true)
    fun listForOwner(ownerId: Long): List<ProjectResponse> =
        projectRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId).map { it.toResponse() }

    @Transactional(readOnly = true)
    fun get(projectId: Long, ownerId: Long): ProjectResponse = requireOwned(projectId, ownerId).toResponse()

    @Transactional
    fun create(ownerId: Long, req: CreateProjectRequest): ProjectResponse {
        val owner = userRepository.getReferenceById(ownerId)
        val project = Project(name = req.name.trim(), description = req.description?.trim(), owner = owner)
        return projectRepository.save(project).toResponse()
    }

    /** Ownership check: another user's project looks exactly like a missing one (404, not 403). */
    fun requireOwned(projectId: Long, ownerId: Long): Project =
        projectRepository.findByIdAndOwnerId(projectId, ownerId)
            ?: throw NotFoundException("Project $projectId not found")
}
