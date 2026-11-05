package com.taskflow.task.sort

import com.taskflow.common.BadRequestException
import org.springframework.stereotype.Component

/** Picks the strategy for a `sort` parameter. Spring injects every TaskSortStrategy bean. */
@Component
class TaskSortResolver(strategies: List<TaskSortStrategy>) {

    private val byKey = strategies.associateBy { it.key }

    fun resolve(key: String): TaskSortStrategy =
        byKey[key] ?: throw BadRequestException("Unsupported sort '$key'. Allowed: ${byKey.keys.sorted()}")
}
