package by.kristalltrans.kristalltransmobile;

public class ChatMessage {

    private String text;
    private String name;
    private String email;
    private String photoUrl;

    public ChatMessage() {
    }

    public ChatMessage(String name, String email, String text, String photoUrl) {
        this.text = text;
        this.name = name;
        this.photoUrl = photoUrl;
        this.email = email;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}
