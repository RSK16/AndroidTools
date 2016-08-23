package com.example.news;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends Activity {

    private RefreshListView listview;
    private ArrayList<String> listDatas;
    private MyAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//去除标题
        setContentView(R.layout.activity_main);
        listview = (RefreshListView) findViewById(R.id.listview);

        listview.setRefreshListenter(new RefreshListView.OnRefreshListener() {
            @Override
            public void onREfresh() {
                new Thread(){
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        listDatas.add(0, "我是下拉刷新出来的数据");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapter.notifyDataSetChanged();
                                listview.onRefreshComplete();
                            }
                        });
                    }
                }.start();
            }

            @Override
            public void onLoadmore() {
                new Thread(){
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        listDatas.add("我是加载更多出来的数据1");
                        listDatas.add("我是加载更多出来的数据2");
                        listDatas.add("我是加载更多出来的数据3");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapter.notifyDataSetChanged();
                                listview.onRefreshComplete();
                            }
                        });
                    }
                }.start();
            }
        });

        listDatas = new ArrayList<String>();
        for (int i=0;i<30;i++){
            listDatas.add("这是一条ListView数据: " + i);
        }
        adapter = new MyAdapter();
        listview.setAdapter(adapter);
    }

    class MyAdapter extends BaseAdapter{

        @Override
        public int getCount() {
            return listDatas.size();
        }

        @Override
        public Object getItem(int position) {
            return listDatas.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView textView = new TextView(MainActivity.this);
            textView.setTextSize(18f);
            textView.setText(listDatas.get(position));

            return textView;
        }
    }
}
