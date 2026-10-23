package com.gingerxyt.ontub;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Handler;
import android.os.Looper;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.database.Cursor;
import android.provider.OpenableColumns;
import android.view.View;
import android.view.Gravity;
import android.widget.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.res.ColorStateList;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    // Set this to the public URL of the deployed ONTUB Node server.
    private static final String API_BASE = "https://YOUR-ONTUB-SERVER.example";
    private static final int PICK_VIDEO = 42;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private LinearLayout root, feed;
    private TextView status, pickedName;
    private EditText titleInput, descInput, creatorInput;
    private Uri pickedVideo;
    private String pickedMime = "video/mp4";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
        loadVideos();
    }

    private GradientDrawable bg(int color, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius); return d;
    }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return t;
    }
    private Button button(String label) {
        Button b = new Button(this); b.setText(label); b.setAllCaps(false);
        b.setTextColor(Color.WHITE); b.setBackgroundTintList(ColorStateList.valueOf(0xFFFF6A35)); return b;
    }
    private EditText input(String hint) {
        EditText e = new EditText(this); e.setSingleLine(true); e.setHint(hint); e.setTextColor(Color.WHITE);
        e.setHintTextColor(0xFF9AA7BB); e.setPadding(16, 8, 16, 8); e.setBackground(bg(0xFF151A24, 18));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.bottomMargin = 10; e.setLayoutParams(p); return e;
    }
    private void buildUi() {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(0xFF0B0E14);
        root.setPadding(18, 18, 18, 12);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        LinearLayout page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL);
        TextView logo = text("On", 32, Color.WHITE, true); TextView tub = text("tub", 32, 0xFFFF6A35, true);
        LinearLayout brand = new LinearLayout(this); brand.addView(logo); brand.addView(tub); page.addView(brand);
        TextView hero = text("Твой мир. Твои видео.", 25, Color.WHITE, true);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2); hp.topMargin=18; hp.bottomMargin=8; page.addView(hero,hp);
        page.addView(text("Ontub — видео, блоги и прямые эфиры.", 15, 0xFFB9C3D3, false));
        TextView uploadHead=text("＋ Добавить видео",20,Color.WHITE,true);
        LinearLayout.LayoutParams up=new LinearLayout.LayoutParams(-1,-2);up.topMargin=24;up.bottomMargin=12;page.addView(uploadHead,up);
        creatorInput=input("Имя онтубера (необязательно)"); page.addView(creatorInput);
        titleInput=input("Название видео"); page.addView(titleInput);
        descInput=input("Описание (необязательно)"); page.addView(descInput);
        Button pick=button("Выбрать видео на устройстве"); page.addView(pick);
        pickedName=text("Файл не выбран",13,0xFF9AA7BB,false);page.addView(pickedName);
        Button send=button("＋"); LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.topMargin=8;page.addView(send,sp);
        status=text("Задай адрес своего работающего сервера в MainActivity.java.",12,0xFFFFB18F,false);
        LinearLayout.LayoutParams st=new LinearLayout.LayoutParams(-1,-2);st.topMargin=10;page.addView(status,st);
        TextView fh=text("Видео на сервере",20,Color.WHITE,true);LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-1,-2);fp.topMargin=26;fp.bottomMargin=8;page.addView(fh,fp);
        feed=new LinearLayout(this);feed.setOrientation(LinearLayout.VERTICAL);page.addView(feed);
        TextView note=text("Ранняя версия ONTUB. Регистрация и вход пока не подключены к серверу.",12,0xFF7E8BA1,false);
        LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2);np.topMargin=24;page.addView(note,np);
        scroll.addView(page);root.addView(scroll);setContentView(root);
        pick.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("video/*");startActivityForResult(i,PICK_VIDEO);});
        send.setOnClickListener(v->uploadVideo());
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==PICK_VIDEO&&resultCode==RESULT_OK&&data!=null&&data.getData()!=null){
            pickedVideo=data.getData();getContentResolver().takePersistableUriPermission(pickedVideo,data.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickedMime=getContentResolver().getType(pickedVideo);if(pickedMime==null)pickedMime="video/mp4";
            pickedName.setText(queryName(pickedVideo)+" · "+pickedMime);
        }
    }
    private String queryName(Uri uri) {
        try(Cursor c=getContentResolver().query(uri,null,null,null,null)){if(c!=null&&c.moveToFirst()){int i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(i>=0)return c.getString(i);}}catch(Exception ignored){}return "video";
    }

    private boolean containsBanTrigger(String value) {
        if (value == null) return false;
        return java.util.regex.Pattern.compile("(^|[^a-zа-яё0-9_])(?:[!/:#])?(?:ban|бан)(?=$|[^a-zа-яё0-9_])", java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.UNICODE_CASE).matcher(value).find();
    }
    private void runBanPrank() {
        final TextView count = text("5", 72, 0xFFFF6A35, true);
        count.setGravity(Gravity.CENTER);
        final TextView message = text("ВАШ КАНАЛ БУДЕТ ЗАБАНЕН ЧЕРЕЗ…", 20, Color.WHITE, true);
        message.setGravity(Gravity.CENTER);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(24, 20, 24, 12);
        box.addView(message);
        box.addView(count);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("⚠️ Системное уведомление").setView(box).setCancelable(false).create();
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "", (d, w) -> d.dismiss());
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.GONE);
        Handler handler = new Handler(Looper.getMainLooper());
        final int[] n = {5};
        Runnable tick = new Runnable() {
            @Override public void run() {
                n[0]--;
                if (n[0] > 0 && dialog.isShowing()) {
                    count.setText(String.valueOf(n[0]));
                    handler.postDelayed(this, 1000);
                } else if (dialog.isShowing()) {
                    count.setText("1");
                    message.setText("ХА, ТЫ ЧТО, ПОВЁЛСЯ? 😂");
                    dialog.setCancelable(true);
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("Фух, канал цел");
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.VISIBLE);
                }
            }
        };
        handler.postDelayed(tick, 1000);
    }

    private void uploadVideo() {
        String prankText = titleInput.getText().toString() + " " + descInput.getText().toString();
        if (containsBanTrigger(prankText)) { runBanPrank(); status.setText("Это был пранк — канал не заблокирован."); return; }
        if(pickedVideo==null){status.setText("Сначала выбери видеофайл.");return;}
        if(titleInput.getText().toString().trim().isEmpty()){status.setText("Введи название видео.");return;}
        if(API_BASE.contains("YOUR-ONTUB-SERVER")){status.setText("Нужно заменить API_BASE на URL работающего сервера Ontub.");return;}
        status.setText("Загрузка началась…");
        final String title=titleInput.getText().toString().trim(),desc=descInput.getText().toString().trim(),creator=creatorInput.getText().toString().trim();
        final Uri uri=pickedVideo;final String mime=pickedMime;
        worker.execute(()->{String result;HttpURLConnection conn=null;
            try {
                String boundary="----ONTUB"+System.currentTimeMillis();URL url=new URL(API_BASE+"/api/videos");conn=(HttpURLConnection)url.openConnection();
                conn.setRequestMethod("POST");conn.setDoOutput(true);conn.setConnectTimeout(15000);conn.setReadTimeout(60000);conn.setRequestProperty("Content-Type","multipart/form-data; boundary="+boundary);
                try(OutputStream out=conn.getOutputStream()){
                    writeField(out,boundary,"title",title);writeField(out,boundary,"description",desc);writeField(out,boundary,"creator",creator.isEmpty()?"Онтубер":creator);
                    String filename=queryName(uri).replace("\"","");out.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"video\"; filename=\""+filename+"\"\r\nContent-Type: "+mime+"\r\n\r\n").getBytes("UTF-8"));
                    try(InputStream in=getContentResolver().openInputStream(uri)){byte[] buf=new byte[64*1024];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);}
                    out.write(("\r\n--"+boundary+"--\r\n").getBytes("UTF-8"));
                }
                int code=conn.getResponseCode();InputStream in=code<400?conn.getInputStream():conn.getErrorStream();String body=readAll(in);
                if(code>=200&&code<300)result="Видео загружено! Обнови список ниже.";else result="Ошибка сервера ("+code+"): "+body;
            }catch(Exception ex){result="Не удалось загрузить: "+ex.getMessage();}finally{if(conn!=null)conn.disconnect();}
            final String msg=result;runOnUiThread(()->{status.setText(msg);if(msg.startsWith("Видео загружено"))loadVideos();});
        });
    }
    private void writeField(OutputStream out,String b,String name,String value)throws IOException{
        out.write(("--"+b+"\r\nContent-Disposition: form-data; name=\""+name+"\"\r\n\r\n"+value+"\r\n").getBytes("UTF-8"));
    }
    private String readAll(InputStream in)throws IOException{if(in==null)return "";try(BufferedReader r=new BufferedReader(new InputStreamReader(in,"UTF-8"))){StringBuilder s=new StringBuilder();String line;while((line=r.readLine())!=null)s.append(line);return s.toString();}}
    private void loadVideos(){
        if(API_BASE.contains("YOUR-ONTUB-SERVER")){feed.removeAllViews();feed.addView(text("После настройки сервера здесь появятся загруженные видео.",14,0xFF9AA7BB,false));return;}
        worker.execute(()->{String result;HttpURLConnection c=null;try{c=(HttpURLConnection)new URL(API_BASE+"/api/videos").openConnection();c.setConnectTimeout(10000);c.setReadTimeout(15000);result=c.getResponseCode()==200?readAll(c.getInputStream()):"[]";}catch(Exception e){result="[]";}finally{if(c!=null)c.disconnect();}final String json=result;runOnUiThread(()->renderVideoList(json));});
    }
    private void renderVideoList(String json){
        feed.removeAllViews();
        // Lightweight parser for the known API response, avoiding extra dependencies.
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("\\{[^{}]*\\}").matcher(json);int count=0;
        while(m.find()){String obj=m.group();String title=field(obj,"title"),creator=field(obj,"creator"),desc=field(obj,"desc"),url=field(obj,"url");
            TextView card=text("▶  "+title+"\n"+creator+"\n"+desc+"\n"+API_BASE+url,15,Color.WHITE,true);card.setPadding(14,14,14,14);card.setBackground(bg(0xFF151A24,16));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=10;feed.addView(card,p);count++;}
        if(count==0)feed.addView(text("Пока нет загруженных видео или сервер недоступен.",14,0xFF9AA7BB,false));
    }
    private String field(String obj,String key){java.util.regex.Matcher m=java.util.regex.Pattern.compile("\""+key+"\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(obj);if(!m.find())return "";return m.group(1).replace("\\n"," ").replace("\\\"","\"").replace("\\\\","\\");}
    @Override protected void onDestroy(){super.onDestroy();worker.shutdownNow();}
}