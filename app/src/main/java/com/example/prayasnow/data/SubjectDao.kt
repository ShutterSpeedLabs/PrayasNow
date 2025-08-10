package com.example.prayasnow.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    
    @Query("SELECT * FROM subjects WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveSubjects(): Flow<List<Subject>>
    
    @Query("SELECT * FROM subjects ORDER BY name ASC")
    fun getAllSubjects(): Flow<List<Subject>>
    
    @Query("SELECT * FROM subjects WHERE id = :subjectId")
    suspend fun getSubjectById(subjectId: String): Subject?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<Subject>)
    
    @Update
    suspend fun updateSubject(subject: Subject)
    
    @Delete
    suspend fun deleteSubject(subject: Subject)
    
    @Query("UPDATE subjects SET isActive = :isActive WHERE id = :subjectId")
    suspend fun updateSubjectActiveStatus(subjectId: String, isActive: Boolean)
}
