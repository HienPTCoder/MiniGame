package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.GameRepository

class PuzzleApplication : Application() {
    val database by lazy {
        Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "zen_tile_puzzle.db"
        ).build()
    }
    
    val repository by lazy {
        GameRepository(database.gameDao())
    }
}
