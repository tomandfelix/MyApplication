package com.example.tom.myapplication;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Tom on 27/10/2014.
 * extends profile to allow for a profile with a rank
 */
public class RankedProfile extends Profile {
    protected int rank;

    public RankedProfile(int id, String firstName, String lastName, String username, String email, int money, int experience, int rank) {
        super(id, firstName, lastName, username, email, money, experience);
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    @Override
    public String toString() {
        return "rank:" + rank + " " + super.toString();

    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(rank);
    }

    private RankedProfile(Parcel in) {
        super(in);
        rank = in.readInt();
    }

    public static final Parcelable.Creator<RankedProfile> CREATOR = new Parcelable.Creator<RankedProfile>() {
        public RankedProfile createFromParcel(Parcel in) {
            return new RankedProfile(in);
        }

        public RankedProfile[] newArray(int size) {
            return new RankedProfile[size];
        }
    };
}
