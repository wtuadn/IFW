package com.wtuadn.ifw

import android.content.pm.ActivityInfo
import android.content.pm.ServiceInfo
import android.graphics.drawable.Drawable
import android.os.Parcel
import android.os.Parcelable

data class AppInfo(var name: String,
                   var pkgName: String,
                   var icon: Drawable? = null,
                   var receivers: Array<ActivityInfo>? = null,
                   var services: Array<ServiceInfo>? = null,
                   var disabledReceivers: ArrayList<String> = arrayListOf(),
                   var disabledServices: ArrayList<String> = arrayListOf()) : Parcelable {

    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            null,
            parcel.createTypedArray(ActivityInfo.CREATOR),
            parcel.createTypedArray(ServiceInfo.CREATOR),
            parcel.createStringArrayList(),
            parcel.createStringArrayList())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(pkgName)
        parcel.writeTypedArray(receivers, flags)
        parcel.writeTypedArray(services, flags)
        parcel.writeStringList(disabledReceivers)
        parcel.writeStringList(disabledServices)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        return other is AppInfo && other.pkgName == pkgName
    }

    companion object CREATOR : Parcelable.Creator<AppInfo> {
        override fun createFromParcel(parcel: Parcel): AppInfo {
            return AppInfo(parcel)
        }

        override fun newArray(size: Int): Array<AppInfo?> {
            return arrayOfNulls(size)
        }
    }
}