package com.example.imageloader;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.imageloader.bean.FolderBean;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final int DATA_LOADED = 0x110;
    private GridView mGridView;
    private List<String> mImgs;
    private ImageAdapter mImgAdapter;

    private ProgressDialog mProgressDialog;

    private RelativeLayout mBottomLy;
    private TextView mDirName;
    private TextView mDirCount;

    private File mCurrentDir;
    private int mMaxCount;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == DATA_LOADED) {
                mProgressDialog.dismiss();
                //绑定数据到View
                data2View();
            }

            super.handleMessage(msg);
        }
    };

    private void data2View() {
        if (mCurrentDir == null) {
            Toast.makeText(MainActivity.this, "没扫描到图片", Toast.LENGTH_SHORT).show();
            return;
        }
        mImgs = Arrays.asList(mCurrentDir.list());
        mImgAdapter = new ImageAdapter(this, mImgs, mCurrentDir.getAbsolutePath());
        mGridView.setAdapter(mImgAdapter);
        mDirCount.setText(mMaxCount + "");
        mDirCount.setText(mCurrentDir.getName());
    }

    private List<FolderBean> mFolderBeans = new ArrayList<FolderBean>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUI();
        initDatas();
        initEvent();
    }

    private void initEvent() {

    }

    /**
     * 利用ContentProvider扫描手机中的所有图片
     */
    private void initDatas() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(MainActivity.this, "当前SD卡不可用", Toast.LENGTH_SHORT).show();
            return;
        } else {
            mProgressDialog =ProgressDialog.show(this,null,"正在加载...");
            new Thread(){
                @Override
                public void run() {
                    Uri mImgUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    ContentResolver cr = MainActivity.this.getContentResolver();
                    Cursor cursor = cr.query(mImgUri, null,
                            MediaStore.Images.Media.MIME_TYPE + "=?", new String[]{"image/jpeg"},
                            MediaStore.Images.Media.DATE_MODIFIED);
                    Set<String> mDirPaths = new HashSet<String>();
                    while (cursor.moveToNext()) {
                        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                        File parentFile = new File(path).getParentFile();
                        if (parentFile == null) {
                            continue;
                        }
                        String dirPath = parentFile.getAbsolutePath();
                        FolderBean folderBean = null;
                        if (mDirPaths.contains(dirPath)) {
                            continue;
                        } else {
                            mDirPaths.add(dirPath);
                            folderBean = new FolderBean();
                            folderBean.setDir(dirPath);
                            folderBean.setFirstImgPath(path);
                        }

                        if (parentFile.list() == null) {
                            continue;
                        }
                        int picSize = parentFile.list(new FilenameFilter() {
                            @Override
                            public boolean accept(File dir, String filename) {
                                if (filename.endsWith(".jpg")
                                        || filename.endsWith(".jpeg")
                                        || filename.endsWith(".png")) {
                                    return true;
                                }
                                return false;
                            }
                        }).length;
                        folderBean.setCount(picSize);
                        mFolderBeans.add(folderBean);
                        if (picSize > mMaxCount) {
                            mMaxCount = picSize;
                            mCurrentDir = parentFile;
                        }
                    }
                    cursor.close();
                    //通知Handler扫描完成
                    mHandler.sendEmptyMessage(DATA_LOADED);

                }
            }.start();
        }

    }

    private void initUI() {
        mGridView = (GridView) findViewById(R.id.id_gridview);
        mBottomLy = (RelativeLayout) findViewById(R.id.id_bottom_ly);
        mDirName = (TextView) findViewById(R.id.id_dir_name);
        mDirCount = (TextView) findViewById(R.id.id_dir_count);
    }


}
class ImageAdapter extends BaseAdapter {
    private static Set<String> mSelectedimg = new HashSet<String>();
    private List<String> mImgPaths;
    private String mDirPath;
    private LayoutInflater mInflater;

    public ImageAdapter(Context context, List<String> mDatas, String dirPath) {
        this.mDirPath = dirPath ;
        this.mImgPaths = mDatas;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return mImgPaths.size();
    }

    @Override
    public Object getItem(int position) {
        return mImgPaths.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_gridview, parent, false);
            viewHolder.mImg = (ImageView) convertView.findViewById(R.id.id_item_image);
            viewHolder.mSelect = (ImageButton) convertView.findViewById(R.id.id_item_select);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        //重置状态
        viewHolder.mImg.setImageResource(R.drawable.pictures_no);
        viewHolder.mSelect.setImageResource(R.drawable.pic_unselected);
        viewHolder.mImg.setColorFilter(null);
        ImageLoader.getInstance(3, ImageLoader.Type.LIFO).loadImage(
                mDirPath+"/"+mImgPaths.get(position),viewHolder.mImg);
        final String filePath = mDirPath + "/" + mImgPaths.get(position);
        viewHolder.mImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //已经被选择
                if (mSelectedimg.contains(filePath)) {
                    mSelectedimg.remove(filePath);
                } else {
                    mSelectedimg.add(filePath);
                }
                notifyDataSetChanged();
            }
        });
        if (mSelectedimg.contains(filePath)) {
            viewHolder.mImg.setColorFilter(Color.parseColor("#77000000"));
            viewHolder.mSelect.setImageResource(R.drawable.pic_selected);
        }
        return convertView;
    }

    private class ViewHolder
    {
        ImageView mImg;
        ImageButton mSelect;
    }

}
