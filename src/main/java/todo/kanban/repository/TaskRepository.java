package todo.kanban.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import todo.kanban.model.Task;
import todo.kanban.model.TaskStatus;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

  Page<Task> findByStatus(TaskStatus status, Pageable pageable);

  @Query(
      "SELECT t FROM Task t WHERE (:status IS NULL OR t.status = :status) ORDER BY t.createdAt"
          + " DESC")
  Page<Task> findByStatusOrderByCreatedAtDesc(
      @Param("status") TaskStatus status, Pageable pageable);

  @Query("SELECT t FROM Task t WHERE (:status IS NULL OR t.status = :status) ORDER BY t.title")
  Page<Task> findByStatusOrderByTitleAsc(@Param("status") TaskStatus status, Pageable pageable);

  @Query(
      "SELECT t FROM Task t WHERE (:status IS NULL OR t.status = :status) ORDER BY t.priority DESC")
  Page<Task> findByStatusOrderByPriorityDesc(@Param("status") TaskStatus status, Pageable pageable);

  Page<Task> findByAssignedToId(Long userId, Pageable pageable);

  Page<Task> findByCreatedById(Long userId, Pageable pageable);
}
