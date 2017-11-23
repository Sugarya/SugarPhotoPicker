package com.sugarya.sugarphotopicker.widget.photorecycle;

import android.net.Uri;

/**
 * Created by Ethan on 2017/10/2.
 * 公共组件服务单详情 照片实体类
 */

public class PhotoEntity {

    private Uri photoUri;
    private String smallResUrl;
    private String largeResUrl;

    public PhotoEntity(Uri photoUri) {
        this.photoUri = photoUri;
    }

    public PhotoEntity(String smallResUrl) {
        this.smallResUrl = smallResUrl;
    }

    public PhotoEntity(String smallResUrl, String largeResUrl) {
        this.smallResUrl = smallResUrl;
        this.largeResUrl = largeResUrl;
    }

    public PhotoEntity(Uri photoUri, String smallResUrl) {
        this.photoUri = photoUri;
        this.smallResUrl = smallResUrl;
    }

    public String getLargeResUrl() {
        return largeResUrl;
    }

    public void setLargeResUrl(String largeResUrl) {
        this.largeResUrl = largeResUrl;
    }

    public String getSmallResUrl() {
        return smallResUrl;
    }

    public void setSmallResUrl(String smallResUrl) {
        this.smallResUrl = smallResUrl;
    }

    public Uri getPhotoUri() {
        return photoUri;
    }

    public void setPhotoUri(Uri photoUri) {
        this.photoUri = photoUri;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PhotoEntity)) return false;

        PhotoEntity that = (PhotoEntity) o;

        if (getPhotoUri() != null ? !getPhotoUri().equals(that.getPhotoUri()) : that.getPhotoUri() != null)
            return false;
        return getSmallResUrl() != null ? getSmallResUrl().equals(that.getSmallResUrl()) : that.getSmallResUrl() == null;

    }

    @Override
    public int hashCode() {
        int result = getPhotoUri() != null ? getPhotoUri().hashCode() : 0;
        result = 31 * result + (getSmallResUrl() != null ? getSmallResUrl().hashCode() : 0);
        return result;
    }
}
