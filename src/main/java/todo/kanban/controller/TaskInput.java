package todo.kanban.controller;

import todo.kanban.model.TaskPriority;
import todo.kanban.model.TaskStatus;

record TaskInput(
    String title, String description, TaskStatus status, TaskPriority priority, Long assignedTo) {}
