package com.mototrip.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mototrip.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses WHERE id = :expenseId")
    suspend fun getExpenseById(expenseId: String): ExpenseEntity?

    @Query("SELECT * FROM expenses WHERE tripId = :tripId ORDER BY occurredAt ASC")
    suspend fun getExpensesByTrip(tripId: String): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE tripId = :tripId ORDER BY occurredAt ASC")
    fun getExpensesByTripFlow(tripId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE stopId = :stopId ORDER BY occurredAt ASC")
    suspend fun getExpensesByStop(stopId: String): List<ExpenseEntity>

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteExpenseById(expenseId: String)
}
