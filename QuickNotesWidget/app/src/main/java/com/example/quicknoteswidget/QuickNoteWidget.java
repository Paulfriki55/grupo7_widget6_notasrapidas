package com.example.quicknoteswidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;
import android.app.PendingIntent;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class QuickNoteWidget extends AppWidgetProvider {
    private static final String TAG = "QuickNoteWidget";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    public static void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = buildWidgetView(context, appWidgetId);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static RemoteViews buildWidgetView(Context context, int widgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_quick_note);

        // Configurar contenido
        String noteContent = getNoteContent(context, widgetId);
        views.setTextViewText(R.id.txt_note, noteContent);

        // Obtener y formatear la fecha y hora
        SharedPreferences prefs = context.getSharedPreferences("NOTES", Context.MODE_PRIVATE);
        long timestamp = prefs.getLong("timestamp_" + widgetId, System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault());
        String dateTime = sdf.format(new Date(timestamp));
        views.setTextViewText(R.id.txt_date_time, dateTime);

        // Configurar aspecto visual
        views.setTextColor(R.id.txt_note, ContextCompat.getColor(context, R.color.text_primary));
        views.setTextColor(R.id.txt_date_time, ContextCompat.getColor(context, R.color.text_secondary));
        views.setInt(R.id.fab_edit, "setColorFilter",
                ContextCompat.getColor(context, R.color.surface_white));

        // Configurar intent de edición
        setupEditIntent(context, views, widgetId);

        return views;
    }

    private static String getNoteContent(Context context, int widgetId) {
        SharedPreferences prefs = context.getSharedPreferences("NOTES", Context.MODE_PRIVATE);
        return prefs.getString("note_" + widgetId, context.getString(R.string.default_note_text));
    }

    private static void setupEditIntent(Context context, RemoteViews views, int widgetId) {
        Intent intent = new Intent(context, EditNoteActivity.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                widgetId,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        views.setOnClickPendingIntent(R.id.fab_edit, pendingIntent);
    }
}

