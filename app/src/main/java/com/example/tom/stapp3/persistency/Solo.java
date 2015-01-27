package com.example.tom.stapp3.persistency;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Flixse on 27/01/2015.
 */
public class Solo extends Quest {
    private int money;
    private int xp;
    private int duration;
    private int difficulty;
    public static final int EASY = 0;
    public static final int MEDIUM = 1;
    public static final int HARD = 2;
    private Runnable validator;

    public Solo( int id, String name, String description, int money, int xp, int duration, int difficulty, Runnable validator){
        super(id, name, description);
        this.money = money;
        this.xp = xp;
        this.duration = duration;
        this.difficulty = difficulty;
        this.validator = validator;
    }

    public int getMoney() {
        return money;
    }

    public int getxp() {
        return xp;
    }

    public int getDuration() {
        return duration;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public Runnable getValidator() {
        return validator;
    }

    @Override
    public String toString() {
        String info = "";
        info += "id:" + id;
        info += (name == null ? "":" name:" + name);
        info += (description == null ? "":" description:" + description);
        info += " money:" + money;
        info += " experience:" + xp;
        info += " duration:" + duration;
        info += " difficulty:";
        switch(difficulty) {
            case EASY:
                info += "EASY";
                break;
            case MEDIUM:
                info += "MEDIUM";
                break;
            case HARD:
                info += "HARD";
                break;
        }
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
        dest.writeInt(xp);
        dest.writeInt(difficulty);
    }

    private Solo(Parcel in) {
        super(in);
        id = in.readInt();
        name = in.readString();
        description = in.readString();
        money = in.readInt();
        xp = in.readInt();
        difficulty = in.readInt();
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
