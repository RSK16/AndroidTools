package com.example.news;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;


/**
 * Created by apple on 16/8/7.
 */
public class RefreshListView extends ListView implements AbsListView.OnScrollListener{
    private float downY;
    private float moveY;
    private int measuredheight;
    private View mHeaderView;
    public static final int PULL_TO_REFRESH =0;//下拉刷新
    public static final int RELEASE_REFRESH =1;//释放刷新
    public static final int REFRESHING =2;//刷新中
    private int currentState = PULL_TO_REFRESH ;//当前刷新模式
    private View mHeaderArrow;
    private TextView mHeaderTittle;
    private ProgressBar pb;
    private int paddingTop;
    private OnRefreshListener mListener;
    private TextView mLastRefreshDesc;
    private RotateAnimation rotateUpAnimation;
    private RotateAnimation rotateDownAnimation;
    private View mFooterView;
    private int mFooterViewHeight;
    private boolean isLoadingMore;

    public RefreshListView(Context context) {
        super(context);
        init();
    }

    public RefreshListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RefreshListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * 初始化头布局
     */
    private void init() {
        initHeaderView();
        initAnimation();
        initFooterView();
        setOnScrollListener(this);
    }

    private void initFooterView() {
        mFooterView = View.inflate(getContext(), R.layout.layout_footer_list, null);
        mFooterView.measure(0,0);
        mFooterViewHeight = mFooterView.getMeasuredHeight();
        //隐藏脚布局
        mFooterView.setPadding(0, -mFooterViewHeight, 0, 0);
        addFooterView(mFooterView);
    }

    /**
     * 初始化动画
     */
    private void initAnimation() {
//        向上转，逆时针0  --  -180
        rotateUpAnimation = new RotateAnimation(0f,-180f,
                Animation.RELATIVE_TO_SELF,0.5f,
                Animation.RELATIVE_TO_SELF,0.5f);
        rotateUpAnimation.setDuration(300);
        rotateUpAnimation.setFillAfter(true);
//        向下转，逆时针-180   --  -360
        rotateDownAnimation = new RotateAnimation(-180f,-360f,
                Animation.RELATIVE_TO_SELF,0.5f,
                Animation.RELATIVE_TO_SELF,0.5f);
        rotateDownAnimation.setDuration(300);
        rotateDownAnimation.setFillAfter(true);
    }

    /**
     * 初始化头布局
     */
    private void initHeaderView() {
        mHeaderView = View.inflate(getContext(), R.layout.layout_header_list, null);
        mHeaderArrow = mHeaderView.findViewById(R.id.iv_arrow);
        pb = (ProgressBar) mHeaderView.findViewById(R.id.pb);
        mHeaderTittle = (TextView) mHeaderView.findViewById(R.id.tv_tittle);
        mLastRefreshDesc = (TextView) mHeaderView.findViewById(R.id.tv_last_refresh);
        //提前手动测量宽高
        mHeaderView.measure(0,0);//按照设置的规则测量
        measuredheight = mHeaderView.getMeasuredHeight();
        //设置内边距，可以隐藏当前空间
        mHeaderView.setPadding(0,-measuredheight,0,0);
        //在设置数据适配器之前添加头布局／脚布局的方法
        addHeaderView(mHeaderView);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // 判断滑动的距离，给Header设置paddingTop
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downY = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                moveY = ev.getY();
                if (currentState == REFRESHING) {
                    return super.onTouchEvent(ev);
                }
                float offset = moveY-downY;
                //只有偏移量>0并且当前第一个条目是0 才放大头部
                if (offset > 0 && getFirstVisiblePosition() == 0) {
//                  int paddingtop = -自身高度＋偏移量
                    paddingTop = (int) (-measuredheight+offset);
                mHeaderView.setPadding(0, paddingTop,0,0);

                if (paddingTop >= 0&&currentState!=RELEASE_REFRESH) {//完全显示
                    //变成释放刷新
                    currentState=RELEASE_REFRESH;
                    updateHeader();
                } else if (paddingTop < 0&&currentState!=PULL_TO_REFRESH){// 不完全显示
                    //切换下拉刷新
                    currentState = PULL_TO_REFRESH;
                    updateHeader();
                }
                return true;
            }
                downY = moveY;
                break;
            case MotionEvent.ACTION_UP:
                if (currentState ==PULL_TO_REFRESH) {
                    mHeaderView.setPadding(0, -measuredheight, 0, 0);
                } else if (currentState==RELEASE_REFRESH){
                    mHeaderView.setPadding(0, 0, 0, 0);
                    currentState=REFRESHING;
                    updateHeader();
                }
                break;
            default:
                break;
        }
        return super.onTouchEvent(ev);
    }

    /**
     * 根据状态更新头布局内容
     */
    private void updateHeader() {
        switch (currentState) {
            case PULL_TO_REFRESH:
                mHeaderArrow.startAnimation(rotateDownAnimation);
                mHeaderTittle.setText("下拉刷新");
                break;
            case RELEASE_REFRESH:
                mHeaderArrow.startAnimation(rotateUpAnimation);
                mHeaderTittle.setText("释放刷新");
                break;
            case REFRESHING:
                mHeaderArrow.clearAnimation();
                mHeaderArrow.setVisibility(View.INVISIBLE);
                pb.setVisibility(View.VISIBLE);
                mHeaderTittle.setText("正在刷新中...");
                if (mListener != null) {
                    mListener.onREfresh();//通知调用者，让其到网络加载更多数据
                }
                break;
        }
    }

    /**
     * 刷新结束，恢复界面
     */
    public void onRefreshComplete() {
        if(isLoadingMore){
            mFooterView.setPadding(0,-mFooterViewHeight,0,0);
            isLoadingMore=false;
        }else{
            currentState=PULL_TO_REFRESH;
            mHeaderTittle.setText("下拉刷新");
            mHeaderView.setPadding(0, -measuredheight, 0, 0);
            pb.setVisibility(View.INVISIBLE);
            mHeaderView.setVisibility(View.VISIBLE);
            String time =getTime();
            mLastRefreshDesc.setText("最后刷新时间："+time);
        }
    }

    private String getTime() {
        long currentTimeMillis = System.currentTimeMillis();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(currentTimeMillis);
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        //状态改变 SCROLL_STATE_IDLE空闲 TOUCH_SCROLL触摸滑动 FING滑翔
        if (isLoadingMore) {
            return;// 已经记载更多 就不去
        }
        if (scrollState == SCROLL_STATE_IDLE && getLastVisiblePosition()>getCount()-1) {
            isLoadingMore = true;
            mFooterView.setPadding(0,0,0,0);
            setSelection(getCount());//跳到最后一条，显示加载更多
            if (mListener != null) {
                mListener.onLoadmore();
            }
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        //滑动过程
    }

    public interface OnRefreshListener{
        void onREfresh();
        void onLoadmore();
    }
    public void setRefreshListenter(OnRefreshListener mListener){
        this.mListener = mListener;
    }
}
