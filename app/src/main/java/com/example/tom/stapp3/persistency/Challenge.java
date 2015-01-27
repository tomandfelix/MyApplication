package com.example.tom.stapp3.persistency;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Flixse on 27/01/2015.
 */
public class Challenge extends Quest {
    private int betAmount;
    private int peopleAmount;
    public Challenge(int id, String name, String description, int peopleAmount){
        super(id, name, description);
        this.peopleAmount = peopleAmount;
    }

    public int getBetAmount() {
        return betAmount;
    }

    public void setBetAmount(int betAmount) {
        this.betAmount = betAmount;
    }

    public int getPeopleAmount() {
        return peopleAmount;
    }

    public int validate(boolean won, Challenge challenge){
        if(won) {
            return challenge.getBetAmount();
        }else{
            return 0;
        }
    }
    @Override
    public String toString() {
        String info = "";
        info += "id:" + id;
        info += (name == null ? "":" name:" + name);
        info += (description == null ? "":" description:" + description);
        info += " bet amount:" + betAmount;
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
        dest.writeInt(betAmount);
        dest.writeInt(peopleAmount);
    }

    protected Challenge(Parcel in) {
        super(in);
        id = in.readInt();
        name = in.readString();
        description = in.readString();
        betAmount = in.readInt();
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
