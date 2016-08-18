package com.example.administrator.mymusicapplication;

public class Music {
    String title,artist,path,image;
    int length;

    public void setLength(int length){
        this.length = length;
    }

    public int getLength(){
        return length;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getImage() {
        return image;
    }

    public void setId(String image) {
        this.image = image;
    }
}
