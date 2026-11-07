package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.IntervalUnit
import com.example.data.model.ScheduleConfig
import com.example.data.model.ScheduleMode
import com.example.util.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read app name string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("APK Sweeper", appName)
  }

  @Test
  fun `formatBytes formats correctly`() {
    assertEquals("0 B", FormatUtils.formatBytes(0))
    assertEquals("500 B", FormatUtils.formatBytes(500))
    assertEquals("1.0 KB", FormatUtils.formatBytes(1024))
    assertEquals("1.5 MB", FormatUtils.formatBytes((1.5 * 1024 * 1024).toLong()))
    assertEquals("2.00 GB", FormatUtils.formatBytes((2L * 1024 * 1024 * 1024)))
  }

  @Test
  fun `scheduleConfig converts intervals correctly`() {
    val daysConfig = ScheduleConfig(intervalValue = 3, intervalUnit = IntervalUnit.DAYS)
    assertEquals(3L, daysConfig.toIntervalDays())

    val weeksConfig = ScheduleConfig(intervalValue = 2, intervalUnit = IntervalUnit.WEEKS)
    assertEquals(14L, weeksConfig.toIntervalDays())

    val monthsConfig = ScheduleConfig(intervalValue = 1, intervalUnit = IntervalUnit.MONTHS)
    assertEquals(30L, monthsConfig.toIntervalDays())
  }

  @Test
  fun `day of week initial delay is positive`() {
    val delay = FormatUtils.calculateInitialDelayForDayOfWeek(
      targetDayOfWeek = Calendar.SUNDAY,
      targetHour = 3,
      targetMinute = 0
    )
    assertTrue("Initial delay must be greater than zero", delay > 0)
  }
}
