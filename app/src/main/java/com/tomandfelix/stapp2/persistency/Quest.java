package com.tomandfelix.stapp2.persistency;

/**
 * Created by Flixse on 27/01/2015.
 */
public abstract class Quest {
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
}
