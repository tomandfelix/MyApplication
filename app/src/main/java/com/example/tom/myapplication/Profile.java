package com.example.tom.myapplication;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Tom on 6/10/2014.
 * Profile class corresponds to profiles in the database
 */
public class Profile implements Parcelable{
    protected int id;
    protected String lastName;
    protected String firstName;
    protected String username;
    protected String email;
    protected int money;
    protected int experience;

    public Profile(int id, String firstName, String lastName, String username, String email, int money, int experience) {
        this.id = id;
        this.lastName = lastName;
        this.firstName = firstName;
        this.username = username;
        this.email = email;
        this.money = money;
        this.experience = experience;
    }

    public int getId() {

        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getMoney() {
        return money;
    }

    public void setMoney(int money) {
        this.money = money;
    }

    public int getExperience() {
        return experience;
    }

    public void setExperience(int experience) {
        this.experience = experience;
    }

    @Override
    public String toString() {
        String info = "";
        info+= "id:" + id;
        info+= (firstName == null ? "":" firstname:" + firstName);
        info+= (lastName == null ? "":" lastname:" + lastName);
        info+= (username == null ? "":" username:" + username);
        info+= (email == null ? "":" email:" + email);
        info+= " money:" + money;
        info+= " experience:" + experience;
        return info;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(lastName);
        dest.writeString(firstName);
        dest.writeString(username);
        dest.writeString(email);
        dest.writeInt(money);
        dest.writeInt(experience);
    }

    protected Profile(Parcel in) {
        id = in.readInt();
        lastName = in.readString();
        firstName = in.readString();
        username = in.readString();
        email = in.readString();
        money = in.readInt();
        experience = in.readInt();
    }

    public static final Parcelable.Creator<Profile> CREATOR = new Parcelable.Creator<Profile>() {
        public Profile createFromParcel(Parcel in) {
            return new Profile(in);
        }

        public Profile[] newArray(int size) {
            return new Profile[size];
        }
    };
}
