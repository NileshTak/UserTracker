package com.usertracker.database;

public class PLang {

    public PLang() {
        super();
    }

    public PLang(String poi_id, String langarrs) {
        // TODO Auto-generated constructor stub

        this.poi_id = poi_id;
        this.langarr = langarrs;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPoi_id() {
        return poi_id;
    }

    public void setPoi_id(String poi_id) {
        this.poi_id = poi_id;
    }

    public String getLangarr() {
        return langarr;
    }

    public void setLangarr(String langarr) {
        this.langarr = langarr;
    }

    private int id;
    private String poi_id;
    private String langarr;

}
