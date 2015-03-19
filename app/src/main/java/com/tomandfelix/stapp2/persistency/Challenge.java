package com.tomandfelix.stapp2.persistency;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Flixse on 27/01/2015.
 */
public class Challenge extends Quest {
    private int peopleAmount;
    private int duration;
    private Runnable validator;
    public Challenge(int id, String name, String description, int peopleAmount, int duration, Runnable validator){
        super(id, name, description);
        this.peopleAmount = peopleAmount;
        this.duration = duration;
        this.validator = validator;
    }


    public int getPeopleAmount() {
        return peopleAmount;
    }

    public Runnable getValidator() {
        return validator;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    @Override
    public String toString() {
        String info = "";
        info += "id:" + id;
        info += (name == null ? "":" name:" + name);
        info += (description == null ? "":" description:" + description);
        info += " people:" + peopleAmount;
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
        dest.writeInt(peopleAmount);
    }

    protected Challenge(Parcel in) {
        super(in);
        id = in.readInt();
        name = in.readString();
        description = in.readString();
        peopleAmount = in.readInt();

    }

    public static final Parcelable.Creator<Challenge> CREATOR = new Parcelable.Creator<Challenge>() {
        public Challenge createFromParcel(Parcel in) {
            return new Challenge(in);
        }

        public Challenge[] newArray(int size) {
            return new Challenge[size];
        }
    };
}
