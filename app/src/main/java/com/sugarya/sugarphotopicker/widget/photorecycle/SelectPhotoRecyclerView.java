package com.sugarya.sugarphotopicker.widget.photorecycle;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.lidong.photopicker.PhotoPickerActivity;
import com.lidong.photopicker.SelectModel;
import com.lidong.photopicker.intent.PhotoPickerIntent;
import com.lidong.photopicker.intent.PhotoPreviewIntent;
import com.sugarya.sugarphotopicker.BuildConfig;
import com.sugarya.sugarphotopicker.R;
import com.sugarya.sugarphotopicker.widget.utils.ImageLoader;
import com.tbruyelle.rxpermissions.RxPermissions;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.engine.impl.PicassoEngine;
import com.zhihu.matisse.internal.entity.CaptureStrategy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.functions.Action1;

import static com.sugarya.sugarphotopicker.widget.photorecycle.SelectPhotoRecyclerView.ShowTypeEnum.ONLY_DISPLAY;
import static com.sugarya.sugarphotopicker.widget.photorecycle.SelectPhotoRecyclerView.ShowTypeEnum.UP_LOADING;


/**
 * Created by Ethan on 2017/10/4.
 * 照片选择器 实现接口 OnPhotoOperatorListener实现上传和删除图片
 * Android 5.0以上，使用知乎相册 Matisse，4.4及以下使用自带的PhotoPicker
 */
public class SelectPhotoRecyclerView extends RecyclerView {

    private static final String TAG = "SelectPhotoRecyclerView";


    private static final int DEFAULT_SPAN_COUNT = 3;
    private static final int DEFAULT_PHOTO_LIMIT_COUNT = 5;
    private static final SparseArray<ShowTypeEnum> SHOW_TYPE_SPARSE = new SparseArray<>();

    static {
        SHOW_TYPE_SPARSE.put(0, ONLY_DISPLAY);
        SHOW_TYPE_SPARSE.put(1, UP_LOADING);
    }

    private int mRequestCode;
    private SelectPhotoAdapter mSelectPhotoAdapter;
    private ShowTypeEnum mShowTypeEnum = UP_LOADING;
    private int mSpanCount = DEFAULT_SPAN_COUNT;
    private int mPhotoLimitCount = DEFAULT_PHOTO_LIMIT_COUNT;
    private Fragment mCurrentFragment;
    private Activity mCurrentActivity;

    private OnItemClickListener mOnItemClickListener;
    private OnAddPhotoClickListener mOnAddPhotoClickListener;
    private OnRemoveItemClickListener mOnRemoveItemClickListener;
    private OnOperatorPhotoListener mOnOperatorPhotoListener;
    public static final String AUTHORITY = BuildConfig.APPLICATION_ID;

    public SelectPhotoRecyclerView(Context context) {
        super(context);
        init();
    }

