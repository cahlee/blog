package com.blog.domain.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findByIsPublicTrueOrderByCreatedAtDesc(Pageable pageable);

    Page<Post> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Post> findByUserIdAndIsPublicTrueOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Post> findByCategoryIdAndIsPublicTrueOrderByCreatedAtDesc(Long categoryId, Pageable pageable);

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.user LEFT JOIN FETCH p.category WHERE p.id = :id")
    Optional<Post> findByIdWithDetails(@Param("id") Long id);

    @Query(value = "SELECT p.* FROM post p WHERE p.is_public = true AND MATCH(p.title, p.content) AGAINST(:keyword IN BOOLEAN MODE)",
            countQuery = "SELECT COUNT(*) FROM post p WHERE p.is_public = true AND MATCH(p.title, p.content) AGAINST(:keyword IN BOOLEAN MODE)",
            nativeQuery = true)
    Page<Post> searchByFullText(@Param("keyword") String keyword, Pageable pageable);

    @Query(value = "SELECT DISTINCT p FROM Post p LEFT JOIN p.tags t WHERE p.isPublic = true AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY p.createdAt DESC",
           countQuery = "SELECT COUNT(DISTINCT p) FROM Post p LEFT JOIN p.tags t WHERE p.isPublic = true AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Post> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Post p JOIN p.tags t WHERE t.name = :tagName AND p.isPublic = true ORDER BY p.createdAt DESC")
    List<Post> findByTagName(@Param("tagName") String tagName);

    @Modifying
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
    void incrementViewCount(@Param("id") Long id);
}
