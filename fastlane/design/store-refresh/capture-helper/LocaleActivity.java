package dev.unshort.capturelocale;
import android.app.Activity;
import android.os.Bundle;
import android.os.LocaleList;
import android.content.res.Configuration;
import java.util.Locale;
public class LocaleActivity extends Activity {
 public void onCreate(Bundle state) {
  super.onCreate(state);
  try {
   Object am=Class.forName("android.app.ActivityManager").getDeclaredMethod("getService").invoke(null);
   Class<?> api=Class.forName("android.app.IActivityManager");
   Configuration config=(Configuration)api.getMethod("getConfiguration").invoke(am);
   config.setLocales(new LocaleList(Locale.forLanguageTag(getIntent().getStringExtra("locale"))));
   Configuration.class.getField("userSetLocale").setBoolean(config,true);
   api.getMethod("updateConfiguration",Configuration.class).invoke(am,config);
   android.util.Log.i("StoreLocale","Locale changed to "+config.getLocales().toLanguageTags());
  } catch(Exception e) { android.util.Log.e("StoreLocale","Failed",e); }
  finish();
 }
}
