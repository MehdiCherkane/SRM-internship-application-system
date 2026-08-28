package com.onep.internship.repository;

import com.onep.internship.model.Application;
import com.onep.internship.model.ApplicationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByStatus(ApplicationStatus status);

    @Query("SELECT a.status, COUNT(a) FROM Application a GROUP BY a.status")
    List<Object[]> countByStatus();

    @Query("SELECT a.university, COUNT(a) FROM Application a WHERE a.status <> :excludedStatus GROUP BY a.university ORDER BY COUNT(a) DESC")
    List<Object[]> findTopUniversities(@Param("excludedStatus") ApplicationStatus excludedStatus, Pageable pageable);

    @Query("SELECT a.major, COUNT(a) FROM Application a WHERE a.status <> :excludedStatus GROUP BY a.major ORDER BY COUNT(a) DESC")
    List<Object[]> findTopMajors(@Param("excludedStatus") ApplicationStatus excludedStatus, Pageable pageable);

    @Query("SELECT FUNCTION('YEAR', a.submittedDate), FUNCTION('MONTH', a.submittedDate), COUNT(a) FROM Application a WHERE a.status <> :excludedStatus GROUP BY FUNCTION('YEAR', a.submittedDate), FUNCTION('MONTH', a.submittedDate) ORDER BY FUNCTION('YEAR', a.submittedDate) ASC, FUNCTION('MONTH', a.submittedDate) ASC")
    List<Object[]> countByMonth(@Param("excludedStatus") ApplicationStatus excludedStatus);

    @Query("SELECT a FROM Application a WHERE LOWER(a.firstName) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '\\' OR LOWER(a.lastName) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '\\' OR LOWER(a.email) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '\\' OR LOWER(a.major) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '\\' OR LOWER(a.university) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '\\'")
    List<Application> search(@Param("q") String q);

    List<Application> findByApplicantIdOrderBySubmittedDateDesc(Long applicantId);

    @Query("SELECT COUNT(a) > 0 FROM Application a WHERE a.applicant.id = :userId " +
           "AND a.status IN :pending " +
           "AND (a.cycleYear = FUNCTION('YEAR', CURRENT_DATE) " +
           "     OR (a.cycleYear IS NULL AND a.submittedDate IS NULL) " +
           "     OR (a.cycleYear IS NULL AND FUNCTION('YEAR', a.submittedDate) = FUNCTION('YEAR', CURRENT_DATE)))")
    boolean existsByApplicantInCurrentCycle(@Param("userId") Long userId, @Param("pending") List<ApplicationStatus> pending);

    @Query("SELECT COUNT(a) > 0 FROM Application a WHERE a.applicant.id = :userId " +
           "AND a.id <> :excludeId " +
           "AND a.status IN :pending " +
           "AND (a.cycleYear = FUNCTION('YEAR', CURRENT_DATE) " +
           "     OR (a.cycleYear IS NULL AND a.submittedDate IS NULL) " +
           "     OR (a.cycleYear IS NULL AND FUNCTION('YEAR', a.submittedDate) = FUNCTION('YEAR', CURRENT_DATE)))")
    boolean existsByApplicantInCurrentCycleExcludingId(@Param("userId") Long userId, @Param("pending") List<ApplicationStatus> pending, @Param("excludeId") Long excludeId);

    @Query("SELECT COUNT(a) > 0 FROM Application a WHERE a.cni = :cni AND a.email = :email " +
           "AND a.status IN :pending " +
           "AND (a.cycleYear = FUNCTION('YEAR', CURRENT_DATE) " +
           "     OR (a.cycleYear IS NULL AND a.submittedDate IS NULL) " +
           "     OR (a.cycleYear IS NULL AND FUNCTION('YEAR', a.submittedDate) = FUNCTION('YEAR', CURRENT_DATE)))")
    boolean existsByCniAndEmailInCurrentCycle(@Param("cni") String cni, @Param("email") String email, @Param("pending") List<ApplicationStatus> pending);

    @Query("SELECT COUNT(a) > 0 FROM Application a WHERE a.cni = :cni AND a.email = :email " +
           "AND a.id <> :excludeId " +
           "AND a.status IN :pending " +
           "AND (a.cycleYear = FUNCTION('YEAR', CURRENT_DATE) " +
           "     OR (a.cycleYear IS NULL AND a.submittedDate IS NULL) " +
           "     OR (a.cycleYear IS NULL AND FUNCTION('YEAR', a.submittedDate) = FUNCTION('YEAR', CURRENT_DATE)))")
    boolean existsByCniAndEmailInCurrentCycleExcludingId(@Param("cni") String cni, @Param("email") String email, @Param("pending") List<ApplicationStatus> pending, @Param("excludeId") Long excludeId);
}