    public SelectPhotoRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SelectPhotoRecyclerView);
        mRequestCode = typedArray.getInt(R.styleable.SelectPhotoRecyclerView_requestCode, 0);
        mSpanCount = typedArray.getInt(R.styleable.SelectPhotoRecyclerView_spanCount, DEFAULT_SPAN_COUNT);
        mPhotoLimitCount = typedArray.getInt(R.styleable.SelectPhotoRecyclerView_limitCount, DEFAULT_PHOTO_LIMIT_COUNT);
        mShowTypeEnum = SHOW_TYPE_SPARSE.get(typedArray.getInt(R.styleable.SelectPhotoRecyclerView_showType, 1));
        typedArray.recycle();
        init();
    }

    private void init() {
        if (mRequestCode <= 0) {
            mRequestCode = (int) (System.currentTimeMillis() % 10000);
        }
        Log.d(TAG, "init: mRequestCode = " + mRequestCode);
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), mSpanCount, LinearLayoutManager.VERTICAL, false);
        mSelectPhotoAdapter = new SelectPhotoAdapter();

        setLayoutManager(layoutManager);
        setAdapter(mSelectPhotoAdapter);
    }

    class SelectPhotoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int TYPE_FOOTER = 11;

        private List<PhotoEntity> mDataList;

        private SelectPhotoAdapter() {
            mDataList = new ArrayList<>();
        }

        @Override
        public int getItemViewType(int position) {
            switch (mShowTypeEnum) {
                case UP_LOADING:
                    int footerCount = getFooterCount();
                    int itemCount = getItemCount();
                    if (footerCount > 0 && position == itemCount - 1) {
                        return TYPE_FOOTER;
                    } else {
                        return super.getItemViewType(position);
                    }
                default:
                    return super.getItemViewType(position);
            }
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Log.d(TAG, "onCreateViewHolder: viewType = " + viewType);
            Context context = parent.getContext();
            if (viewType == TYPE_FOOTER) {
                View footerView = LayoutInflater.from(context).inflate(R.layout.item_footer_add, parent, false);
                return new FooterViewHolder(footerView);
            } else {
                View bodyView = LayoutInflater.from(context).inflate(R.layout.item_submit_photo, parent, false);
                return new PhotoViewHolder(bodyView);
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof PhotoViewHolder) {
                PhotoViewHolder photoViewHolder = (PhotoViewHolder) holder;
                if (position >= 0 && position < mDataList.size()) {
                    PhotoEntity photoEntity = mDataList.get(position);
                    photoViewHolder.onBindViewHolder(photoEntity);
                }
            }
        }

        @Override
        public int getItemCount() {
            return getBodyCount() + getFooterCount();
        }

        private int getBodyCount() {
            return mDataList.size();
        }

        private int getFooterCount() {
            int result;
            switch (mShowTypeEnum) {
                case ONLY_DISPLAY:
                    result = 0;
                    break;
                case UP_LOADING:
                    if (getBodyCount() >= mPhotoLimitCount) {
                        result = 0;
                    } else {
                        result = 1;
                    }
                    break;
                default:
                    result = 0;
            }
            return result;
        }

        void notifyAllData(List<PhotoEntity> photoEntityList) {
            if (photoEntityList == null || photoEntityList.isEmpty()) {
                mDataList.clear();
                notifyDataSetChanged();
                return;
            }
            if (!mDataList.isEmpty()) {
                mDataList.clear();
                notifyDataSetChanged();
            }

            mDataList.addAll(photoEntityList);
            int itemCount = photoEntityList.size();
            notifyItemRangeChanged(0, itemCount);
        }

        void addNotifyData(List<PhotoEntity> photoEntityList) {
            mDataList.addAll(photoEntityList);
            notifyDataSetChanged();
        }

        void addNotifyData(PhotoEntity photoEntity) {
            mDataList.add(photoEntity);
            notifyDataSetChanged();
        }

        void removeNotifyData(PhotoEntity removePhotoEntity) {
            if (mDataList.contains(removePhotoEntity)) {
                int position = mDataList.indexOf(removePhotoEntity);
                mDataList.remove(position);
                notifyItemRemoved(position);
            }
        }

        List<PhotoEntity> getDataList() {
            return mDataList;
        }

        class PhotoViewHolder extends RecyclerView.ViewHolder {

            @BindView(R.id.img_item_submit_photo)
            ImageView mImgSubmit;

            @BindView(R.id.img_item_submit_cancel)
            ImageView mImgCancel;


            PhotoViewHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
            }

            void onBindViewHolder(PhotoEntity photoEntity) {
                if (photoEntity == null) {
                    return;
                }
                Uri photoUri = photoEntity.getPhotoUri();
                String resUrl = photoEntity.getSmallResUrl();
                if (!TextUtils.isEmpty(resUrl)) {
                    ImageLoader.display(getContext(), resUrl, mImgSubmit);
                } else {
                    ImageLoader.display(getContext(), photoUri, mImgSubmit, 85);
                }

                setupShowState(mShowTypeEnum);
            }

            @OnClick(R.id.img_item_submit_cancel)
            void onRemovePhotoClick() {
                int position = getAdapterPosition();
                if (position >= 0 && position < mDataList.size()) {
                    PhotoEntity removePhotoEntity = mDataList.get(position);
                    if (mOnRemoveItemClickListener != null) {
                        mOnRemoveItemClickListener.onRemoveClick(removePhotoEntity);
                    } else if (mOnOperatorPhotoListener != null) {
                        removePhotoData(removePhotoEntity);
                        mOnOperatorPhotoListener.deletePhoto(removePhotoEntity);
                    }
                }
            }

            @OnClick(R.id.img_item_submit_photo)
            void onItemClick() {
                int position = getAdapterPosition();
                if (position >= 0 && position < mDataList.size()) {
                    PhotoEntity entity = mDataList.get(position);
                    String largeResUrl = entity.getLargeResUrl();
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onItemClick(largeResUrl);
                    } else {
                        if (mCurrentFragment != null) {
                            openPreview(mCurrentFragment, largeResUrl);
                        } else if (mCurrentActivity != null) {
                            openPreview(mCurrentActivity, largeResUrl);
                        }
                    }
                }
            }

            void setupShowState(ShowTypeEnum typeEnum) {
                switch (typeEnum) {
                    case ONLY_DISPLAY:
                        mImgCancel.setVisibility(INVISIBLE);
                        break;
                    default:
                        mImgCancel.setVisibility(VISIBLE);
                }
            }

        }

        class FooterViewHolder extends RecyclerView.ViewHolder {

            @BindView(R.id.container_footer_add)
            RelativeLayout mContainerFooterAdd;

            FooterViewHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
            }

            @OnClick(R.id.container_footer_add)
            void onItemClick() {
                rx.Observable<Boolean> observable;
                final Context context;
                if (mCurrentFragment != null) {
                    observable = new RxPermissions(mCurrentFragment.getActivity())
                            .request(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA);
                    context = mCurrentFragment.getContext();
                } else {
                    context = mCurrentActivity;
                    observable = new RxPermissions(mCurrentActivity)
                            .request(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA);
                }

                observable.subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean granted) {
                        if (granted) {
                            openGalleryWrapper();
                        } else {
                            Toast.makeText(context, "相册开启需要获取权限", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            private void openGalleryWrapper() {
                if (mOnAddPhotoClickListener != null) {
                    mOnAddPhotoClickListener.onAddClick();
                    return;
                }

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                    if (mCurrentFragment != null) {
                        openGalleryByMatisse(mCurrentFragment);
                    } else if (mCurrentActivity != null) {
                        openGalleryByMatisse(mCurrentActivity);
                    }
                    return;
                }

                if (mCurrentFragment != null) {
                    openAlbums(mCurrentFragment);
                } else if (mCurrentActivity != null) {
                    openAlbums(mCurrentActivity);
                }
            }
        }
    }

    private int dip2px(float dipValue) {
        float scale = getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    //**********************************************对外提供的方法

    public SelectPhotoAdapter getSelectPhotoAdapter() {
        return mSelectPhotoAdapter;
    }

    public void addNewPhotoData(List<PhotoEntity> photoEntityList) {
        if (mSelectPhotoAdapter != null) {
            mSelectPhotoAdapter.addNotifyData(photoEntityList);
        }
    }

    public void addNewPhotoData(PhotoEntity photoEntity) {
        if (mSelectPhotoAdapter != null) {
            mSelectPhotoAdapter.addNotifyData(photoEntity);
        }
    }

    public void removePhotoData(PhotoEntity photoEntity) {
        if (mSelectPhotoAdapter != null) {
            mSelectPhotoAdapter.removeNotifyData(photoEntity);
        }
    }

    public void notifyPhotoData(List<PhotoEntity> photoEntityList) {
        if (mSelectPhotoAdapter != null) {
            mSelectPhotoAdapter.notifyAllData(photoEntityList);
        }
    }

    /**
     * 通过打开相册
     */
    public void openAlbums(Fragment fragment) {
        if (fragment == null) {
            return;
        }
        int count = mPhotoLimitCount - mSelectPhotoAdapter.getBodyCount();
        Log.d(TAG, "openAlbums: count = " + count);
        Context context = fragment.getContext();
        PhotoPickerIntent pickIntent = new PhotoPickerIntent(context);
        pickIntent.setSelectModel(SelectModel.MULTI);
        pickIntent.setShowCarema(true); // 是否显示拍照
        pickIntent.setMaxTotal(count); // 最多选择照片数量
        fragment.startActivityForResult(pickIntent, mRequestCode);
    }

    /**
     * 通过打开相册
     */
    public void openAlbums(Fragment fragment, int currentSelectPhotoMaxCount) {
        if (fragment == null) {
            return;
        }
        Context context = fragment.getContext();
        PhotoPickerIntent pickIntent = new PhotoPickerIntent(context);
        pickIntent.setSelectModel(SelectModel.MULTI);
        pickIntent.setShowCarema(true); // 是否显示拍照
        pickIntent.setMaxTotal(currentSelectPhotoMaxCount); // 最多选择照片数量
        fragment.startActivityForResult(pickIntent, mRequestCode);
    }

    /**
     * 通过打开相册
     */
    public void openAlbums(Activity activity) {
        if (activity == null) {
            return;
        }
        int count = mPhotoLimitCount - mSelectPhotoAdapter.getBodyCount();
        Log.d(TAG, "openAlbums: count = " + count);
        PhotoPickerIntent pickIntent = new PhotoPickerIntent(activity);
        pickIntent.setSelectModel(SelectModel.MULTI);
        pickIntent.setShowCarema(true); // 是否显示拍照
        pickIntent.setMaxTotal(count); // 最多选择照片数量
        activity.startActivityForResult(pickIntent, mRequestCode);
    }

    /**
     * 照片预览
     *
     * @param activity
     */
    public void openPreview(Activity activity, String pathStr) {
        if (activity == null || TextUtils.isEmpty(pathStr)) {
            return;
        }
        ArrayList<String> pathList = new ArrayList<>();
        pathList.add(pathStr);

        PhotoPreviewIntent previewIntent = new PhotoPreviewIntent(activity);
        previewIntent.setCurrentItem(0);
        previewIntent.setPhotoPaths(pathList);
        previewIntent.setIsCanDelete(false);
        activity.startActivityForResult(previewIntent, mRequestCode * 2);
    }

    /**
     * 照片预览
     *
     * @param fragment
     */
    public void openPreview(Fragment fragment, String pathStr) {
        if (fragment == null || TextUtils.isEmpty(pathStr)) {
            return;
        }

        ArrayList<String> pathList = new ArrayList<>();
        pathList.add(pathStr);

        Context context = fragment.getContext();
        PhotoPreviewIntent previewIntent = new PhotoPreviewIntent(context);
        previewIntent.setCurrentItem(0);
        previewIntent.setPhotoPaths(pathList);
        previewIntent.setIsCanDelete(false);
        fragment.startActivityForResult(previewIntent, mRequestCode * 2);
    }

    /**
     * 通过知乎Matisse类库 打开相册
     *
     * @param fragment
     */
    public void openGalleryByMatisse(Fragment fragment) {
        if (fragment == null) {
            return;
        }
        int count = mPhotoLimitCount - mSelectPhotoAdapter.getBodyCount();
        Matisse.from(fragment)
                .choose(MimeType.allOf())
                .theme(R.style.Matisse_Zhihu)
                .capture(true)
                .captureStrategy(new CaptureStrategy(true, AUTHORITY))
                .countable(true)
                .maxSelectable(count)
                .gridExpectedSize(dip2px(120))
                .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                .thumbnailScale(0.85f)
                .imageEngine(new PicassoEngine())
                .forResult(mRequestCode);
    }

    /**
     * 打开相册(不带拍照)
     *
     * @param fragment
     */
    public void openGalleryWithoutCapture(Fragment fragment) {
        if (fragment == null) {
            return;
        }
        int count = mPhotoLimitCount - mSelectPhotoAdapter.getBodyCount();
        Matisse.from(fragment)
                .choose(MimeType.allOf())
                .theme(R.style.Matisse_Zhihu)
                .capture(false)
                .captureStrategy(new CaptureStrategy(true, AUTHORITY))
                .countable(true)
                .maxSelectable(count)
                .gridExpectedSize(dip2px(120))
                .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                .thumbnailScale(0.85f)
                .imageEngine(new PicassoEngine())
                .forResult(mRequestCode);
    }

    /**
     * 打开相册
     *
     * @param activity
     */
    public void openGalleryByMatisse(Activity activity) {
        if (activity == null) {
            return;
        }
        int count = mPhotoLimitCount - mSelectPhotoAdapter.getBodyCount();
        Matisse.from(activity)
                .choose(MimeType.allOf())
                .theme(R.style.Matisse_Zhihu)
                .capture(true)
                .captureStrategy(new CaptureStrategy(true, AUTHORITY))
                .countable(true)
                .maxSelectable(count)
                .gridExpectedSize(dip2px(120))
                .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                .thumbnailScale(0.85f)
                .imageEngine(new PicassoEngine())
                .forResult(mRequestCode);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            // 选择照片
            if (requestCode == mRequestCode) {
//                ArrayList<String> filePathList = data.getStringArrayListExtra(PhotoPickerActivity.EXTRA_RESULT);
//                List<Uri> uriList = Matisse.obtainResult(data);
//                List<PhotoEntity> photoEntityList = new ArrayList<>();
//                for(Uri uri : uriList){
//                    photoEntityList.add(new PhotoEntity(uri));
//                }
//                addNewPhotoData(photoEntityList);
//
//                if (mOnOperatorPhotoListener != null) {
//                    mOnOperatorPhotoListener.uploadPhoto(uriList);
//                }

                //如果Matisse获取不到数据，意味着使用photoPicker打开的
                List<Uri> uriList = Matisse.obtainResult(data);
                if (uriList == null || uriList.isEmpty()) {
                    ArrayList<String> filePathList = data.getStringArrayListExtra(PhotoPickerActivity.EXTRA_RESULT);
                    uriList = new ArrayList<>();
                    if (filePathList != null && !filePathList.isEmpty()) {
                        for (String path : filePathList) {
                            File imageFile = new File(path);
                            uriList.add(Uri.fromFile(imageFile));
                        }
                    }
                }

                List<PhotoEntity> photoEntityList = new ArrayList<>();
                for (Uri uri : uriList) {
                    photoEntityList.add(new PhotoEntity(uri));
                }
                addNewPhotoData(photoEntityList);

                if (mOnOperatorPhotoListener != null) {
                    mOnOperatorPhotoListener.uploadPhoto(uriList);
                }
            }
        }
    }

    /**
     * 上传图片
     *
     * @param fragment
     * @param imageList
     */
    protected void uploadImage(final Fragment fragment, List<String> imageList) {

    }


    /**
     * 删除图片
     *
     * @param fragment
     * @param removePhotoEntity
     */
    protected void deleteServiceOrderDetailImage(Fragment fragment, final PhotoEntity removePhotoEntity) {

    }


    public int getPhotoLimitCount() {
        return mPhotoLimitCount;
    }

    public ShowTypeEnum getShowTypeEnum() {
        return mShowTypeEnum;
    }

    /**
     * 设置展示类型
     *
     * @param showTypeEnum
     */
    public void setShowTypeEnum(ShowTypeEnum showTypeEnum) {
        mShowTypeEnum = showTypeEnum;
    }

    public void setCurrentFragment(Fragment currentFragment) {
        mCurrentFragment = currentFragment;
    }

    public void setCurrentActivity(Activity currentActivity) {
        mCurrentActivity = currentActivity;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    public void setOnAddPhotoClickListener(OnAddPhotoClickListener onAddPhotoClickListener) {
        mOnAddPhotoClickListener = onAddPhotoClickListener;
    }

    public void setOnRemoveItemClickListener(OnRemoveItemClickListener onRemoveItemClickListener) {
        mOnRemoveItemClickListener = onRemoveItemClickListener;
    }

    public void setOnOperatorPhotoListener(OnOperatorPhotoListener onOperatorPhotoListener) {
        mOnOperatorPhotoListener = onOperatorPhotoListener;
    }

    public enum ShowTypeEnum {
        /**
         * 可进行上传图片操作
         */
        UP_LOADING,

        /**
         * 只展示图片
         */
        ONLY_DISPLAY
    }

    public interface OnItemClickListener {
        void onItemClick(String largeResUrl);
    }

    public interface OnRemoveItemClickListener {
        void onRemoveClick(PhotoEntity removePhotoEntity);
    }

    public interface OnAddPhotoClickListener {
        void onAddClick();
    }

    public interface OnOperatorPhotoListener {

        /**
         * 上传加载图片
         *
         * @param imageList
         */
        void uploadPhoto(List<Uri> imageList);

        /**
         * 删除图片
         *
         * @param removePhotoEntity
         */
        void deletePhoto(PhotoEntity removePhotoEntity);

    }


}
