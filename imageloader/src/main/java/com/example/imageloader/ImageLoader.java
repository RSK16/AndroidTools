package com.example.imageloader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Created by apple on 16/8/17.
 */
public class ImageLoader {
    private static ImageLoader mInstance;
    /**
     * 图片缓存 的核心对象
     */
    private LruCache<String, Bitmap> mLruCache;
    /**
     * 线程池
     */
    private ExecutorService mThreadPool;
    private static final int DEADULT_THREAD_COUNT = 1;
    /**
     *  队列的调度方式
     */
    private Type mType = Type.LIFO;
    /**
     * 任务队列
     */
    private LinkedList<Runnable> mTaskQueue;
    /**
     * 后台轮询线程
     */
    private Thread mPoolThread;
    private Handler mPoolThreadHandler;
    /**
     * UI线程中的Handler
     */
    private Handler mUIHandler;
    private Semaphore mSemaphorePoolThreadHandler = new Semaphore(0);
    private Semaphore mSemaphorePoolThreadPool;
    public enum Type{
        FIFO,LIFO;
    }

    private ImageLoader(int threadCount, Type type) {
        init(threadCount,type);
    }

    /**
     * 初始化
     * @param threadCount
     * @param type
     */
    private void init(int threadCount, Type type)
    {
        //后台轮询线程
        mPoolThread = new Thread()
        {
            @Override
            public void run() {
                Looper.prepare();
                mPoolThreadHandler = new Handler()
                {
                    @Override
                    public void handleMessage(Message msg)
                    {
                        //线程池取取出一个任进行执行
                        mThreadPool.execute(getTask());
                        try {
                            mSemaphorePoolThreadPool.acquire();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                //释放一个信号量
                mSemaphorePoolThreadHandler.release();
                Looper.loop();
            }
        };
        mPoolThread.start();

        //获取我们应用的最大内存
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheMemory = maxMemory/8;
        mLruCache = new LruCache<String, Bitmap>(cacheMemory){
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes()*value.getHeight();
            }
        };

        //创建线程池
        mThreadPool = Executors.newFixedThreadPool(threadCount);
        mTaskQueue = new LinkedList<Runnable>();
        mType = type;
        mSemaphorePoolThreadPool = new Semaphore(threadCount);
    }

    /**
     * 从任务队列取出一个任务方法
     * @return
     */
    private Runnable getTask() {
        if (mType == Type.FIFO) {
            return mTaskQueue.removeFirst();
        } else if (mType == Type.LIFO) {
            return mTaskQueue.removeLast();
        }
        return null;
    }

    public static ImageLoader getInstance(int i, Type lifo)
    {
        if (mInstance == null) {
            synchronized (ImageLoader.class) {
                if (mInstance == null) {
                    mInstance = new ImageLoader(DEADULT_THREAD_COUNT,Type.LIFO);
                }
            }
        }
        return mInstance;
    }

    /**
     * 根据path
     * @param path
     * @param imageView
     */
    public void loadImage(final String path, final ImageView imageView)
    {
        imageView.setTag(path);

        if (mUIHandler == null) {
            mUIHandler = new Handler()
            {
                @Override
                public void handleMessage(Message msg) {
                    //获取得到图片，为imageview回调设置图片
                    ImageBeanHolder holder = (ImageBeanHolder) msg.obj;
                    Bitmap bm = holder.bitmap;
                    ImageView imageView = holder.imageView;
                    String path = holder.path;

                    //将path与getTag()存储路径进行比较
                    if (imageView.getTag().toString().equals(path)) {
                        imageView.setImageBitmap(bm);
                    }
                }
            };
        }
        //根据path在缓存中获取bitmap
        Bitmap bm = getBitmapFromLruCache(path);

        if (bm != null) {
            refreshBitmap(bm, path, imageView);
        } else {
            addTasks(new Runnable(){

                @Override
                public void run() {
                    //加载图片
                    //图片的压缩
                    //1.获得图片需要显示的大小
                    ImageSize imageViewSize = getImageViewSize(imageView);
                    //2.图片的压缩
                    Bitmap bm = decodeSampledBitmapFromPath(path, imageViewSize.width, imageViewSize.height);
                    //3.把图片加入到缓存
                    addBitmapToLruCache(path,bm);

                    refreshBitmap(bm, path, imageView);

                    mSemaphorePoolThreadPool.release();
                }
            });
        }
    }

    /**
     *
     * @param bm
     * @param path
     * @param imageView
     */
    private void refreshBitmap(Bitmap bm, String path, ImageView imageView) {
        Message message = Message.obtain();
        ImageBeanHolder holder = new ImageBeanHolder();
        holder.bitmap = bm;
        holder.path = path;
        holder.imageView = imageView;
        message.obj = holder;
        mUIHandler.sendMessage(message);
    }

    /**
     * 把图片加入到缓存
     * @param path
     * @param bm
     */
    private void addBitmapToLruCache(String path, Bitmap bm) {
        if (getBitmapFromLruCache(path) == null) {
            if (bm != null) {
                mLruCache.put(path, bm);
            }
        }
    }

    /**
     * 根据图片需要显示的宽高对图片进行适当的压缩
     * @param path
     * @param width
     * @param height
     * @return
     */
    private Bitmap decodeSampledBitmapFromPath(String path, int width, int height) {
        //获得图片的宽高，并不把图片加载到内存中
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        options.inSampleSize = caculateInSampleSize(options, width, height);

        //使用获取到的inSampleSize再次解析图片
        options.inJustDecodeBounds=false;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        return bitmap;
    }

    /**
     * 根据图片需要显示的宽高以及实际的宽高计算压缩比
     * @param options
     * @param reqwidth
     * @param reqheight
     * @return
     */
    private int caculateInSampleSize(BitmapFactory.Options options, int reqwidth, int reqheight) {
        int width = options.outWidth;
        int height = options.outHeight;
        int inSampleSize = 1;

        if (width > reqwidth || height > reqheight) {
            int widthRadio = Math.round(width * 1.0f / reqwidth);
            int heightRadio = Math.round(height * 1.0f / reqheight);

            inSampleSize = Math.max(widthRadio, heightRadio);
        }
        return inSampleSize;
    }

    /**
     * 根据获取适当的压缩的宽高
     * @param imageView
     * @return
     */
    private ImageSize getImageViewSize(ImageView imageView) {
        ImageSize imageSize = new ImageSize();
        DisplayMetrics displayMetrics = imageView.getContext().getResources().getDisplayMetrics();
        ViewGroup.LayoutParams lp = imageView.getLayoutParams();

        int width = imageView.getWidth();//获取imageview的实际高
        if (width <= 0) {
            width = lp.width;//获取imageview在layout声明的宽
        }
        if (width <= 0) {
            //width = imageView.getMaxWidth();//检查最大值
            width = getImageViewFieldValue(imageView, "mMaxWidth");
        }
        if (width <= 0) {
            width = displayMetrics.heightPixels;
        }

        int height = imageView.getWidth();//获取imagview的实际高
        if (height <= 0) {
            height = lp.height;//获取imageview在layout声明的高
        }
        if (height <= 0) {
            //height = imageView.getMaxWidth();//检查最大值
            height = getImageViewFieldValue(imageView, "mMaxHeight");
        }
        if (height <= 0) {
            height = displayMetrics.heightPixels;
        }
        imageSize.width = width;
        imageSize.height = height;
        return imageSize;
    }

    /**
     * 通过反射获取imageview的某个属性值
     * @param object
     * @param fieldName
     * @return
     */
    private static int getImageViewFieldValue(Object object,String fieldName){
        int value = 0;
        try {
            Field field = ImageView.class.getDeclaredField(fieldName);
            field.setAccessible(true);

            int fieldValue = field.getInt(object);
            if (fieldValue > 0 && fieldValue < Integer.MAX_VALUE) {
                value = fieldValue;
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return value;
    }

    private synchronized void addTasks(Runnable runnable) {
        mTaskQueue.add(runnable);
        try {
            if (mPoolThreadHandler == null) {
                mSemaphorePoolThreadHandler.acquire();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mPoolThreadHandler.sendEmptyMessage(0x110);
    }

    /**
     * 根据path在缓存中获取bitmap
     * @param key
     * @return
     */
    private Bitmap getBitmapFromLruCache(String key) {
        return mLruCache.get(key);
    }

    private class ImageSize
    {
        int width;
        int height;
    }
    private class ImageBeanHolder
    {
        Bitmap bitmap;
        ImageView imageView;
        String path;
    }
}
