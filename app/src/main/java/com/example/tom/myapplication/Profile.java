package com.example.tom.myapplication;

/**
 * Created by Tom on 6/10/2014.
 * Profile class corresponds to profiles in the database
 */
public class Profile {
    private int id;
    private String lastName;
    private String firstName;
    private String username;
    private String email;

    public Profile(int id, String lastName, String firstName, String username, String email) {
        this.id = id;
        this.lastName = lastName;
        this.firstName = firstName;
        this.username = username;
        this.email = email;
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
}
