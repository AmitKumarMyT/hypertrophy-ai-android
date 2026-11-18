package com.praveen.hypertrophy.glance

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.praveen.hypertrophy.MainActivity
import com.praveen.hypertrophy.Store

class HypertrophyGlanceReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HypertrophyGlanceWidget()
}

class HypertrophyGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val store = Store(context)
        val state = store.load()
        val session = state.session

        val setsDone = session?.plan?.sumOf { it.done } ?: 0
        val targetSets = session?.plan?.sumOf { it.sets } ?: 0
        val streakDays = (state.log.size / 3).coerceAtLeast(1)

        val totalVolume = state.log.takeLast(10).sumOf { (it.kg * it.reps).toInt() }

        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(20.dp)
                        .background(Color(0xFF0E0E10))
                        .padding(14.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = GlanceModifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Header: Title & Streak Badge
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "HYPERTROPHY",
                                style = TextStyle(
                                    color = androidx.glance.unit.ColorProvider(Color(0xFFE2F952)),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.defaultWeight())
                            Box(
                                modifier = GlanceModifier
                                    .cornerRadius(8.dp)
                                    .background(Color(0xFF1E2024))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "🔥 ${streakDays}D STREAK",
                                    style = TextStyle(
                                        color = androidx.glance.unit.ColorProvider(Color.White),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }

                        Spacer(modifier = GlanceModifier.height(8.dp))

                        // Metric Row: Workout Sets & Total Volume
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(
                                    text = "TODAY'S SETS",
                                    style = TextStyle(
                                        color = androidx.glance.unit.ColorProvider(Color(0xFF9E9E9E)),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Text(
                                    text = "$setsDone / ${targetSets.coerceAtLeast(setsDone)}",
                                    style = TextStyle(
                                        color = androidx.glance.unit.ColorProvider(Color.White),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(
                                    text = "RECENT VOLUME",
                                    style = TextStyle(
                                        color = androidx.glance.unit.ColorProvider(Color(0xFF9E9E9E)),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Text(
                                    text = "${totalVolume} kg",
                                    style = TextStyle(
                                        color = androidx.glance.unit.ColorProvider(Color(0xFF38BDF8)),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        Spacer(modifier = GlanceModifier.height(8.dp))

                        // Quick Action Footer Button
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .cornerRadius(10.dp)
                                .background(Color(0xFFE2F952))
                                .padding(vertical = 6.dp, horizontal = 12.dp)
                                .clickable(actionStartActivity<MainActivity>()),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚡ OPEN WORKOUT & AI COACH",
                                style = TextStyle(
                                    color = androidx.glance.unit.ColorProvider(Color(0xFF0E0E10)),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
