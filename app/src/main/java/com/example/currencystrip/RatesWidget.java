package com.example.currencystrip;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.app.PendingIntent;
import android.widget.RemoteViews;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
import java.util.Locale;

public class RatesWidget extends AppWidgetProvider {
  @Override public void onUpdate(Context ctx, AppWidgetManager manager, int[] ids) {
    for (int id : ids) {
      RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget);
      String usd = ctx.getSharedPreferences("rates",0).getString("USD","—");
      String eur = ctx.getSharedPreferences("rates",0).getString("EUR","—");
      rv.setTextViewText(R.id.usd,"$ " + usd + " ₽");
      rv.setTextViewText(R.id.eur,"€ " + eur + " ₽");
      Intent refresh = new Intent(ctx, RatesWidget.class).setAction("com.example.currencystrip.REFRESH");
      rv.setOnClickPendingIntent(R.id.usd, PendingIntent.getBroadcast(ctx,0,refresh,PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
      rv.setOnClickPendingIntent(R.id.eur, PendingIntent.getBroadcast(ctx,0,refresh,PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
      manager.updateAppWidget(id,rv);
    }
    new Thread(() -> {
      try {
        HttpURLConnection c = (HttpURLConnection)new URL("https://www.cbr.ru/scripts/XML_daily.asp").openConnection();
        c.setConnectTimeout(12000); c.setReadTimeout(12000);
        try (InputStream in = c.getInputStream()) {
          Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
          NodeList nodes = doc.getElementsByTagName("Valute");
          android.content.SharedPreferences.Editor ed = ctx.getSharedPreferences("rates",0).edit();
          for(int i=0;i<nodes.getLength();i++) {
            Element e=(Element)nodes.item(i);
            String code=e.getElementsByTagName("CharCode").item(0).getTextContent();
            if(code.equals("USD")||code.equals("EUR")) {
              double value=Double.parseDouble(e.getElementsByTagName("Value").item(0).getTextContent().replace(',','.'));
              double nominal=Double.parseDouble(e.getElementsByTagName("Nominal").item(0).getTextContent());
              ed.putString(code,String.format(Locale.forLanguageTag("ru-RU"),"%.2f",value/nominal));
            }
          }
          ed.apply();
        } finally { c.disconnect(); }
        AppWidgetManager m=AppWidgetManager.getInstance(ctx);
        int[] all=m.getAppWidgetIds(new android.content.ComponentName(ctx,RatesWidget.class));
        for(int id:all) {
          RemoteViews rv=new RemoteViews(ctx.getPackageName(),R.layout.widget);
          android.content.SharedPreferences p=ctx.getSharedPreferences("rates",0);
          rv.setTextViewText(R.id.usd,"$ " + p.getString("USD","—") + " ₽");
          rv.setTextViewText(R.id.eur,"€ " + p.getString("EUR","—") + " ₽");
          m.partiallyUpdateAppWidget(id,rv);
        }
      } catch(Exception ignored) { }
    }).start();
  }
  @Override public void onReceive(Context ctx, Intent intent) {
    super.onReceive(ctx,intent);
    if("com.example.currencystrip.REFRESH".equals(intent.getAction())) {
      AppWidgetManager m=AppWidgetManager.getInstance(ctx);
      onUpdate(ctx,m,m.getAppWidgetIds(new android.content.ComponentName(ctx,RatesWidget.class)));
    }
  }
}
