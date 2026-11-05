package com.taskflow.task

enum class TaskStatus { TODO, IN_PROGRESS, DONE }

enum class TaskPriority(val rank: Int) { LOW(1), MEDIUM(2), HIGH(3) }
