//package io.oddlot.ledger
//
//import android.util.Log
//import androidx.room.Room
//import androidx.test.InstrumentationRegistry
//import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
//import io.oddlot.ledger.classes.App
//import io.oddlot.ledger.data.AppDatabase
//import io.oddlot.ledger.data.Tab
//
//import org.junit.Test
//import org.junit.runner.RunWith
//
//import org.junit.Assert.*
//
//
//@RunWith(AndroidJUnit4ClassRunner::class)
//class TabTests {
//
//    val testTab = Tab(0, "Cecilia")
//
//    @Test
//    fun InsertTabTest() {
//        // Context of the app under test.
//        val appContext = InstrumentationRegistry.getTargetContext()
//
//        val db = Room.databaseBuilder(appContext, AppDatabase::class.java, "AppDatabase")
//            .build()
//
//        db.tabDao().insert(testTab)
//        Log.d("TEST", db.itemDao().getItems().toString())
//
//        val tab = db.tabDao().get(0)
//
//        assertEquals(testTab, tab)
//    }
//}
