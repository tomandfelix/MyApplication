package com.tomandfelix.stapp2.persistency;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Flixse on 27/01/2015.
 */
public class Quest implements Parcelable {
    protected int id;
    protected String name;
    protected String description;

    protected Quest(int id) {
        this.id = id;
    }

    public Quest(int id, String name, String description){
        this.id = id;
        this.description = description;
        this.name = name;
    }
    public int getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public String getDescription() {
        return description;
    }



    @Override
    public String toString() {
        String info = "";
        info += "id:" + id;
        info += (name == null ? "":" name:" + name);
        info += (description == null ? "":" description:" + description);
        return info;
    }
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeString(description);
    }

    protected Quest(Parcel in) {
        id = in.readInt();
        name = in.readString();
        description = in.readString();

    }

    public static final Parcelable.Creator<Quest> CREATOR = new Parcelable.Creator<Quest>() {
        public Quest createFromParcel(Parcel in) {
            return new Quest(in);
        }

        public Quest[] newArray(int size) {
            return new Quest[size];
        }
    };
}
