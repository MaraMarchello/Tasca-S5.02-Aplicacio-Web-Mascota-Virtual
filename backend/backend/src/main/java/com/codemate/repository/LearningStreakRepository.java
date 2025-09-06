package com.codemate.repository;

import com.codemate.model.LearningStreak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LearningStreakRepository extends JpaRepository<LearningStreak, Long> {
    
    Optional<LearningStreak> findByUserId(Long userId);
    
    @Query("SELECT ls FROM LearningStreak ls WHERE ls.currentStreak >= :minStreak ORDER BY ls.currentStreak DESC")
    List<LearningStreak> findUsersWithStreakAtLeast(@Param("minStreak") Integer minStreak);
    
    @Query("SELECT ls FROM LearningStreak ls ORDER BY ls.currentStreak DESC")
    List<LearningStreak> findAllOrderByCurrentStreakDesc();
    
    @Query("SELECT ls FROM LearningStreak ls ORDER BY ls.longestStreak DESC")
    List<LearningStreak> findAllOrderByLongestStreakDesc();
    
    @Query("SELECT AVG(ls.currentStreak) FROM LearningStreak ls WHERE ls.currentStreak > 0")
    Double getAverageCurrentStreak();
    
    @Query("SELECT MAX(ls.currentStreak) FROM LearningStreak ls")
    Integer getMaxCurrentStreak();
    
    @Query("SELECT MAX(ls.longestStreak) FROM LearningStreak ls")
    Integer getMaxLongestStreak();
    
    @Query("SELECT COUNT(ls) FROM LearningStreak ls WHERE ls.currentStreak >= 7")
    Long countUsersWithWeekStreak();
    
    @Query("SELECT COUNT(ls) FROM LearningStreak ls WHERE ls.currentStreak >= 30")
    Long countUsersWithMonthStreak();
    
    @Query("SELECT COUNT(ls) FROM LearningStreak ls WHERE ls.currentStreak >= 100")
    Long countUsersWithHundredDayStreak();
    
    @Query("SELECT COUNT(ls) FROM LearningStreak ls WHERE ls.lastActivityDate = :date")
    Long countActiveUsersOnDate(@Param("date") LocalDate date);
    
    @Query("SELECT SUM(ls.pointsEarnedToday) FROM LearningStreak ls WHERE ls.lastActivityDate = CURRENT_DATE")
    Long getTotalPointsEarnedToday();
    
    @Query("SELECT SUM(ls.scenariosCompletedToday) FROM LearningStreak ls WHERE ls.lastActivityDate = CURRENT_DATE")
    Long getTotalScenariosCompletedToday();
    
    @Query("SELECT SUM(ls.commandsExecutedToday) FROM LearningStreak ls WHERE ls.lastActivityDate = CURRENT_DATE")
    Long getTotalCommandsExecutedToday();
    
    // Leaderboard queries
    @Query("SELECT ls FROM LearningStreak ls WHERE ls.currentStreak > 0 ORDER BY ls.currentStreak DESC, ls.totalActiveDays DESC")
    List<LearningStreak> getStreakLeaderboard();
    
    @Query("SELECT ls FROM LearningStreak ls ORDER BY ls.pointsEarnedToday DESC, ls.scenariosCompletedToday DESC")
    List<LearningStreak> getDailyLeaderboard();
    
    @Query("SELECT ls FROM LearningStreak ls ORDER BY ls.totalActiveDays DESC, ls.longestStreak DESC")
    List<LearningStreak> getTotalActivityLeaderboard();
}
