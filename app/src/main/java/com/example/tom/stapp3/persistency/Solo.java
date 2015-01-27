package com.example.tom.stapp3.persistency;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Flixse on 27/01/2015.
 */
public class Solo extends Quest {
    private int money;
    private int xP;
    private int duration;
    private String difficulty;
    public Solo( int id, String name, String description, int money, int xP, int duration, String difficulty){
        super(id, name, description);
        this.money = money;
        this.xP = xP;
        this.duration = duration;
        this.difficulty = difficulty;
    }

    public int getMoney() {
        return money;
    }

    public int getxP() {
        return xP;
    }

    public int getDuration() {
        return duration;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void validate(){

    }

    @Override
    public String toString() {
        String info = "";
        info += "id:" + id;
        info += (name == null ? "":" name:" + name);
        info += (description == null ? "":" description:" + description);
        info += " money:" + money;
        info += " experience:" + xP;
        info += " duration:" + duration;
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
        dest.writeInt(money);
        dest.writeInt(xP);
        dest.writeString(description);
    }

    private Solo(Parcel in) {
        super(in);
        id = in.readInt();
        name = in.readString();
        description = in.readString();
        money = in.readInt();
        xP = in.readInt();



    }

    public static final Parcelable.Creator<Solo> CREATOR = new Parcelable.Creator<Solo>() {
        public Solo createFromParcel(Parcel in) {
            return new Solo(in);
        }

        public Solo[] newArray(int size) {
            return new Solo[size];
        }
    };
}
