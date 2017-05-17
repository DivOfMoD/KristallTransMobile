package by.kristalltrans.kristalltransmobile;

public class Document {

    private String date;
    private String name;
    private String photoUrl;

    public Document() {
    }

    public Document(String name, String date, String photoUrl) {
        this.date = date;
        this.name = name;
        this.photoUrl = photoUrl;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}
