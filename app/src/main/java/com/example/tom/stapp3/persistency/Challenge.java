package com.example.tom.stapp3.persistency;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Flixse on 27/01/2015.
 */
public class Challenge extends Quest {
    private int betAmount;
    public Challenge(int id, String name, String description, int betAmount){
        super(id, name, description);
        this.betAmount = betAmount;
    }

    public int getBetAmount() {
        return betAmount;
    }

    public int validate(boolean won, Challenge challenge){
        return challenge.getBetAmount();
    }
    @Override
    public String toString() {
        String info = "";
        info += "id:" + id;
        info += (name == null ? "":" name:" + name);
        info += (description == null ? "":" description:" + description);
        info += "bet amount: " + betAmount;
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
        dest.writeInt(betAmount);
    }

    protected Challenge(Parcel in) {
        super(in);
        id = in.readInt();
        name = in.readString();
        description = in.readString();
        betAmount = in.readInt();

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
